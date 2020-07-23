package com.utilities;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;

@Slf4j
public class InputValidator {

    public static Consumer<String []> commandLineArguments(){
        return args -> {
            if(args.length == 0) {
                log.info("Please provide command line argument");
                System.exit(0);
            }
            else if(!StringUtils.isNumeric(args[0])){
                log.info("Please provide port number as command line argument");
                System.exit(0);
            }
        };
    }
}
