A web crawler on the html files and xml files based in SparkJava.

Features:

+ Web Interface:

  + Login, logout, signin functions.

  + Only logged in users can access other contents.

  + User could query on the url of the website to get the crawled content.

  + User could create XPath "channels", and could view the docs by channels.
  
  The WebInterface program, when run from the command-line, should take as the first argument a path for the BerkeleyDB data storage instance, and as a second argument, the directory of the website.


+ Web Crawler: 

  + Engine: has a normal threadpool edition and a Storm edition.

  + Crawlering html and xml files and save them in a local Berkeley database.

  + Sort the files based on the XPath channels predefined form the web interface.

  The crawler will take the following 4 command-line arguments (in this specific order, and the first three are required):
 
  1. The URL of the Web page at which to start. 
  1. The directory of the Berkeley DB
  1. The maximum size, in megabytes, of a document to be retrieved from a Web server
  1. An optional argument indicating the number of files (HTML and XML) to retrieve before stopping.
