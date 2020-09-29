#! /bin/bash
for host in host1 host2 host3 host4
do
 scp target/httpproxy-1.0-jar-with-dependencies.jar root@${host}:/opt/proxy
 ssh root@${host} "service proxy restart"
done
