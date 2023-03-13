if [ "$*" = "" ]; then
    echo "输入hostname(以空格分割)"
    read hosts
else
  echo 目标为"$*"
  hosts=$*
fi

for host in $hosts; do
  echo "部署到${host}"
  scp target/httpproxy-1.0-SNAPSHOT-all.jar root@${host}:/opt/proxy/forwardproxy-1.0-jar-with-dependencies.jar
  ssh root@${host} '
  systemctl stop proxy
  systemctl start proxy
  systemctl status proxy  --no-page
  '
done
