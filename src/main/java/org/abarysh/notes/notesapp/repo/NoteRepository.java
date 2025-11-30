package org.abarysh.notes.notesapp.repo;

import org.abarysh.notes.notesapp.domain.entity.Note;
import org.abarysh.notes.notesapp.domain.enums.NoteTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Set;

public interface NoteRepository extends MongoRepository<Note, String>, CustomNoteRepository {

}
