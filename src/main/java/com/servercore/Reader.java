package com.servercore;
import com.google.common.primitives.Bytes;
import com.utilities.Adaptor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;




@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Reader {
    private static Reader reader;
    private final ByteBuffer readByteBuffer = ByteBuffer.allocate(256*256);
    private final ArrayList<Byte> messageInBytes = new ArrayList<>();


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

            messageQueue.put(readingMessageFromBufferIntoByteArray());
            clearingMessageBufferAndMessageList();

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

    private byte[] readingMessageFromBufferIntoByteArray() { //every time new allocation on heap
        Adaptor.lock.lock();
        try{
            log.info("Execution of readingMessageFromBufferIntoByteArray() method started");

            log.info("Flipping into read mode");
            readByteBuffer.flip();

            log.info("Reading message from buffer");
            while (readByteBuffer.hasRemaining())
                messageInBytes.add(readByteBuffer.get());

            log.info("Execution of readingMessageFromBuffer() method ended");
            return Bytes.toArray(messageInBytes);
        }finally {
            Adaptor.lock.unlock();
        }

    }

    private void clearingMessageBufferAndMessageList() {
        log.info("Execution of clearingMessageBufferAndMessageList() method started");

        log.info("Clearing the buffer");
        readByteBuffer.clear();
        log.info("Clearing the arraylist");
        messageInBytes.clear();

        log.info("Execution of clearingMessageBufferAndMessageList() method ended");
    }
}



