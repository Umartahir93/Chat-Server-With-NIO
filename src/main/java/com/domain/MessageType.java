package com.domain;

import lombok.Getter;

@Getter
public enum MessageType {
    LOGIN("LI"),
    LOGOUT("LO"),
    GENERATED_ID("ID"),
    DATA("DT");

    private final byte [] messageCode;

    MessageType(String code){
        this.messageCode = code.getBytes();
    }

}
