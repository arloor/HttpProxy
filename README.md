# Proxynew 重新设计的基于netty的http代理

项目地址[Proxynew](https://github.com/arloor/proxynew)

上一个[proxyme](https://github.com/arloor/proxyme)项目使用java NIO实现了代理功能。

这个项目是一个基于netty的http代理。两个代理一起看，估计对java网络编程学习有一点帮助吧。

## 目标

首先是学习netty，其次是完成上一个项目没有实现的：

`浏览器——代理客户端——加密/解密——代理服务器——web服务器`

这一套东西应该是可以用来完成不可描述的目的。

## 目前进度

整个功能：代理客户端、代理服务器、加密解密都已经实现了功能。

其中代理客户端和代理服务器可以说比较完善，应该没啥问题。

加密解密现在支持字节取反和AES128加密，都十分稳定

## 加密开关

proxcommon模块下有一个`proxy.properties`文件，有如下内容：

```shell
#是否加密 默认为false
crypto=true
#加密类型 AES|SIMPLE  若不能识别输入的type自动设为SIMPLE
crypto.type=AES
#当加密类型不为SIMPLE时使用
crypto.key=你想用我的代理吗？？？
```



## 在vps上运行情况

可以翻墙，且跑满家里带宽。经过一个多月的使用，可以说十分稳定了。

```jshelllanguage
2018-11-18 08:44:54.446 [nioEventLoopGroup-4-1] INFO  c.a.p.r.DefaultHttpMessageDecoderAdapter - 处理请求 HttpRequest{body='null', method='CONNECT', host='adservice.google.co.jp', port=443, path=adservice.google.co.jp:443}
2018-11-18 08:44:54.457 [nioEventLoopGroup-2-1] INFO  c.a.p.proxyconnection.ProxyConnectionHandler - 连接成功: adservice.google.co.jp:443

2018-11-18 08:44:54.630 [nioEventLoopGroup-4-1] INFO  c.a.p.r.DefaultHttpMessageDecoderAdapter - 处理请求 HttpRequest{body='null', method='CONNECT', host='ogs.google.com', port=443, path=ogs.google.com:443}
2018-11-18 08:44:54.650 [nioEventLoopGroup-2-2] INFO  c.a.p.proxyconnection.ProxyConnectionHandler - 连接成功: ogs.google.com:443

2018-11-18 08:45:45.016 [nioEventLoopGroup-4-1] INFO  c.a.p.r.DefaultHttpMessageDecoderAdapter - 处理请求 HttpRequest{body='null', method='CONNECT', host='r1---sn-a5meknek.googlevideo.com', port=443, path=r1---sn-a5meknek.googlevideo.com:443}
2018-11-18 08:45:45.139 [nioEventLoopGroup-2-1] INFO  c.a.p.proxyconnection.ProxyConnectionHandler - 连接成功: r1---sn-a5meknek.googlevideo.com:443

2018-11-18 08:45:54.703 [nioEventLoopGroup-4-1] INFO  c.a.p.r.DefaultHttpMessageDecoderAdapter - 处理请求 HttpRequest{body='null', method='CONNECT', host='www.google.co.jp', port=443, path=www.google.co.jp:443}
2018-11-18 08:45:54.754 [nioEventLoopGroup-2-1] INFO  c.a.p.proxyconnection.ProxyConnectionHandler - 连接成功: www.google.co.jp:443
```



