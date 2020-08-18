package com.servercore;

import com.google.common.primitives.Bytes;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

import static java.lang.System.exit;


@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Reader {
    private static Reader reader;

    //ask about this
    private final ByteBuffer readByteBuffer = ByteBuffer.allocate(256*256);


    public static Reader getReaderInstance(){
        if(reader == null)
            reader = new Reader();

        return reader;
    }

    public void readMessagesFromClient(SelectionKey selectionKey, BlockingQueue<byte []> messageQueue) {
        log.info("Read Event has occurred on channel");
        log.info("Execution of method readMessagesFromClient started");

        try{
            log.info("Get the socket channel on which read event has occurred");
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

            log.info("Calling checkConnectionWithClient method ()");
            if (!checkConnectionWithClient(socketChannel)) return;

            byte[] messageFromBuffer = readingMessageFromBufferIntoByteArray();
            messageQueue.put(messageFromBuffer);

        }catch (Exception exception){
            log.error("Exception occurred ",exception);
            exception.printStackTrace();
        }

    }

    private boolean checkConnectionWithClient(SocketChannel socketChannel) throws IOException {
        log.info("Execution of checkConnectionWithClient() method started");

        if((socketChannel.read(readByteBuffer)) == -1){
            log.info("Connection session is not on with client");
            removeClientInformationFromServer(socketChannel);
            log.info("Closing channel with client");
            socketChannel.close();
            return false;
        }

        log.info("Connection is ON with client");
        log.info("Execution of socketChannel() method ended");
        return true;
    }

    /**
     * See this method in detail later
     *
     * @param socketChannel
     */
    private void removeClientInformationFromServer(SocketChannel socketChannel) {
        Optional<Integer> found = Optional.empty();
        for (Map.Entry<Integer, SocketChannel> entry : ClientInfoHolder.informationOfConnectedClients.entrySet()) {
            if (socketChannel.equals(entry.getValue())) {
                Integer integerSocketChannelEntryKey = entry.getKey();
                found = Optional.of(integerSocketChannelEntryKey);
                break;
            }
        }
        int key = found.get();

        ClientInfoHolder.informationOfConnectedClients.remove(key);
        ClientInfoHolder.informationOfMagicNumber.remove(key);
    }

    private byte[] readingMessageFromBufferIntoByteArray() {
        log.info("Execution of readingMessageFromBufferIntoByteArray() method started");

        log.info("Flipping into read mode");
        readByteBuffer.flip();

        /*
        Note below test was successful
        and it can only be carried out wit help of
        simulator
         */

        //ADDED THIS FOR more than one message reading TESTING THROUGH SIMULATOR START
        byte[] messageInBytes = new byte[readByteBuffer.limit()];
        if (messageInBytes.length == 65536){
            //exit(0);
            System.out.println("Buffer is full");
        }else
            System.out.println("Buffer not full");
        //ADDED THIS FOR more than one message reading  TESTING THROUGH SIMULATOR END




        
        log.info("Reading message from buffer");

        while (readByteBuffer.hasRemaining())
            readByteBuffer.get(messageInBytes);

        log.info("Clearing the buffer");
        readByteBuffer.clear();


        log.info("Execution of readingMessageFromBuffer() method ended");
        return messageInBytes;
    }
}



