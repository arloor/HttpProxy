# forward——基于netty的前向代理

站在netty的头上，一个500行的http1.1代理

安装方式：

```shell script
mkdir /opt/forward
wget -O /opt/forward/forwardproxy-1.0-jar-with-dependencies.jar http://cdn.arloor.com/forward/forwardproxy-1.0-jar-with-dependencies.jar
wget -O /lib/systemd/system/forward.service http://cdn.arloor.com/forward/forward.service
systemctl start forward
```