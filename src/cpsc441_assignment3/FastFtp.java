
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FastFtp Class
 *
 * FastFtp implements a basic FTP application based on UDP data transmission.
 * The main method is send() which takes a file name as input argument and send
 * the file to the specified destination host.
 *
 */
public class FastFtp {
    int windowSize;
    int timeout;
    private int segmentID;
    public enum PacketStates { SEND, TIMEOUT, RESEND, WAIT }; 

    /**
     * Constructor to initialize the program
     *
     * @param windowSize	Size of the window for Go-Back_N (in segments)
     * @param rtoTimer	The time-out interval for the retransmission timer (in
     * milli-seconds)
     */
    public FastFtp(int windowSize, int rtoTimer) {
        //TODO complete implementation of FastFtp
        this.windowSize = windowSize;
        timeout = rtoTimer;
        segmentID = 0;
    }

    /**
     * <h1>Sends the specified file to the specified destination host:</h1><p> 1. send file
     * name and receiver server confirmation over TCP</p><p> 2. send file segment by
     * segment over UDP 3. send end of transmission over TCP</p><p> 3. clean up</p>
     *
     * @param serverName	Name of the remote server
     * @param serverPort	Port number of the remote server
     * @param fileName	Name of the file to be transferred to the remote server
     */
    public void send(String serverName, int serverPort, String fileName) {
        ///////////////////////////////////////////////////////////////////////////////////
        //  Open up TCP socket to complete handshake with the server before transmission //
        ///////////////////////////////////////////////////////////////////////////////////
        
        Socket socket_TCP;
        try {
            socket_TCP = new Socket(InetAddress.getByName(serverName), serverPort);
        } catch (UnknownHostException ex) {
            //TODO deal with this exception
        } catch (IOException ex) {
            //TODO deal with this exception
        }
        
        
    }
    
     /**<h1>Process Send</h1>
     * <ol>
     * <li>Send segment to the UDP socket</li>
     * <li>Add segment to the transmission queue txQueue</li>
     * <li>if txQueue.size() == 1, start the timer</li>
     * </ol>
     * @param ack 
     */
    public synchronized void processSend(Segment seg)   {
        
    }
    
    /**<h1>Process ACK</h1>
     * <ul>
     * <li>If ACK not in the current window, do nothing</li>
     * <li>Otherwise:</li>
     * <ul> <li>Cancel the timer</li>
     *      <li><p>
     *          while(txQueue.element().getSeqNum() &lt ack.getSeqNum())
     *          </p><p>
     *          &#9;txQueue.remove();
     *          </p></li><li>
     *          if not txQueue.isEmpty(), start timer
     *      </li>
     * </ul>
     * </ul>
     * @param ack 
     */
    public synchronized void processACK(Segment ack)    {
        
    }
    
    /**<h1>Process Timeout</h1>
     * <ol>
     * <li>Get the list of all pending segments by calling txQueue.toArray()</li>
     * <li>Go through the list and send all segments to the UDP socket</li>
     * <li>If not txQueue.isEmpty(), start the timer</li>
     * </ol>
     */
    public synchronized void processTimeout()   {
        
    }
    
    

    /**
     * A simple test driver
     *
     */
    public static void main(String[] args) {
        int windowSize = 10; //segments
        int timeout = 100; // milli-seconds

        String serverName = "localhost";
        String fileName = "";
        int serverPort = 0;

        // check for command line arguments
        if (args.length == 3) {
            // either privide 3 paramaters
            serverName = args[0];
            serverPort = Integer.parseInt(args[1]);
            fileName = args[2];
        } else if (args.length == 2) {
            // or just server port and file name
            serverPort = Integer.parseInt(args[0]);
            fileName = args[1];
        } else {
            System.out.println("wrong number of arguments, try agaon.");
            System.out.println("usage: java FastFtp server port file");
            System.exit(0);
        }

        FastFtp ftp = new FastFtp(windowSize, timeout);

        System.out.printf("sending file \'%s\' to server...\n", fileName);
        ftp.send(serverName, serverPort, fileName);
        System.out.println("file transfer completed.");
    }

}
