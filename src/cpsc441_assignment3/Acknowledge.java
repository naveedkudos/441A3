package cpsc441_assignment3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Deals with receiving ACK's in parallel with sending segments
 * @author brad
 */
public class Acknowledge extends Thread {
    /**
     * Use this to terminate the thread safely
     */
    private boolean TERMINATE;
    DatagramSocket server;
    FastFtp parent;
    boolean DEBUG = false;
    
    public Acknowledge(DatagramSocket serverSocket, FastFtp parent)    {
        System.out.println("Starting the ACK Thread");
        TERMINATE = false;
        server = serverSocket;
        this.parent = parent;
    }
    
    @Override
    public void run() {
        while (!TERMINATE)  {
            byte[] data = new byte[Segment.MAX_PAYLOAD_SIZE];
            DatagramPacket pkt = new DatagramPacket(data, data.length);
            try {
                server.receive(pkt);
                if (DEBUG)
                    System.out.println("Received a packet from the server");
            } catch (SocketException ex)    {
                TERMINATE = true;
                break;
            } catch (IOException ex) {
                return;
            }
            parent.processACK(new Segment(pkt));
        }
        System.out.println("Terminating ACK thread");
    }
    
    /**
     * Call for thread to terminate safely
     */
    public void terminate()  {
        TERMINATE = true;
    } 
}
