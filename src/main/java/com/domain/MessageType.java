package com.domain;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum for message types which server can receive
 *
 * @author umar.tahir@afiniti.com
 */

@Getter
public enum MessageType {
    LOGIN("LI"),
    LOGOUT("LO"),
    GENERATED_ID("ID"),
    DATA("DT");

    private final String messageCode;

    MessageType(String code) {
        this.messageCode = code;
    }

    /**
     * It gives us enum on its string value
     *
     * @param text given value
     * @return enum against it
     */

    public static Optional<MessageType> fromTextGetMessageType(String text) {
        return Arrays.stream(values()).
                filter(messageType -> messageType.messageCode.equalsIgnoreCase(text))
                .findFirst();
    }
}
