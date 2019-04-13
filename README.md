[![](https://travis-ci.org/arloor/HttpProxy.svg?branch=master)](https://travis-ci.org/arloor/HttpProxy)
[![](https://img.shields.io/github/release/arloor/HttpProxy.svg?style=flat)](https://github.com/arloor/HttpProxy/releases)
[![](https://img.shields.io/github/last-commit/arloor/HttpProxy.svg?style=flat)](https://github.com/arloor/HttpProxy/commit/master)
![](https://img.shields.io/github/languages/code-size/arloor/HttpProxy.svg?style=flat)

# HttpProxy 基于netty的代理

这是一个轻量、稳定、高性能的http代理，仅仅依赖netty和日志框架，实现http中间人代理和https隧道代理。google、youtube视频、测试代理速度、作为git的代理、作为docker的代理等场景都运行完美。

## 运行日志

客户端运行日志
```
2019-01-30 22:50:27.455 [nioEventLoopGroup-2-3] INFO  com.arloor.proxyclient.ProxyConnenctionHandler - 返回 0字节：浏览器关闭连接，因此关闭到代理服务器的连接
2019-01-30 22:51:20.443 [nioEventLoopGroup-2-2] INFO  com.arloor.proxyclient.ProxyConnenctionHandler - 连接成功: 到代理服务器,允许读浏览器请求
2019-01-30 22:51:20.443 [nioEventLoopGroup-2-3] INFO  com.arloor.proxyclient.ProxyConnenctionHandler - 连接成功: 到代理服务器,允许读浏览器请求
2019-01-30 22:51:20.444 [nioEventLoopGroup-2-3] INFO  com.arloor.proxyclient.ProxyConnenctionHandler - 向代理服务器发送请求成功。
2019-01-30 22:51:20.444 [nioEventLoopGroup-2-2] INFO  com.arloor.proxyclient.ProxyConnenctionHandler - 向代理服务器发送请求成功。
2019-01-30 22:51:21.177 [nioEventLoopGroup-4-1] INFO  com.arloor.proxyclient.ProxyConnenctionHandler - 返回响应 1111字节 proxy/xx.xx.xx.xx:9090
2019-01-30 22:51:21.184 [nioEventLoopGroup-4-1] INFO  com.arloor.proxyclient.ProxyConnenctionHandler - 返回响应 1035字节 proxy/xx.xx.xx.xx:9090
2019-01-30 22:51:21.187 [nioEventLoopGroup-4-1] INFO  com.arloor.proxyclient.ProxyConnenctionHandler - 返回响应 3216字节 proxy/xx.xx.xx.xx:9090
2019-01-30 22:51:23.413 [nioEventLoopGroup-4-1] INFO  com.arloor.proxyclient.ProxyConnenctionHandler - 返回响应 24字节 proxy/xx.xx.xx.xx:9090
2019-01-30 22:51:23.414 [nioEventLoopGroup-2-3] INFO  com.arloor.proxyclient.ProxyConnenctionHandler - 向代理服务器发送请求成功。
```

服务器运行日志
```
2019-01-30 22:53:37.226 [nioEventLoopGroup-4-1] INFO  com.arloor.proxyserver.NewProxyConnectionHandler - success：通知隧道建立成功 /220.184.48.168:48020
2019-01-30 22:53:37.230 [nioEventLoopGroup-2-4] INFO  com.arloor.proxyserver.NewProxyConnectionHandler - 连接成功: ssl.gstatic.com:443
2019-01-30 22:53:37.230 [nioEventLoopGroup-4-1] INFO  com.arloor.proxyserver.NewProxyConnectionHandler - success：通知隧道建立成功 /220.184.48.168:48018
2019-01-30 22:53:37.234 [nioEventLoopGroup-2-1] INFO  com.arloor.proxyserver.NewProxyConnectionHandler - 连接成功: www.gstatic.com:443
2019-01-30 22:53:37.243 [nioEventLoopGroup-4-1] INFO  com.arloor.proxyserver.NewProxyConnectionHandler - success：通知隧道建立成功 /220.184.48.168:48022
2019-01-30 22:53:37.395 [nioEventLoopGroup-4-1] INFO  c.a.p.NewProxyConnectionHandler$SendBack2ClientHandler - 返回响应 224字节 www.gstatic.com/172.217.4.131:443
2019-01-30 22:53:37.399 [nioEventLoopGroup-2-2] INFO  com.arloor.proxyserver.NewProxyConnectionHandler - 连接成功: www.google.com:443
2019-01-30 22:53:37.399 [nioEventLoopGroup-4-1] INFO  com.arloor.proxyserver.NewProxyConnectionHandler - success：通知隧道建立成功 /220.184.48.168:48006
2019-01-30 22:53:37.408 [nioEventLoopGroup-4-1] INFO  c.a.p.NewProxyConnectionHandler$SendBack2ClientHandler - 返回响应 2464字节 lh3.googleusercontent.com/216.58.216.33:443
2019-01-30 22:53:37.429 [nioEventLoopGroup-4-1] INFO  c.a.p.NewProxyConnectionHandler$SendBack2ClientHandler - 返回响应 2448字节 ssl.gstatic.com/216.58.217.195:443
2019-01-30 22:53:37.526 [nioEventLoopGroup-2-3] INFO  com.arloor.proxyserver.NewProxyConnectionHandler - 连接成功: www.google.com:443
2019-01-30 22:53:37.527 [nioEventLoopGroup-4-1] INFO  com.arloor.proxyserver.NewProxyConnectionHandler - success：通知隧道建立成功 /220.184.48.168:48026
2019-01-30 22:53:37.546 [nioEventLoopGroup-4-1] INFO  c.a.p.NewProxyConnectionHandler$SendBack2ClientHandler - 返回响应 576字节 www.gstatic.com/172.217.4.131:443
2019-01-30 22:53:37.551 [nioEventLoopGroup-4-1] INFO  c.a.p.NewProxyConnectionHandler$SendBack2ClientHandler - 返回响应 224字节 www.google.com/216.58.217.196:443
2019-01-30 22:53:37.568 [nioEventLoopGroup-4-1] INFO  c.a.p.NewProxyConnectionHandler$SendBack2ClientHandler - 返回响应 576字节 lh3.googleusercontent.com/216.58.216.33:443
```


## 配置  编译  运行


这个项目使用了maven父子模块，配置文件是proxycommon模块的resourses文件夹下的proxy.properties，内容如下：

```
#是否加密 默认为false
crypto=true
#加密类型 AES|SIMPLE  若不能识别输入的type自动设为SIMPLE
crypto.type=AES
#当加密类型不为SIMPLE时使用
crypto.key=你想用我的代理吗？？？
#作为分割
crypto.delimiter=br
#代理服务器运行在8080
server.port=9090
#默认为127.0.0.1
server.host=proxy
#默认为8081
client.port=9091
```
在这个配置下，代理客户端会运行在9091端口，服务器运行在proxy:9090。使用AES加密内容，key是“你想用我的代理吗？？？”经过SHA-256变换后取128位。

请注意`server.host=proxy`这一项配置，可以考虑自行将proxy改为localhost或者某个ip，当然也可以在hosts文件中增加proxy的ip。

配置了server.host之后，就可以使用maven来打包了：

在项目根目录下执行mvn package即可生成proxyclient-\*-jar-with-dependencies.jar和proxyserver-\*-jar-with-dependencies.jar。这两个jar包分别在proxyclient、proxyserver模块的target文件夹内，执行这两个jar包即运行成功。

## 在国外VPS上搭建此代理

为了方便地在VPS上部署，提供了基于docker的服务端部署方式。并且开发了go语言编写的客户端以方便日常使用。详情点击以下链接：

[快速安装HttpProxy——另一种翻墙方式](http://arloor.com/posts/other/proxynew-docker-install/)


## 长期使用发现并解决的问题

上面说了，代理的核心就是http请求解析、加密、转发。然而在核心之外，有些问题容易被忽略，在2个多月的使用中，才被我发现和解决。

如果有人会看我的代码，千万不要忽略下面所提及的代码。

### 对端关闭问题

这个问题其实就是，服务器关闭了到代理服务器的连接，代理服务器要反馈给（关闭）到代理客户端的连接，最终要反馈给浏览器。

代码很简单：

```java
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (remoteChannel != null && remoteChannel.isActive()) {
            remoteChannel.writeAndFlush(PooledByteBufAllocator.DEFAULT.buffer()).addListener(future -> {
                remoteChannel.close().addListener(future1 -> {
                    logger.info("返回0字节：browser关闭连接，因此关闭到webserver连接");
                });
            });
        }
        super.channelInactive(ctx);
    }
```

其实就是本channel关闭（channelInactive），我就关闭关联的另一条连接。同时注意！需要确保，另一条连接的所有消息已经成功写好。

原来的代码是这样：

```java
@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (remoteChannel != null && remoteChannel.isActive()) {
            remoteChannel.close().addListener(future1 -> {
                logger.info("返回0字节：browser关闭连接，因此关闭到webserver连接");
            });
        }
        super.channelInactive(ctx);
    }
```

区别就是少了个writeAndFLush（空buf）。少了这个的问题是：channel的关闭由另一个线程执行了，与netty一个线程管理一个channel的方法论不一致了。可能，这条channel还有数据没写完，就被别人关了。想想，酱爆还在洗澡，包租婆关了水，肯定要骂娘啊。

增加一个writeAndFLush（空buf），然后使用listener来关闭对端channel，其实就是，我通知你关闭，关闭有你自己执行（在执行前，你可以把澡洗好）

### 背压，注意不要写太快

其实就是要监测 channel还可不可写。如果channel已经不可写了，还拼命地去写，那么这些来不及写地内容就会存在内存里，而且因为netty使用直接内存作为ByteBuf，导致溢出地还是直接内存（堆外内存）看堆内存甚至还是正常的。

代理的场景是，读a，将读到的内容全部写到b。当b不能写了，就不读a了。

实现很简单：

```java
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        boolean canWrite = ctx.channel().isWritable();
        logger.warn(ctx.channel() + " 可写性：" + canWrite);
        //流量控制，不允许继续读
        remoteChannel.config().setAutoRead(canWrite);
        super.channelWritabilityChanged(ctx);
    }
```

通过setAutoRead来控制还读不读。这个问题的详细解读可以看[Netty直接内存溢出问题解决](http://arloor.com/posts/netty/netty-direct-memory-leak/)


## TODO

1. 增加多用户认证——优先级不高
2. 编写go客户端——优先级较高
3. 学习开发安卓的代理客户端——有难度
    - 研究了一下，安卓的代理客户端和这个项目相差很多，因为安卓提供的Vpnservice要传输的是ip数据报。
    - 初步思路见[安卓Vpn开发思路](http://arloor.com/posts/other/android-vpnservice-and-vpn-dev/)
