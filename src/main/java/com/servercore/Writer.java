package com.servercore;

import com.domain.MessageType;
import com.domain.Packet;
import com.google.common.primitives.Bytes;
import com.utilities.Adaptor;
import com.utilities.UtilityFunction;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * This class is used to send message to client.
 * Here we read message from queue. The queue is
 * share between Reader and Writer. We can also
 * refer this class as consumer
 *
 * @author umar.tahir@afiniti.com
 */

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class Writer extends Thread {
    private BlockingQueue<byte[]> messageQueue;
    private ServerSocketChannel serverSocketChannel;
    private final Pipeline pipeline = new Pipeline();


    /**
     * This method gets called when we start the writer thread
     * Here we are taking elements from the message queue and
     * passing it forward to do further processing
     *
     * @since 29/7/2020
     */

    @Override
    public void run() {
        log.info("Execution of writer thread started");

        while (serverSocketChannel.isOpen()){
            log.info("Keep taking byte [] from queue till server socket is accepting connections");

            try {
                byte[] byteArrayTakenFromQueue = messageQueue.take();
                log.info("Took byte array from message queue");

                log.info("Converting byte[] into packet object");

                List<Packet> packets = getAllThePacketsFromByteArray(byteArrayTakenFromQueue);

                for (Packet packet:packets){
                    log.info("Calling takePacketAndPerformAction method");
                    takePacketAndPerformAction(packet);
                    log.info("Message has been processed");
                }


            } catch (InterruptedException | IOException e) { //made changes here
                e.printStackTrace();
            }

        }


    }


    List<Packet> getAllThePacketsFromByteArray(byte [] messages){
        log.info("Calling getAllThePacketsFromByteArray method");
        ArrayList<Packet> packets = new ArrayList<>();

        pipeline.getMessageByteQueue().addAll(Bytes.asList(messages));
        pipeline.startPipeline();

        while (pipeline.isContinuePipeLineProcess()){

            int stage = pipeline.getPipelineSteps().get(pipeline.getCurrentStage()).getAsInt();
            log.info("Next stage of pipeline to be executed "+stage);

            if (pipeline.isPacketIsReady())
                packets.add(pipeline.getPacket());

        }

        return packets;
    }



    /**
     * This method take packet and analyze the packed to identify
     * correct course of action. Action can be based on login,
     * logout and Data message type
     *
     * @param packet contains all the information dervied from
     *               bytes array
     *
     * @throws IOException exception occurred while sending message
     * through socket channel
     *
     * @since 3/8/2020
     *
     */

    private void takePacketAndPerformAction(Packet packet) throws IOException {
        log.info("Execution of takePacketAndPerformAction method started");

        if (packet.getMessageType().equals(MessageType.DATA)) {
            log.info("Message type is Data. Call its course of action to send message to " +
                    "desired client");
            performForwardMessageToTheClientActivity(packet);
        } else if (packet.getMessageType().equals(MessageType.LOGIN)) {
            log.info("Message type is login. Calling its course of action");
            performLoginActivity(packet);
        } else if (packet.getMessageType().equals(MessageType.LOGOUT)) {
            log.info("Message type is logout. Calling its course of action");
            performLogoutActivity(packet);
        }



        log.info("Execution of takePacketAndPerformAction method ended");
    }

    /**
     * This method is used to send message to designated client
     * mentioned in destinationId
     *
     * @param packet information obtained from user no chances made into that packet
     * @throws IOException occurred while writing to the client
     */

    private void performForwardMessageToTheClientActivity(Packet packet) throws IOException {
        log.info("Execution of forwardMessageToTheDestination started");

        if (!ClientInfoHolder.authenticateClient(packet)) return;
        log.info("Client Authenticated");

        log.info("Getting socket channel using destination id");
        SocketChannel socketChannel = ClientInfoHolder.getSocketChannel(packet.getMessageDestinationId());

        if (socketChannel != null) {
            log.info("Socket channel is present");
            forwardMessage(packet, socketChannel);

        } else {
            log.info("Calling sendErrorMessage method");
            sendErrorMessage(packet);
        }

    }

    /**
     * This method is used to forward message to the client
     *
     * @param packet        received from client for other client
     * @param socketChannel of destination client
     * @throws IOException handled above
     */

    private void forwardMessage(Packet packet, SocketChannel socketChannel) throws IOException {
        log.info("Execution of forwardMessage started");

        log.info("Convert packet into bytes");
        byte[] packetInBytes = Adaptor.getBytesArrayFromPacket(packet);

        log.info("Calling sendingMessageToClient method");
        int bytesSent = sendingMessageToClient(packetInBytes, socketChannel);
        log.info("Number of bytes forwarded " + bytesSent);

        log.info("Execution of forwardMessage ended");
    }

    /**
     * This method gets called when no specified destination is present
     *
     * @param packet send from client
     * @throws IOException handled above
     */

    private void sendErrorMessage(Packet packet) throws IOException {
        log.info("Execution of sendErrorMessage method started");

        Packet packetInCaseNoSocketPresent = Adaptor.getPacketWhenNoSocketPresent(packet);

        log.info("Convert new packet into bytes");
        byte[] packetInBytes = Adaptor.getBytesArrayFromPacket(packetInCaseNoSocketPresent);

        log.info("Send message to the sender about status of message");
        int bytesSent = sendingMessageToClient(packetInBytes, ClientInfoHolder.getSocketChannel(packetInCaseNoSocketPresent.getMessageDestinationId()));

        log.info("Number of bytes sent back to client " + bytesSent);
    }

    /**
     * This method performs things necessary to mark client as
     * logged in the system
     *
     * @param packet login packet
     * @throws IOException exception occurred while sending message
     *                     through socket channel
     * @since 3/8/2020
     */

    private void performLoginActivity(Packet packet) throws IOException {
        log.info("Execution of performLoginActivity method started");

        int sourceId = packet.getMessageSourceId();
        log.info("Generating Magic Bytes for client and than will build a new packet object to send");
        Packet loggedInPacket = Adaptor.getLoggedInPacket(packet, ClientInfoHolder.generateMagicNumberForAuthentication(sourceId));

        SocketChannel socketChannel = ClientInfoHolder.getSocketChannel(packet.getMessageSourceId());

        if(socketChannel == null){
            log.info("No socket channel with this id can be found");
            log.info("Discarding message request");
            return;
        }

        log.info("Calling sendingMessageToClient on the input which is byte [] and socket channel ");
        int bytesSentToClient = sendingMessageToClient(Adaptor.getBytesArrayFromPacket(loggedInPacket),socketChannel);
        log.info("Number of bytes sent to client are " + bytesSentToClient);


        log.info("Execution of perform login activity ended ");
    }


    /**
     * This course takes place when message from client is of
     * logout type
     *
     * @param packet received from client
     * @throws IOException exception occurred
     */

    private void performLogoutActivity(Packet packet) throws IOException {
        log.info("Execution of performLogoutActivity method started");

        if (!ClientInfoHolder.authenticateClient(packet)) return;
        log.info("Authentication successful ");

        log.info("logging out client");
        ClientInfoHolder.informationOfMagicNumber.remove(packet.getMessageSourceId());

        log.info("Get latest packet");
        Packet latestPacket = Adaptor.getLoggedOutPacket(packet);

        log.info("Calling sendingMessageToClient method");
        sendingMessageToClient(Adaptor.getBytesArrayFromPacket(latestPacket), ClientInfoHolder.getSocketChannel(latestPacket.getMessageDestinationId()));

        log.info("Execution of performLogoutActivity method ended");

    }

    /**
     * This message is used to send message to the client
     *
     * @param packetInBytes packet to send
     * @param channel       to whom to send to
     * @throws IOException exception
     * @apiNote partial write wont be a problem here **
     */

    public int sendingMessageToClient(byte[] packetInBytes, SocketChannel channel) throws IOException {
        log.info("Execution of sendingMessageToClient started");
        log.info("Creating buffer with allocation of private backend space with size {}", packetInBytes.length);
        ByteBuffer messageToServerBuffer = ByteBuffer.allocate(packetInBytes.length); //improvement
        log.info("Putting data in bulk into buffer.");
        messageToServerBuffer.put(packetInBytes);

        log.info("Flipping the buffer because internally write uses buffer pos index");
        messageToServerBuffer.flip();

        int bytesWritten = channel.write(messageToServerBuffer);

        log.info("This amount of bytes are sent to client " + bytesWritten);
        log.info("Message sent to the client");
        log.info("Clearing the buffer");

        messageToServerBuffer.clear();
        log.info("Execution of sendMessageToServer ended");
        return bytesWritten;

    }
}


