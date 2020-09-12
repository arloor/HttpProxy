 #! /bin/bash
 echo "目标服务器："&&read host
 scp target/forwardproxy-1.0-jar-with-dependencies.jar root@${host}:/opt/proxy
 ssh root@${host} "service proxy restart"