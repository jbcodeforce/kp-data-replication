import os
import random, json


records = []
for i in range(0,1000):
    records.append({"id": i, "account_id": "acc_" + str(i % 15), "amount": random.gauss(50,3)})

     
with open('data.json','w') as f:
    json.dump(records,f)