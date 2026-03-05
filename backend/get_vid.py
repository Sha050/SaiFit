import motor.motor_asyncio
import asyncio
import json
from bson import json_util

async def main():
    client = motor.motor_asyncio.AsyncIOMotorClient('mongodb+srv://shakthipranavuni_db_user:8V31B9HzTINqScO9@cluster0.xr6glkd.mongodb.net/?appName=Cluster0')
    db = client.saifit_db
    res = await db.results.find({}).to_list(1)
    print(json.dumps(res, default=json_util.default, indent=2))

if __name__ == '__main__':
    asyncio.run(main())
