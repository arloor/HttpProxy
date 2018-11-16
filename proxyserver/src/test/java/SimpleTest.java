import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class SimpleTest {
    public static void main(String[] args){
        Bootstrap bootstrap=new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {

                        ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
                                byte[] buff=new byte[byteBuf.writerIndex()];
                                byteBuf.readBytes(buff);
                                System.out.println(new String(buff));
                                ctx.channel().close().addListener(future -> {
                                    System.out.println("================");
                                    System.out.println("关闭");
                                });
                            }

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                ctx.writeAndFlush(Unpooled.wrappedBuffer(("CONNECT hm.baidu.com:443 HTTP/1.1\r\n" +
                                        "Host: hm.baidu.com:443\r\n" +
                                        "Proxy-Connection: keep-alive\r\n" +
                                        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36\r\n\r\n").getBytes()));
                                super.channelActive(ctx);
                            }
                        });
                    }
                });
        ChannelFuture future = bootstrap.connect("127.0.0.1", 8080);
    }
}
