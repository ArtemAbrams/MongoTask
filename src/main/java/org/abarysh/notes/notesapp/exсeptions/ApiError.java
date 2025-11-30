package org.abarysh.notes.notesapp.exсeptions;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ApiError {

    private Instant occurredAt;
    private int status;
    private String error;
    private String message;
    private String path;

}
