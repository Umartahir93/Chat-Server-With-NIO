/*
    ========== DRIVER PROGRAM ================

 */
package com.driver;

import com.servercore.InternalCore;
import com.utilities.InputValidator;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

/**
 * This class is the entry point for the server
 * Our Server Program Execution will starts from
 * here
 */
@Slf4j
public class Driver {

    /**
     *============ Entry point of chat server================
     *===== Port will be provided to chat server from =======
     *=============== command line arguments ================
     *
     * @param args Command line params which should be port
     * number
     *
     */

    public static void main(String[] args) {
        try{

            InputValidator.commandLineArguments().accept(args);
            log.info("=========Starting Server at {} ==========", LocalDate.now().toString());
            int port = Integer.parseInt(args[0]);
            InternalCore internalCore = new InternalCore(port);
            log.info("Calling startServer method() at time {}", LocalDate.now().toString());
            internalCore.startListeningRequests();

        }catch (Exception exception){
            log.error("Exception occurred in the Driver Class at {}",LocalDate.now().toString());
        }finally {
            log.info("=========Stopping Server at {} ========== ", LocalDate.now().toString());
        }
    }
}
