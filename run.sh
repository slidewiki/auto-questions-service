#!/usr/bin/env bash
env=""
if [ "$#" -eq 1 ]; then
    if [ "$1" = "prod" ]; then
        env="$1"
        echo "$env"
    fi
fi

cd 'mytomcatdocker'

if [ "$env" != "prod" ]; then
    docker build -f Dockerfile-dev -t andy/tomcat-dev .
    docker run --rm --name tomcat-dev -p 8080:8080 andy/tomcat &
else
    ### Scenario 2 ###
    # Running this docker container with another container for spotlight on the same host
    docker build -f Dockerfile-prod -t andy/tomcat-prod .
    docker run --rm --link spotlight:english_spotlight --name tomcat -p 8080:8080 andy/tomcat-prod &
fi

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
