package cpsc441_assignment3;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
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
    int timeout;
    private int segmentID;
    TxQueue window;
    DatagramSocket socket_UDP;
    Timer timer;

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
        //TODO remove this
        System.out.println("Starting to send file client side...");
        
        try {
            socket_UDP = new DatagramSocket(serverPort, InetAddress.getByName(serverName));
        } catch (SocketException ex) {
            //TODO deal with this exception
            ex.printStackTrace();
        } catch (UnknownHostException ex) {
            //TODO deal with this exception
            ex.printStackTrace();
        }
        System.out.println("Line 68");//TODO remove this
        Socket socket_TCP = null;
        DataOutputStream handshakeOut = null;
        DataInputStream handshakeIn = null;
        byte response = 1;
        try {
            socket_TCP = new Socket(InetAddress.getByName(serverName), serverPort);
            handshakeOut = new DataOutputStream(socket_TCP.getOutputStream());
            handshakeOut.write((fileName + "\r\n\r\n").getBytes());
            handshakeIn = new DataInputStream(socket_TCP.getInputStream());
            System.out.println("Line 78");//TODO remove this
            response = handshakeIn.readByte();
            System.out.println("Line 80");//TODO remove this
        } catch (UnknownHostException ex) {
            //TODO deal with this exception
            ex.printStackTrace();
        } catch (IOException ex) {
            //TODO deal with this exception
            ex.printStackTrace();
        }
        System.out.println("Line 86");//TODO remove this
        if (response != 0)  {
            System.out.println("Error FastFtp line 85, not deal with");
            //TODO deal with this error
        }
        
        
        
        ///////////////////////////////////////////////////////////////////////////////////
        //                          Start ACK recieving thread                           //
        ///////////////////////////////////////////////////////////////////////////////////
        
        System.out.println("Start ACK recieving thread");//TODO remove this 
        
        Acknowledge ackThread = null;
        ackThread = new Acknowledge(serverPort, this);
        ackThread.start();  //TODO ensure to clean up if needed
        
        
        ///////////////////////////////////////////////////////////////////////////////////
        //                Create dataInputStream from file                               //
        ///////////////////////////////////////////////////////////////////////////////////
        DataInputStream fileInput = null;
        try {
            fileInput = new DataInputStream(new FileInputStream(fileName));
            int count = -1;
            byte[] buffer = new byte[Segment.MAX_PAYLOAD_SIZE];
            while ((count = fileInput.read(buffer)) != -1) {

        
            ///////////////////////////////////////////////////////////////////////////////////
            //                Create segment with next sequence number                       //
            ///////////////////////////////////////////////////////////////////////////////////
                Segment segment = new Segment(segmentID, buffer);
                segmentID++;
                
                System.out.println("Packet number " + (segmentID - 1) + " created");//TODO remove this
                
            ///////////////////////////////////////////////////////////////////////////////////
            //              Yield to other threads if queue is full                          //
            ///////////////////////////////////////////////////////////////////////////////////
                while (window.isFull())     {
                    Thread.yield();
                }
                System.out.println("Finished yielding to threads waiting for space in queue");//TODO remove this
        
            ///////////////////////////////////////////////////////////////////////////////////
            //                          Send the segment                                     //
            ///////////////////////////////////////////////////////////////////////////////////
                processSend(segment);
                System.out.println("Segment " + segment.getSeqNum() + " has been sent");//TODO remove this
            }
        } catch (FileNotFoundException ex) {
            // TODO deal with this exception
            ex.printStackTrace();
        } catch (IOException ex) {
            //TODO deal with this exception
            ex.printStackTrace();
        } finally   {
            try {
                fileInput.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                //TODO deal with exception
            }
        }

        
        
        ///////////////////////////////////////////////////////////////////////////////////
        //       Wait until queue is empty, send end of transmission message             //
        ///////////////////////////////////////////////////////////////////////////////////
        
        System.out.println("Send end of transmission");//TODO remove this
        while (!window.isEmpty())   {
            Thread.yield();
        }
        try {
            handshakeOut.writeByte(0);
        } catch (IOException ex) {
            //TODO deal with this exception
            ex.printStackTrace();
        }
        ///////////////////////////////////////////////////////////////////////////////////
        //                              Clean Up                                         //
        ///////////////////////////////////////////////////////////////////////////////////
        try {
            socket_TCP.close();
            socket_UDP.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            //TODO deal with exception
        }
        ackThread.terminate();
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
        // Send the segment
        DatagramPacket pkt;
        pkt = new DatagramPacket(seg.getBytes(), Segment.MAX_SEGMENT_SIZE);
        try {
            socket_UDP.send(pkt);
        } catch (IOException ex) {
            
        }
        
        // Add the segment to the queue
        try {
            window.add(seg);
        } catch (InterruptedException ex) {
            // TODO deal with exception
            ex.printStackTrace();
        }
        
        if (window.size() == 1)     {
            timer = new Timer(true);
            timer.schedule(new TimerHandler(this), timeout);
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
        if (ack.getSeqNum() >= window.element().getSeqNum() && ack.getSeqNum() < window.element().getSeqNum() + window.size())  {
            while (window.element().getSeqNum() <= ack.getSeqNum()) {
                try {
                    window.remove();
                } catch (InterruptedException ex) {
                    //TODO deal with exception
                    ex.printStackTrace();
                }
            }
            if (!window.isEmpty())  {
                timer.cancel();
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
        Segment[] segments = window.toArray();
        for (Segment i: segments)   {
            processSend(i);
        }
        if (!window.isEmpty())  {
            timer.cancel();
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
