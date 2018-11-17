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

加密解密部分很简单：只用了按字节取反。。。。

这肯定不够，但是当前的代码结构也算比较方便加入新的加密解密方法。

## 加密开关

proxcommon模块下有一个`proxy.properties`文件，有如下内容：

```js
#代理是否加密，默认为false
crypto=false
```

## 运行

在非加密状态下，proxyserver可以单独运行（当然也可以配合proxyclient实现proxyclient——不加密——proxyserver）

在加密状态下，proxyclient和proxyserver需要同时运行，浏览器请求从proxyclient进入，经加密后传输到proxyserver，最后到webserver。

当然，proxyclient无论如何不可以单独运行



