package com.arloor.forwardproxy.util;


import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.Optional;

public class OsHelper {
    private static final Logger logger = LoggerFactory.getLogger(OsHelper.class);
    private static final OS os = parseOS();


    public static Class<? extends ServerSocketChannel> serverSocketChannelClazz() {
        return os.serverSocketChannelClazz;
    }

    public static Class<? extends SocketChannel> socketChannelClazz() {
        return os.socketChannelClazz;
    }

    public static EventLoopGroup buildEventLoopGroup(int num) {
        return os.buildEventLoopGroup(num);
    }

    public static boolean isUnix() {
        return os.equals(OS.Unix);
    }

    public static boolean isWindows() {
        return os.equals(OS.Windows);
    }

    public static boolean isMac() {
        return os.equals(OS.MacOS);
    }

    private enum OS {
        MacOS("mac", KQueueServerSocketChannel.class, KQueueSocketChannel.class) {
            @Override
            EventLoopGroup buildEventLoopGroup(int num) {
                return new KQueueEventLoopGroup(num);
            }
        },
        Unix("unix", EpollServerSocketChannel.class, EpollSocketChannel.class) {
            @Override
            EventLoopGroup buildEventLoopGroup(int num) {
                return new EpollEventLoopGroup(num);
            }
        },
        Windows("windows", NioServerSocketChannel.class, NioSocketChannel.class) {
            @Override
            EventLoopGroup buildEventLoopGroup(int num) {
                return new NioEventLoopGroup(num);
            }
        },
        Other("other", NioServerSocketChannel.class, NioSocketChannel.class) {
            @Override
            EventLoopGroup buildEventLoopGroup(int num) {
                return new NioEventLoopGroup(num);
            }
        };

        String name;
        Class<? extends ServerSocketChannel> serverSocketChannelClazz;
        Class<? extends SocketChannel> socketChannelClazz;

        abstract EventLoopGroup buildEventLoopGroup(int num);

        OS(String name, Class<? extends ServerSocketChannel> serverSocketChannelClass, Class<? extends SocketChannel> socketChannelClass) {
            this.name = name;
            this.serverSocketChannelClazz = serverSocketChannelClass;
            this.socketChannelClazz = socketChannelClass;
        }
    }

    private static OsHelper.OS parseOS() {
        String osName = System.getProperty("os.name");
        logger.info("当前系统为： " + osName);
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pid = name.split("@")[0];
        logger.info("该进程pid= " + pid);
        osName = Optional.ofNullable(osName).orElse("").toLowerCase();
        if ((osName.contains("win"))) {
            return OS.Windows;
        } else if (osName.contains("mac")) {
            return OS.MacOS;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.indexOf("aix") > 0) {
            return OS.Unix;
        } else {
            return OS.Other;
        }
    }
}

