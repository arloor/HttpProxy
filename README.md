## 基于netty的http代理

1. 支持普通GET/POST和CONNECT隧道代理
2. 代理支持over TLS(也就是surge、小火箭等软件说的https proxy)
3. 防止主动嗅探是否为http代理
4. 使用openssl、epoll等技术，支持TLS v1.3。

## 配置解析

```shell script
ask4Authcate=true
```

这是防止主动嗅探的开关，true则会主动要求客户端发送用户密码，会存在被主动嗅探的风险

```shell script
# http代理配置
http.enable=true
http.port=80
http.reverseBit=false
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

## 日志

```shell
2020-07-05 00:15:29.558 INFO  - 120.232.163.69 CONNECT www.google.com:443  {www.google.com}
2020-07-05 00:15:29.569 INFO  - 120.232.163.69 CONNECT android.clients.google.com:443  {android.clients.google.com}
2020-07-05 00:15:30.471 INFO  - 120.232.163.69 CONNECT www.googleapis.com:443  {www.googleapis.com}
2020-07-05 00:15:31.035 INFO  - 120.232.163.69 CONNECT clients4.google.com:443  {clients4.google.com}
2020-07-05 00:15:31.040 INFO  - 120.232.163.69 CONNECT oauthaccountmanager.googleapis.com:443  {oauthaccountmanager.googleapis.com}
2020-07-05 00:15:31.961 INFO  - 120.232.163.69 CONNECT accounts.google.com:443  {accounts.google.com}
2020-07-05 00:15:32.731 INFO  - 120.232.163.69 CONNECT www.google.com:443  {www.google.com}
2020-07-05 00:15:32.798 INFO  - 120.232.163.69 CONNECT lh5.googleusercontent.com:443  {lh5.googleusercontent.com}
2020-07-05 00:15:33.216 INFO  - 120.232.163.69 CONNECT www.google.com.hk:443  {www.google.com.hk}
2020-07-05 00:15:33.239 INFO  - 120.232.163.69 CONNECT www.google.com.hk:443  {www.google.com.hk}
2020-07-05 00:15:33.368 INFO  - 120.232.163.69 CONNECT www.google.com.hk:443  {www.google.com.hk}
2020-07-05 00:15:35.114 INFO  - 120.232.163.69 CONNECT www.gstatic.com:443  {www.gstatic.com}
2020-07-05 00:15:35.337 INFO  - 120.232.163.69 CONNECT adservice.google.com.hk:443  {adservice.google.com.hk}
2020-07-05 00:15:38.482 INFO  - 120.232.163.69 CONNECT github.githubassets.com:443  {github.githubassets.com}
2020-07-05 00:15:38.489 INFO  - 120.232.163.69 CONNECT github.com:443  {github.com}
2020-07-05 00:15:38.509 INFO  - 120.232.163.69 CONNECT github.com:443  {github.com}
2020-07-05 00:15:40.441 INFO  - 120.232.163.69 CONNECT avatars0.githubusercontent.com:443  {avatars0.githubusercontent.com}
2020-07-05 00:15:40.444 INFO  - 120.232.163.69 CONNECT avatars3.githubusercontent.com:443  {avatars3.githubusercontent.com}
2020-07-05 00:15:40.636 INFO  - 120.232.163.69 CONNECT content-autofill.googleapis.com:443  {content-autofill.googleapis.com}
2020-07-05 00:15:40.807 INFO  - 120.232.163.69 CONNECT www.google-analytics.com:443  {www.google-analytics.com}
2020-07-05 00:15:40.827 INFO  - 120.232.163.69 CONNECT github.com:443  {github.com}
2020-07-05 00:15:40.835 INFO  - 120.232.163.69 CONNECT github.com:443  {github.com}
2020-07-05 00:15:41.187 INFO  - 120.232.163.69 CONNECT alive.github.com:443  {alive.github.com}
2020-07-05 00:15:42.195 INFO  - 120.232.163.69 CONNECT api.github.com:443  {api.github.com}
2020-07-05 00:15:44.031 INFO  - 120.232.163.69 CONNECT avatars3.githubusercontent.com:443  {avatars3.githubusercontent.com}
2020-07-05 00:15:44.039 INFO  - 120.232.163.69 CONNECT avatars0.githubusercontent.com:443  {avatars0.githubusercontent.com}
2020-07-05 00:15:44.042 INFO  - 120.232.163.69 CONNECT avatars2.githubusercontent.com:443  {avatars2.githubusercontent.com}
```

## 电报讨论组

电报讨论组 https://t.me/popstary
