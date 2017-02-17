# Question Generation Microservice #

[![License](https://img.shields.io/badge/License-MPL%202.0-green.svg)](https://github.com/slidewiki/notification-service/blob/master/LICENSE)

This microservice generates questions automatically for slide/deck content

The service makes use of [DBPediaSpotlight](https://github.com/dbpedia-spotlight/dbpedia-spotlight/) to annotate text and find DBPedia resources in it.

To start the service, simply clone the repo and run the `run.sh` shell script.

This will start a tomcat server at ```http://localhost:8080``` docker container and maven will deploy a war file to tomcat. Once the server is running, you can simply POST data to the server as shown:

```
curl -X POST -H "Content-Type: application/json" -d '{"text": "Germany won the FIFA World Cup"}' "http://localhost:8080/qgen/"
```

The username/password for the tomcat server is set in the tomcat-users.xml and the server settings in server.xml. Both are located in the mytomcatdocker folder.

<!---
### Running with the spotlight docker container

One can also run a container of DBPediaSpotlight on the same host as this one and communicate with it.

This of course makes the solution purely local because no requests would be made to the spotlight web service, but extra effort is required to run this as of now.  

For doing so, one needs to do the following (before running the tomcat docker container):

Build the spotlight docker container as shown on the [spotlight-docker](https://github.com/dbpedia-spotlight/spotlight-docker/tree/master/v0.7.1/english) repo

One needs to run the spotlight container with the name "spotlight" (without quotes) like this:
```
docker run -i --name spotlight -p 2222:80 english_spotlight spotlight.sh
```

Once the spotlight container is up and running:

```
docker run --rm --link spotlight:english_spotlight --name tomcat -p 8080:8080 andy/tomcat &
```
--->