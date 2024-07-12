import os
from django.conf import settings
from django.shortcuts import render
import pandas as pd
from scipy import sparse
from sklearn.metrics.pairwise import cosine_similarity
from django.http import JsonResponse
import json
import csv
from io import StringIO
import re

csv_file_path = os.path.join(settings.BASE_DIR, 'data', 'dataset.csv')

# Global variables to store data
item_similarity_df = None
activity_ratings = None
current_data_list = None

def home():
    global item_similarity_df, activity_ratings

    # Read dataset
    selections = pd.read_csv(csv_file_path, index_col=0)
    selections = selections.fillna(0)  

    # Standardization not needed for binary data
    # selections_std = selections.apply(standardize) 

    # Compute item similarity (cosine similarity is suitable for binary data)
    item_similarity = cosine_similarity(selections.T)
    item_similarity_df = pd.DataFrame(item_similarity, index=selections.columns, columns=selections.columns)

    # Get recommendations based on current_data_list (if available)
    if current_data_list:
        similar_activities = pd.DataFrame()
        for i, (activity, _) in enumerate(current_data_list):
            similar_activities[i] = get_similar_activities(activity, 1)

        similar_activities = similar_activities.transpose()
        result = similar_activities.sum().sort_values(ascending=False)
        top_activities = result.index.tolist()

        # Filter for values greater than zero
        activity_ratings = [(activity_name, result[activity_name]) for activity_name in top_activities if result[activity_name] > 0]

    with open("activity_ratings.json", "w") as json_file:
        json.dump(activity_ratings, json_file)


def get_similar_activities(activity_name, rating):  
    #  We ignore the rating as it is always 1
    return item_similarity_df[activity_name] 
def receive_csv_data(request):
    if request.method == 'POST':
        csv_data = request.POST.get('csv_data') 
        current_data = request.POST.get('current_data')
       
        global current_data_list
        current_data_list = convert_to_tuple_list(current_data)
        print(current_data_list)

        global received_csv_data
        received_csv_data = csv_data
        convert_to_csv(received_csv_data)
        #print(received_csv_data)

        return JsonResponse(activity_ratings, safe=False)
    else:
        return JsonResponse({'error': 'Only POST requests are supported'})
    

def convert_to_tuple_list(input_string):
    try:
        word_list = re.findall(r'\b(\w+)\b', input_string.strip()) 
        
        tuple_list = [(word, 1) for word in word_list]
        return tuple_list

    except re.error as e:  
        print(f"Error parsing input string using regular expression: {e}")
        return [] 
    

def convert_to_csv(string_data):
    csv_file = os.path.join("data", "dataset.csv")
    string_io = StringIO(string_data)
    with open(csv_file, 'w') as csvFile:
        header = string_io.readline().strip()
        csvFile.write(header + '\n')
        
        for line in string_io.readlines():
            csvFile.write(line.strip() + '\n')
    
    string_io.close()
    home()

    

def get_result_data(request):
    # Process data and generate result
    result_data = {'result': 'Your result data here'}

    # Return result data as JSON response
    return JsonResponse(result_data)
