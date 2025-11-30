package org.abarysh.notes.notesapp.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.abarysh.notes.notesapp.domain.enums.NoteTag;

import java.time.Instant;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteDetailsResponse {

    private String id;
    private String title;
    private Instant createdDate;
    private String text;
    private Set<NoteTag> tags;

}
