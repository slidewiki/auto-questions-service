#!/usr/bin/env bash

cd 'mytomcatdocker'

docker pull andyfaizan/tomcat-dev # image on docker hub; building not required
docker run --rm --name tomcat-dev -p 8080:8080 andyfaizan/tomcat-dev &

cd '..'

mvn clean

if [ "$?" -ne 0 ]; then
    echo "Maven clean unsuccessful!"
    exit 1
else
  echo "Maven clean successful"
  mvn tomcat7:redeploy
  if [ "$?" -ne 0 ]; then
      echo "Maven deploy unsuccessful!"
      exit 1
  else
    echo "Maven deploy successful"
  fi
fi
