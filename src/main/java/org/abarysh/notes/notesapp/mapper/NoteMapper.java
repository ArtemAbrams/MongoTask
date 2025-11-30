package org.abarysh.notes.notesapp.mapper;

import lombok.experimental.UtilityClass;
import org.abarysh.notes.notesapp.domain.dto.NoteDetailsResponse;
import org.abarysh.notes.notesapp.domain.dto.NoteSummaryResponse;
import org.abarysh.notes.notesapp.domain.entity.Note;

@UtilityClass
public class NoteMapper {

    public NoteSummaryResponse toSummary(Note note) {
        return NoteSummaryResponse.builder()
                .title(note.getTitle())
                .createdDate(note.getCreatedDate())
                .build();
    }

    public NoteDetailsResponse toDetails(Note note) {
        return NoteDetailsResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .createdDate(note.getCreatedDate())
                .text(note.getText())
                .tags(note.getTags())
                .build();
    }

}
