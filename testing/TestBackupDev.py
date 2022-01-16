import requests
import sys
import os
from distutils.dir_util import copy_tree
import shutil

# Set the base URL
baseUrl = "http://localhost:10013/jbr/"

print('---------------------------------------------------------------------------------------------------------------')
print('Testing the development instance of the Backup service on URL (' + baseUrl + ')')
print('---------------------------------------------------------------------------------------------------------------')

# ----------------------------------------------------------------------------------------------------------------------
# Check if the summary is valid - this is a dependency of the test.
response = requests.get(baseUrl + "int/backup/summary")
data = response.json()

print("Summary: " + str(data["valid"]))
if not data["valid"]:
    sys.exit("Summary is not valid, cannot run test.")

# ----------------------------------------------------------------------------------------------------------------------
# Clean up the database.
# Delete all the synchronize
response = requests.get(baseUrl + "ext/backup/synchronize")

if response.status_code != 200:
    sys.exit("Failed to get details of synchronize items.")

sychronizes = response.json()
for nextSynchronize in sychronizes:
    response = requests.delete(baseUrl + "ext/backup/synchronize", json={"id": nextSynchronize["id"]})
    if response.status_code != 200:
        sys.exit("Failed to delete synchronize " + nextSynchronize["id"])

# Clear out the file data in the database
response = requests.delete(baseUrl + "int/backup/reset")
if response.text != "OK":
    sys.exit("Failed to reset data in the database" + response.text)

# Delete all the sources
sources = requests.get(baseUrl + "ext/backup/source").json()
for nextSource in sources:
    response = requests.delete(baseUrl + "ext/backup/source", json={"id": nextSource["id"]})
    if response.status_code != 200:
        sys.exit("Failed to delete source " + str(response.status_code) + " " + str(nextSource["id"]))

# ----------------------------------------------------------------------------------------------------------------------
# Setup the files in preparation for the test
if os.path.exists(os.path.join(os.getcwd(), 'working')):
    shutil.rmtree(os.path.join(os.getcwd(), 'working'))

os.mkdir(os.path.join(os.getcwd(), 'working'))
copy_tree(os.path.join(os.getcwd(), 'src', 'initial'), os.path.join(os.getcwd(), 'working'))

# -----------------------------------------------------------------------------------------------------------------------
# Setup the test, create sources
response = requests.post(baseUrl + "ext/backup/source", json={
    "id": 1,
    "location": {"id": 1},
    "path": os.path.join(os.getcwd(), 'working', 'source1'),
    "status": "OK",
    "type": "STD"})
if response.status_code != 200:
    sys.exit("Failed to create source id = 1.")

response = requests.post(baseUrl + "ext/backup/source", json={
    "id": 2,
    "location": {"id": 1},
    "path": os.path.join(os.getcwd(), 'working', 'source2'),
    "status": "OK",
    "type": "STD"})
if response.status_code != 200:
    sys.exit("Failed to create source id = 2.")

response = requests.post(baseUrl + "ext/backup/source", json={
    "id": 3,
    "location": {"id": 1},
    "path": os.path.join(os.getcwd(), 'working', 'source3'),
    "status": "OK",
    "type": "STD"})
if response.status_code != 200:
    sys.exit("Failed to create source id = 3.")

response = requests.post(baseUrl + "ext/backup/synchronize", json={
    "id": 1,
    "source": {"id": 1, "location": {"id": 1}},
    "destination": {"id": 2, "location": {"id": 1}}
})
if response.status_code != 200:
    sys.exit("Failed to create synchronize id = 1.")

response = requests.post(baseUrl + "ext/backup/synchronize", json={
    "id": 2,
    "source": {"id": 1, "location": {"id": 1}},
    "destination": {"id": 3, "location": {"id": 1}}
})
if response.status_code != 200:
    sys.exit("Failed to create synchronize id = 1.")

print("Gather")
response = requests.post(baseUrl + "int/backup/gather", data="temp")
if response.status_code != 200:
    sys.exit("Failed gather. " + str(response.status_code))

print("Sync")
response = requests.post(baseUrl + "int/backup/sync", data="temp")
if response.status_code != 200:
    sys.exit("Failed gather. " + str(response.status_code))

print("Gather")
response = requests.post(baseUrl + "int/backup/gather", data="temp")
if response.status_code != 200:
    sys.exit("Failed gather. " + str(response.status_code))

print(
    '----------------------------------------------------------------------------------------------------------------')
print('Successfully run backup test.')
print(
    '----------------------------------------------------------------------------------------------------------------')
