#! /bin/bash

# maven package
mvn package
docker build -t proxyserver:3.0 .
docker run  -it -p 9090:9090 proxyserver:3.0