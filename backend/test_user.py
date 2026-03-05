import urllib.request
import urllib.error
import json

data = json.dumps({
    '_id': '', 
    'first_name': 'Test2', 
    'last_name': 'User2', 
    'email': 'test2@example.com', 
    'age': 25, 
    'gender': 'Male', 
    'region': 'India', 
    'role': 'athlete'
}).encode('utf-8')

req = urllib.request.Request(
    'http://localhost:8000/api/v1/users/', 
    data=data, 
    headers={'Content-Type': 'application/json'}
)

try: 
    print(urllib.request.urlopen(req).read().decode('utf-8'))
except urllib.error.HTTPError as e: 
    print(e.read().decode())
