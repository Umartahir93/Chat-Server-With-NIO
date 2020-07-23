package com.servercore;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Queue;


@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Reader {

    static void readMessagesFromClient(SelectionKey selectionKey,Map<SocketChannel, Queue<ByteBuffer>> pendingData) {
        log.info("Read Event has occurred on channel");
        log.info("Execution of method readMessagesFromClient started");

        try{
            log.info("Get the socket channel on which read event has occurred");
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            log.info("Allocate a buffer for the message");
            ByteBuffer buffer = ByteBuffer.allocateDirect(2048);
            int read = socketChannel.read(buffer);

            if(read == -1){
                log.info("Removing socket from map since number of bytes are -1");
                socketChannel.close(); //this line is so important without it your server can be halted (SEE IN DETAIL) Later
                pendingData.remove(socketChannel);
                return;

            }

            buffer.flip();
            pendingData.get(socketChannel).add(buffer);
            socketChannel.register(selectionKey.selector(),SelectionKey.OP_WRITE);

            Writer.messageWritingThreadPool.submit(()->Writer.writeMessagesToTheClient(selectionKey,pendingData));

        }catch (Exception exception){
            log.error("Exception occurred ",exception);
            exception.printStackTrace();
        }


    }
}



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


