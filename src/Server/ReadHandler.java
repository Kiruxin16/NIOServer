package Server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ReadHandler {


    private Path currientDir;
    private ByteBuffer buffer;
    private  SocketChannel channel;
    private static List<ReadHandler> handPool= new ArrayList<>();


    private ReadHandler(SocketChannel channel){
        buffer = ByteBuffer.allocate(1024);

        currientDir = Path.of(System.getProperty("user.home"));//Path.of(".");
        this.channel=channel;
    }

    public static ReadHandler getReadHandler(SocketChannel chnl){


            for (ReadHandler rh : handPool) {
                if (rh.channel == chnl) {
                    return rh;
                }
            }

        ReadHandler newHandler = new ReadHandler(chnl);
        handPool.add(newHandler);
        return newHandler;



    }

    public void start() throws IOException{
        StringBuilder msg = new StringBuilder();
        while (channel.isOpen()) {

            int read =channel.read(buffer);
            if(read<0){
                channel.close();
                return;
            }
            if(read==0){
                break;
            }
            buffer.flip();
            while (buffer.hasRemaining()){
                msg.append((char)buffer.get());
            }
            buffer.clear();
        }
        msg.delete(msg.length()-2,msg.length());
        String command = msg.toString();

        //Список файлов
        if(command.equals("ls")){
            Files.list(currientDir).forEach(path -> {
                byte[] message = String.format(path.getFileName()+"  ").getBytes(StandardCharsets.UTF_8);
                try {
                    channel.write(ByteBuffer.wrap(message));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            channel.write(ByteBuffer.wrap("\r\n".getBytes(StandardCharsets.UTF_8)));

        }else if(command.startsWith("cd ")) {//Смена директории
            Path newDir = currientDir.resolve(command.split(" ",2)[1]);
            if (!Files.exists(newDir)){
                channel.write(ByteBuffer.wrap(String.format(newDir.normalize().toString()+" is not founded\r\n").getBytes(StandardCharsets.UTF_8)));
            }else if(Files.isDirectory(newDir)){
                currientDir=newDir;
            }else{
                channel.write(ByteBuffer.wrap(String.format(newDir.toString()+" is not a directory\r\n").getBytes(StandardCharsets.UTF_8)));
            }

        } else if (command.startsWith("cat ")){
            Path filePath = currientDir.resolve(command.split(" ",2)[1]);
            if (!Files.exists(filePath)){
                channel.write(ByteBuffer.wrap(String.format(filePath.normalize().toString()+" is not founded\r\n").getBytes(StandardCharsets.UTF_8)));
            }else if(!Files.isDirectory(filePath)){
                channel.write(ByteBuffer.wrap("\r\n".getBytes(StandardCharsets.UTF_8)));
                for(String str :Files.readAllLines(filePath)) {
                    channel.write(ByteBuffer.wrap(String.format(str+"\r\n").getBytes(StandardCharsets.UTF_8)));
                }

            }else{
                channel.write(ByteBuffer.wrap(String.format(filePath.toString()+" is not a file\r\n").getBytes(StandardCharsets.UTF_8)));
            }
        } else{
            channel.write(ByteBuffer.wrap(String.format("unknown command "+command.split(" ")[0]+"\r\n").getBytes(StandardCharsets.UTF_8)));
        }
        channel.write(ByteBuffer.wrap(String.format("\r\n"+currientDir.normalize()+"->").getBytes(StandardCharsets.UTF_8)));



    }
}
