package CNLabassignment.LA3;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
/* Type 0 : Data
 * Type 1 : SYN
 * Type 2 : SYN-ACK
 * TYPE 3 : ACK
 */
public class UDPServer {

    private static final Logger logger = LoggerFactory.getLogger(UDPServer.class);

    private void listenAndServe(int port) throws IOException {
    	List<Packet> packetList = new ArrayList<Packet>();

        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(new InetSocketAddress(port));
            logger.info("EchoServer is listening at {}", channel.getLocalAddress());
            ByteBuffer buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);
            int requestPacketSize=0;
            for (; ; ) {
                buf.clear();
                SocketAddress router = channel.receive(buf);
                
                // Parse a packet from the received raw data.
                buf.flip();
                Packet packet = Packet.fromBuffer(buf);
                buf.flip();
                //logger.info("Packet: {}", packet);
                //requestPacketSize = packet.getPayload().
                if(packet.getType() == 1)
                {
                	String payload = new String(packet.getPayload(), UTF_8);
                    //logger.info("Packet: {}", packet);
                    logger.info("Payload: {}", payload);
                    logger.info("Router: {}", router); 
                    requestPacketSize = Integer.parseInt(payload.split(":")[1]);
                	Packet resp = packet.toBuilder()
                            .setType(2)
                            .setPayload("Payload is SYN-ACK".getBytes())
                            .create();
                	channel.send(resp.toBuffer(), router);
                }
                else
                	if(packet.getType() == 0)
                	{
                		packetList.add(packet);
                		String payload = new String(packet.getPayload(), UTF_8);
                        logger.info("Packet: {}", packet);
                        logger.info("Payload: {}", payload);
                        logger.info("Router: {}", router);
                        Packet resp = packet.toBuilder()
                                .setPayload("Hi from server".getBytes())
                                .create();
                        channel.send(resp.toBuffer(), router);

                	}

                // Send the response to the router not the client.
                // The peer address of the packet is the address of the client already.
                // We can use toBuilder to copy properties of the current packet.
                // This demonstrate how to create a new packet from an existing packet.
               
            }
        }
    }

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        parser.acceptsAll(asList("port", "p"), "Listening port")
                .withOptionalArg()
                .defaultsTo("8007");

        OptionSet opts = parser.parse(args);
        int port = Integer.parseInt((String) opts.valueOf("port"));
        UDPServer server = new UDPServer();
        server.listenAndServe(port);
    }
}