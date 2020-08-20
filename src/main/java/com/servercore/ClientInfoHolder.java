package com.servercore;

import com.domain.Packet;
import com.utilities.Adaptor;
import com.utilities.Constants;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class holds client's information who are connected to
 * server. The information contains Number of clients connected
 * to server, socket information of connected clients, magic no
 * which is assigned to client and later it is used to authenticate
 * the client
 * <p>
 * It also include some utility methods related to information of
 * clients
 *
 * @author umar.tahir@afiniti.com
 */

@Slf4j
@NoArgsConstructor
public class ClientInfoHolder {

    protected static final Map<Integer, SocketChannel> informationOfConnectedClients = new ConcurrentHashMap<>();
    protected static final ExecutorService threadPoolToGenerateAndSendId = Executors.newFixedThreadPool(Constants.NUMBER_OF_THREADS_IN_THREAD_POOL);
    protected static final Map<Integer, Integer> informationOfMagicNumber = new HashMap<>();
    private static final AtomicInteger clientCounter = new AtomicInteger(0);
    private Writer writer = new Writer();
    private Adaptor adaptor = new Adaptor();

    /**
     * This method create client Id which will be used
     * by client to identify himself
     *
     * @return client id
     */

    private static int createClientID() {
        return clientCounter.incrementAndGet();
    }

    /**
     * This function is used to generate magic number and put it in
     * map for further access
     *
     * @param sourceId represents ID assigned to client
     * @return magic number to send to client
     */

    public static int generateMagicNumberForAuthentication(int sourceId) {
        int magicNumber = new Random(System.nanoTime()).nextInt(Constants.UPPER_LIMIT_FOR_RANDOM_NUMBER);
        informationOfMagicNumber.put(sourceId, magicNumber);
        return magicNumber;
    }

    /**
     * This method is used to check the magic number we received from client
     * is correct or not
     *
     * @param packet client packet
     * @return true if correct
     * @apiNote needs improvement here
     */

    public static boolean authenticateClient(Packet packet) {
        if (ClientInfoHolder.informationOfMagicNumber.get(packet.getMessageSourceId()) == packet.getMagicBytes()) {
            log.info("Authentication failed. Discarding the whole message");
            return true;
        }
        return false;
    }

    /**
     * This returns socket of client to whom we want to send message to
     *
     * @param messageSourceId we get socket channel from source id
     * @return socket channel
     */

    public static SocketChannel getSocketChannel(int messageSourceId) {
        log.info("Execution of getSocketChannelFromSourceId started");

        return ClientInfoHolder.informationOfConnectedClients.get(messageSourceId);
    }

    /**
     * This method takes the channel which is created after the
     * acceptance of connection. In this method we generate Id
     * and pass it to client.
     *
     * @param channel client channel
     *
     */

    public void sendGeneratedSourceIdToClient(SocketChannel channel) {
        log.info("Execution of sendGeneratedSourceIdToClient method started");
        int clientId = createClientID();

        log.info("Client Id created: " + clientId);

        try {
            log.info("Calling createClientID() and makingPacketWithGeneratedId()");
            Packet packet = adaptor.makingPacketWithGeneratedId(clientId);

            log.info("Calling getBytesArrayFromPacket() method and sendingMessageToClient");
            writer.sendingMessageToClient(adaptor.getBytesArrayFromPacket(packet), channel);

            log.info("Calling savingInfoOfConnectedClients() method");
            savingInfoOfConnectedClients(clientId, channel);

        } catch (Exception exception) {
            log.error("Exception occur while sending packet to client");
            exception.printStackTrace();
        }
        log.info("Execution of sendGeneratedSourceIdToClient method ended");
    }

    /**
     * Save the client's information
     *
     * @param clientId id of the client
     * @param channel  client's channel
     */

    private void savingInfoOfConnectedClients(int clientId, SocketChannel channel) {
        informationOfConnectedClients.put(clientId, channel);
    }
}
