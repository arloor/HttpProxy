package com.arloor.forwardproxy;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.Optional;
import java.util.function.Function;

public class OsHelper {
    private static final Logger logger = LoggerFactory.getLogger(OsHelper.class);
    private static final OS os =parseOS();


    public static Class serverSocketChannelClazz(){
        return os.serverSocketChannelClazz;
    }

    public static Class socketChannelClazz(){
        return os.socketChannelClazz;
    }

    public static EventLoopGroup buildEventLoopGroup(int num){
        return os.eventLoopBuilder.apply(num);
    }

    public static boolean isUnix(){
        return os.equals(OS.Unix);
    }



    private enum OS {
        MacOS("mac", KQueueServerSocketChannel.class, KQueueSocketChannel.class, threadNum -> new KQueueEventLoopGroup(threadNum)),
        Unix("unix", EpollServerSocketChannel.class, EpollSocketChannel.class, threadNum -> new EpollEventLoopGroup(threadNum)),
        Windows("windows", NioServerSocketChannel.class, NioSocketChannel.class, threadNum -> new NioEventLoopGroup(threadNum)),
        Other("other", NioServerSocketChannel.class, NioSocketChannel.class, threadNum -> new NioEventLoopGroup(threadNum));

        String name;
        Class serverSocketChannelClazz;
        Class socketChannelClazz;
        Function<Integer, EventLoopGroup> eventLoopBuilder;

        OS(String name, Class serverSocketChannelClass, Class socketChannelClass, Function<Integer, EventLoopGroup> integerEventLoopGroupFunction) {
            this.name = name;
            this.serverSocketChannelClazz = serverSocketChannelClass;
            this.socketChannelClazz = socketChannelClass;
            this.eventLoopBuilder = integerEventLoopGroupFunction;
        }
    }

    private static OsHelper.OS parseOS() {
        String osName = System.getProperty("os.name");
        logger.info("当前系统为： "+osName);
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pid = name.split("@")[0];
        logger.info("该进程pid= " + pid);
        osName = Optional.ofNullable(osName).orElse("").toLowerCase();
        if ((osName.indexOf("win") >= 0)) {
            return OS.Windows;
        } else if (osName.indexOf("mac") >= 0) {
            return OS.MacOS;
        } else if (osName.indexOf("nix") >= 0 || osName.indexOf("nux") >= 0 || osName.indexOf("aix") > 0) {
            return OS.Unix;
        } else {
            return OS.Other;
        }
    }
}
