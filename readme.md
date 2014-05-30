## Rocchio Classification

Java application ReutersClassificator classifies documents from Reuters SGML collection into categories using Rocchio Classification. Application is given the directory containing SGML files full of training and testing documents for classification into topics. After parsing the SGML files, we calculate term vectors for each Reuters document and then divide all training documents into categories of topics. Then we calculate the centroids for all topics and iterate over all testing documents to assign them into appropriate topic by cosine similarity.

### Instructions for compilation
- install Java 8 (8 is needed!)
- download the JAR from [here](https://bitbucket.org/jakubzitny/reutersclassificator/downloads/rc.jar)
- download the reuters collection (available [here](http://bit.ly/1jakcFu)) and unpack it
- run the program from commandline
``
java -jar rc.jar -d /path/to/reuters/directory
``

### Major features

Originally we developed very unefficient classification program that took more than 3 minutes to execute. We modified some parts of the code, added parallel execution and option to index ocuments right into the memory. These mods managed to speed up the application down to roughly 30s on 4-core Linux machine.

If in-memory indexing (-r option) is not turned on, the program uses very low amount of memory and still manages to finish under 40 seconds (on 4-core machine).

However, the success rate of documents properly assigned to their categories is only 55.7%. We believe this is due to the classification method we decided to use. All documents that were incorrectly assigned to the category usually missed the correct category just by few. The exlanation for this is the nature of Rocchio classification in big heterogenous datasets.

To see all available command line options run following:
``
java -jar rc.jar -h
``

### Libraries
- Apache Lucene - for calculating term vectors
- Apache Commons CLI 1.2 - for parsing CLI arguments

### Testing stats
- 2 core MacBook - 70s
- 4 core Ubuntu VPS - 32s