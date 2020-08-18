package com.utilities;

public class Constants {

    public static final int SERVER_SOURCE_ID = 0;
    public static final String MESSAGE_FROM_SERVER = "";
    public static final int NO_MAGIC_BYTES_DEFINED = 0;
    public static final int NUMBER_OF_THREADS_IN_THREAD_POOL = 1;
    public static final int UPPER_LIMIT_FOR_RANDOM_NUMBER = 500000;
    public static final int START_OF_MAGIC_BYTES_INCLUSIVE = 0;
    public static final int END_OF_MAGIC_BYTES_EXCLUSIVE = 4;
    public static final int START_OF_MESSAGE_TYPE_INCLUSIVE = 4;
    public static final int END_OF_MESSAGE_TYPE_EXCLUSIVE = 6;
    public static final int START_OF_SOURCE_ID_INCLUSIVE = 6;
    public static final int END_OF_SOURCE_ID_EXCLUSIVE = 10;
    public static final int START_OF_DEST_ID_INCLUSIVE = 10;
    public static final int END_OF_DEST_ID_EXCLUSIVE = 14;
    public static final int START_OF_MESSAGE_LENGTH_INCLUSIVE = 14;
    public static final int END_OF_MESSAGE_LENGTH_EXCLUSIVE = 18;
    public static final int START_OF_MESSAGE_INCLUSIVE = 18;
    public static final String MESSAGE_WHEN_DESTINATION_ID_NOT_PRESENT = "Please specify correct Id. User with this ID not present";


    public static final int STAGE_DOESNT_EXEC_SUCCESSFULLY = -1;
    public static final int BYTE_ARRAY_SIZE_FOR_INT = 4;
    public static final int BYTE_ARRAY_SIZE_FOR_MESSAGE_TYPE = 2;
}
