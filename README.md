**声明：本项目仅以学习为目的，请在当地法律允许的范围内使用本程序。任何因错误用途导致的法律责任，与本项目无关！**

## 基于netty的http代理

1. 支持普通GET/POST和CONNECT隧道代理
2. 代理支持over TLS(也就是surge、小火箭等软件说的https proxy)
3. 防止主动嗅探是否为http代理
4. 使用openssl、epoll等技术，支持TLS v1.3。

## 配置解析

```shell script
ask4Authcate=true
```

这是防止主动嗅探的开关，true则会主动要求客户端发送用户密码，会存在被主动嗅探的风险。所以建议设置为false，除非是直接通过SwitchyOmega(chrome插件)使用

```shell script
# http代理配置
http.enable=true
http.port=80
http.auth=arloor:httpforarloor
```

http代理部分的配置，没啥好说的

```shell script
# over Tls配置
https.enable=true
https.port=443
https.auth=arloor:httpforarloor
https.fullchain.pem=1_xxx.com_bundle.crt
https.privkey.pem=2_xxx.com.key
```

https代理部分的配置，主要就是证书相关的几个配置需要说明

https.fullchain.pem 是域名证书+根证书的简单拼接（fullchain是指完整的证书寻找路径，域名证书是用根证书签发的，而签发根证书的证书系统中已自带，由这个完整路径，浏览器才能判断该证书是否有效）

https.privkey.pem 是私钥

以腾讯云上的免费ssl证书为例，nginx文件夹中的`1_xxx.com_bundle.crt`是fullchain，`2_xxx.com.key`是privkey，相信代码从业者能够从这里举一反三，从而知道从其他途径签发的证书应该如何配置。

## 客户端说明

1. 可以使用支持https的软件，例如：surge、shadowrocket、clash
2. chrome浏览器可以通过[SwitchyOmega](https://chrome.google.com/webstore/detail/proxy-switchyomega/padekgcemlokbadohgkifijomclgjgif)插件使用本代理（不推荐，会存在被嗅探的风险）
3. Java开发人员可以使用[connect](https://github.com/arloor/connect)项目

## 网速监控


1. `http(s)://host:port/net`提供了基于echarts.js的网速监控，展示最近500秒的网速
2. `http(s)://host:port/metrics`提供了prometheus的exporter，可以方便地接入prometheus监控


## 性能测试

环境：一台2核4G的服务器(华为云 Intel(R) Xeon(R) Gold 6278C CPU @ 2.60GHz)(好像是目前最强的服务器芯片)

代理客户端：[connect](https://github.com/arloor/connect)项目

软件：proxychains-ng iperf3

实验原理: iperf3是专业的测速软件。通过`proxychains`将iperf3测速的流量走`httpproxy`，从而测试httpproxy的性能。流量传输如下描述：

```shell
iperf3-client ---> connect ---> httpproxy ---> iperf3-server
```

在测速期间，以上四个进程使用2个cpu核心。

connect的jvm参数：

```shell script
# 分别为最小堆，最大堆，新生代大小，触发gc的元空间大小（一般是fullgc）两个survivor与eden区比值（=4，则2:4，默认为8即每个survivor为1/10的年轻代大小）
heap_option='-Xms2000m -Xmx2000m -Xmn600m -XX:MetaspaceSize=40M -XX:SurvivorRatio=8'
```

httpproxy的jvm参数：

```shell script
# 分别为最小堆，最大堆，新生代大小，触发gc的元空间大小（一般是fullgc）两个survivor与eden区比值（=6，则2:6，默认为8即每个survivor为1/10的年轻代大小）
heap_option='-Xms2000m -Xmx2000m -Xmn600m -XX:MetaspaceSize=40M -XX:SurvivorRatio=8'
```

从GC日志上看，没有看到fullGC，为减少youngGC，年轻代的大小刻意地设置地比较大，副作用用jvm占用的内存比较大（500MB，来自top的res字段），实际使用场景下，并不需要这么大的年轻代。

### 性能测试结果

HttpProxy上行速度(iperf3 295秒测试结果)：10.1 Gbits/sec ——**单线程单tcp连接跑满万兆网卡**

```shell script
[ ID] Interval           Transfer     Bandwidth
[  9]   0.00-295.72 sec  0.00 Bytes  0.00 bits/sec                  sender
[  9]   0.00-295.72 sec   349 GBytes  10.1 Gbits/sec                  receiver
iperf3: interrupt - the client has terminated
```

HttpProxy上行速度(iperf3 361秒测试结果)：10.3 Gbits/sec ——**单线程单tcp连接跑满万兆网卡**

```shell script
[ ID] Interval           Transfer     Bandwidth       Retr
[  9]   0.00-361.09 sec   431 GBytes  10.3 Gbits/sec   72             sender
[  9]   0.00-361.09 sec  0.00 Bytes  0.00 bits/sec                  receiver
iperf3: interrupt - the client has terminated
```

流量详情：来自`http(s)://host:port/net`

![](网速监控.png)


资源占用：

```shell script
top - 14:34:36 up 180 days, 21:39,  6 users,  load average: 3.82, 3.05, 2.56
Tasks: 130 total,   2 running, 128 sleeping,   0 stopped,   0 zombie
%Cpu(s): 47.2 us, 45.4 sy,  0.0 ni,  2.8 id,  0.0 wa,  0.5 hi,  4.0 si,  0.0 st
MiB Mem :   3940.4 total,    272.3 free,   2348.8 used,   1319.4 buff/cache
MiB Swap:   4069.0 total,   4029.8 free,     39.2 used.   1107.6 avail Mem

    PID USER      PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND
2175578 root      20   0 4619020 758548  19876 S  79.7  18.8  10:04.91 java
2175835 root      20   0 4579832 587452  20880 S  77.1  14.6   2:28.55 java
2167795 root      20   0   10264   2748   2496 R  15.0   0.1   3:59.31 iperf3
2175851 root      20   0   21076   2544   2348 S  13.6   0.1   0:24.98 iperf3
```



## 电报讨论组

电报讨论组 https://t.me/popstary
