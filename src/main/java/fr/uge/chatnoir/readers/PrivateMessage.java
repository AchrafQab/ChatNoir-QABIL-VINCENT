package fr.uge.chatnoir.readers;

import java.util.Objects;

public class PrivateMessage {
    private final String recipient;
    private final String message;

    public PrivateMessage(String recipient, String message) {
        this.recipient = Objects.requireNonNull(recipient);
        this.message = Objects.requireNonNull(message);
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }
}
