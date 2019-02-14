#! /bin/bash

mvn clean
# maven package
mvn package
docker build -t arloor/proxyserver:3.0 .
#上传到我的docker hub（其他用户没密码不行的
docker push arloor/proxyserver:3.0
#docker run  -it -p 9090:9090 arloor/proxyserver:3.0