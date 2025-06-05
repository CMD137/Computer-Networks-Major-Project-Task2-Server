import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Server {
    public static void main(String[] args) {
        try(DatagramSocket socket=new DatagramSocket(10087)){
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(
                    receiveData, receiveData.length);

            int seq=0;
            int ack=0;

            System.out.println("UDP server 已启动");
            socket.receive(receivePacket);

            //第一次握手，client的SYN
            socket.receive(receivePacket);

            //第二次握手，发送自己的SYN（实际上没用，只是为了模拟）和ACK

            //第三次握手，client的ack。连接建立成功。

            //数据传输阶段







        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

