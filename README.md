#Proxynew 重新设计的基于netty的http代理

上一个[proxyme](https://github.com/arloor/proxyme)项目使用java NIO实现了代理功能，对java NIO的api有了一些认识。

也发现使用java NIO编写程序确实不够舒服。

这个项目是一个基于netty的http代理。

## 遇到的坑

java的正则表达式在字符串很长的情况下会出现stackovrflow。

嗯，很少遇到这个问题啊。所以不要用正则表达式去匹配http请求！

## http代理原理

可以参见[proxyme](https://github.com/arloor/proxyme)

## 通过这个小项目可以学习什么。。。

算是一个netty的demo吧

## 为什么不使用netty的HttpRequestDecoder

因为一开始我不知道有这个，知道有这个之后，却同时发现不适用。因为connect请求之后传输的请求都不能通过这个解析。

## 打包

mvn clean package