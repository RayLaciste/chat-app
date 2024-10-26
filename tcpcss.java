import java.io.*;
import java.net.*;

class tcpcss {
    public static void main(String[] args) throws Exception {
    
        DatagramSocket serverSocket = new DatagramSocket(12345);
        System.out.println("UDP Server is running...");

        byte[] receiveData = new byte[1024];
        byte[] sendData = new byte[1024];

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            String sentence = new String(receivePacket.getData());
            System.out.println("FROM CLIENT: " + sentence);

            String modifiedSentence = sentence.toUpperCase();
            sendData = modifiedSentence.getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
            serverSocket.send(sendPacket);
        }
    }
}