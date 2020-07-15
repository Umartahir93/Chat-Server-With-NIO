package com.chattypes;

import com.clientprocessor.ServerThread;
import lombok.extern.slf4j.Slf4j;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents GroupChat in our Chat application
 *
 * @apiNote Internal synchronization can be bottleneck.
 *
 */

@Slf4j
public class GroupChat {
    private final HashMap<Socket, DataOutputStream> outputStreams = new HashMap<>();

    /**
     * This method add Users in same group and to handle each
     * User it also creates a per-client thread
     *
     * @param socket returned by Server socket and it represents
     *               connection with remote socket
     *
     * @throws IOException Occurred during execution of functions
     *                     in Socket Class
     *
     */
    public void addUserToGroupAndStartCommunicationThread (Socket socket) throws IOException {

        log.info("addUserToGroupAndStartCommunicationThread method execution started");
        log.info("DataOutputStream gets created from socket");
        DataOutputStream writingBackToClient = new DataOutputStream(socket.getOutputStream());

        log.info("Putting socket and data stream in hashMap. This is thread safe");
        synchronized (outputStreams){
            outputStreams.put(socket,writingBackToClient);
        }

        log.info("Creating and starting a thread for a communication between different clients");
        new ServerThread(this, socket).start();
    }

    /**
     * This method is called inorder to send messages to all the members
     * of the group
     *
     *
     * @param message came from one of the member
     * @throws IOException occurred during sending of messages to all the user
     */
    public void sendMessageToAllGroupMembers(String message) throws IOException {
        log.info("Execution of sendMessageToAllGroupMembers method started ");
        synchronized (outputStreams){

            log.info("Trying to read value from HashMap<Socket,DataOutPutSocket>");
            for (Map.Entry<Socket,DataOutputStream> entry : outputStreams.entrySet()){
                DataOutputStream dout = (DataOutputStream) entry.getValue();
                log.info("Value read from HashMap<Socket,DataOutPutSocket>");
                log.info("Sending message to the client");
                dout.writeUTF( message );
                log.info("Message sent to the client");
            }
            log.info("Message sent to all the clients");

        }

        log.info("Execution of sendMessageToAllGroupMembers method ended");
    }

    /**
     * This method is used to remove all the dead connections from a group
     *
     *
     * @param socket representing connections
     * @throws IOException can be caused because of operations in Socket class
     *
     */
    public void removeConnection (Socket socket) throws IOException {
        log.info("Execution of removeConnection started");
        synchronized( outputStreams ) {
            log.info("Removing Connection for socket");
            outputStreams.remove(socket);
            socket.close();
        }
    }
}
