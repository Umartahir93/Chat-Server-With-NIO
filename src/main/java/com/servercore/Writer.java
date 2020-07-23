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
public class Writer {

    static ExecutorService messageWritingThreadPool = Executors.newFixedThreadPool(Constants.NUMBER_OF_THREADS_IN_WRITING_POOL);

    static void writeMessagesToTheClient(SelectionKey selectionKey, Map<SocketChannel, Queue<ByteBuffer>> pendingData)  {
        log.info("Execution of Writer thread started");
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        log.info("Got the socket channel");

        Queue<ByteBuffer> queue = pendingData.get(socketChannel);
        log.info("Got the queue for specific socket");
        ByteBuffer buffer;

        try{

            while ((buffer = queue.peek())!=null){
                log.info("Going to write message to the buffer");
                socketChannel.write(buffer);
                if(!buffer.hasRemaining()){
                    log.info("Emptying buffer");
                    queue.poll();
                }else {
                    return;
                }
            }

            log.info("Registering Reading event in selector for specific socket channel");
            socketChannel.register(selectionKey.selector(),SelectionKey.OP_READ);

        }catch (Exception exception){
            log.error("Exception occurred");
        }

    }

}
