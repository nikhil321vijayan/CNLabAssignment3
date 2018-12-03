package CNLabassignment.LA3;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

/* Type 0 : Data
 * Type 1 : SYN
 * Type 2 : SYN-ACK
 * TYPE 3 : ACK
 */
public class UDPServer {

	private static final Logger logger = LoggerFactory.getLogger(UDPServer.class);

	@SuppressWarnings("unchecked")
	private void listenAndServe(int port) throws IOException {
		List<Packet> packetList = new ArrayList<Packet>();

		try (DatagramChannel channel = DatagramChannel.open()) {
			channel.bind(new InetSocketAddress(port));
			logger.info("EchoServer is listening at {}", channel.getLocalAddress());
			ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN).order(ByteOrder.BIG_ENDIAN);
			int requestPacketSize = 0;
			SocketAddress router = null;
			List<Packet> tempList = new ArrayList<Packet>();
			for (;;) {
				buf.clear();
				channel.configureBlocking(false);
				Selector selector = Selector.open();
				channel.register(selector, OP_READ);
				// logger.info("");
				selector.select(20000);

				Set<SelectionKey> keys = selector.selectedKeys();
				if (keys.isEmpty()) {

					break;
				}
				router = channel.receive(buf);

				// Parse a packet from the received raw data.
				buf.flip();
				Packet packet = Packet.fromBuffer(buf);
				buf.flip();

				if (packet.getType() == 1) {
					String payload = new String(packet.getPayload(), UTF_8);
					// logger.info("Packet: {}", packet);
					logger.info("Payload: {}", payload);
					logger.info("Router: {}", router);
					requestPacketSize = Integer.parseInt(payload.split(":")[1]);
					Packet resp = packet.toBuilder().setType(2)
							.setPayload(("SYN-ACK " + packet.getSequenceNumber()).getBytes()).create();
					channel.send(resp.toBuffer(), router);
					logger.info("Sending SYN-ACK");
				} else if (packet.getType() == 0) {
					if (packetList.size() == 0) {
						packetList.add(packet);
						logger.info("Received packet " + packet.getSequenceNumber());
						Packet resp = packet.toBuilder().setType(3)
								.setPayload(("ACK" + packet.getSequenceNumber()).getBytes()).create();
						channel.send(resp.toBuffer(), router);
						logger.info("Sending ACK " + packet.getSequenceNumber());
					} else {
						boolean packetExists = false;
						for (Packet pkt : packetList) {
							if (pkt.getSequenceNumber() != packet.getSequenceNumber()) {
								tempList.add(pkt);
								logger.info("Received packet " + packet.getSequenceNumber());
								Packet resp = packet.toBuilder().setType(3)
										.setPayload(("ACK" + packet.getSequenceNumber()).getBytes()).create();
								channel.send(resp.toBuffer(), router);
								logger.info("Sending ACK " + packet.getSequenceNumber());
							} else if (pkt.getSequenceNumber() == packet.getSequenceNumber()) {
								logger.info("Received packet " + packet.getSequenceNumber());
								Packet resp = packet.toBuilder().setType(3)
										.setPayload(("ACK" + packet.getSequenceNumber()).getBytes()).create();
								channel.send(resp.toBuffer(), router);
								logger.info("Sending ACK " + packet.getSequenceNumber());
							}
						}
						packetList.addAll(tempList);
					}

					// String payload = new String(packet.getPayload(), UTF_8);

				}

				// Send the response to the router not the client.
				// The peer address of the packet is the address of the client already.
				// We can use toBuilder to copy properties of the current packet.
				// This demonstrate how to create a new packet from an existing packet.

			}

			logger.info("Outside for!!");
			String request = "";
			Collections.sort(packetList);
			for (Packet p : packetList) {
				String payload = new String(p.getPayload(), UTF_8);
				request += payload;
			}
			logger.info("Request received: " + request);
			// logger.info("peer in server: " + packetList.get(0).getPeerPort() + ", " +
			// packetList.get(0).getPeerAddress());
			processRequest(packetList.get(packetList.size() - 1).getPeerAddress(),
					packetList.get(packetList.size() - 1).getPeerPort(), channel, router);
		}
	}

	private void processRequest(InetAddress peerAddress, int port, DatagramChannel channel, SocketAddress routerAddress)
			throws IOException {
		// TODO Auto-generated method stub

		String response = "A Friendly Clown\n"
				+ "On one corner of my dresser sits a smiling toy clown on a tiny unicycle--a gift I\n"
				+ "received last Christmas from a close friend. The clown's short yellow hair, made of yarn,\n"
				+ "covers its ears but is parted above the eyes. The blue eyes are outlined in black with thin,\n"
				+ "dark lashes flowing from the brows. It has cherry-red cheeks, nose, and lips, and its broad\n"
				+ "grin disappears into the wide, white ruffle around its neck. The clown wears a fluffy, twotone\n"
				+ "nylon costume. The left side of the outfit is light blue, and the right side is red. The\n"
				+ "two colors merge in a dark line that runs down the center of the small outfit. Surrounding\n"
				+ "its ankles and disguising its long black shoes are big pink bows. The white spokes on the\n"
				+ "wheels of the unicycle gather in the center and expand to the black tire so that the wheel\n"
				+ "somewhat resembles the inner half of a grapefruit. The clown and unicycle together stand\n"
				+ "about a foot high. As a cherished gift from my good friend Tran, this colorful figure greets\n"
				+ "me with a smile every time I enter my room.\n" + "The Blond Guitar\n"
				+ "My most valuable possession is an old, slightly warped blond guitar--the first\n"
				+ "instrument I taught myself how to play. It's nothing fancy, just a Madeira folk guitar, all\n"
				+ "scuffed and scratched and finger-printed. At the top is a bramble of copper-wound strings,\n"
				+ "each one hooked through the eye of a silver tuning key. The strings are stretched down a\n"
				+ "long, slim neck, its frets tarnished, the wood worn by years of fingers pressing chords and\n"
				+ "picking notes. The body of the Madeira is shaped like an enormous yellow pear, one that"
				+ "was slightly damaged in shipping. The blond wood has been chipped and gouged to gray,\n"
				+ "particularly where the pick guard fell off years ago. No, it's not a beautiful instrument, but\n"
				+ "it still lets me make music, and for that I will always treasure it.\n" + "Gregory\n"
				+ "Gregory is my beautiful gray Persian cat. He walks with pride and grace,\n"
				+ "performing a dance of disdain as he slowly lifts and lowers each paw with the delicacy of\n"
				+ "a ballet dancer. His pride, however, does not extend to his appearance, for he spends\n"
				+ "most of his time indoors watching television and growing fat. He enjoys TV commercials,\n"
				+ "especially those for Meow Mix and 9 Lives. His familiarity with cat food commercials has\n"
				+ "led him to reject generic brands of cat food in favor of only the most expensive brands.\n"
				+ "Gregory is as finicky about visitors as he is about what he eats, befriending some and\n"
				+ "repelling others. He may snuggle up against your ankle, begging to be petted, or he may\n"
				+ "imitate a skunk and stain your favorite trousers. Gregory does not do this to establish his\n"
				+ "territory, as many cat experts think, but to humiliate me because he is jealous of my\n"
				+ "friends. After my guests have fled, I look at the old fleabag snoozing and smiling to\n"
				+ "himself in front of the television set, and I have to forgive him for his obnoxious, but\n"
				+ "endearing, habits.";

		// packets creation
		byte[] myBytes = response.getBytes();
		int msgSize = myBytes.length;
		Long packetCounter = 1L;
		List<Packet> responsePacketList = new ArrayList<Packet>();
		int last = 0;
		Packet p;
		for (int i = 0; i < msgSize; i++) {
			if (i % 1013 == 0 && i != 0) {

				byte[] tempByte;
				tempByte = Arrays.copyOfRange(myBytes, i - 1013, i);
				p = new Packet.Builder().setType(0).setSequenceNumber(packetCounter).setPortNumber(port)
						.setPeerAddress(peerAddress).setPayload(tempByte).create();
				responsePacketList.add(p);
				last = i;
				packetCounter++;
			} else if (i == msgSize - 1) {
				byte[] tempByte = Arrays.copyOfRange(myBytes, last, msgSize);
				p = new Packet.Builder().setType(0).setSequenceNumber(packetCounter).setPortNumber(port)
						.setPeerAddress(peerAddress).setPayload(tempByte).create();
				responsePacketList.add(p);
				packetCounter++;
			}
		}

		// send list of response packets
		for (Packet pkt : responsePacketList) {
			channel.send(pkt.toBuffer(), routerAddress);
		}
		logger.info("Response packet list sent.. Number of packets in list: " + responsePacketList.size());

		// Try to receive a packet within timeout.
		ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
		List<Packet> ACKList = new ArrayList<Packet>();
		for (; ACKList.size() < responsePacketList.size();) {
			// System.out.println("ACKList size: " + ACKList.size());
			if (ACKList.size() == responsePacketList.size()) {
				logger.info("Received all ACKs");
				break;
			} else {
				buf.clear();
				channel.configureBlocking(false);
				Selector selector = Selector.open();
				channel.register(selector, OP_READ);
				logger.info("Waiting for client's acknowledgement");
				selector.select(5000);

				// if ACK is missing re-send the packet selectively
				Set<SelectionKey> keys = selector.selectedKeys();
				if (keys.isEmpty() && ACKList.size() < responsePacketList.size()) {
					for (Packet p1 : responsePacketList) {
						boolean flag = false;
						for (Packet ack1 : ACKList) {
							if (ack1.getSequenceNumber() != p1.getSequenceNumber())
								continue;
							else
								flag = true;
						}
						if (!flag) {
							channel.send(p1.toBuffer(), routerAddress);
							logger.info("Missing ACK " + p1.getSequenceNumber() + "... Re-sending packet");
						}
					}
					continue;
				}

				SocketAddress router = channel.receive(buf);
				buf.flip();
				Packet resp = Packet.fromBuffer(buf);
				boolean ACKExists = false;

				// check if received packet type is ACK
				if (resp.getType() == 3) {
					if (ACKList.size() == 0) {
						ACKList.add(resp);
					} else {
						for (Packet pkt : ACKList) {
							if (pkt.getSequenceNumber() != resp.getSequenceNumber()) {
								continue;
							} else
								ACKExists = true;
						}
						if (!ACKExists) {
							ACKList.add(resp);
							//System.out.println("ACK list size: " + ACKList.size());
						}
					}
					String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
					logger.info("Recieved: " + payload);
					keys.clear();
				}
			}
		}
	}

	public static void main(String[] args) throws IOException {
		OptionParser parser = new OptionParser();
		parser.acceptsAll(asList("port", "p"), "Listening port").withOptionalArg().defaultsTo("8007");

		OptionSet opts = parser.parse(args);
		int port = Integer.parseInt((String) opts.valueOf("port"));
		UDPServer server = new UDPServer();
		server.listenAndServe(port);
	}
}