import ebooklib
from ebooklib import epub
import os
import re
from sklearn.feature_extraction.text import TfidfVectorizer, CountVectorizer
from sklearn.decomposition import NMF, LatentDirichletAllocation
from time import time
from scipy import spatial
from bs4 import BeautifulSoup
import pandas as pd
import numpy as np

def chunk_generation():
    chunks = []
    for i in range(0,len(bodycontent),10000):
        chunks.append(' '.join(bodycontent[i:i+10000]))
    return chunks

# Styling
def color_green(val):
    color = 'green' if val > .1 else 'black'
    return 'color: {col}'.format(col=color)

def make_bold(val):
    weight = 700 if val > .1 else 400
    return 'font-weight: {weight}'.format(weight=weight)

def print_top_words(model, feature_names, n_top_words):
    for topic_idx, topic in enumerate(model.components_):
        message = "Topic #%d: " % topic_idx
        message += " ".join([feature_names[i]
                             for i in topic.argsort()[:-n_top_words - 1:-1]])
        print(message)
    print()
    

def cleanhtml(raw_html):
  cleanr = re.compile('<.*?>')
  cleantext = re.sub(cleanr, '', raw_html)
  return cleantext


x="out.txt" #This is the preprocessed content sent by the Java application        
#print(x)
#x=sys.argv[1:]#
with open(x, 'r') as file:
    data = file.read().replace('\n', ' ')
    data= cleanhtml(data)

start_book = data[:2500] #TODO: Hyperparameter tune the parameter
end_book = data[:-2500]

groundTruth=['murder','pistol','death'] 

n_samples = 2
n_features = 1000
n_components = 10
n_top_words = 25

print("Extracting tf features for LDA...")
tf_vectorizer = CountVectorizer( max_features= n_features, stop_words='english')

t0 = time()
tf = tf_vectorizer.fit_transform([start_book])
print("done in %0.3fs." % (time() - t0))
print("Fitting LDA models with tf features, "
        "n_samples=%d and n_features=%d..."
#       % (n_samples, n_features))
lda = LatentDirichletAllocation(n_components=n_components, max_iter=5,learning_method='online',learning_offset=50., random_state=0)
lda.fit(tf)
print("done in %0.3fs." % (time() - t0))

print("\nTopics in LDA model:")
tf_feature_names = tf_vectorizer.get_feature_names()
print_top_words(lda, tf_feature_names, n_top_words)

lda_output = lda.transform(tf)

topicnames = ["Topic" + str(i) for i in range(n_components)]
docnames = ["Chunk" + str(i) for i in range(2)]

print(topicnames)
print(docnames)
print(lda_output)







