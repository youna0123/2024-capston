from sys import get_asyncgen_hooks
from typing_extensions import final
from pymongo import MongoClient
from openai import OpenAI
import os
import json

client = MongoClient("[connection_string]")
db = client['address']
collection_top = db['top3']
collection_skirt = db['skirt3']

openai_client = OpenAI(api_key="[api_key]")

def generate_embedding(text):
    response = openai_client.embeddings.create(
        model="text-embedding-3-small",
        input=text
    )
    return response.data[0].embedding

etc = input("구체적으로 원하는 게 있어?: ")

def classify_top_etc(etc):
    prompt = f"""
    Please classify the following items based on text information:

    1. Main color
    2. Seasons
    3. Top length
    4. Fit
    5. Sleeve length
    6. Pattern
    7. Neckline
    8. Style
    9. TPO
    10. Other: Detailed description of other tops

    Please respond in json format. Please do not add ```json in front. And don't add ``` after it. All labels must be printed. If that information is not available, just print the label for that item. Don't create information that doesn't exist. Do not classify graphics as Pattern, but as Other.
    if user enter Korean, translate it to English before classifying. If user enter English, no need to translate.
    Identify whether the user wants a specific collection limited to top, skirt, pants, dress, or outer, or if they are looking for an overall vibe.
    If the request is not for top, ensure not to apply that request.
    Prioritize the request for an overall vibe, but ensure that the specific details of individual garments are not overlooked.
    Do not add any fields other than the given 10

    """

    response = openai_client.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": [
            {"type": "text", "text": prompt},
            {"type": "text", "text": etc}
        ]}]
    )
    print(response.choices[0].message.content)
    return response.choices[0].message.content


def search_top(path, queryVector):
    results = collection_top.aggregate([
        {"$vectorSearch": {
            "index": "top3_vector_index",
            "path": path,
            "queryVector": queryVector,
            "numCandidates": 500,
            "limit": 500
        }},
        {"$addFields": {"score": {"$meta": "vectorSearchScore"}}},
        {"$project": {"itemuUrl": 1, "imageUrl": 1, "category": 1, "score": 1, "_id": 0}}
    ])

    return [result for result in results]

top_categories = []
while True:
    t_category = input("카테고리 선택 (Enter 누르면 종료, 다라고 입력하면 모두 선택됨): ")
    if not t_category:
        break
    if t_category == "다":
        top_categories = ["Sweatshirt", "Shirt & Blouse", "Hooded T-shirt", "Knit", "Long-sleeved T-shirt", "Short-sleeved T-shirt", "Sleeveless T-shirt"]
        break
    top_categories.append(t_category)

classification_top = classify_top_etc(etc)
classification_top_dict = json.loads(classification_top)

initial_top_results = []

t_ccc = ["Main color", "Seasons", "Top length", "Fit", "Sleeve length", "Pattern", "Neckline", "Style", "TPO", "Other"]
t_ddd = ["color", "season", "topLength", "fit", "sleeveLength", "pattern", "neckline", "style", "tpo", "etc"]

for i, k in zip(t_ccc, t_ddd):
  a = classification_top_dict[i]
  if a != "":
    b = generate_embedding(a)
    search_top_results = search_top(k, b)
    for s_t_result in search_top_results:
      find = False
      for item in initial_top_results:
        if item['imageUrl'] == s_t_result['imageUrl']:
            item['score'] += s_t_result['score']
            find = True
            break
      if not find:
        initial_top_results.append(s_t_result)

top_temp=[]
for i_t_result in initial_top_results:
  if i_t_result['category'] in top_categories:
    top_temp.append(i_t_result)

sorted_t_list = sorted(top_temp, key=lambda x: x['score'], reverse=True)

for t in sorted_t_list[:10]:
    print(t["imageUrl"],t["score"])

def classify_skirt_etc(etc):
    prompt = f"""
    Please classify the following items based on text information:

    1. Main color
    2. Seasons
    3. Skirt type
    4. Pattern
    5. Style
    6. TPO
    7. Other: Detailed description of other skirts

    Please respond in json format. Please do not add ```json in front. And don't add ``` after it. All labels must be printed. If that information is not available, just print the label for that item. Don't create information that doesn't exist. Do not classify graphics as Pattern, but as Other.
    if user enter Korean, translate it to English before classifying. If user enter English, no need to translate.
    Identify whether the user wants a specific collection limited to top, skirt, pants, dress, or outer, or if they are looking for an overall vibe.
    If the request is not for skirt, ensure not to apply that request.
    Prioritize the request for an overall vibe, but ensure that the specific details of individual garments are not overlooked.
    Do not add any fields other than the given 7.
    """
    response = openai_client.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": [
            {"type": "text", "text": prompt},
            {"type": "text", "text": etc}
        ]}]
    )
    print(response.choices[0].message.content)
    return response.choices[0].message.content

def search_skirt(path, queryVector):
    results = collection_skirt.aggregate([
        {"$vectorSearch": {
            "index": "skirt3_vector_index",
            "path": path,
            "queryVector": queryVector,
            "numCandidates": 500,
            "limit": 500
        }},
        {"$addFields": {"score": {"$meta": "vectorSearchScore"}}},
        {"$project": {"itemuUrl": 1, "imageUrl": 1, "category": 1, "score": 1, "_id": 0}}
    ])

    return [result for result in results]

skirt_categories = []
while True:
    s_category = input("카테고리 선택 (Enter 누르면 종료, 다라고 입력하면 모두 선택됨): ")
    if not s_category:
        break
    if s_category == "다":
        skirt_categories = ["Miniskirt", "Midi skirt", "Long skirt"]
        break
    skirt_categories.append(s_category)

classification_skirt = classify_skirt_etc(etc)
classification_skirt_dict = json.loads(classification_skirt)

initial_skirt_results = []

s_ccc = ["Main color", "Seasons", "Skirt type", "Pattern", "Style", "TPO", "Other"]
s_ddd = ["color", "season", "skirtType", "pattern", "style", "tpo", "etc"]

for i, k in zip(s_ccc, s_ddd):
  a = classification_skirt_dict[i]
  if a != "":
    b = generate_embedding(a)
    search_skirt_results = search_skirt(k, b)
    for s_s_result in search_skirt_results:
      find = False
      for item in initial_skirt_results:
        if item['imageUrl'] == s_s_result['imageUrl']:
            item['score'] += s_s_result['score']
            find = True
            break
      if not find:
        initial_skirt_results.append(s_s_result)

skirt_temp=[]
for i_s_result in initial_skirt_results:
  if i_s_result['category'] in skirt_categories:
    skirt_temp.append(i_s_result)

sorted_skirt_list = sorted(skirt_temp, key=lambda x: x['score'], reverse=True)

for s in sorted_skirt_list[:10]:
    print(s["imageUrl"],s["score"])