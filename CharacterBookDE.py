import spacy
import re
from spacy import displacy
from collections import Counter
import de_core_news_sm
nlp = de_core_news_sm.load()
from collections import Counter

def getCharacterMap(path):
    f=open(path, "r", encoding='utf8')
    if f.mode == 'r':
        contents = f.read()
        cleanedContent = reomveHtmlTags(contents.lower())
        doc=nlp(cleanedContent)
        filtered = []
        for X in doc.ents:
            if(X.label_ == 'PER'):
                filtered.append(X.text)
        c = Counter(filtered).most_common(2)
        if len(c) >= 2:
            print(c.__getitem__(0) + c.__getitem__(1))
            
def reomveHtmlTags(raw_html):
  cleaner = re.compile('<.*?>')
  cleantext = re.sub(cleaner, '', raw_html)
  return cleantext           

path="bookForCharacter.txt"
getCharacterMap(path)
