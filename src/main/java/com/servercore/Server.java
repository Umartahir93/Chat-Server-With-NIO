/*
    ========= Server Core ==========
 */
package com.servercore;
import com.utilities.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.time.LocalDate;
import java.util.concurrent.*;


@Slf4j
@RequiredArgsConstructor
public class Server {
    private final int port;
    private ServerSocketChannel serverSocketChannel;
    private final BlockingQueue<byte[]> messageQueue = new LinkedBlockingQueue<>();
    private final ExecutorService threadPoolToGenerateAndSendId = Executors.newFixedThreadPool(Constants.NUMBER_OF_THREADS_IN_THREAD_POOL);


    public void startListeningRequests() {
        log.info("Execution of startListeningRequests started");
        Selector selector = null;

        try {

            log.info("Calling settingUpServerChannelAndSelector method");
            selector = settingUpServerChannelAndSelector();

            log.info("Starting writer thread");
            Thread writerThread = new Writer(messageQueue,serverSocketChannel);
            writerThread.setDaemon(true); // see this later
            writerThread.start();


            log.info("Calling eventsListenerOfRegisteredChannels method");
            eventsListenerOfRegisteredChannels(selector);


        } catch (Exception exception) {
            log.error("Exception occurred at {}", LocalDate.now().toString());
            log.error("Exception cause is", exception);
            exception.printStackTrace();
        }finally {
            finallyBlockExecutionForGraceFulShutdown(selector);
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
                if (selectionKey.isValid()) processorOfAcceptOrReadEvent(selectionKey);


            }
        }
        log.info("Finished eventsListenerOfRegisteredChannels execution method");
    }

    private void processorOfAcceptOrReadEvent(SelectionKey selectionKey) throws IOException {
        log.info("Execution processorOfAcceptOrReadEvent method started");

        if (selectionKey.isAcceptable())
            acceptClientConnectionRequest(selectionKey);
        else if (selectionKey.isReadable())
            readAndProcessMessagePacket(selectionKey);

        log.info("Execution channelEventsProcessor method ended");
    }

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

        ClientInfoHolder clientInformationMaintainer = new ClientInfoHolder();

        threadPoolToGenerateAndSendId.submit(()->{
            clientInformationMaintainer.sendGeneratedSourceIdToClient(socketChannel);
            key.selector().wakeup();
        });

        log.info("Execution of acceptClientConnectionRequest method has stopped");
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
