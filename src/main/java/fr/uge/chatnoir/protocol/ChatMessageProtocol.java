package fr.uge.chatnoir.protocol;

public class ChatMessageProtocol {
    public static final int AUTH_REQUEST = 1;
    public static final int AUTH_RESPONSE = 11;
    public static final int PUBLIC_MESSAGE = 2;
    public static final int PRIVATE_MESSAGE = 3;
    public static final int FILE_SHARE = 4;
    public static final int FILE_UNSHARE = 5;
    public static final int FILE_LIST_REQUEST = 6;
    public static final int FILE_LIST_RESPONSE = 16;
    public static final int FILE_DOWNLOAD_REQUEST = 7;
    public static final int FILE_DOWNLOAD_RESPONSE = 17;
}
