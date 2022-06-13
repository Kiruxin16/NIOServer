package Server;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;


public class NioServer {
    private ServerSocketChannel server;
    private Selector selector;




    public NioServer() throws IOException {
        server = ServerSocketChannel.open();
        selector= Selector.open();

        server.bind(new InetSocketAddress((8189)));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);

    }

    public void launch() throws IOException {
        while (server.isOpen()){
            selector.select();

            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iter = keys.iterator();
            while (iter.hasNext()){
                SelectionKey key = iter.next();
                if (key.isAcceptable()){
                    handleAccept();
                }
                if(key.isReadable()){
                    SocketChannel channel = (SocketChannel)key.channel();
                    ReadHandler.getReadHandler(channel).start();


                }
                iter.remove();
            }
        }
    }


    private void handleAccept() throws IOException {
        SocketChannel channel = server.accept();
        channel.configureBlocking(false);
        channel.register(selector,SelectionKey.OP_READ);

        channel.write(ByteBuffer.wrap("Connected\r\n".getBytes(StandardCharsets.UTF_8)));

    }
}

