package org.abarysh.notes.notesapp.repo;

import org.abarysh.notes.notesapp.domain.entity.Note;
import org.abarysh.notes.notesapp.domain.enums.NoteTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface CustomNoteRepository {

    Page<Note> findAllFiltered(Set<NoteTag> tags, Pageable pageable);

}
