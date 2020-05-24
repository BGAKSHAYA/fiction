# SIMFIC 2.0
## New Features
### User Interface (by Akshaya)
We have a simple user interface to present the relevant books given a query book and an option to change the language and genre of the searched book. The results are fetched from the Java REST API and is presented along with the important aspects of the search results that we get through the global regression.

The result section consists of the top 10 relevant books where apart from the book name and author we present other details of the book like the Summary of the book, Published Date, Genre Categories the book belongs to along with the avergae rating. These details are extracted from asynchronous calls made to **Google Books API** and are maintained in a static file to avoid any delay in the application. As the gensim document summarization didn't perform well with the known set of books we opted the Google Books API.

When the user clicks <u>See More</u> of a book, we save this information in **Mongo DB** along with Query bookid,Result Bookid, Rank of the clicked book and Device information like the browser, operating system. Below is the sample document stored in MongoDB
```sh
{
    rank : 5,
    browser: "Chrome"
    os:"Windows",
    os_version: "windows-10",
    query: "135",
    resultBookClicked:"pg35297"
}
```
This angular and node app is hosted in Heroku at ____

Git link to the repository
https://github.com/BGAKSHAYA/fictionUI

### Feature 01 (by Rashmi)

Git link to the branch:
https://github.com/BGAKSHAYA/fiction/tree/Feature-1


### Feature 02 (by Maneendra)
#### Dialog Interaction
This is a chunk level feature which measures the dialogs ratio with regards to the number of sentences. To identify the quotes, **CoreNLP QuoteAnnotator** which can handle multiline and cross paragrphs quotes has been used. Then the quotes ratio has been calculated from deviding it with number of sentences of the chunk. Apart from the quotes ration, no of distinct speakers who has been involved in dialog interactions have been identified via **QuoteAttributionAnnotator.SpeakerAnnotation** and some customer rules. Then the speaker ratio has been calculted with the number of maxmium speakers count. Following are the feature numbers in the extracted csv file.

```sh
F32 = Quotation Ratio
F33 = Speaker Ratio
```
#### Identify Main Character

This is a book level feature which identified whether the plot has a main character or not. **CoreNLP CorefCoreAnnotations.CorefChainAnnotation** has been used to identify characters and character count, coreference cluster ID and gender of the character. Furthermore number of first person words are counted as it can be used for the identification process. Finally the character map has been sorted based on the character count value and below rules are used for the decision making.But this process needs some improvements due to the performance issues facing now.

- Difference between first and second character counts
- Narrator count ratio which is first person word count divide by the no of tokens

Following is the feature number in the extracted csv file.

```sh
F34 = Main Charcater(If found=1, else 0)
```

##### Issues
- To get the real benifit of **CoreNLP CorefCoreAnnotations.CorefChainAnnotation**, document should be annotated at once. But it takes really long time for the annotation process and therefore sentence by sentence annotation has done. Even for the sentence annotation it takes more than one hour for a single book and timing should be improved.
- Character identification process should be improved as sometimes it does not identify the names in the text.
- Rules used for decision making should be improved with the test run with 670 books.

Git link to the branch:
https://github.com/BGAKSHAYA/fiction/tree/FEATURE_2

### Feature 03 - Option 2 (by Akshaya)
As a part of this feature, we have selected the groud truth for mystery genre and extracted topics at the start chunk and the end chunk of the book using LDA. These extracted topics are still needed to be compared to the ground truth to get the similarity score at the start and ending of the book. 

### Feature 04 (by Akshaya)
Global Regression is performed in WEKA of Java. Once the top results are fetched we create a training dataset with the feature values making the inputs and the similarity score makes the class label. The dataset is divided into 80% for training and 20% for validation and 5 fold cross validation is applied to get the Root Mean Squared Error. The parameter ridge is set to 0.5 based on hyperparameter tuning. Finally the features with the highest absolute weights are taken as the important aspects. 

Git link to the branch: https://github.com/BGAKSHAYA/fiction/tree/Feature-4

