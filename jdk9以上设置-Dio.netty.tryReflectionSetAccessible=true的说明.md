## jdk9以上设置-Dio.netty.tryReflectionSetAccessible=true的说明

要统计netty直接内存使用量，实际使用的是netty中PlatformDependent类的`DIRECT_MEMORY_COUNTER`变量。

netty在初始化这个变量前，会检查时候能反射拿到DirectByteBuffer的构造方法。

在jdk9以上，拿构造方法被认为是`illegal reflective access`会看到这样的警告信息：

```shell
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by io.netty.util.internal.ReflectionUtil (file:/C:/Users/arloor/.m2/repository/io/netty/netty-all/4.1.53.Final/netty-all-4.1.53.Final.jar) to constructor java.nio.DirectByteBuffer(long,int)
WARNING: Please consider reporting this to the maintainers of io.netty.util.internal.ReflectionUtil
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
```

所以有人就在github上提issue了：[https://github.com/netty/netty/pull/7650](https://github.com/netty/netty/pull/7650)

netty的解决方案是：默认关闭这个反射拿构造方法的操作，相关代码：

```java
    // PlatformDependent0.java
private static boolean explicitTryReflectionSetAccessible0(){
        // we disable reflective access
        return SystemPropertyUtil.getBoolean("io.netty.tryReflectionSetAccessible",javaVersion()< 9);
        }
```

我们为了统计直接内存使用量，所以需要把这个打开
