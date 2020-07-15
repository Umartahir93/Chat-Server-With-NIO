/*
    ========= Server Core ==========
 */
package com.servercore;
import com.chattypes.GroupChat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;

/**
 * This class Contains server core logic
 * Here we have a Listener which keeps
 * listening for client connection requests
 * in 'While-Accept Loop'
 */
@Slf4j
@RequiredArgsConstructor
public class Server {
    private final int port;
    ServerSocket serverSocket;
    GroupChat groupChat = new GroupChat();

    /**
     * This function is entry point of server core
     * It will perform two major functions
     * First it will call listener
     * Second it will handle all the exceptions occure
     * at server core level
     */
    public void startServer() {
        log.info("Execution of startServer started");

        try {

            log.info("Calling clientConnectionRequestListener method");
            clientConnectionRequestListener();


        } catch (IOException ioException) {
            log.error("IO Exception occurred at {}", LocalDate.now().toString());
            log.error("IO Exception cause is {}", ioException.getCause().toString());
            ioException.printStackTrace();

        } catch (Exception exception) {
            log.error("Exception occurred at {}", LocalDate.now().toString());
            log.error("Exception cause is {}", exception.getCause().toString());
            exception.printStackTrace();
        }

        log.info("Execution of startServer ended");
    }

    /**
     * This function is vital and act as client request
     * listener with the help of his While-Accept loop
     *
     * @throws IOException Can be caused by Socket/ServerSocket operations
     *
     * @apiNote Need to come up with better approach than while(true)
     */
    private void clientConnectionRequestListener() throws IOException {

        log.info("Execution of clientConnectionRequestListener started");

        try {
            log.info("Creating ServerSocket on port {}",port);
            serverSocket = new ServerSocket(port);
            log.info("ServerSocket created on port {}", port);

            while (true) {
                log.info("Calling acceptClientConnectionRequest method");
                acceptClientConnectionRequest();
            }

        }finally {
            if(!serverSocket.isClosed())
            {
                log.info("Closing server socket at port {}" , port);
                serverSocket.close();
                log.info("Server socket port {} closed" , port);
            }

            log.info("Execution of clientConnectionRequestListener method ended");
        }
    }

    /**
     * This is helper function which accepts client
     * connection request and returns a scoket after
     * acceptance
     *
     * it Also calls method to add user in group
     *
     * @throws IOException Can be caused by Socket/ServerSocket operations
     *
     * @apiNote Here the limitation is , No matter how fast connections
     * are coming, and no matter how many processors or network interfaces your
     * machine has, you get the connections one at a time because of
     * accept method()
     */
    private void acceptClientConnectionRequest() throws IOException {
        log.info("Execution of acceptClientConnectionRequest method started");
        log.info("Waiting for client request");
        Socket socket = serverSocket.accept();
        //BLOCKING CALL HANDSHAKING WITHIN SECONDS SIMULATOR
        log.info("Client connection request accepted and socket opened");
        log.info("Adding User socket to Group");
        groupChat.addUserToGroupAndStartCommunicationThread(socket);

        log.info("Execution of acceptClientConnectionRequest method ended");
    }

}
