# Question Generation Microservice #

[![License](https://img.shields.io/badge/License-MPL%202.0-green.svg)](https://github.com/slidewiki/notification-service/blob/master/LICENSE)

This microservice generates questions automatically for slide/deck content

The service makes use of [DBPediaSpotlight](https://github.com/dbpedia-spotlight/dbpedia-spotlight/) to annotate text and find DBPedia resources in it.

To start the service, simply clone the repo and run the `run.sh` shell script.

This will start a tomcat server at ```http://localhost:8080``` docker container and maven will deploy a war file to tomcat. Once the server is running, you can simply POST data to the server as shown:

```
curl -X POST -H "Content-Type: application/json" -d '{"text": "Germany won the FIFA World Cup","confidence": 0.35}' "http://localhost:8080/qgen/"
```

The username/password for the tomcat server is set in the tomcat-users.xml and the server settings in server.xml. Both are located in the mytomcatdocker folder.
