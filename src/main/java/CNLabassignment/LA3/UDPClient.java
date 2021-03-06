package CNLabassignment.LA3;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

/* Type 0 : Data
 * Type 1 : SYN
 * Type 2 : SYN-ACK
 * TYPE 3 : ACK
 */

public class UDPClient {

	private static final Logger logger = LoggerFactory.getLogger(UDPClient.class);
	/*static String msg = "You do not do, you do not do\n" + "Any more, black shoe\n"
			+ "In which I have lived like a foot\n" + "For thirty years, poor and white,\n"
			+ "Barely daring to breathe or Achoo.\n" + "\n" + "Daddy, I have had to kill you.\n"
			+ "You died before I had time—\n" + "Marble-heavy, a bag full of God,\n"
			+ "Ghastly statue with one gray toe\n" + "Big as a Frisco seal\n" + "\n"
			+ "And a head in the freakish Atlantic\n" + "Where it pours bean green over blue\n"
			+ "In the waters off the beautiful Nauset.\n" + "I used to pray to recover you.\n" + "Ach, du.\n" + "\n"
			+ "In the German tongue, in the Polish town\n" + "Scraped flat by the roller\n" + "Of wars, wars, wars.\n"
			+ "But the name of the town is common.\n" + "My Polack friend\n" + "\n" + "Says there are a dozen or two.\n"
			+ "So I never could tell where you\n" + "Put your foot, your root, \n" + "I never could talk to you.\n"
			+ "The tongue stuck in my jaw.\n" + "\n" + "It stuck in a barb wire snare.\n" + "Ich, ich, ich, ich,\n"
			+ "I could hardly speak.\n" + "I thought every German was you.\n" + "And the language obscene\n" + "\n"
			+ "An engine, an engine, \n" + "Chuffing me off like a Jew.\n" + "A Jew to Dachau, Auschwitz, Belsen.\n"
			+ "I began to talk like a Jew.\n" + "I think I may well be a Jew.\n" + "\n"
			+ "The snows of the Tyrol, the clear beer of Vienna \n" + "Are not very pure or true.\n"
			+ "With my gypsy ancestress and my weird luck\n" + "And my Taroc pack and my Taroc pack\n"
			+ "I may be a bit of a Jew.\n" + "\n" + "I have always been scared of you,\n"
			+ "With your Luftwaffe, your gobbledygoo.\n" + "And your neat mustache\n"
			+ "And your Aryan eye, bright blue.\n" + "Panzer-man, panzer-man, O You——\n" + "\n"
			+ "Not God but a swastika\n" + "So black no sky could squeak through.\n" + "Every woman adores a Fascist,\n"
			+ "The boot in the face, the brute\n" + "Brute heart of a brute like you.\n" + "\n"
			+ "You stand at the blackboard, daddy,\n" + "In the picture I have of you,\n"
			+ "A cleft in your chin instead of your foot\n" + "But no less a devil for that, no not\n"
			+ "Any less the black man who\n" + "\n" + "Bit my pretty red heart in two.\n"
			+ "I was ten when they buried you.\n" + "At twenty I tried to die\n" + "And get back, back, back to you.\n"
			+ "I thought even the bones would do.\n" + "\n" + "But they pulled me out of the sack,\n"
			+ "And they stuck me together with glue.\n" + "And then I knew what to do.\n" + "I made a model of you,\n"
			+ "A man in black with a Meinkampf look";*/
	static Packet resp;

	private static boolean handshake(SocketAddress routerAddr, InetSocketAddress serverAddr, DatagramChannel channel,
			int size) throws IOException {

		Packet p = new Packet.Builder().setType(1).setSequenceNumber(1l).setPortNumber(serverAddr.getPort())
				.setPeerAddress(serverAddr.getAddress()).setPayload(("Number of packets:" + size).getBytes()).create();

		do {
			logger.info("Sending handshake request ");
			channel.send(p.toBuffer(), routerAddr);

			channel.configureBlocking(false);
			Selector selector = Selector.open();
			channel.register(selector, OP_READ);
			logger.info("Waiting for the response");
			selector.select(5000);

			Set<SelectionKey> keys = selector.selectedKeys();
			if (keys.isEmpty()) {
				// channel.send(p.toBuffer(), routerAddr);
				logger.error("No response at Client..resending request");
				continue;
			}

			// We just want a single response.
			ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
			SocketAddress router = channel.receive(buf);
			buf.flip();
			resp = Packet.fromBuffer(buf);
			// if()
			// logger.info("Packet: {}", resp);
			logger.info("Router address: {}", router);
			String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
			logger.info("Payload: {}", payload);
			if (payload.contains("SYN-ACK"))
				break;
			keys.clear();
			// logger.info("SYN-ACK not received");

		} while (resp == null);
		logger.info("connection request accepted");
		return true;

	}

	@SuppressWarnings("unchecked")
	private static void runClient(SocketAddress routerAddr, InetSocketAddress serverAddr, String request) throws IOException {
		try (DatagramChannel channel = DatagramChannel.open()) {

			byte[] myBytes = request.getBytes();
			int msgSize = myBytes.length;
			Long packetCounter = 1L;
			List<Packet> requestPacketList = new ArrayList<Packet>();
			int last = 0;
			Packet p;
			List<Packet> responsePacketList = new ArrayList<Packet>();

			// packets creation
			for (int i = 0; i < msgSize; i++) {
				if (i % 1013 == 0 && i != 0) {

					byte[] tempByte;
					tempByte = Arrays.copyOfRange(myBytes, i - 1013, i);
					p = new Packet.Builder().setType(0).setSequenceNumber(packetCounter)
							.setPortNumber(serverAddr.getPort()).setPeerAddress(serverAddr.getAddress())
							.setPayload(tempByte).create();
					requestPacketList.add(p);
					last = i;
					packetCounter++;
				} else if (i == msgSize - 1) {
					byte[] tempByte = Arrays.copyOfRange(myBytes, last, msgSize);
					p = new Packet.Builder().setType(0).setSequenceNumber(packetCounter)
							.setPortNumber(serverAddr.getPort()).setPeerAddress(serverAddr.getAddress())
							.setPayload(tempByte).create();
					requestPacketList.add(p);
					packetCounter++;
				}
			}

			// handshake:
			boolean handshakeResult = handshake(routerAddr, serverAddr, channel, requestPacketList.size());
			if (handshakeResult) {
				for (Packet pkt : requestPacketList) {
					channel.send(pkt.toBuffer(), routerAddr);

				}
				logger.info("Request packet list sent");
			}

			// Try to receive a packet within timeout.
			ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
			List<Packet> ACKList = new ArrayList<Packet>();
			List<Packet> tempACKList = new ArrayList<Packet>();
			List<Packet> tempResponseList = new ArrayList<Packet>();

			for (;;) {
				if (ACKList.size() == requestPacketList.size()) {
					break;
				} else {
					buf.clear();
					channel.configureBlocking(false);
					Selector selector = Selector.open();
					channel.register(selector, OP_READ);
					logger.info("Waiting for the response");
					selector.select(5000);

					// check if ACK is received for all packets, else re-send packets selectively
					Set<SelectionKey> keys = selector.selectedKeys();
					if (keys.isEmpty() && ACKList.size() != requestPacketList.size()) {
						for (Packet p1 : requestPacketList) {
							boolean flag = false;
							for (Packet ack1 : ACKList) {
								if (ack1.getSequenceNumber() != p1.getSequenceNumber())
									continue;
								else
									flag = true;
							}
							if (!flag) {
								channel.send(p1.toBuffer(), routerAddr);
								logger.info("Missing ACK " + p1.getSequenceNumber() + "... Re-sending packet");
							}
						}
						continue;
					}

					SocketAddress router = channel.receive(buf);
					buf.flip();
					Packet resp = Packet.fromBuffer(buf);

					// check if packet type is ACK
					if (resp.getType() == 3) {
						if (ACKList.size() == 0) {
							ACKList.add(resp);
						} else {

							for (Packet pkt : ACKList) {
								if (pkt.getSequenceNumber() != resp.getSequenceNumber()) {
									tempACKList.add(pkt);
								}
							}
							ACKList.addAll(tempACKList);
						}
						String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
						logger.info("Recieved: " + payload);

						keys.clear();

					}

				}

			}

			logger.info("Outside for!!");
			for (;;) {
				// break the loop if last response packet is received

				buf.clear();
				channel.configureBlocking(false);
				Selector selector = Selector.open();
				channel.register(selector, OP_READ);
				logger.info("Waiting for the response from server..");
				selector.select(25000);

				// check if ACK is received for all packets, else re-send packets selectively
				Set<SelectionKey> keys = selector.selectedKeys();
				if (keys.isEmpty()) {
					// do something
					break;
				}

				SocketAddress router = channel.receive(buf);
				buf.flip();
				Packet resp = Packet.fromBuffer(buf);

				if (resp.getType() == 0) {
					if (responsePacketList.size() == 0) {
						responsePacketList.add(resp);
						logger.info("Received response packet " + resp.getSequenceNumber());
						Packet responseACK = resp.toBuilder().setType(3)
								.setPayload(("ACK" + resp.getSequenceNumber()).getBytes()).create();
						channel.send(responseACK.toBuffer(), router);
						logger.info("Sending ACK " + resp.getSequenceNumber());
					} else {
						boolean packetExists = false;
						for (Packet pkt : responsePacketList) {
							if (pkt.getSequenceNumber() != resp.getSequenceNumber()) {
								continue;
							}
							else
								if(pkt.getSequenceNumber() == resp.getSequenceNumber()) {
									logger.info("Received response packet " + resp.getSequenceNumber());
									Packet responseACK = resp.toBuilder().setType(3)
											.setPayload(("ACK" + resp.getSequenceNumber()).getBytes()).create();
									channel.send(responseACK.toBuffer(), router);
									logger.info("Sending ACK " + resp.getSequenceNumber());
									packetExists = true;
									break;
								}
						}
						if(!packetExists)
							responsePacketList.add(resp);
					}
				}
			}
			String responseFromServer = "";
			Collections.sort(responsePacketList);
			for(Packet p1 : responsePacketList) {
				String payload = new String(p1.getPayload(), UTF_8);
				responseFromServer += payload;
			}
			logger.info("Response from the server received: \n" + responseFromServer);
		}

	}

	public static void main(String[] args) throws IOException {
		OptionParser parser = new OptionParser();
		parser.accepts("router-host", "Router hostname").withOptionalArg().defaultsTo("localhost");

		parser.accepts("router-port", "Router port number").withOptionalArg().defaultsTo("3000");

		parser.accepts("server-host", "EchoServer hostname").withOptionalArg().defaultsTo("localhost");

		parser.accepts("server-port", "EchoServer listening port").withOptionalArg().defaultsTo("8007");

		OptionSet opts = parser.parse(args);

		// Router address
		String routerHost = (String) opts.valueOf("router-host");
		System.out.println("routerHost: " + routerHost);
		int routerPort = Integer.parseInt((String) opts.valueOf("router-port"));
		System.out.println("routerPort: " + routerPort);

		// Server address
		String serverHost = (String) opts.valueOf("server-host");
		int serverPort = Integer.parseInt((String) opts.valueOf("server-port"));

		SocketAddress routerAddress = new InetSocketAddress(routerHost, routerPort);
		InetSocketAddress serverAddress = new InetSocketAddress(serverHost, serverPort);

		String request = "";
		for (int i = 0; i < args.length; i++) {
			System.out.println("arg " + i + ": " + args[i]);
		}
		FSClientLibrary library = new FSClientLibrary();
		
			if(args[0].toLowerCase().contentEquals("get"))
			{
				if(args[1].equals("/"))
				{
					try {
						request = library.getAllFiles();
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else
					if(args[1].matches("/.+"))
					{	
						String filename = "";
						filename = args[1].substring(1);
						System.out.println("filename: " + filename);
						request = library.getFileContent(filename);
					}
			}
			else
				if(args[0].toLowerCase().contentEquals("post"))
				{
					if(args[1].matches("/.+"))
					{
						String toFilename = args[1].substring(1);
						String fromFilename = args[2];
						System.out.println("post filename :: " + toFilename);
						request = library.postFile(toFilename, fromFilename);
					}
				}
		runClient(routerAddress, serverAddress, request);
	}
}
