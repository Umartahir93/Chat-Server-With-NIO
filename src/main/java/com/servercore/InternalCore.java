/*
    ========= Server Core ==========
 */
package com.servercore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.time.LocalDate;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class holds internal core  implementation of server.
 * Here we are accepting connections reading read events and
 * starting writer thread.
 * <p>
 * This class also contains message queue. In which we put our messages
 * and it is shared among producer and consumer
 *
 * @author umar.tahir@afiniti.com
 */

@Slf4j
@RequiredArgsConstructor
public class InternalCore {
    private final int port;
    private final BlockingQueue<byte[]> messageQueue = new LinkedBlockingQueue<>();
    private ServerSocketChannel serverSocketChannel;


    /**
     * This method act as listener for the server
     * We start our server program from this point
     * It also handles exceptions which are thrown
     * from below methods
     *
     * @since 30 July 2020
     */

    public void startListeningRequests() {
        log.info("Execution of startListeningRequests started");
        Selector selector = null;

        try {

            log.info("Calling settingUpServerChannelAndSelector() method");
            selector = settingUpServerChannelAndSelector();

            log.info("Starting writer thread and it will run until serverSocketChannel is open");
            Thread writerThread = new Writer(messageQueue,serverSocketChannel);
            writerThread.start();

            log.info("Writer thread started");

            log.info("Calling eventsListenerOfRegisteredChannels method");
            eventsListenerOfRegisteredChannels(selector);


        } catch (Exception exception) {
            log.error("Exception occurred at {}", LocalDate.now().toString());
            log.error("Exception cause is", exception);
            exception.printStackTrace();
        } finally {
            finallyBlockExecutionForGraceFulShutdown(selector);
        }

        log.info("Execution of startListeningRequests ended");
    }


    /**
     * This method set up server channel and selector which are
     * used to accept client connections and through selector we
     * can access server socket channel
     *
     * @return selector to which server socket channel is registered at the
     * moment
     * @throws IOException through exception which is handled above
     */
    private Selector settingUpServerChannelAndSelector() throws IOException {

        log.info("Execution of settingUpServerChannelAndSelector method started");
        serverSocketChannel = ServerSocketChannel.open();
        log.info("ServerSocketChannel opened");

        serverSocketChannel.bind(new InetSocketAddress(port));
        log.info("ServerSocketChannel binding done on port {}", port);

        serverSocketChannel.configureBlocking(false);
        log.info("ServerSocketChannel is now non blocking");

        Selector selector = Selector.open();
        log.info("Selector opened");

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        log.info("Server Socket Channel registered with Selector");

        log.info("Execution of settingUpServerChannelAndSelector method ended");

        return selector;
    }

    /**
     * This method deals with all the events that are generated in
     * the registered channels. At the start only server socket
     * channel is registered with the selector.
     * <p>
     * But moving forward selector will have more than one channels
     *
     * @param selector with registered server socket channel
     * @throws IOException is handled above
     */

    private void eventsListenerOfRegisteredChannels(Selector selector) throws IOException {
        log.info("Started eventsListenerOfRegisteredChannels execution method");
        while (serverSocketChannel.isOpen()) {
            log.info("Waiting for event to occur");
            selector.select();
            log.info("The event has occurred");

            for (SelectionKey selectionKey : selector.selectedKeys()) {
                log.info("Retrieving key's ready-operation set");
                selectionKey.readyOps();
                log.info("Removing the selection key");
                selector.selectedKeys().remove(selectionKey);
                log.info("Selection key removed");
                log.info("Call processorOfAcceptOrReadEvent method if selection key is valid");
                if (selectionKey.isValid())
                    processorOfAcceptOrReadEvent(selectionKey);

            }
        }
        log.info("Finished eventsListenerOfRegisteredChannels execution method");
    }

    /**
     * Process that channel in which event has occurred. It can
     * process two types of events occurred in the channel.
     * <p>
     * Event types are Accept or read events
     *
     * @param selectionKey Using this key we can access the channel
     * @throws IOException is handled above
     */

    private void processorOfAcceptOrReadEvent(SelectionKey selectionKey) throws IOException {
        log.info("Execution processorOfAcceptOrReadEvent method started");

        if (selectionKey.isAcceptable())
            acceptClientConnectionRequest(selectionKey);
        else if (selectionKey.isReadable())
            readAndProcessMessagePacket(selectionKey);

        log.info("Execution channelEventsProcessor method ended");
    }

    /**
     * This method is used to accept client connection requests and
     * we generate source id for each connected clients. The generation
     * of source id task is passed to a thread pool
     *
     * @param key of channel in which event occurred
     * @throws IOException handled above
     */

    private void acceptClientConnectionRequest(SelectionKey key) throws IOException {
        log.info("Execution of acceptClientConnectionRequest method has started");
        log.info("Get the server socket channel on which event has occurred");

        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = channel.accept();
        log.info("Connection got accepted");

        socketChannel.configureBlocking(false);
        log.info("Socket Channel is now non blocking");

        socketChannel.register(key.selector(), SelectionKey.OP_READ);
        log.info("Socket Channel got registered on read events");

        log.info("Calling submitIdGenerationJob()");
        submitIdGenerationJob(socketChannel, key);

        log.info("Execution of acceptClientConnectionRequest method has stopped"); //see till here

    }

    /**
     * This method is used to submit a job to threadpool for id generation
     * and sending that to client
     *
     * @param socketChannel channel for which Id is created
     * @param key           used to access the channel
     */
    private void submitIdGenerationJob(SocketChannel socketChannel, SelectionKey key) {
        log.info("Execution of submitIdGenerationJob method started");
        ClientInfoHolder clientInformationMaintainer = new ClientInfoHolder();

        log.info("Your job is submitted to thread pool");

        ClientInfoHolder.threadPoolToGenerateAndSendId.submit(() -> {
            clientInformationMaintainer.sendGeneratedSourceIdToClient(socketChannel);
            key.selector().wakeup();
        });

        log.info("Execution of submitIdGenerationJob method ended");
    }

    private void readAndProcessMessagePacket(SelectionKey selectionKey) {
        log.info("Execution of processClientMessages method started");
        Reader.getReaderInstance().readMessagesFromClient(selectionKey,messageQueue);
        log.info("Execution of processClientMessages method ended");
    }

    private void finallyBlockExecutionForGraceFulShutdown(Selector selector) {
        log.info("Execution of finallyBlockExecutionForGraceFulShutdown started");

        if(serverSocketChannel.isOpen()){
            log.info("Closing Server Socket Channel");
            try {
                serverSocketChannel.close();
                log.info("serverSocketChannel is closed");
            } catch (IOException e) {
                log.error("Error occurred while closing server socket channel");
                e.printStackTrace();
            }
        }

        log.info("Calling selectorShutdown");
        selectorShutdown(selector);
    }

    private void selectorShutdown(Selector selector) {
        log.info("Execution of selectorShutdown started");
        if(selector != null && selector.isOpen()){

            for (SelectionKey selectionKey : selector.keys()) {
                log.info("Selecting Channel from key");
                SelectableChannel channel = selectionKey.channel();
                if (channel instanceof SocketChannel) {
                    log.info("channel is instance of SocketChannel");
                    SocketChannel socketChannel = (SocketChannel) channel;

                    try {
                        socketChannel.close();
                        log.info("Socket Channel closed");
                    } catch (IOException e) {
                        log.error("Error occurred while closing socket");
                        e.printStackTrace();
                    }

                    log.info("Cancelling selection key");
                    selectionKey.cancel();
                }
            }

            log.info("Closing the selector");
            try {
                selector.close();
                log.info("Selector closed");
            } catch (IOException e) {
                log.error("Error occurred while closing selector");
                e.printStackTrace();
            }
        }
    }
}
