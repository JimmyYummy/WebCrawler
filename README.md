A web crawler on the html files and xml files based in SparkJava.

Features:

Web Interface:
Login, logout, signin functions.
Only logged in users can access other contents.
User could query on the url of the website to get the crawled content.
User could create XPath "channels", and could view the docs by channels.

Web Crawler: 
Engine: has a normal threadpool edition and a Storm edition.
Crawlering html and xml files and save them in a local Berkeley database.
Sort the files based on the XPath channels predefined form the web interface.
