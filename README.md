mifare-tutorial
===============

Example Service Provider for the MIFARE Classic tutorial in Fidesmo Developer Portal [to be linked when the tutorial page is published].

It is a very simple server that makes a few MIFARE Classic operations, implementing a counter (of invocations to Ctulhu, a somewhat unsettling pet).

Written in Java, using the [Spark framework](http://sparkjava.com/) and built using Maven and IntelliJ Idea IDE.

To build from the command line:
```mvn build```

To deploy in Heroku:
```git push heroku master```

The webapp's base URL in Heroku is: `https://mifare-tutorial.herokuapp.com/`



