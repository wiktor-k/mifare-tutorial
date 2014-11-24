mifare-tutorial
===============

Example Service Provider for the MIFARE Classic tutorial in Fidesmo Developer Portal [to be linked when the tutorial page is published].

It is a very simple server that makes a few MIFARE Classic operations, implementing a counter (of invocations to Ctulhu, a somewhat unsettling pet). It implements two services:

1. 'invoke' service
-------------------
Increments a counter stored in a MIFARE memory block.
This is the sequence of operations:
- Get MIFARE Virtual Card. If no virtual card assigned to this service provider exists on the card, a new one is created.
- If a new card was created, Initialize MIFARE Card
- Read one block (block 1 of sector 1) and parse the counter value
- Increment the counter value
- Write the new counter value on block 1 of sector 1

2. 'delete' service
-------------------
- Deletes the MIFARE Virtual Card created by the 'invoke' service


About the code
--------------
Written in Java, using the [Spark framework](http://sparkjava.com/) and built using Maven and IntelliJ Idea IDE.

Before building, you need to define the app credentials (assigned to your application in [Fidesmo Developer Portal](https://developer.fidesmo.com/)) as environment variables.

To build from the command line:
```
export INVOKE_APPID = "xxxxxxxx"
export INVOKE_APPKEYS = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
mvn install
```

To deploy in Heroku, defining the same environment variables as well:
```
heroku config:set INVOKE_APPID=xxxxxxxx
heroku config:set INVOKE_APPKEYS=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
git push heroku master```

