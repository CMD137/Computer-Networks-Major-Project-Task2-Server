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
                //模拟包在网络中传输花费的时间
                //然后发现由于网络层不保证有序性，单纯等待操作就能达到乱序的效果，所以就不需要下面按几率丟包了。
                Thread.sleep(100);


                socket.receive(receivePacket);

                /*
                //模拟丢包  50%丢包率
                if (Math.random() < 0.5) {
                    System.out.println("模拟丢包");
                    //continue;
                }
                */


                //直接getData()在后面加上缓冲区的无效字符
                // Message msg = Message.deserialize(receivePacket.getData());
                byte[] raw = receivePacket.getData();
                int length = receivePacket.getLength();  // 实际有效长度
                byte[] actualData = new byte[length];
                System.arraycopy(raw, 0, actualData, 0, length);

                Message msg = Message.deserialize(actualData);


                System.out.println("收到数据包: " + msg);
                if (msg.getType() != 3) {
                    System.out.println("错误：预期PSH+ACK消息");
                    break;
                }

                //期望seq应为serverACK+1；
                if(msg.getSeqNum()==serverAck+1){
                    serverAck +=msg.getData().length();
                    Message ack = new Message((short) 1, serverSeq, serverAck);
                    byte[] ackBytes = ack.serialize();
                    DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, clientAddress, clientPort);
                    socket.send(ackPacket);
                    System.out.println("发送ACK: " + ack);
                }else {
                    System.out.println("收到失序包,直接丢弃！\t期望seq："+(serverAck+1)+"得到seq："+msg.getSeqNum());
                    //只设计了超时重传，所以不重发
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

