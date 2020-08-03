package com.utilities;

import com.domain.MessageType;
import com.domain.Packet;
import com.google.common.primitives.Bytes;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptor class which converts object from one form to another here
 * we are converting packet into byte array vice versa. This class also
 * returns new Packet objects as well.
 *
 * @author umar.tahir@afiniti.com
 *
 */

@Slf4j
public class Adaptor {

    /**
     * In this method we will make a packet with generated
     * client id information. And for rest of information
     * in the packet includes constants which doesnt
     * have significant service value except it provides
     * meta data about packet.
     *
     * @param clientID id
     *
     * @return packet
     *
     */

     public static Packet makingPacketWithGeneratedId(int clientID) {
        return Packet.builder().magicBytes(Constants.NO_MAGIC_BYTES_DEFINED).messageType(MessageType.GENERATED_ID).
                messageSourceId(Constants.SERVER_SOURCE_ID).messageDestinationId(clientID).
                messageLength(Constants.MESSAGE_FROM_SERVER.length()).
                message(Constants.MESSAGE_FROM_SERVER).build();
    }

    /**
     * This helper function is used to generate bytes array
     * from the packet. The byte is supposed to follow all the
     * standards set up by use.
     *
     *  first,  4 bytes for magic number
     *  second, 2 bytes for message types
     *  third,  4 bytes for source id
     *  fourth, 4 bytes for destination id
     *  fifth,  4 bytes for message length
     *  six,    x number of bytes for message
     *
     * @param packet any packet
     *
     * @return byte array
     *
     */

    public static byte[] getBytesArrayFromPacket(Packet packet) {
        log.info("Execution of convertPacketIntoByteArray method started");
        List<Byte> byteArrayList = new ArrayList<>();

        byteArrayList.addAll(Bytes.asList(ByteBuffer.allocate(4).putInt(packet.getMagicBytes()).array()));
        byteArrayList.addAll(Bytes.asList(packet.getMessageType().getMessageCode().getBytes()));
        byteArrayList.addAll(Bytes.asList(ByteBuffer.allocate(4).putInt(packet.getMessageSourceId()).array()));
        byteArrayList.addAll(Bytes.asList(ByteBuffer.allocate(4).putInt(packet.getMessageDestinationId()).array()));
        byteArrayList.addAll(Bytes.asList(ByteBuffer.allocate(4).putInt(packet.getMessageLength()).array()));
        byteArrayList.addAll(Bytes.asList(packet.getMessage().getBytes()));

        log.info("returning bytes array");
        log.info("Execution of convertMessagePacketIntoTheByteArray method ended");

        return Bytes.toArray(byteArrayList);
    }

    /**
     *
     * Method that converts byte array into packet class object
     *
     * The bytes array follow all the standards set up by use.
     *
     *  *  first,  4 bytes for magic number
     *  *  second, 2 bytes for message types
     *  *  third,  4 bytes for source id
     *  *  fourth, 4 bytes for destination id
     *  *  fifth,  4 bytes for message length
     *  *  six,    x number of bytes for message
     *
     * @param message this is message in byte [] which needs to
     *                be converted
     *
     * @return packet object
     *
     */

    public static Packet getPacketFromBytesArray(byte[] message){
        log.info("Execution of convertByteArrayIntoPacket method started");

        int magicBytes          = UtilityFunction.getIntFromByteArray(message,Constants.START_OF_MAGIC_BYTES_INCLUSIVE, Constants.END_OF_MAGIC_BYTES_EXCLUSIVE);

        String messageTypeValue = UtilityFunction.getStringFromByteArray(message,Constants.START_OF_MESSAGE_TYPE_INCLUSIVE,Constants.END_OF_MESSAGE_TYPE_EXCLUSIVE);
        MessageType messageType = MessageType.fromTextGetMessageType(messageTypeValue).get();

        int sourceId            = UtilityFunction.getIntFromByteArray(message,Constants.START_OF_SOURCE_ID_INCLUSIVE,Constants.END_OF_SOURCE_ID_EXCLUSIVE);
        int destId              = UtilityFunction.getIntFromByteArray(message,Constants.START_OF_DEST_ID_INCLUSIVE,Constants.END_OF_DEST_ID_EXCLUSIVE);

        int messageLength       = UtilityFunction.getIntFromByteArray(message,Constants.START_OF_MESSAGE_LENGTH_INCLUSIVE,Constants.END_OF_MESSAGE_LENGTH_EXCLUSIVE);
        String messageOfClient  = UtilityFunction.getStringFromByteArray(message,Constants.START_OF_MESSAGE_INCLUSIVE,message.length);

        log.info("Execution of convertByteArrayIntoPacket method ended");

        return Packet.builder().magicBytes(magicBytes).messageType(messageType).messageSourceId(sourceId).messageDestinationId(destId)
                .messageLength(messageLength).message(messageOfClient).build();
    }

    /**
     *
     * This method is used to build Packet with login details
     * which we will send to client
     *
     * @param packet received from client (request)
     *
     * @param magicNumber generated magic number for logged in client
     *
     * @return new packet to send to clients
     *
     */

    public static Packet getLoggedInPacket(Packet packet,int magicNumber) {
        return Packet.builder().magicBytes(magicNumber).messageType(MessageType.LOGIN).messageSourceId(Constants.SERVER_SOURCE_ID).
                messageDestinationId(packet.getMessageSourceId()).messageLength(packet.getMessageLength()).
                message(packet.getMessage()).build();
    }

    /**
     * This packet is built in case we cant find destination socket
     *
     * @param packet old
     *
     * @return new packet
     *
     */

    public static Packet getPacketWhenNoSocketPresent(Packet packet) {
        return Packet.builder().magicBytes(packet.getMagicBytes()).messageSourceId(Constants.SERVER_SOURCE_ID).
                messageDestinationId(packet.getMessageSourceId()).messageType(MessageType.DATA).
                message(Constants.MESSAGE_WHEN_DESTINATION_ID_NOT_PRESENT).
                messageLength(Constants.MESSAGE_WHEN_DESTINATION_ID_NOT_PRESENT.length()).build();
    }

    /**
     *
     *  This method returns new packet with logged out information
     *
     * @param packet old packet
     *
     * @return new logged out packet
     *
     */

    public static Packet getLoggedOutPacket(Packet packet) {
        return Packet.builder().magicBytes(packet.getMagicBytes()).messageType(MessageType.LOGOUT).
                messageSourceId(Constants.SERVER_SOURCE_ID).messageDestinationId(packet.getMessageSourceId()).
                messageLength(packet.getMessage().length()).message(packet.getMessage()).build();
    }

}
