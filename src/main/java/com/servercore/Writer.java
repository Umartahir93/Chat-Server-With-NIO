package com.servercore;

import com.domain.MessageType;
import com.domain.Packet;
import com.google.common.primitives.Bytes;
import com.utilities.Adaptor;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
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
    private final Adaptor adaptor = new Adaptor();


    /**
     * This method gets called when we start the writer thread
     * Here we are taking elements from the message queue and
     * passing it forward to do further processing
     *
     * @since 29/7/2020
     */

    @Override
    public void run() {
        log.error("Execution of writer thread started");

        while (serverSocketChannel.isOpen()){
            log.error("Keep taking byte [] from queue till server socket is accepting connections");

            try {

                log.error("Took byte array from message queue");
                log.error("Converting byte[] into packet object");
                List<Packet> packets = getAllThePacketsFromByteArray(messageQueue.take());


                for (Packet packet:packets){
                    log.error("Calling takePacketAndPerformAction method");
                    takePacketAndPerformAction(packet);
                    log.error("Message has been processed");
                }




            } catch (InterruptedException | IOException e) { //made changes here
                log.error("Closing Writer thread");
                e.printStackTrace();
            }

        }


    }


    List<Packet> getAllThePacketsFromByteArray(byte [] messages){
        Adaptor.lock.lock();
        try{
            log.error("Calling getAllThePacketsFromByteArray method");
            ArrayList<Packet> packets = new ArrayList<>();

            pipeline.getMessageByteQueue().addAll(Bytes.asList(messages));
            pipeline.startPipeline();

            while (pipeline.isContinuePipeLineProcess()){

                int stage = pipeline.getPipelineSteps().get(pipeline.getCurrentStage()).getAsInt();
                log.error("Next stage of pipeline to be executed "+stage);

                if (pipeline.isPacketIsReady())
                    packets.add(pipeline.getPacket());

            }

            return packets;

        }finally {
            Adaptor.lock.unlock();
        }

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
        log.error("Execution of takePacketAndPerformAction method started");

        if (packet.getMessageType().equals(MessageType.DATA)) {
            log.error("Message type is Data. Call its course of action to send message to " +
                    "desired client");
            performForwardMessageToTheClientActivity(packet);
        } else if (packet.getMessageType().equals(MessageType.LOGIN)) {
            log.error("Message type is login. Calling its course of action");
            performLoginActivity(packet);
        } else if (packet.getMessageType().equals(MessageType.LOGOUT)) {
            log.error("Message type is logout. Calling its course of action");
            performLogoutActivity(packet);
        }



        log.error("Execution of takePacketAndPerformAction method ended");
    }

    /**
     * This method is used to send message to designated client
     * mentioned in destinationId
     *
     * @param packet information obtained from user no chances made into that packet
     * @throws IOException occurred while writing to the client
     */

    private void performForwardMessageToTheClientActivity(Packet packet) throws IOException {
        log.error("Execution of forwardMessageToTheDestination started");

        if (!ClientInfoHolder.authenticateClient(packet)) return;
        log.error("Client Authenticated");

        log.error("Getting socket channel using destination id");
        SocketChannel socketChannel = ClientInfoHolder.getSocketChannel(packet.getMessageDestinationId());

        if (socketChannel != null) {
            log.error("Socket channel is present");
            forwardMessage(packet, socketChannel);

        } else {
            log.error("Calling sendErrorMessage method");
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
        log.error("Execution of forwardMessage started");

        log.error("Convert packet into bytes");
        byte[] packetInBytes = adaptor.getBytesArrayFromPacket(packet);

        log.error("Calling sendingMessageToClient method");
        int bytesSent = sendingMessageToClient(packetInBytes, socketChannel);
        log.error("Number of bytes forwarded " + bytesSent);

        log.error("Execution of forwardMessage ended");
    }

    /**
     * This method gets called when no specified destination is present
     *
     * @param packet send from client
     * @throws IOException handled above
     */

    private void sendErrorMessage(Packet packet) throws IOException {
        log.error("Execution of sendErrorMessage method started");

        Packet packetInCaseNoSocketPresent = adaptor.getPacketWhenNoSocketPresent(packet);

        log.error("Convert new packet into bytes");
        byte[] packetInBytes = adaptor.getBytesArrayFromPacket(packetInCaseNoSocketPresent);

        log.error("Send message to the sender about status of message");
        int bytesSent = sendingMessageToClient(packetInBytes, ClientInfoHolder.getSocketChannel(packetInCaseNoSocketPresent.getMessageDestinationId()));

        log.error("Number of bytes sent back to client " + bytesSent);
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
        log.error("Execution of performLoginActivity method started");

        int sourceId = packet.getMessageSourceId();
        log.error("Generating Magic Bytes for client and than will build a new packet object to send");
        Packet loggedInPacket = adaptor.getLoggedInPacket(packet, ClientInfoHolder.generateMagicNumberForAuthentication(sourceId));

        SocketChannel socketChannel = ClientInfoHolder.getSocketChannel(packet.getMessageSourceId());

        if(socketChannel == null){
            log.error("No socket channel with this id can be found");
            log.error("Discarding message request");
            return;
        }

        log.error("Calling sendingMessageToClient on the input which is byte [] and socket channel ");
        int bytesSentToClient = sendingMessageToClient(adaptor.getBytesArrayFromPacket(loggedInPacket),socketChannel);
        log.error("Number of bytes sent to client are " + bytesSentToClient);


        log.error("Execution of perform login activity ended ");
    }


    /**
     * This course takes place when message from client is of
     * logout type
     *
     * @param packet received from client
     * @throws IOException exception occurred
     */

    private void performLogoutActivity(Packet packet) throws IOException {
        log.error("Execution of performLogoutActivity method started");

        if (!ClientInfoHolder.authenticateClient(packet)) return;
        log.error("Authentication successful ");

        log.error("logging out client");
        ClientInfoHolder.informationOfMagicNumber.remove(packet.getMessageSourceId());

        log.error("Get latest packet");
        Packet latestPacket = adaptor.getLoggedOutPacket(packet);

        log.error("Calling sendingMessageToClient method");
        sendingMessageToClient(adaptor.getBytesArrayFromPacket(latestPacket), ClientInfoHolder.getSocketChannel(latestPacket.getMessageDestinationId()));

        log.error("Execution of performLogoutActivity method ended");

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
        log.error("Execution of sendingMessageToClient started");
        log.error("Creating buffer with allocation of private backend space with size {}", packetInBytes.length);
        ByteBuffer messageToServerBuffer = ByteBuffer.allocate(packetInBytes.length); //improvement
        log.error("Putting data in bulk into buffer.");
        messageToServerBuffer.put(packetInBytes);

        log.error("Flipping the buffer because internally write uses buffer pos index");
        messageToServerBuffer.flip();

        int bytesWritten = channel.write(messageToServerBuffer);

        log.error("This amount of bytes are sent to client " + bytesWritten);
        log.error("Message sent to the client");
        log.error("Clearing the buffer");

        messageToServerBuffer.clear();
        log.error("Execution of sendMessageToServer ended");
        return bytesWritten;

    }
}


