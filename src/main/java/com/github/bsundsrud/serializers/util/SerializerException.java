package com.github.bsundsrud.serializers.util;

/**
 * Thrown when serialization fails or is not possible (type mismatches, missing getters, etc)
 */
public class SerializerException extends Exception {
    public SerializerException() {
        super();
    }

    public SerializerException(String message) {
        super(message);
    }

    public SerializerException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerializerException(Throwable cause) {
        super(cause);
    }

}
