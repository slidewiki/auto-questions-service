cd 'mytomcatdocker'

docker build -t andy/tomcat .

docker run --rm -p 8080:8080 andy/tomcat &

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
