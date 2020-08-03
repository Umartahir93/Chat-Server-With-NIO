package com.utilities;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 *
 * This class contains helper functions which are used
 * by different classes in order to help out classes to
 * provide some functionality
 *
 * @author umar.tahir@afiniti.com
 */

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilityFunction {


    /**
     * Helper function which converts bytes into String
     *
     * @param message in byte array
     *
     * @param start start index in byte array for specific bytes
     *
     * @param end end index in byte array for specific bytes.
     *            Remember this exclusive
     *
     * @return String
     *
     */

    public static String getStringFromByteArray(byte[] message, int start ,int end){
        return new String(Arrays.copyOfRange(message ,start , end));
    }

    /**
     * Helper function which converts bytes into integer
     *
     * @param message in byte array
     *
     * @param start start index in byte array for specific bytes
     *
     * @param end index in byte array for specific bytes.
     *        Remember this exclusive
     *
     * @return integer
     *
     */

    public static int getIntFromByteArray(byte[] message,int start ,int end){
        return ByteBuffer.wrap(Arrays.copyOfRange(message ,start , end)).getInt();
    }


}
