package com.servercore;

import com.domain.Packet;
import com.google.common.primitives.Bytes;
import com.utilities.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@NoArgsConstructor
public class ClientInfoHolder {

    private static final AtomicInteger clientCounter = new AtomicInteger();
    protected static final Map<Integer,SocketChannel> informationOfConnectedClients = new ConcurrentHashMap<>();
    protected static final Map<Integer,Integer> informationOfMagicNumber = new HashMap<>();

    /**
     * This method takes the channel which is created after the
     * acceptance of connection. In this method we generate Id
     * and pass it to client.
     *
     * @param channel client channel
     */
    public void sendGeneratedSourceIdToClient(SocketChannel channel){
         log.info("Execution of sendGeneratedSourceIdToClient method started");

         try{
             log.info("Execution of sendGeneratedSourceIdToClient method started");
             log.info("Calling createClientID() and makingPacketWithGeneratedId()");
             int clientId = createClientID();
             Packet packet = makingPacketWithGeneratedId(clientId);
             log.info("Calling convertPacketIntoByteArray() method and sendingMessageToClient");
             sendingMessageToClient(convertPacketIntoByteArray(packet),channel);
             log.info("Calling savingInfoOfConnectedClients() method");
             savingInfoOfConnectedClients(clientId,channel);

         }catch (Exception exception){
             log.error("Exception occur while sending packet to client");
             exception.printStackTrace();
         }
         log.info("Execution of sendGeneratedSourceIdToClient method ended");
    }



    /**
     * Save the client's information
     *
     * @param clientId id of the client
     * @param channel client's channel
     */
    private void savingInfoOfConnectedClients(int clientId,SocketChannel channel) {
        informationOfConnectedClients.put(clientId,channel);
    }



    /**
     * This method create client Id which will be used
     * by client to identify himself
     *
     * @return client id
     */
    private static int createClientID(){
        return clientCounter.getAndIncrement();
    }



    /**
     * In this method we will make a packet with new information
     *
     * @param clientID id
     * @return packet
     */
    private Packet makingPacketWithGeneratedId(int clientID) {
        return Packet.builder().magicBytes(Constants.NO_MAGIC_BYTES_DEFINED).messageSourceId(Constants.SERVER_SOURCE_ID).
                messageDestinationId(clientID).messageLength(Constants.MESSAGE_FROM_SERVER.length()).
                message(Constants.MESSAGE_FROM_SERVER).build();
    }



    /**
     * This function is used to generate magic number and put it in
     * map for further access
     *
     * @param sourceId represents ID assigned to client
     * @return magic number to send to client
     * s
     */
    public static int generateMagicNumberForAuthentication(int sourceId){
        int magicNumber =  new Random(System.nanoTime()).nextInt(Constants.UPPER_LIMIT_FOR_RANDOM_NUMBER);
        informationOfMagicNumber.put(sourceId,magicNumber);
        return magicNumber;
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

}
