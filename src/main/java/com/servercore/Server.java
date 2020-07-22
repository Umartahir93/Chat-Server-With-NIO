/*
    ========= Server Core ==========
 */
package com.servercore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.LocalDate;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


@Slf4j
@RequiredArgsConstructor
public class Server {
    private final int port;
    private ServerSocketChannel serverSocketChannel;
    private final Map<SocketChannel, Queue<ByteBuffer>> pendingData = new ConcurrentHashMap<>();


    public void startListeningRequests() {
        log.info("Execution of startListeningRequests started");

        try {

            log.info("Calling settingUpServerChannelAndSelector method");
            Selector selector = settingUpServerChannelAndSelector();
            log.info("Calling channelEventsListenerViaSelector method");
            channelEventsListenerViaSelector(selector);


        } catch (Exception exception) {
            log.error("Exception occurred at {}", LocalDate.now().toString());
            log.error("Exception cause is", exception);
            exception.printStackTrace();
        }finally {
            if(serverSocketChannel.isOpen()){
                log.info("Closing Server Socket Channel");
                try {
                    serverSocketChannel.close();
                } catch (IOException e) {
                    log.error("Error occurred while closing server socket channel");
                    e.printStackTrace();
                }
            }
        }
        
        log.info("Execution of startListeningRequests ended");
    }

    private Selector settingUpServerChannelAndSelector() throws IOException {

        log.info("Execution of settingUpServerChannelAndSelector started");
        serverSocketChannel = ServerSocketChannel.open();
        log.info("ServerSocketChannel opened");

        serverSocketChannel.bind(new InetSocketAddress(port));
        log.info("ServerSocketChannel binding done on port {}",port);

        serverSocketChannel.configureBlocking(false);
        log.info("ServerSocketChannel is now non blocking");

        Selector selector = Selector.open();
        log.info("Selector opened");

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        log.info("Server Socket Channel registered with Selector");

        return selector;
    }

    private void channelEventsListenerViaSelector(Selector selector) throws IOException {
        log.info("Started channelEventsListenerViaSelector execution method");
        while (serverSocketChannel.isOpen()) {
            log.info("Waiting for event to occur");
            selector.select();
            log.info("The event has occurred");

            for (SelectionKey selectionKey : selector.selectedKeys()) {
                log.info("Retrieving key's ready-operation set");
                selectionKey.readyOps();
                log.info("Calling channelEventsProcessor method");
                channelEventsProcessor(selectionKey);
                log.info("Removing the selection key");
                selector.selectedKeys().remove(selectionKey);
                log.info("Selection key removed");
            }
        }
    }

    private void channelEventsProcessor(SelectionKey selectionKey) throws IOException {
        log.info("Execution channelEventsProcessor method started");
        if (selectionKey.isValid()) {
            log.info("Selection key is valid");

            if (selectionKey.isAcceptable()){
                log.info("Accept event has occurred");
                log.info("Calling acceptClientConnectionRequest to accept client connection");
                acceptClientConnectionRequest(selectionKey);

            }
            else {
                log.info("Calling process Client Messages");
                processClientMessages(selectionKey);
            }

        }
        log.info("Execution channelEventsProcessor method ended");
    }

    private void acceptClientConnectionRequest(SelectionKey key) throws IOException {

        log.info("Get the server socket channel on which event has occurred");
        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = channel.accept();
        log.info("Connection got accepted");
        socketChannel.configureBlocking(false);
        log.info("Socket Channel is now non blocking");

        socketChannel.register(key.selector(), SelectionKey.OP_READ);
        log.info("Socket Channel got registered on read events");

        pendingData.put(socketChannel,new ConcurrentLinkedQueue<>());

    }

    private void processClientMessages(SelectionKey selectionKey) throws IOException {
        log.info("Execution of processClientMessages method started");

        if (selectionKey.isReadable()) {
            log.info("Read Event has occurred on channel");
            log.info("Calling readClientMessagesFromClient");
            Reader.readMessagesFromClient(selectionKey,pendingData);
        }

        log.info("Execution of processClientMessages method ended");
    }

}
