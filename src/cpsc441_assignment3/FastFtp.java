package cpsc441_assignment3;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Timer;

/**
 * FastFtp Class
 *
 * FastFtp implements a basic FTP application based on UDP data transmission.
 * The main method is send() which takes a file name as input argument and send
 * the file to the specified destination host.
 *
 */
public class FastFtp {
    int timeout;
    private int segmentID;
    TxQueue window;
    DatagramSocket socket_UDP;
    Timer timer;
    String serverName;
    int serverPort;
    
    boolean DEBUG = false;

    /**
     * Constructor to initialize the program
     *
     * @param windowSize	Size of the window for Go-Back_N (in segments)
     * @param rtoTimer	The time-out interval for the retransmission timer (in
     * milli-seconds)
     */
    public FastFtp(int windowSize, int rtoTimer) {
        window = new TxQueue(windowSize);
        timeout = rtoTimer;
        segmentID = 0;
        socket_UDP = null;
    }

    /**
     * <h1>Sends the specified file to the specified destination host:</h1><p> 1. send file
     * name and receiver server confirmation over TCP</p><p> 2. send file segment by
     * segment over UDP </p><p>3. send end of transmission over TCP</p><p> 4. clean up</p>
     *
     * @param serverName	Name of the remote server
     * @param serverPort	Port number of the remote server
     * @param fileName	Name of the file to be transferred to the remote server
     */
    public void send(String serverName, int serverPort, String fileName) {
        ///////////////////////////////////////////////////////////////////////////////////
        //  Open up TCP socket to complete handshake with the server before transmission //
        ///////////////////////////////////////////////////////////////////////////////////
        this.serverName = serverName;
        this.serverPort = serverPort;
        if (DEBUG)  
            System.out.println("Attempting to send file " + fileName + " to server: " + serverName + " " + serverPort);
        // Check to ensure file exists
        
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File does not exist");
            return;
        }
        
        
        Socket socket_TCP = null;
        DataOutputStream handshakeOut = null;
        DataInputStream handshakeIn = null;
        byte response = 1;
        try {
            socket_TCP = new Socket(InetAddress.getByName(serverName), serverPort);
            handshakeOut = new DataOutputStream(socket_TCP.getOutputStream());
            handshakeOut.writeUTF(fileName);
            handshakeIn = new DataInputStream(socket_TCP.getInputStream());
            response = handshakeIn.readByte();
        } catch (UnknownHostException ex) {
            System.out.println("Host cannot be found");
            try {
                socket_TCP.close();
            } catch (IOException ex1) {
                // Acceptable
            }
            return;
        } catch (IOException ex) {
            System.out.println("An error has occurred");
            try {
                handshakeOut.writeByte(0);
                socket_TCP.close();
            } catch (IOException ex1) {
            } catch (NullPointerException ex1)  {}
            return;
        }
        if (response != 0)  {
            System.out.println("No server response, terminating attempt");
            try {
                handshakeOut.writeByte(0);
                socket_TCP.close();
            } catch (IOException ex) {
                // Acceptable exception
            }
            return;
        }
        try {
            socket_UDP = new DatagramSocket(socket_TCP.getLocalPort());
        } catch (SocketException ex) {
            ex.printStackTrace();
            try {
                handshakeOut.writeByte(0);
                socket_TCP.close();
                socket_UDP.close();
            } catch (IOException ex1) {
                // Acceptable exception
            }
            return;
        } 
        
        
        
        ///////////////////////////////////////////////////////////////////////////////////
        //                          Start ACK recieving thread                           //
        ///////////////////////////////////////////////////////////////////////////////////
        
        
        Acknowledge ackThread = null;
        ackThread = new Acknowledge(socket_UDP, this);
        ackThread.start();  
        
        
        ///////////////////////////////////////////////////////////////////////////////////
        //                Create dataInputStream from file                               //
        ///////////////////////////////////////////////////////////////////////////////////
        DataInputStream fileInput = null;
        try {
            fileInput = new DataInputStream(new FileInputStream(file));
            int count = -1;
            byte[] buffer = new byte[Segment.MAX_PAYLOAD_SIZE];
            while ((count = fileInput.read(buffer)) != -1) {

        
            ///////////////////////////////////////////////////////////////////////////////////
            //                Create segment with next sequence number                       //
            ///////////////////////////////////////////////////////////////////////////////////
                Segment segment = new Segment(segmentID, Arrays.copyOf(buffer, count));
                segmentID++;
                
                
            ///////////////////////////////////////////////////////////////////////////////////
            //              Yield to other threads if queue is full                          //
            ///////////////////////////////////////////////////////////////////////////////////
                while (window.isFull())     {
                    Thread.yield();
                }
        
            ///////////////////////////////////////////////////////////////////////////////////
            //                          Send the segment                                     //
            ///////////////////////////////////////////////////////////////////////////////////
                processSend(segment);
            }
        } catch (FileNotFoundException ex) {
            // Should never throw this exception (already been checked for)
            try {
                handshakeOut.writeByte(0);
            } catch (IOException ex1) {
                System.out.println("Could not terminate transfer");
            }
            try {
                socket_TCP.close();
                socket_UDP.close();
                ackThread.terminate();
                ackThread.interrupt();
            } catch (IOException ex1) {
                System.out.println("Could not close socket(s)");
            }
            return;
        } catch (IOException ex) {
            ex.printStackTrace();
            ackThread.terminate();
            ackThread.interrupt();
            try {
                handshakeOut.writeByte(0);
                socket_TCP.close();
                socket_UDP.close();
            } catch (IOException ex1) {
                //Acceptable exception
            }
        } finally   {
            try {
                fileInput.close();
            } catch (IOException ex) {
                // This exception is permitted
            }
        }

        
        
        ///////////////////////////////////////////////////////////////////////////////////
        //       Wait until queue is empty, send end of transmission message             //
        ///////////////////////////////////////////////////////////////////////////////////
        while (!window.isEmpty())   {
            Thread.yield();
        }
        try {
            handshakeOut.writeByte(0);
        } catch (IOException ex) {
            System.out.println("could not terminate transmission with server");
            System.exit(-1);
        }
        ///////////////////////////////////////////////////////////////////////////////////
        //                              Clean Up                                         //
        ///////////////////////////////////////////////////////////////////////////////////
        try {
            socket_TCP.close();
            socket_UDP.close();
        } catch (IOException ex) {
            // This is permitted
        }
        ackThread.terminate();
        ackThread.interrupt();
        timer.cancel();
    }
    
     /**<h1>Process Send</h1>
     * <ol>
     * <li>Send segment to the UDP socket</li>
     * <li>Add segment to the transmission queue txQueue</li>
     * <li>if txQueue.size() == 1, start the timer</li>
     * </ol>
     * @param seg the segment to send
     */
    public synchronized void processSend(Segment seg) {
        if (DEBUG)
            System.out.println("Sending segment " + seg.getSeqNum());
        // Send the segment
        DatagramPacket pkt = null;
        try {
            pkt = new DatagramPacket(seg.getBytes(), (Segment.MAX_SEGMENT_SIZE - (Segment.MAX_PAYLOAD_SIZE - seg.getLength())), InetAddress.getByName(serverName), serverPort);
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }
        try {
            socket_UDP.send(pkt);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
        
        // Add the segment to the queue
        try {
            window.add(seg);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        
        if (window.size() == 1)     {
            timer = new Timer(true);
            timer.schedule(new TimerHandler(this), timeout);
        }
    }
    
    public synchronized void reSend(Segment seg)    {
        if (DEBUG)
            System.out.println("Resending segment " + seg.getSeqNum());
         // Send the segment
        DatagramPacket pkt = null;
        try {
            pkt = new DatagramPacket(seg.getBytes(), (Segment.MAX_SEGMENT_SIZE - (Segment.MAX_PAYLOAD_SIZE - seg.getLength())), InetAddress.getByName(serverName), serverPort);
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }
        try {
            socket_UDP.send(pkt);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
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
     * @param ack the received ack
     */
    public synchronized void processACK(Segment ack)    {
        if (DEBUG)
            System.out.println("Processing ACK # " + ack.getSeqNum());
        
        if (ack.getSeqNum() > window.element().getSeqNum() && (ack.getSeqNum() <= (window.element().getSeqNum() + window.size())))  {
            while (window.element().getSeqNum() < ack.getSeqNum()) {
                try {
                    if (DEBUG)
                        System.out.println("Removing segment " + window.element().getSeqNum() + " from window");
                    window.remove();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    System.exit(-1);
                }
                if (window.isEmpty())
                    break;
            }
            if (!window.isEmpty())  {
                timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerHandler(this), timeout);
            }
        }
    }
    
    /**<h1>Process Timeout</h1>
     * <ol>
     * <li>Get the list of all pending segments by calling txQueue.toArray()</li>
     * <li>Go through the list and send all segments to the UDP socket</li>
     * <li>If not txQueue.isEmpty(), start the timer</li>
     * </ol>
     */
    public synchronized void processTimeout()   {
        if (DEBUG)
            System.out.println("Timeout has occured");
        Segment[] segments = window.toArray();
        for (Segment i: segments)   {
            reSend(i);
        }
        if (!window.isEmpty())  {
            timer.cancel();
            timer = new Timer();
            timer.schedule(new TimerHandler(this), timeout);
        }
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
