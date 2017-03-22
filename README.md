# Question Generation Microservice #

[![License](https://img.shields.io/badge/License-MPL%202.0-green.svg)](https://github.com/slidewiki/notification-service/blob/master/LICENSE)

This microservice generates questions automatically for slide/deck content

The service makes use of [DBPediaSpotlight](https://github.com/dbpedia-spotlight/dbpedia-spotlight/) to annotate text and find DBPedia resources in it.

### Prerequisites

The service requires [JDK 8](http://openjdk.java.net/install/), [cURL](https://curl.haxx.se/download.html), [docker](https://docs.docker.com/engine/installation/) and [maven](https://maven.apache.org/download.cgi) to run. 

### Running The Service

To start the service, simply clone the repo and run the `run.sh` shell script.

The script pulls a pre-built docker image (containing tomcat) from docker hub and runs it.
This starts a tomcat server at ```http://localhost:8080```. Then maven deploys a war file to tomcat.

The username/password for the tomcat server is set in the tomcat-users.xml and the server settings in server.xml. Both are located in the mytomcatdocker folder.

#### REST Endpoints

##### Production

```GET /qgen/{level}/{id}```

This endpoint is for getting questions for a deck by passing the difficulty level and deck id.
The difficulty level can be either "easy", "medium" or "hard" (without quotes).

```
curl -X GET "http://localhost:8080/qgen/easy/997-1"

```

##### Development

The following endpoints are for a development environment only and might be removed in the future

```POST /qgen/{level}/text/```

For testing the system by sending text to it

```
curl -X POST -H "Content-Type: text/plain" -d 'Germany won the 2014 FIFA World Cup.' "http://localhost:8080/qgen/text"
```

```POST /qgen/text/numbers/```

For getting questions related to values/numerals found in the text. This is done using the Stanford CoreNLP tools.
 
```
curl -X POST -H "Content-Type: text/plain" -d 'Germany won the 2014 FIFA World Cup.' "http://localhost:8080/qgen/text/numbers"
```

```POST /qgen/select/{level}/text/```

For select multiple choice questions
```
curl -X POST -H "Content-Type: text/plain" -d 'Mount Rushmore has faces carved onto it.' "http://localhost:8080/qgen/select/easy/text"
```
### Running with the spotlight docker container (Production Environment)

One can also run a container of DBPediaSpotlight on the same host as this one and communicate with it.

This of course makes the solution purely local because no requests would be made to the spotlight web service, but extra effort is required to run this as of now.  

For doing so, one needs to do the following (before running the tomcat docker container):

Build the spotlight docker container as shown on the [spotlight-docker](https://github.com/dbpedia-spotlight/spotlight-docker/tree/master/v0.7.1/english) repo

Then run the spotlight container with the name "spotlight" (without quotes) like this:
```
docker run -i --name spotlight -p 2222:80 english_spotlight spotlight.sh
```

Once the spotlight container is up and running, run the shell script with an argument "prod":

```./run.sh prod```

The prod argument will use the production docker container designed to run with the spotlight container.

Both dev and prod images are available on DockerHub [here](https://hub.docker.com/r/andyfaizan/).
