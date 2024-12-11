import pandas as pd
from pymongo import MongoClient
import openai
from sklearn.feature_extraction.text import CountVectorizer
import json
from tenacity import retry, stop_after_attempt, wait_fixed, retry_if_exception_type
from openai import OpenAI, BadRequestError

client = MongoClient("[connection_string]")
db = client["address"]

client2 = openai.OpenAI(api_key="[api_key]")

df = pd.read_excel("[excel_file_name]")

@retry(
    stop=stop_after_attempt(2),
    wait=wait_fixed(5),
    retry=retry_if_exception_type(BadRequestError)
)
def classify_item(thumbnail_url):
    prompt = f"""
    Please classify the following items based on image information:
    1. Category: Select from [Sweatshirt, Shirt & Blouse, Hooded T-shirt, Knit, Long-sleeved T-shirt, Short-sleeved T-shirt, Sleeveless T-shirt]
    2. Main color
    3. Seasons
    4. Top length
    5. Fit
    6. Sleeve length
    7. Pattern
    8. Neckline
    9. Style
    10.TPO
    11. Other: Detailed description of other tops

    Please respond in JSON format. Please do not add ```json in front. And don't add ``` after it. If there are more than 2 tops or no tops, print impossible without quotes, not in json format. Do not enter null values. Don't leave out Other
    """

    response = client2.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": [
            {"type": "text", "text": prompt},
            {"type": "image_url", "image_url": {"url": thumbnail_url}}
        ]}]
    )
    print(thumbnail_url)
    print(response.choices[0].message.content)
    return response.choices[0].message.content

def get_embedding(text):
    response = client2.embeddings.create(
        model="text-embedding-3-small",
        input=text
    )
    return response.data[0].embedding

def process_and_insert_data():
    for _, row in df.iterrows():
        classification = classify_item(row['썸네일 URL'])
        if classification.startswith('impo'):
          continue
        classification_dict = json.loads(classification)
        top_result = db['top3'].insert_one({
            "itemuUrl": row['url'],
            "imageUrl": row['썸네일 URL'],
            "category": classification_dict['Category'],
            "color": get_embedding(classification_dict['Main color']),
            "season": get_embedding(classification_dict['Seasons']),
            "topLength": get_embedding(classification_dict['Top length']),
            "fit": get_embedding(classification_dict['Fit']),
            "sleeveLength": get_embedding(classification_dict['Sleeve length']),
            "pattern": get_embedding(classification_dict['Pattern']),
            "neckline": get_embedding(classification_dict['Neckline']),
            "style": get_embedding(classification_dict['Style']),
            "tpo": get_embedding(classification_dict['TPO']),
            "etc": get_embedding(classification_dict['Other'])
        })

process_and_insert_data()