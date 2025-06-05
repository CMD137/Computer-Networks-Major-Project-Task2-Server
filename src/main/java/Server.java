import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Server {
    public static void main(String[] args) {
        try(DatagramSocket socket=new DatagramSocket(10087)){
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            int serverSeq = 0;
            int serverAck;

            System.out.println("UDP server 已启动");

            //第一次握手，client的SYN
            socket.receive(receivePacket);
            Message synMsg = Message.deserialize(receivePacket.getData());
            System.out.println("收到第一次握手: " + synMsg);

            if (synMsg.getType() != 0) {
                System.out.println("错误：预期SYN消息");
                return;
            }


            //第二次握手，发送自己的SYN（实际上没用，只是为了模拟）和ACK
            serverAck = synMsg.getSeqNum(); // 按照TCP：ACK应该是客户端seq+1，但是为了和client的GBN的累计确认统一，ack=seq
            Message synAckMsg = new Message((short) 2, serverSeq, serverAck);
            byte[] synAckBytes = synAckMsg.serialize();

            InetAddress clientAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();
            DatagramPacket synAckPacket = new DatagramPacket(synAckBytes, synAckBytes.length, clientAddress, clientPort);
            socket.send(synAckPacket);

            System.out.println("发送第二次握手: " + synAckMsg);


            //第三次握手，client的ack。连接建立成功。
            socket.receive(receivePacket);
            Message ackMsg = Message.deserialize(receivePacket.getData());
            System.out.println("收到第三次握手: " + ackMsg);

            if (ackMsg.getType() != 1) {
                System.out.println("错误：预期ACK消息");
                return;
            }


            //数据传输阶段
            System.out.println("连接建立成功，进入数据传输阶段");
            while (true){
                socket.receive(receivePacket);

                //模拟丢包  30%丢包率
                if (Math.random() < 0.3) {
                    continue;
                }

                Message msg = Message.deserialize(receivePacket.getData());
                System.out.println("收到数据包: " + msg);
                if (msg.getType() != 3) {
                    System.out.println("错误：预期PSH+ACK消息");
                    break;
                }

                serverAck = msg.getSeqNum();
                Message ack = new Message((short) 1, serverSeq, serverAck);
                byte[] ackBytes = ack.serialize();
                DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, clientAddress, clientPort);
                socket.send(ackPacket);
                System.out.println("发送ACK: " + ack);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

