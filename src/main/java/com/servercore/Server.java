/*
    ========= Server Core ==========
 */
package com.servercore;
import com.github.javafaker.Faker;
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
import java.util.HashMap;
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
    private final Map<String,SocketChannel> clientConnectionData = new HashMap<>();


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
                log.info("Removing the selection key");
                selector.selectedKeys().remove(selectionKey);
                log.info("Selection key removed");
                log.info("Calling channelEventsProcessor method");
                channelEventsProcessor(selectionKey);

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
                SocketChannel channel = acceptClientConnectionRequest(selectionKey);
            }

            else {
                
                log.info("Calling process Client Messages");
                processClientMessages(selectionKey);
            }

        }
        log.info("Execution channelEventsProcessor method ended");
    }

    private SocketChannel acceptClientConnectionRequest(SelectionKey key) throws IOException {

        log.info("Get the server socket channel on which event has occurred");
        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = channel.accept();
        log.info("Connection got accepted");
        socketChannel.configureBlocking(false);
        log.info("Socket Channel is now non blocking");

        socketChannel.register(key.selector(), SelectionKey.OP_READ);
        log.info("Socket Channel got registered on read events");

        pendingData.put(socketChannel,new ConcurrentLinkedQueue<>());

        return socketChannel;

    }

    private String assigningUserNameToTheClient(SocketChannel channel) {
        Faker faker = new Faker();
        String userName = faker.name().username();
        boolean assumeUserNameNotPresentInMap = true;

        while (assumeUserNameNotPresentInMap){

            if(!clientConnectionData.containsKey(userName)){
                clientConnectionData.put(userName,channel);
                assumeUserNameNotPresentInMap = false;
            } else userName = faker.name().username();
        }

        return userName;
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

/*
change this:
    First I need to ask
    1. Randomly assign user name to client and save it <>
    2. Send the username to the client <>
    3. Ask the client to whom he wanted to talk <>
    4. If he says some username: Create a link
    5. If that link is present than proceed to talking
    6. If link is not present send message to client please tell us name

    7. Exit strategy - LATER

 */