# SIMFIC 2.0
## New Features
### User Interface (by Akshaya)
We have a simple user interface to present the relevant books given a query book and an option to change the language and genre of the searched book. The results are fetched from the Java REST API and are presented along with the important aspects of the search results that we get through the global regression.
The result section consists of relevant books where apart from the book name and author we present other details of the book like the Summary of the book, Published Date, Genre Categories the book belongs to along with the average rating. These details are extracted from asynchronous calls made to **Google Books API** and are maintained in a static file to avoid any delay in the application. As the gensim document summarization didn't perform well with the known set of books we opted for the Google Books API.
When the user clicks See More of a book, we save this information in **Mongo DB** along with Query bookid, Result  Bookid, Rank of the clicked book, and Device information like the browser, operating system. Below is the sample document stored in MongoDB

```sh
{
    rank: 5,
    browser: "Chrome"
    os:"Windows",
    os_version: "windows-10",
    query: "135",
    resultBookClicked:"pg35297"
}
```
This angular and node app is hosted in Heroku at https://fictionui.herokuapp.com/

Please find the java war file and feature extracted csv file in https://drive.google.com/drive/folders/1i6-6_Zfh9bB1HujRAoCM9wxyxwKarIQ0
. Run this war file in Tomcat and place feature extracted file in the D Drive.


Git link to the repository
https://github.com/BGAKSHAYA/fictionUI

### Feature 01 (by Rashmi)
To identify content based genre following steps are followed.
- The pre-processed book is divided into 5 equal chunks.
- Using Doc2Vec the features for the book are generated with vectors size of 100
- The dimensionality reduction technique called PCA is performed on these vectors to reduce the vector dimensions from 100 to 2.
- These 2 features for 5 chunks(Total 10 features ) are concatenated to the rest of the features.
- Hyperparameter tuning is performed using different vector sizes such as 300,500. Also, different techniques for dimensionality reduction are used such as MDS, ICA

To run this code install latest version of gensim,nltk and sklearn

Git link to the branch:
https://github.com/BGAKSHAYA/fiction/tree/Feature-1


### Feature 02 (by Maneendra)

Git link to the branch:
https://github.com/BGAKSHAYA/fiction/tree/FEATURE_2

#### Dialog Interaction
This is a chunk level feature that measures the dialogs ratio with regards to the number of sentences. To identify the quotes, **CoreNLP QuoteAnnotator** which can handle multiline and cross paragraphs quotes has been used. Then the quotes ratio has been calculated from dividing it with the number of sentences of the chunk. Apart from the quotes ratio, several distinguished speakers who have been involved in dialog interactions have been identified via **QuoteAttributionAnnotator.SpeakerAnnotation** and some customer rules. Then the speaker ratio has been calculated with the number of maximum speakers count. The following are the feature numbers in the extracted CSV file.

```sh
F32 = Quotation Ratio
F33 = Speaker Ratio
```
#### Identify Main Character

This is a book level feature that identified whether the plot has a main character or not. **CoreNLP CorefCoreAnnotations.CorefChainAnnotation** has been used to identify characters and character count, coreference cluster-ID, and gender of the character. Furthermore the number of first-person words is counted as it can be used for the identification process. Finally the character map has been sorted based on the character count value and below rules are used for the decision making. But this process needs some improvements due to the performance issues facing now.
- Difference between first and second character counts
- Narrator count ratio which is first-person word count divide by the no of tokens
The following is the feature number in the extracted CSV file.

```sh
F34 = Main Charcater(If found=1, else 0)
```

##### Issues

- To get the real benefit of CoreNLP CorefCoreAnnotations.CorefChainAnnotation, the document should be annotated at once. But it takes a long time for the annotation process and therefore sentence by sentence annotation has done. Even for the sentence annotation it takes more than one hour for a single book and timing should be improved.

Git link to the branch:
https://github.com/BGAKSHAYA/fiction/tree/FEATURE_2

### Feature 03 - Option 2 (by Akshaya)
As a part of this feature, we have selected the ground truth for the mystery genre and extracted topics at the start chunk and the end chunk of the book using LDA. These extracted topics are still needed to be compared to the ground truth to get the similarity score at the start and end of the book.

Git link to the branch:
https://github.com/BGAKSHAYA/fiction/tree/feature-3

### Feature 04 (by Akshaya)
Global Regression is performed in WEKA of Java. Once the top results are fetched we create a training dataset with the feature values making the inputs and the similarity score makes the class label. The dataset is divided into 80% for training and 20% for validation and 5 fold cross-validation is applied to get the Root Mean Squared Error. The parameter ridge is set to 0.5 based on hyperparameter tuning. Finally the features with the highest absolute weights are taken as important aspects.

Git link to the branch: https://github.com/BGAKSHAYA/fiction/tree/Feature-4
