package cpsc441_assignment3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deals with recieving ACK's in parallel with sending segments
 * @author brad
 */
public class Acknowledge implements Runnable {
    /**
     * Use this to terminate the thread safely
     */
    private boolean TERMINATE;
    DatagramSocket server;
    FastFtp parent;
    
    public Acknowledge(int serverPort, FastFtp parent)    {
        TERMINATE = false;
        try {
            server = new DatagramSocket(serverPort);
        } catch (SocketException ex) {
            // TODO deal with this exception
        }
        this.parent = parent;
    }
    
    @Override
    public void run() {
        while (!TERMINATE)  {
            byte[] data = new byte[Segment.MAX_PAYLOAD_SIZE];
            DatagramPacket pkt = new DatagramPacket(data, data.length);
            try {
                server.receive(pkt);
            } catch (IOException ex) {
                // TODO deal with this exception
            }
            parent.processACK(new Segment(pkt)); // TODO make sure it can't block here
        }
        cleanup();
    }
    
    /**
     * Call for thread to terminate safely
     */
    public void terminate()  {
        TERMINATE = true;
    } 
    
    /**
     * Call for thread to clean up properly
     */
    private void cleanup()  {
        server.close();
    }
}
