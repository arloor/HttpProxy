**声明：本项目仅以学习为目的，请在当地法律允许的范围内使用本程序。任何因错误用途导致的法律责任，与本项目无关！**

## 基于netty的http代理

- 支持普通GET/POST和CONNECT隧道代理
- 代理支持over TLS(也就是surge、小火箭等软件说的https proxy)
- 防止主动嗅探是否为http代理
- 使用openssl、epoll等技术，支持TLS v1.3。

## 支持的客户端

| 平台            | 支持的客户端                                                                                                                                                            |
|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Linux、Windows | [clash_for_windows](https://github.com/Fndroid/clash_for_windows_pkg)、[go语言客户端](https://github.com/arloor/forward)、[Java语言客户端](https://github.com/arloor/connect) |
| MacOS         | ClashX、ClashX Pro|                                                                                                                                                 |                                                                                                                                                           |                                                                                                                                                           |
| IOS           | [Surge](https://apps.apple.com/us/app/surge-4/id1442620678)、[shawdowrocket](https://apps.apple.com/us/app/shadowrocket/id932747118)                               |
| Android       | [ClashForAndroid](https://github.com/Kr328/ClashForAndroid)                                                                                                       |
| chrome        | [SwitchyOmega](https://chrome.google.com/webstore/detail/proxy-switchyomega/padekgcemlokbadohgkifijomclgjgif)插件（不推荐，会存在被嗅探的风险）                                    |

## 网速监控

1. `http(s)://host:port/net`提供了基于echarts.js的网速监控，展示最近500秒的网速，如下图所示
 ![](/实时网速.png)
2 `http(s)://host:port/metrics`提供了prometheus的exporter，可以方便地接入prometheus监控，提供网速、内存等监控指标，如下所示

```shell
# HELP proxy_out 上行流量
# TYPE proxy_out counter
proxy_out{host="localhost",} 65205
# HELP proxy_in 下行流量
# TYPE proxy_in counter
proxy_in{host="localhost",} 21205
# HELP proxy_out_rate 上行网速
# TYPE proxy_out_rate gauge
proxy_out_rate{host="localhost",} 23967
# HELP proxy_in_rate 下行网速
# TYPE proxy_in_rate gauge
proxy_in_rate{host="localhost",} 5758
# HELP direct_memory_total netty直接内存 对于jdk9+，请增加-Dio.netty.tryReflectionSetAccessible=true，对于jdk16+，请增加--add-opens java.base/java.nio=ALL-UNNAMED
# TYPE direct_memory_total gauge
direct_memory_total{host="localhost",} 33554439
```

[jdk9以上设置netty直接内存监控的说明](/jdk9以上设置netty直接内存监控的说明.md)

## 运行指南

```shell
# 需要使用jdk17
maven clean package
java -jar target/httpproxy-1.0-SNAPSHOT-all.jar -c proxy.properties
```

### 配置文件说明

`proxy.properties`内容如下

```shell script
# true：主动索要Proxy-Authorization，可能会被探测到是代理服务器。除非通过Chrom插件SwitchyOmega使用该代理，否则不建议设置为true
# false: 不索要Proxy-Authorization
ask4Authcate=false

# http代理部分设置
# true:开启，false:不开启
http.enable=false                 
# http代理的端口
http.port=6789
# http代理的用户名和密码，逗号分割多个用户。不设置则不启用鉴权
#http.auth=user1:passwd,user2:passwd

# https代理部分设置
# true:开启，false:不开启
https.enable=true
# https代理的端口
https.port=443
# http代理的用户名和密码，逗号分割多个用户。不设置则不启用鉴权
#https.auth=user1:passwd,user2:passwd
# TLS证书的fullchain（从CA证书到域名证书）
https.fullchain.pem=/path/to/fullchain.cer
# TLS证书的私钥
https.privkey.pem=/path/to/private.key
```

### TLS证书的更多说明

以腾讯云的免费ssl证书为例，nginx文件夹中的`1_xxx.com_bundle.crt`是fullchain，`2_xxx.com.key`是private.key。相信代码从业者能够从这里举一反三，从而知道从其他途径签发的证书应该如何配置。

测试时，可以使用项目内的`cert.pem`和`privkey.pem`（他们由openssl生成），同时需要设置设置chrome不验证localhost的证书

```shell
## 证书生成脚本
openssl req -x509 -newkey rsa:4096 -sha256 -nodes -keyout privkey.pem -out cert.pem -days 3650

## chrome不验证本地证书
打开 chrome://flags/#allow-insecure-localhost
```

## 性能测试

[性能测试](性能测试.md)

## 推荐查看Rust语言版本

[rust_http_proxy](https://github.com/arloor/rust_http_proxy)

轻量、高性能、内存占用低
