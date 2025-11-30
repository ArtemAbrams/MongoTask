package org.abarysh.notes.notesapp.domain.entity;

import lombok.*;
import org.abarysh.notes.notesapp.domain.enums.NoteTag;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notes")
public class Note {

    @Id
    protected String id;

    @Indexed
    private Instant createdDate;

    @Indexed
    private String title;

    private String text;

    private Set<NoteTag> tags;

}
