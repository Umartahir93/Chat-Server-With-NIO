/*
    ========= Clinet Communication Processes ==============
 */
package com.clientprocessor;
import com.chattypes.GroupChat;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * The class represents per client processing
 * by creating thread for each client
 *
 */

@Slf4j
@AllArgsConstructor
public class ServerThread extends Thread{
    private final GroupChat groupChat;
    private Socket socket;

    /**
     * This is entry point for each per client thread created
     * Here we are doing two things:
     *
     * Firstly we are calling utility method for communication
     * between clients
     *
     * Secondly, we are handling all the Exceptions occurred at the
     * server thread level
     */
    @Override
    public void run(){
        log.info("Thread {} Execution started ",Thread.currentThread().getName());
        try {

            log.info("Creating data input stream from socket");
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            log.info("Data input stream gets created from socket");

            communicationBetweenClients(dataInputStream);


        } catch (IOException exception) {
            log.info("===== Catch Executed ============");
            log.error(" connection lost with client");
            //log.error("IOException cause is {}",exception.getCause().toString());
            //exception.printStackTrace();

        } catch (Exception exception){
            log.error("Exception occurred while doing communication between clients");
            log.error("Exception cause is {}",exception.getCause().toString());
            exception.printStackTrace();
        }
    }

    /**
     * This method is used to send your messages to all the clients
     * here we are also calling a method to remove dead connections
     *
     * @param dataInputStream represents messages sent from client
     * @throws IOException occurred during operations at the socket level
     */
    private void communicationBetweenClients(DataInputStream dataInputStream) throws IOException {
        log.info("Execution of communicationBetweenClients method started");
        try {
            while (true) {
                log.info("Trying to read Message from the client");
                String clientMessage = dataInputStream.readUTF();

                //blocking call //haar client apnyeee dataInputStream peer suun rahaa haai
                //haar client ke apnee blocking call haai

                //how to deal with this blocking mechanism?

                log.info("Message read from the client");
                log.info("Trying to send Message to all other clients");
                groupChat.sendMessageToAllGroupMembers(clientMessage);
            }
        }finally {
            log.info("===== Finally Executed ============");
            log.info("Removing connection details from server ");
            groupChat.removeConnection( socket );
            log.info("Thread {} Execution ended ",Thread.currentThread().getName());
        } //solve the exception overridden problem which hassan bhai highlighted
    }
}
