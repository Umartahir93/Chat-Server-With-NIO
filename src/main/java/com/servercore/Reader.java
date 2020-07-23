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
         * I think maybe this Needs to improve this multithreading part
         *
         * it should something like producer consumer strategy
         * where there will be only one queue -ASK hassan bhai
         *
         * but I think we are using producer consumer in our project
         *
         * haar client kee id hoo geee unique identier, message identifer, server consumer threa
         * source message destination keyaa haaai
         *
         * ---------------------------------------------
         *
         * source tou packet is a jayee gaaaa. Message kaa structure aisyee banyee
         *
         *
         * 2 bytes -->  magic bytes
         * 2 bytes -->  message type (login, logout, data) authentication
         * 2 bytes -->  source
         * 2 bytes -->  destination
         * 2 bytes ---> message length
         *
         * username | sasadasdasdsadsadasdasdasdasdd
         *
         *-----------------------------------------------
         * Packet protocol
         * processing
         * communication k lyee protocol khud bannaaa paryee gaaa
         *
         * --------------------------------------------------
         *
         * Us case main message pattern change ordering
         * in order processing karni haai
         *
         *
         */

        Writer.messageWritingThreadPool.submit(()->Writer.writeMessagesToTheClient(selectionKey,pendingData));


    }
}


