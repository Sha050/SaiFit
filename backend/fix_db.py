import motor.motor_asyncio
import asyncio

async def main():
    try:
        from app.core.config import settings
        mongo_url = settings.MONGODB_URL
    except:
        mongo_url = "mongodb://localhost:27017"
        
    client = motor.motor_asyncio.AsyncIOMotorClient(mongo_url)
    db = client.saifit_db
    
    # We update the database
    res1 = await db.users.update_many({"gender": "MALE"}, {"$set": {"gender": "Male"}})
    res2 = await db.users.update_many({"role": "ATHLETE"}, {"$set": {"role": "athlete"}})
    print(f"Fixed {res1.modified_count} genders and {res2.modified_count} roles")

asyncio.run(main())
