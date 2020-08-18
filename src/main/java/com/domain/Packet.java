package com.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



/**
 * This class represents domain class for protocol.
 * Protocol is designed in this way:
 * <p>
 * Magic bytes | Message Type | Source id | Destination id | Message Length     | Message
 * 4 Bytes     | 2 Bytes      | 4 Bytes   | 4 Bytes        |  4 Bytes           | x amount of bytes
 *
 * @author umar.tahir@afiniti.com
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
