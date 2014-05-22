## Rocchio Classification

Java application ReutersClassificator classifies documents from Reuters SGML collection into categories. Application is given the directory containing SGML files full of training and testing documents for classification into topics. After parsing the SGML files, we calculate TF-IDF for each Reuters document and then divide all training documents into categories of topics. Then we calculate the centroids for all topics and iterate over all testing documents to assign them into appropriate topic by cosine similarity.

### Instructions for compilation
- install Java 8
- download the JAR from [here](https://bitbucket.org/jakubzitny/reutersclassificator/downloads/rc.jar)
- download the reuters collection (available [here](http://bit.ly/1jakcFu)) and unpack it
- run the program from commandline
``
java -jar rc.jar -d /path/to/reuters/directory
``

### Libraries
Apache Lucene - for calculating TF-IDF weighs
Apache Commons CLI 1.2 - for parsing CLI arguments
