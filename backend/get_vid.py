import motor.motor_asyncio
import asyncio
import json
import os
from bson import json_util
from dotenv import load_dotenv

load_dotenv()

async def main():
    mongo_url = os.getenv("MONGODB_URL", "mongodb://localhost:27017")
    db_name = os.getenv("MONGODB_DB_NAME", "saifit_db")
    client = motor.motor_asyncio.AsyncIOMotorClient(mongo_url)
    db = client[db_name]
    res = await db.results.find({}).to_list(1)
    print(json.dumps(res, default=json_util.default, indent=2))

if __name__ == '__main__':
    asyncio.run(main())
