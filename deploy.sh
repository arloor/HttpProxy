#! /bin/bash
for host in someme.me
do
 scp target/httpproxy-1.0-jar-with-dependencies.jar root@${host}:/opt/proxy/forwardproxy-1.0-jar-with-dependencies.jar
 ssh root@${host} "service proxy restart"
done
