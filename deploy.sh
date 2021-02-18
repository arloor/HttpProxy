echo "输入hostname(以空格分割)"
read hosts
for host in $hosts; do
  echo "部署到${host}"
  scp target/httpproxy-1.0-SNAPSHOT-all.jar root@${host}:/opt/proxy/forwardproxy-1.0-jar-with-dependencies.jar
  ssh root@${host} '
  service proxy stop
  service proxy start
  service proxy status --no-page
  '
done
