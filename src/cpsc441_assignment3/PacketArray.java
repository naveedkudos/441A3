/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cpsc441_assignment3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * <h1>
 * Packet Array
 * </h1>
 * <p>
 * Contains an array of Datagram packets for the purpose of sending them to a UDP server.
 * Minimal overhead, exceptions thrown in methods are merely thrown by the methods instead of dealt
 * with. 
 * </p>
 * @author brad
 */
public class PacketArray {
    private String serverName;
    private int portNum;
    private DatagramPacket[] packets;
    private int size;
    private DatagramSocket socket;
    
    public PacketArray(String server, int port, int size) throws SocketException    {
        packets = new DatagramPacket[size];
        this.size = size;
        serverName = server;
        portNum = port;
        socket = new DatagramSocket();
    }
    
    protected void send(int index) throws IOException   {
        socket.send(packets[index]);
    }
    
    public void set(byte[] buffer, int count, int index) throws UnknownHostException   {
        packets[index] = new DatagramPacket(buffer, count, InetAddress.getByName(serverName), portNum);
    }
    
    /**
     * Used to clear the contents of the datagram array
     */
    public void clear()     {
        packets = new DatagramPacket[size];
    }
    
    /**
     * close the socket and make the array null for garbage collector
     */
    public void shutdown()    {
        socket.close();
        
    }
}
