from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import os

from app.db.mongodb import connect_to_mongo, close_mongo_connection, db
from app.core.config import settings
from app.api import users, submissions, leaderboard, upload, evaluation, achievements, benchmarks


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: connect to MongoDB (or fall back to in-memory)
    await connect_to_mongo()
    yield
    # Shutdown: close MongoDB connection
    await close_mongo_connection()


app = FastAPI(
    title=settings.PROJECT_NAME,
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    lifespan=lifespan,
)

# CORS — allow the Android app and any local dev tools to connect
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Ensure upload directory exists
UPLOAD_DIR = os.path.join(os.path.dirname(__file__), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)

# Serve uploaded videos
app.mount("/uploads", StaticFiles(directory=UPLOAD_DIR), name="uploads")


@app.get("/")
async def root():
    return {"message": "Welcome to SAI Fit API", "status": "running"}


@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "database": "mongodb_atlas" if not db.using_fallback else "in_memory_fallback",
    }


# Include routers
app.include_router(
    users.router,
    prefix=f"{settings.API_V1_STR}/users",
    tags=["users"],
)
app.include_router(
    submissions.router,
    prefix=f"{settings.API_V1_STR}/submissions",
    tags=["submissions"],
)
app.include_router(
    leaderboard.router,
    prefix=f"{settings.API_V1_STR}/leaderboard",
    tags=["leaderboard"],
)
app.include_router(
    upload.router,
    prefix=f"{settings.API_V1_STR}/upload",
    tags=["upload"],
)
app.include_router(
    evaluation.router,
    prefix=f"{settings.API_V1_STR}/evaluation",
    tags=["evaluation"],
)
app.include_router(
    achievements.router,
    prefix=f"{settings.API_V1_STR}/athletes",
    tags=["achievements"],
)
app.include_router(
    benchmarks.router,
    prefix=f"{settings.API_V1_STR}/benchmarks",
    tags=["benchmarks"],
)
