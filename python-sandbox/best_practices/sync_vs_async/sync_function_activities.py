import urllib.parse

import requests
from temporalio import activity


# @@@SNIPSTART python-sync-vs-async-sync-functions
@activity.defn
def greet_in_spanish(name: str) -> str:
    greeting = call_service("get-spanish-greeting", name)
    return greeting

# Utility method for making calls to the microservices
def call_service(stem: str, name: str) -> str:
    base = f"http://localhost:9999/{stem}"
    url = f"{base}?name={urllib.parse.quote(name)}"

    response = requests.get(url)
    return response.text
# @@@SNIPEND
