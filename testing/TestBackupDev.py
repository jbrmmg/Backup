import requests
import sys

# Set the base URL
baseUrl = "http://localhost:10013/jbr/"

print('----------------------------------------------------------------------------------------------------------------')
print('Testing the development instance of the Backup service on URL (' + baseUrl + ')')
print('----------------------------------------------------------------------------------------------------------------')

# Check if the summary is valid - this is a dependency of the test.
response = requests.get(baseUrl + "int/backup/summary")
data = response.json()

print("Summary: " + str(data["valid"]))
if not data["valid"]:
    sys.exit('Summary is not valid, cannot run test.')

# Delete all the synchronize
sychronizes = requests.get(baseUrl + "ext/backup/synchronize").json()
for nextSynchronize in sychronizes:
    requests.delete(baseUrl + "ext/backup/synchronize", json={"id": nextSynchronize["id"]})

# Delete all the import files
requests.post(baseUrl + "int/backup/import", json={"path": "", "source": 0})

# Delete all the files
files = requests.get(baseUrl + "int/backup/files").json()
for nextFile in files:
    requests.delete(baseUrl + "int/backup/file", json={"id": nextFile["id"]})

# Delete all the sources
sources = requests.get(baseUrl + "ext/backup/source").json()
for nextSource in sources:
    requests.delete(baseUrl + "ext/backup/source", json={"id": nextSource["id"]})

print('----------------------------------------------------------------------------------------------------------------')
print('Successfully run backup test.')
print('----------------------------------------------------------------------------------------------------------------')
