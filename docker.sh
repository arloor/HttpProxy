#! /bin/bash

# maven package
mvn package
docker build -t proxyserver .
docker run  -it -p 9090:9090 proxyserver