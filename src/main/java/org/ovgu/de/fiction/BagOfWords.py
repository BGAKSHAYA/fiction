#!/usr/bin/env python
# coding: utf-8

# In[42]:


import os

contents = []
path = r'C:\Users\49152\Documents\GutenbergDataset\Short_epubs_extracted'

book_names = [book.split('-')[0] for book in os.listdir(path)]
print(book_names)
for file in os.listdir(path):
    print(file)
    with open(os.path.join(path, file), mode='r') as file:
        #Store book contents as string
        content = ''
        for line in file:
        # Use BeautifulSoup to remove html tags
            soup = BeautifulSoup(line, 'html.parser')
            content += soup.get_text()
        contents.append(content)

#print(contents)


# Data Loading

# In[61]:


from sklearn.feature_extraction.text import TfidfVectorizer
import pandas as pd

# tf-idf based vectors
tf = TfidfVectorizer( stop_words = "english", max_features = 50)
#Fit the model
tf_transformer = tf.fit_transform(contents)
print(tf.get_feature_names())
print(tf_transformer.shape)

features = pd.DataFrame(tf_transformer.toarray(), columns=['f'+str(i) for i in range(50)])
print(features)
features.insert(0, 'BookName' , book_names)
print(features)
features.to_csv('Features.csv', index=None)


# In[ ]:




