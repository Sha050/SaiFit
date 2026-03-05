from motor.motor_asyncio import AsyncIOMotorClient
import logging
from app.core.config import settings

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# In-memory fallback that mimics the Motor collection API surface used by
# the rest of the codebase.  Activated only when the real MongoDB cluster
# is unreachable (e.g. IP not whitelisted, no internet, Atlas downtime).
# ---------------------------------------------------------------------------

class _InMemoryObjectId:
    """Mimics bson.ObjectId enough for our use-case."""
    _counter = 0

    def __init__(self):
        _InMemoryObjectId._counter += 1
        self._id = f"mem_{_InMemoryObjectId._counter:06d}"

    def __str__(self):
        return self._id


class _InMemoryCursor:
    """Supports async iteration and .to_list()."""

    def __init__(self, docs: list):
        self._docs = list(docs)

    async def to_list(self, length: int = 100):
        return self._docs[:length]

    def __aiter__(self):
        return _InMemoryCursorIter(self._docs)


class _InMemoryCursorIter:
    def __init__(self, docs):
        self._docs = docs
        self._index = 0

    def __aiter__(self):
        return self

    async def __anext__(self):
        if self._index >= len(self._docs):
            raise StopAsyncIteration
        doc = self._docs[self._index]
        self._index += 1
        return doc


class _InsertOneResult:
    def __init__(self, inserted_id):
        self.inserted_id = inserted_id


class _InMemoryCollection:
    """A minimal in-memory collection supporting the operations used in
    the API layer: insert_one, find_one, find, aggregate."""

    def __init__(self, name: str):
        self.name = name
        self._docs: list[dict] = []

    async def insert_one(self, doc: dict):
        oid = _InMemoryObjectId()
        doc = dict(doc)              # shallow copy so caller can't mutate
        doc["_id"] = str(oid)
        self._docs.append(doc)
        return _InsertOneResult(doc["_id"])

    async def find_one(self, query: dict | None = None):
        for doc in self._docs:
            if self._matches(doc, query or {}):
                return doc
        return None

    def find(self, query: dict | None = None):
        """Returns a cursor-like object."""
        matched = [d for d in self._docs if self._matches(d, query or {})]
        return _InMemoryCursor(matched)

    def aggregate(self, pipeline: list):
        """Very simplified aggregate — supports $match, $sort, $limit, $project."""
        docs = list(self._docs)
        for stage in pipeline:
            if "$match" in stage:
                filt = stage["$match"]
                docs = [d for d in docs if self._matches(d, filt)]
            elif "$sort" in stage:
                for key, direction in reversed(list(stage["$sort"].items())):
                    docs.sort(key=lambda d, k=key: d.get(k, 0), reverse=(direction == -1))
            elif "$limit" in stage:
                docs = docs[:stage["$limit"]]
            elif "$project" in stage:
                projected = []
                for d in docs:
                    new_doc = {}
                    for field, spec in stage["$project"].items():
                        if isinstance(spec, dict) and "$toString" in spec:
                            src = spec["$toString"]
                            new_doc[field] = str(d.get(src.lstrip("$"), ""))
                        elif spec == 1:
                            new_doc[field] = d.get(field)
                        else:
                            new_doc[field] = d.get(field)
                    projected.append(new_doc)
                docs = projected
        return _InMemoryCursor(docs)

    @staticmethod
    def _matches(doc: dict, query: dict) -> bool:
        for key, val in query.items():
            if doc.get(key) != val:
                return False
        return True


class _InMemoryDB:
    """Acts like motor's Database — returns collections via attribute access."""

    def __init__(self):
        self._collections: dict[str, _InMemoryCollection] = {}

    def __getattr__(self, name: str):
        if name.startswith("_"):
            return super().__getattribute__(name)
        if name not in self._collections:
            self._collections[name] = _InMemoryCollection(name)
        return self._collections[name]

    def __getitem__(self, name: str):
        return self.__getattr__(name)


# ---------------------------------------------------------------------------
# Global database singleton
# ---------------------------------------------------------------------------

class DataBase:
    client: AsyncIOMotorClient = None
    db = None
    using_fallback: bool = False


db = DataBase()


async def connect_to_mongo():
    """Try to connect to MongoDB Atlas.  If it fails, fall back to the
    in-memory store so the server always starts."""
    logger.info("Connecting to MongoDB...")
    try:
        import certifi
        client = AsyncIOMotorClient(
            settings.MONGODB_URL,
            serverSelectionTimeoutMS=5000,   # fail fast (5 s instead of 20 s)
            connectTimeoutMS=5000,
            tlsCAFile=certifi.where()
        )
        # Verify connection
        await client.admin.command("ping")
        db.client = client
        db.db = client[settings.MONGODB_DB_NAME]
        db.using_fallback = False
        logger.info("Successfully connected to MongoDB Atlas!")
    except Exception as e:
        logger.warning(f"MongoDB Atlas unreachable ({e}). Using in-memory fallback.")
        db.client = None
        db.db = _InMemoryDB()
        db.using_fallback = True


async def close_mongo_connection():
    if db.client:
        logger.info("Closing MongoDB connection...")
        db.client.close()
        logger.info("MongoDB connection closed.")
