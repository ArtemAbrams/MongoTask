package org.abarysh.notes.notesapp.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.abarysh.notes.notesapp.domain.enums.NoteTag;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteRequest {

    private String id;

    @NotBlank(message = "title must not be blank")
    private String title;

    @NotBlank(message = "text must not be blank")
    private String text;

    private Set<NoteTag> tags;

}
