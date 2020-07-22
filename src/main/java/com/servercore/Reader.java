package com.servercore;

import com.utilities.Constants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Reader {

    static void readMessagesFromClient(SelectionKey selectionKey,Map<SocketChannel, Queue<ByteBuffer>> pendingData) throws IOException {

        ExecutorService messageWritingThreadPool = Executors.newFixedThreadPool(Constants.NUMBER_OF_THREADS_IN_WRITING_POOL);

        log.info("Get the socket channel on which read event has occurred");
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        log.info("Allocate a buffer for the message");
        ByteBuffer buffer = ByteBuffer.allocateDirect(2048);
        StringBuilder stringBuilder = new StringBuilder();
        int read = 0;


        while ((read = socketChannel.read(buffer))>0){
            buffer.flip();
            byte[]bytes = new byte[buffer.limit()];
            buffer.get(bytes);
            stringBuilder.append(new String(bytes));
            buffer.compact();
        }

        System.out.println(stringBuilder);

        if(read == -1){
            log.info("Removing socket from map since number of bytes are -1");
            pendingData.remove(socketChannel);
            return;
        }else{
            pendingData.get(socketChannel).add(buffer);
        }

        socketChannel.register(selectionKey.selector(),SelectionKey.OP_WRITE);

        /**
         * Needs to improve this multithreading part
         *
         */

        messageWritingThreadPool.submit(()->Writer.writeMessagesToTheClient(selectionKey,pendingData));

    }
}
