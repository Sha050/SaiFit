from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    PROJECT_NAME: str = "SAI Fit API"
    API_V1_STR: str = "/api/v1"

    # MongoDB settings — defaults allow server to start even without .env
    MONGODB_URL: str = "mongodb://localhost:27017"
    MONGODB_DB_NAME: str = "saifit_db"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore",
    )


settings = Settings()
