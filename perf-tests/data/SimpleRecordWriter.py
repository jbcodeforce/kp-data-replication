import os
import random, json


records = []
for i in range(0,100):
    records.append({"id": i, "value": random.gauss(10,2)})

     
with open('records.json','w') as f:
    json.dump(records,f)