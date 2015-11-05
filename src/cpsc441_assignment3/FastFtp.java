
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
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
     * Sends the specified file to the specified destination host: 1. send file
     * name and receiver server confirmation over TCP 2. send file segment by
     * segment over UDP 3. send end of transmission over tcp 3. clean up
     *
     * @param serverName	Name of the remote server
     * @param serverPort	Port number of the remote server
     * @param fileName	Name of the file to be trasferred to the rmeote server
     */
    public void send(String serverName, int serverPort, String fileName) {
        TxQueue packets = new TxQueue(1000);
        int count;
        byte[] buffer = new byte[Segment.MAX_PAYLOAD_SIZE];
        DataInputStream dataStream = null;
        try {
            dataStream = new DataInputStream(new FileInputStream(fileName));
        } catch (FileNotFoundException ex) {
            //TODO deal with this exception
        }
        boolean terminate = false;
        try {
            while (true) {
                for (int i = 0; i < 1000; i++) {
                    if ((count = dataStream.read(buffer)) == -1) {
                        terminate = true;
                        break;
                    }
                    packets.add(new Segment(segmentID, buffer));
                    if (segmentID == windowSize - 1)    {
                        segmentID = 0;
                    } else  {
                        segmentID++;
                    }
                }
                if (terminate)
                    break;
                sendPackets(packets, serverName, serverPort);
            }
        } catch (IOException ex) {
            //TODO deal with this exception
        } catch (InterruptedException ex) {
            //TODO deal with this exception
        }
    }
    
    private void sendPackets(TxQueue packets, String server, int port)  {
        PacketStates currentState = PacketStates.SEND;
        while (true)    {
            switch (currentState)   {
                case SEND:
                    
                    
                    
                    break;
                    
                case WAIT:
                    
                    
                    
                    
                    break;
                    
                case RESEND:
                    
                    
                    
                    
                    break;
                    
                case TIMEOUT:
                    
                    
                    
                    
                    break;
                    
            }
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
