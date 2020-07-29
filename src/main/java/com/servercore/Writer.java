package com.servercore;

import com.domain.MessageType;
import com.domain.Packet;
import com.google.common.primitives.Bytes;
import com.utilities.Constants;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;

@Slf4j
@RequiredArgsConstructor
public class Writer extends Thread{
    private final BlockingQueue<byte[]> messageQueue;
    private final ServerSocketChannel serverSocketChannel;

    /**
     * This method gets called when we start the writer thread
     * Here we are taking elements from the message queue and
     * passing it forward to do further processing
     *
     * @since  29/7/2020
     *
     */

    @Override
    public void run() {
        log.info("Execution of writer thread started");

        while (serverSocketChannel.isOpen()){
            log.info("Keep taking byte [] from queue till server socket is accepting connections");

            try {
                byte [] message = messageQueue.take(); // threads blocks if queue is empty
                log.info("Took byte array from message queue");
                log.info("Calling process method");

                process(message);

                log.info("Message has been processed");

            } catch (InterruptedException | IOException e) { //made changes here
                e.printStackTrace();
            }

        }


    }

    /**
     * Process method takes byte[] array and convert that byte array
     * into packet class object. After that we pass the packet object
     * for further analysis and processing to takePacketAndPerformAction
     *
     * @param message message took from queue
     * @throws IOException occurred during socket operations
     *
     * @since 29/7/2020
     */

    private void process(byte[] message) throws IOException {
        log.info("Execution of process method started");

        log.info("Converting byte[] into packet object");
        Packet packet = convertByteArrayIntoPacket(message);

        log.info("Calling takePacketAndPerformAction method");
        takePacketAndPerformAction(packet);

        log.info("Execution of process ended");
    }

    /**
     * Method that converts byte array into packet class object
     * @param message this is message in byte [] which needs to
     *                be converted
     * @return packet object
     */

    private Packet convertByteArrayIntoPacket(byte[] message){
        log.info("Execution of convertByteArrayIntoPacket method started");
        int magicBytes          = getIntFromByteArray(message,Constants.START_OF_MAGIC_BYTES_INCLUSIVE, Constants.END_OF_MAGIC_BYTES_EXCLUSIVE);
        String messageTypeValue = getStringFromByteArray(message,Constants.START_OF_MESSAGE_TYPE_INCLUSIVE,Constants.END_OF_MESSAGE_TYPE_EXCLUSIVE);
        MessageType messageType = identifyMessageType(messageTypeValue);
        int sourceId            = getIntFromByteArray(message,Constants.START_OF_SOURCE_ID_INCLUSIVE,Constants.END_OF_SOURCE_ID_EXCLUSIVE);
        int destId              = getIntFromByteArray(message,Constants.START_OF_DEST_ID_INCLUSIVE,Constants.END_OF_DEST_ID_EXCLUSIVE);
        int messageLength       = getIntFromByteArray(message,Constants.START_OF_MESSAGE_LENGTH_INCLUSIVE,Constants.END_OF_MESSAGE_LENGTH_EXCLUSIVE);
        String messageOfClient  = getStringFromByteArray(message,Constants.START_OF_MESSAGE_INCLUSIVE,message.length);

        log.info("Execution of convertByteArrayIntoPacket method ended");
        return Packet.builder().magicBytes(magicBytes).messageType(messageType).messageSourceId(sourceId).messageDestinationId(destId)
                .messageLength(messageLength).message(messageOfClient).build();
    }

    /**
     * Helper function of convertByteArrayIntoPacket which converts bytes into
     * String
     *
     * @param message in byte array
     * @param start start index in byte array for specific bytes
     * @param end end index in byte array for specific bytes.
     *            Remember this exclusive
     * @return String
     */

    private String getStringFromByteArray(byte[] message, int start ,int end){
        return Arrays.toString(Arrays.copyOfRange(message ,start , end));
    }

    /**
     * Helper function of convertByteArrayIntoPacket which converts bytes into
     * integer
     * @param message in byte array
     * @param start start index in byte array for specific bytes
     * @param end index in byte array for specific bytes.
     *        Remember this exclusive
     *
     * @return integer
     */

    private int getIntFromByteArray(byte[] message,int start ,int end){
        return ByteBuffer.wrap(Arrays.copyOfRange(message ,start , end)).getInt();
    }

    /**
     * Helper function of convertByteArrayIntoPacket and it returns Enum
     *
     * @param message in byte array
     * @return Enum
     */

    private MessageType identifyMessageType(String message){
        return Enum.valueOf(MessageType.class,message);
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
     */

    private void takePacketAndPerformAction(Packet packet) throws IOException {
        log.info("Execution of takePacketAndPerformAction method started");

        if(packet.getMessageType().equals(MessageType.LOGIN)) {
            log.info("Message type is login. Calling its course of action");
            performLoginActivity(packet);
        }
        else if(packet.getMessageType().equals(MessageType.LOGOUT)) {
            log.info("Message type is logout. Calling its course of action");
            performLogoutActivity(packet);
        }
        else if(packet.getMessageType().equals(MessageType.DATA)) {
            log.info("Message type is Data. Call its course of action to send message to " +
                    "desired client");
            performForwardMessageToTheClientActivity(packet);
        }

        log.info("Execution of takePacketAndPerformAction method ended");
    }

    /**
     * This method performs things necessary to mark client as
     * logged in the system
     *
     * @param packet login packet
     * @throws IOException exception occurred while sending message
     * through socket channel
     *
     */

    private void performLoginActivity(Packet packet) throws IOException {
        log.info("Execution of performLoginActivity method started");
        log.info("Generating Magic Bytes for client and than will build a new packet object to send");

        int sourceId = packet.getMessageSourceId();

        Packet loggedInPacket = buildLoggedInPacket(packet ,
                ClientInfoHolder.generateMagicNumberForAuthentication(sourceId)
        );

        log.info("Calling sendingMessageToClient on the input which is byte [] and socket channel ");
        sendingMessageToClient(
                convertPacketIntoByteArray(loggedInPacket),
                getSocketChannel(packet.getMessageSourceId())
        );
    }

    /**
     *
     * This utility method is used to build Packet with login details
     * which we will send to client
     *
     * @param packet received from client (request)
     * @param magicNumber generated magic number for logged in client
     * @return new packet to send to clients
     *
     */

    private Packet buildLoggedInPacket(Packet packet,int magicNumber) {
        return Packet.builder().magicBytes(magicNumber).messageType(MessageType.LOGIN).messageSourceId(Constants.SERVER_SOURCE_ID).
                messageDestinationId(packet.getMessageSourceId()).messageLength(packet.getMessageLength()).
                message(packet.getMessage()).build();
    }

    /**
     * This method takes the packet and convert into byte array which we
     * will send to client
     *
     * @param packet new packet to sent to client
     * @return byte []
     *
     */

    public byte [] convertPacketIntoByteArray(Packet packet) {
        log.info("Execution of convertPacketIntoByteArray method started");
        List<Byte> byteArrayList = new ArrayList<>();

        byteArrayList.addAll(Bytes.asList(ByteBuffer.allocate(4).putInt(packet.getMagicBytes()).array()));
        byteArrayList.addAll(Bytes.asList(packet.getMessageType().getMessageCode()));
        byteArrayList.addAll(Bytes.asList(ByteBuffer.allocate(4).putInt(packet.getMessageSourceId()).array()));
        byteArrayList.addAll(Bytes.asList(ByteBuffer.allocate(4).putInt(packet.getMessageDestinationId()).array()));
        byteArrayList.addAll(Bytes.asList(ByteBuffer.allocate(4).putInt(packet.getMessageLength()).array()));
        byteArrayList.addAll(Bytes.asList(packet.getMessage().getBytes()));

        log.info("returning bytes array");
        log.info("Execution of convertMessagePacketIntoTheByteArray method ended");

        return Bytes.toArray(byteArrayList);

    }

    /**
     * This returns socket of client to whom we want to send message to
     *
     * @param messageSourceId we get socket channel from source id
     *
     * @return socket channel
     */

    private SocketChannel getSocketChannel(int messageSourceId) {
        log.info("Execution of getSocketChannelFromSourceId started");
        return ClientInfoHolder.informationOfConnectedClients.get(messageSourceId);
    }


    /**
     * This message is used to send message to the client
     *
     * @param packetInBytes packet to send
     * @param channel to whom to send to
     * @throws IOException exception
     */

    public void sendingMessageToClient(byte [] packetInBytes, SocketChannel channel) throws IOException {
        log.info("Execution of writingMessageToServer started");
        log.info("Creating buffer with allocation of private backend space with size {}",packetInBytes.length);
        ByteBuffer messageToServerBuffer = ByteBuffer.allocate(packetInBytes.length);

        log.info("Putting data in bulk into buffer.");
        messageToServerBuffer.put(packetInBytes);

        while(messageToServerBuffer.hasRemaining()){
            log.info("Sending message to the server");
            channel.write(messageToServerBuffer);
        }

        log.info("Message sent to server");
        log.info("Clearing the buffer");
        messageToServerBuffer.clear();
        log.info("Execution of sendMessageToServer ended");
    }


    /**
     * This course takes place when message from client is of
     * logout type.
     *
     * @param packet received from client
     * @throws IOException exception occurred
     */

    private void performLogoutActivity(Packet packet) throws IOException {
        log.info("Execution of performLogoutActivity method started");

        if (!authenticateClient(packet)) return;

        ClientInfoHolder.informationOfMagicNumber.remove(packet.getMessageSourceId());
        log.info("Authentication successful ");

        log.info("Getting socket channel using source id");
        log.info("Convert packet into bytes");
        Packet latestPacket = buildLoggedOutPacket(packet);
        byte [] packetInBytes = convertPacketIntoByteArray(latestPacket);

        log.info("Going to send message on socket");
        sendingMessageToClient(packetInBytes,getSocketChannel(latestPacket.getMessageDestinationId()));
        log.info("Execution of performLogoutActivity method ended");

    }

    /**
     * This method is used to check the magic number we received from client
     * is correct or not
     *
     * @param packet client packet
     * @return true if correct
     *
     * @apiNote needs improvement here
     */

    private boolean authenticateClient(Packet packet) {
        if(ClientInfoHolder.informationOfMagicNumber.get(packet.getMessageSourceId()) == packet.getMagicBytes()){
            log.info("Authentication failed. Discarding the whole message");
            return true;
        }
        return false;
    }

    /**
     * This method returns new packet with logged out information
     *
     * @param packet old packet
     * @return new logged out packet
     */

    private Packet buildLoggedOutPacket(Packet packet) {
        return Packet.builder().magicBytes(packet.getMagicBytes()).messageType(MessageType.LOGOUT).
                messageSourceId(Constants.SERVER_SOURCE_ID).messageDestinationId(packet.getMessageSourceId()).
                messageLength(packet.getMessage().length()).message(packet.getMessage()).build();
    }


    /**
     * This method is used to send message to designated client
     * mentioned in destinationId
     *
     *
     * @param packet information obtained from user no chances made into that packet
     *
     * @throws IOException occurred while writing to the client
     */

    private void performForwardMessageToTheClientActivity(Packet packet) throws IOException {
        log.info("Execution of forwardMessageToTheDestination started");

        if (!authenticateClient(packet)) return;
        log.info("Client Authenticated");

        if(partialReadMessage(packet)) return;
        log.info("Read full message");

        log.info("Getting socket channel using source id");
        SocketChannel socketChannel = getSocketChannel(packet.getMessageDestinationId());

        if(socketChannel != null) {
            log.info("Socket channel is present");
            log.info("Convert packet into bytes");
            byte [] packetInBytes = convertPacketIntoByteArray(packet);
            sendingMessageToClient(packetInBytes,socketChannel);

        } else {
            log.info("Socket channel not present");
            Packet packetInCaseNoSocketPresent = buildPacketInCaseNoSocketPresent(packet);
            log.info("Convert new packet into bytes");
            byte [] packetInBytes = convertPacketIntoByteArray(packetInCaseNoSocketPresent);
            log.info("Send message to the sender about status of message");
            sendingMessageToClient(packetInBytes,
                    getSocketChannel(packetInCaseNoSocketPresent.getMessageDestinationId()));
        }

    }

    /**
     * This packet is built in case we cant find destination socket
     *
     * @param packet old
     * @return new
     */

    private Packet buildPacketInCaseNoSocketPresent(Packet packet) {
       return Packet.builder().magicBytes(packet.getMagicBytes()).messageSourceId(Constants.SERVER_SOURCE_ID).
               messageDestinationId(packet.getMessageSourceId()).messageType(MessageType.DATA).
               message(Constants.MESSAGE_WHEN_DESTINATION_ID_NOT_PRESENT).
               messageLength(Constants.MESSAGE_WHEN_DESTINATION_ID_NOT_PRESENT.length()).build();
    }

    /**
     *
     *   ============================================
     *      THIS NEEDS PROPER IMPLEMENTATION
     *   ============================================
     *
     *
     * @param packet
     * @return
     */

    private boolean partialReadMessage(Packet packet) {
        if(packet.getMessage().length() != packet.getMessageLength()){
            return true;
        }
        return false;
    }


}


