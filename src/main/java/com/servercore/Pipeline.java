package com.servercore;

import com.domain.MessageType;
import com.domain.Packet;
import com.utilities.Constants;
import com.utilities.UtilityFunction;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.*;


/**
 *
 * This pipeline is created to read bytes
 * and convert it into packet. If there
 * are no bytes to consume at specific
 * stage than pipeline will get halt at
 * stage until we get something to consume
 *
 * The bytes array follow all the standards set up by use.
 *      *
 *      *  *  first,  4 bytes for magic number
 *      *  *  second, 2 bytes for message types
 *      *  *  third,  4 bytes for source id
 *      *  *  fourth, 4 bytes for destination id
 *      *  *  fifth,  4 bytes for message length
 *      *  *  six,    x number of bytes for message
 *
 */

@Getter
@Setter
@Slf4j
public class Pipeline {
    private List<IntSupplier> pipelineSteps = new ArrayList<>();
    private Queue<Byte> messageByteQueue = new LinkedList<>();
    private boolean continuePipeLineProcess;
    private boolean packetIsReady;
    private int currentStage;
    private Packet packet;


    //This block will gets executed on creation of the object
    {
        pipelineSteps.add(initiatePipeline());
        pipelineSteps.add(firstStage());
        pipelineSteps.add(secondStage());
        pipelineSteps.add(thirdStage());
        pipelineSteps.add(fourthStage());
        pipelineSteps.add(fifthStage());
        pipelineSteps.add(sixthStage());
        pipelineSteps.add(finalStage());
    }


    /**
     *
     * Method which starts pipeline by making flag set to true
     *
     */

    void startPipeline(){
        continuePipeLineProcess = true;
    }


    /**
     * Here the pipeline is initiated
     *
     * @return the next stage to execute
     *
     */

    public IntSupplier initiatePipeline() {
        return () -> {
            log.info("Execution of initiatePipeline stage started");
            packetIsReady = false;
            packet = Packet.builder().build();
            currentStage = 1;
            log.info("Execution of initiatePipeline stage ended");
            return currentStage;

        };
    }

    /**
     * This stage gets magic number from the
     * bytes array and convert it into no
     *
     * @return the next stage to execute
     *
     */

    public IntSupplier firstStage(){
        return ()-> {
            log.info("Execution of firstStage stage started");
            int magicNumber = getIntFromBytes().getAsInt();

            if(magicNumber != -1 ){
                log.info("Setting Magic Bytes");
                packet.setMagicBytes(magicNumber);
                currentStage++;
                return currentStage;
            }

            continuePipeLineProcess = false;
            return Constants.STAGE_DOESNT_EXEC_SUCCESSFULLY;
        };
    }

    /**
     *
     *  Helper function to convert bytes to integer
     *
     * @return integer value or -1 in case of
     * stage doesnt executed successfully
     *
     */

    private IntSupplier getIntFromBytes(){
        return () -> {
            if(messageByteQueue.size() >= Constants.BYTE_ARRAY_SIZE_FOR_INT){
                byte [] bytes = new byte[Constants.BYTE_ARRAY_SIZE_FOR_INT];

                for(int i = 0; i < Constants.BYTE_ARRAY_SIZE_FOR_INT ; i++){
                    bytes[i] = messageByteQueue.poll();
                }

                return UtilityFunction.getIntFromByteArray(bytes);

            }
            return Constants.STAGE_DOESNT_EXEC_SUCCESSFULLY;
        };
    }

    /**
     *
     * The second stage which returns message type
     * after converting byte array into message
     * type
     *
     * @return next stage to execute
     *
     */

    public IntSupplier secondStage(){
        return ()->{
            MessageType type = getQueueMessageTypeFunction().get();
            if(type != null){
                packet.setMessageType(type);
                currentStage++;
                return currentStage;
            }

            continuePipeLineProcess = false;
            return Constants.STAGE_DOESNT_EXEC_SUCCESSFULLY;
        };
    }

    /**
     *
     * Helper function to convert bytes into
     * messageType
     *
     * @return next stage to execute
     *
     */

    private Supplier<MessageType> getQueueMessageTypeFunction() {
        return ()->{
            if(messageByteQueue.size() >= Constants.BYTE_ARRAY_SIZE_FOR_MESSAGE_TYPE){
                byte [] bytes = new byte[Constants.BYTE_ARRAY_SIZE_FOR_MESSAGE_TYPE];

                for(int i = 0; i < Constants.BYTE_ARRAY_SIZE_FOR_MESSAGE_TYPE ; i++){
                    bytes[i] = messageByteQueue.poll();

                    if(bytes[i] == 32){
                        System.out.println(packet.getMagicBytes());
                        while (!messageByteQueue.isEmpty())
                            System.out.println(messageByteQueue.poll());

                    }
                }

                System.out.println("bytes size "+bytes.length+" String "+new String(bytes));
                return MessageType.fromTextGetMessageType(new String(bytes)).get();
            }
            return null;
        };
    }

    /**
     *
     * Third stage of pipeline to convert bytes into source id
     *
     * @return next stage to execute
     *
     */

    public IntSupplier thirdStage(){
        return ()-> {
            int sourceId = getIntFromBytes().getAsInt();

            if(sourceId != -1 ){
                packet.setMessageSourceId(sourceId);
                currentStage++;
                return currentStage;
            }

            continuePipeLineProcess = false;
            return Constants.STAGE_DOESNT_EXEC_SUCCESSFULLY;

        };
    }


    /**
     *
     * fourth stage of pipeline to convert bytes into dest id
     *
     * @return next stage to execute
     *
     */

    public IntSupplier fourthStage(){
        return ()-> {
            int destinationId = getIntFromBytes().getAsInt();

            if(destinationId != -1 ){
                packet.setMessageDestinationId(destinationId);
                currentStage++;
                return currentStage;
            }

            continuePipeLineProcess = false;
            return Constants.STAGE_DOESNT_EXEC_SUCCESSFULLY;
        };
    }


    /**
     *
     * fifth stage of pipeline to convert bytes into length
     *
     * @return next stage to execute
     *
     */

    public IntSupplier fifthStage(){
        return ()-> {
            int length = getIntFromBytes().getAsInt();

            if(length != -1 ){
                packet.setMessageLength(length);
                currentStage++;
                return currentStage;
            }

            continuePipeLineProcess = false;
            return Constants.STAGE_DOESNT_EXEC_SUCCESSFULLY;

        };
    }

    /**
     *
     * fifth stage of pipeline to convert bytes into message
     *
     * @return next stage to execute
     *
     */

    public IntSupplier sixthStage(){
        return ()->{
            String message = getIntegerStringFunction().apply(packet.getMessageLength());
            packet.setMessage(message);
            return currentStage;
        };
    }

    /**
     *
     * Helper function to convert bytes into string
     *
     * @return string
     *
     */

    private Function<Integer, String> getIntegerStringFunction() {
        return length->{
            byte [] bytes;
            int arraySize;

            if(messageByteQueue.isEmpty()){
                continuePipeLineProcess = packet.getMessageType().equals(MessageType.LOGIN) || packet.getMessageType().equals(MessageType.LOGOUT);
                if (continuePipeLineProcess) currentStage++;
                return "";
            }

            if(messageByteQueue.size() < length)
                arraySize = messageByteQueue.size();

            else{
                arraySize = length;
                currentStage++;
            }


            bytes = new byte[arraySize];
            for(int i = 0 ; i<bytes.length ; i++)
                bytes[i] = (byte) messageByteQueue.poll();


            return new String(bytes);
        };
    }

    /**
     *
     * final stage tells us packet is ready to consume
     *
     * @return next stage to execute
     *
     */

    public IntSupplier finalStage(){
        return () -> {
            currentStage = 0;
            packetIsReady=true;
            continuePipeLineProcess = !messageByteQueue.isEmpty();
            return currentStage;
        };
    }

}
