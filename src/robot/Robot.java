package robot;
Фируз лох!
import java.io.*;
import java.net.*;
import java.util.HashMap;


public class Robot {
    public static void main(String[] args){
        Client cl = new Client();
        cl.cli_main();
    }
}

class Client {
    FileOutputStream fout;
    DatagramSocket socket;
    DatagramPacket packet;
    InetAddress addr;
    int port;
    byte[] data;
    byte[] id_conn;
    int currMax;
    HashMap<Integer, byte[]> packets_queue;
    byte[] photoBytes;
    public Client(){
        photoBytes = new byte[0];
        fout = null;
        port = 4000;
        data = new byte[264];
        id_conn = new byte[9];
        currMax = 0;
        packets_queue = new HashMap<>();
        try{
            addr = InetAddress.getByName("baryk.fit.cvut.cz");
            socket = new DatagramSocket();
        }
        catch(IOException ex){
        } 
    }
    boolean Connect()throws IOException{
        byte[] tmp = new byte[10];
        tmp[8] = 4;
        tmp[9] = 1;
        packet = new DatagramPacket(tmp, tmp.length, addr, port);
        socket.send(packet);
        socket.setSoTimeout(100);
        try{
            packet = new DatagramPacket(data, data.length);
            socket.receive(packet);
            data = packet.getData();
            return data[8] == 4;
        }catch(SocketTimeoutException e){
            return false;
        }
    }
    boolean Disconnect()throws IOException{
        byte[] tmp = new byte[9];
        System.arraycopy(id_conn, 0, tmp, 0, 4);
        tmp[8] = 2;
        tmp[6] = (byte)(currMax >> 8);
        tmp[7] = (byte)(currMax);
        packet = new DatagramPacket(tmp, tmp.length, addr, port);
        socket.send(packet);
        socket.setSoTimeout(1000);
        try{
            packet = new DatagramPacket(data, data.length);
            socket.receive(packet);
            data = packet.getData();
            return data[8] == 2;
        }catch(SocketTimeoutException e){
            return true;
        }
    }
    void SendError()throws IOException{
        byte[] tmp = new byte[9];
        System.arraycopy(id_conn, 0, tmp, 0, 4);
        tmp[8] = 1;
        tmp[6] = (byte)(currMax >> 8);
        tmp[7] = (byte)(currMax);
        packet = new DatagramPacket(tmp, tmp.length, addr, port);
        socket.send(packet);
    }
    boolean CorrectIDConnection(){
        for(int i = 0; i < 4; ++i)
            if(id_conn[i] != data[i])
                return false;
        return true;
    }
    void SavePhoto()throws IOException{
        fout = new FileOutputStream("/home/firuz/Dropbox/Study/PSI/UDP-client(task_2)/Taras/downloaded.png");
        
        fout.write(photoBytes);
        fout.close();
    }
    void AppendToArray(byte[] arr){
        int l = arr.length-9;
        byte [] tmp = new byte [photoBytes.length+l];
        System.arraycopy(photoBytes, 0, tmp, 0, photoBytes.length);
        
        int j = photoBytes.length;
        for(int i = 9; i < l+9; ++i)
            tmp[j+i-9] = arr[i];
        photoBytes = tmp;
    }
    public void cli_main(){
        try{
            while(Connect() == false){}
            System.arraycopy(data, 0, id_conn, 0, 4);
            socket.setSoTimeout(0);
            packet = new DatagramPacket(id_conn, id_conn.length, addr, port);
            socket.send(packet);
            
            while(true){
                packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                data = packet.getData();
                if(CorrectIDConnection() == false)
                    continue;
                if(data[8] == 2){
//                    while(Disconnect() == false){}
                    Disconnect();
                    SavePhoto();
                    break;
                }
//                if(data[8] == 1){
//                    SendError();
//                    break;
//                }
                
                int svrSeq = ConvertBytesToInt(data[4], data[5]);
                
                if(svrSeq == currMax && data[8] == 0){
                    currMax += packet.getLength()-9;
                    if(currMax > 65535)
                        currMax -= 65536;
                    AppendToArray(data);
                    while(packets_queue.containsKey(currMax)){
                        int tmp = currMax;
                        byte[] tmp_data = packets_queue.get(tmp);
                        currMax += tmp_data.length-9;
                        if(currMax > 65535)
                            currMax -= 65536;
                        AppendToArray(tmp_data);
//                        System.out.println("currMax: " + currMax);
                        System.out.println("getting from map of previous packets" + "seq: " + tmp);
                        packets_queue.remove(tmp);
                        
                    }
                }
                else{// if(svrSeq > currMax){
                    System.out.println("out of order packet, but i will remeber it." + " seq: " + svrSeq);
                    byte[] tmp = new byte[packet.getLength()];
                    System.arraycopy(data, 0, tmp, 0, tmp.length);
                    packets_queue.put(svrSeq, tmp);
                }
                
                id_conn[6] = (byte)(currMax >> 8);
                id_conn[7] = (byte)(currMax);
                packet = new DatagramPacket(id_conn, id_conn.length, addr, port);
                socket.send(packet);
                System.out.println("ack= " + currMax + " flags= " + id_conn[8]);
            }
            
            socket.close();
        }
        catch(IOException e){}
        
    }
    int ConvertBytesToInt(byte high, byte low){
        String s = String.format("%8s", Integer.toBinaryString(high & 0xFF)).replace(' ', '0');
        String s1 = String.format("%8s", Integer.toBinaryString(low & 0xFF)).replace(' ', '0');
        int toSend = 0;
        
        s += s1;
        for (int i = 15; i >= 0; i--)
            if(s.charAt(i) == '1')
                toSend += (int)Math.pow(2, 15-i);
        return toSend;
    }
}
