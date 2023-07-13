## 堆外内存代码监控

JDK 默认采用 Cleaner 回收释放 DirectByteBuffer，Cleaner 继承于 PhantomReference，因为依赖 GC 进行处理，所以回收的时间是不可控的。对于 hasCleaner 的 DirectByteBuffer，Java 提供了一系列不同类型的 MXBean 用于获取 JVM 进程线程、内存等监控指标，代码实现如下：来自[Netty 在项目开发中的一些最佳实践](https://learn.lianglianglee.com/%E4%B8%93%E6%A0%8F/Netty%20%E6%A0%B8%E5%BF%83%E5%8E%9F%E7%90%86%E5%89%96%E6%9E%90%E4%B8%8E%20RPC%20%E5%AE%9E%E8%B7%B5-%E5%AE%8C/30%20%20%E5%AE%9E%E8%B7%B5%E6%80%BB%E7%BB%93%EF%BC%9ANetty%20%E5%9C%A8%E9%A1%B9%E7%9B%AE%E5%BC%80%E5%8F%91%E4%B8%AD%E7%9A%84%E4%B8%80%E4%BA%9B%E6%9C%80%E4%BD%B3%E5%AE%9E%E8%B7%B5.md)

```java
BufferPoolMXBean directBufferPoolMXBean = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class).get(0);

LOGGER.info("DirectBuffer count: {}, MemoryUsed: {} K", directBufferPoolMXBean.getCount(), directBufferPoolMXBean.getMemoryUsed()/1024);
```

对于 Netty 中 noCleaner 的 DirectByteBuffer，直接通过 PlatformDependent.usedDirectMemory() 读取即可。但是随着JDK的发展，在新JDK中要加些jvm参数才能获取到。

## jdk9以上设置 `-Dio.netty.tryReflectionSetAccessible=true` 的说明

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

## jdk16以上设置 `--add-opens java.base/java.nio=ALL-UNNAMED` 的说明

如果不设置，会在反射获取 `DirectByteBuffer` 的构造函数 `private java.nio.DirectByteBuffer(long,int)` 的时候抛出下面的异常，导致无法获取：

```shell
java.lang.reflect.InaccessibleObjectException: 
Unable to make private java.nio.DirectByteBuffer(long,int) accessible: 
module java.base does not "opens java.nio" to unnamed module @5a4aa2f2
```

![](/directByteBufferConstructor.png)

相关的一些issue：[renaissance-benchmarks的issue](https://github.com/renaissance-benchmarks/renaissance/issues/241)
