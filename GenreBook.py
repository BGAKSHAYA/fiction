from gensim.models.doc2vec import Doc2Vec, TaggedDocument
from nltk.tokenize import word_tokenize
import re
import sys
from sklearn.decomposition import PCA
#from sklearn.decomposition import FastICA
#from sklearn.manifold import TSNE
#import matplotlib.pyplot as plt
import warnings
warnings.filterwarnings("ignore")


def genreBook(data):
           
        tagged_data = [TaggedDocument(words=word_tokenize(_d.lower()), tags=[str(i)]) for i, _d in enumerate(data)]
               
        vec_size = 300
        alpha = 0.025
        
        #Doc2Vec Model
        model = Doc2Vec(size=vec_size,
                        alpha=alpha, 
                        min_alpha=0.0025,
                        min_count=1,
                        dm =1)
              
        model.build_vocab(tagged_data)        
        pca = PCA(n_components=2)
        
        # Tested with different dimensionality reduction models
        #tsne_model = TSNE(perplexity=40, n_components=2, init='pca', n_iter=2500, random_state=23)
        #mds=MDS(n_components=2)
        #ica = FastICA(n_components=2)
        reduced_feature = pca.fit_transform(model.docvecs.vectors_docs)
         
        reduced_feature=' '.join([' '.join(map(str, map("{:.4f}".format,i))) for i in reduced_feature])
        
        print(reduced_feature)
        
        # create a scatter plot of the projection
        '''plt.scatter(result[:, 0], result[:, 1])
        words = list(model.wv.vocab)
        #print(words)
        for i, word in enumerate(words):
           plt.annotate(word, xy=(result[i, 0], result[i, 1]))
        plt.show()'''
        
def reomveHtmlTags(raw_html):
  cleaner = re.compile('<.*?>')
  cleantext = re.sub(cleaner, '', raw_html)
  return cleantext

filename = "bookWordList.txt"
with open(filename, 'r', encoding='utf8') as file:
    book_data = file.read().replace('\n', ' ')
    book_data= reomveHtmlTags(book_data)
    
book_data = book_data.split(" ")
chunked_data = []
no_of_equal_chunks = 5
book_data_len = len(book_data)
equal_chunk_length = book_data_len//no_of_equal_chunks

for index in range(0,5) :
    chunk = book_data[index : index+equal_chunk_length]
    if(index + (equal_chunk_length*2) > book_data_len):
        chunk  = book_data[index:]
    chunked_data.append(" ".join(chunk))
    
genreBook(chunked_data)