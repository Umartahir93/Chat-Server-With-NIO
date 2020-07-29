package com.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


/*
    ### Description ###
    ### Magic bytes | Message Type | Source id | Destination id | Message Length     | Message
    ### 4 Bytes     | 2 Bytes      | 4 Bytes   | 4 Bytes        |  4 Bytes           | x amount of bytes

 */

@Getter
@Setter
@Builder
public class Packet {
    private int magicBytes;
    private MessageType messageType;
    private int messageSourceId;
    private int messageDestinationId;
    private int messageLength;
    private String message;

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
 *
 * 2 bytes -->  message type (login, logout, data) authentication  - Done
 * 2 bytes -->  source  -- Use Id here
 * 2 bytes -->  destination  -- User Id here as well
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