package org.openmrs.module.sync.serialization;

import org.openmrs.api.APIException;

public class SyncSerializationException extends APIException {

    public static final long serialVersionUID = 0L;

    public SyncSerializationException() {
    }

    public SyncSerializationException(Throwable t) {
        super(t);
    }

    public SyncSerializationException(String message) {
        this(message, null);
    }

    public SyncSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
