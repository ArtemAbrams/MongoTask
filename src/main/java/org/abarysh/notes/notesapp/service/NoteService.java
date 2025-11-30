package org.abarysh.notes.notesapp.service;

import org.abarysh.notes.notesapp.domain.dto.NoteDetailsResponse;
import org.abarysh.notes.notesapp.domain.dto.NoteRequest;
import org.abarysh.notes.notesapp.domain.dto.NoteSummaryResponse;
import org.abarysh.notes.notesapp.domain.dto.NoteWordStatsResponse;
import org.abarysh.notes.notesapp.domain.enums.NoteTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface NoteService {

    NoteDetailsResponse createOrUpdate(NoteRequest request);

    void delete(String id);

    NoteDetailsResponse getById(String id);

    Page<NoteSummaryResponse> list(Set<NoteTag> tags, Pageable pageable);

    NoteWordStatsResponse getStats(String id);

}
