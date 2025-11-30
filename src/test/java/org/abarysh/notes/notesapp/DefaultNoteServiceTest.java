package org.abarysh.notes.notesapp;

import org.abarysh.notes.notesapp.domain.dto.NoteDetailsResponse;
import org.abarysh.notes.notesapp.domain.dto.NoteRequest;
import org.abarysh.notes.notesapp.domain.dto.NoteSummaryResponse;
import org.abarysh.notes.notesapp.domain.dto.NoteWordStatsResponse;
import org.abarysh.notes.notesapp.domain.entity.Note;
import org.abarysh.notes.notesapp.domain.enums.NoteTag;
import org.abarysh.notes.notesapp.exсeptions.NotFoundException;
import org.abarysh.notes.notesapp.repo.NoteRepository;
import org.abarysh.notes.notesapp.service.impl.DefaultNoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultNoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @InjectMocks
    private DefaultNoteService noteService;

    private static final Instant CREATED_AT = Instant.parse("2025-02-27T00:00:00Z");

    @Test
    void createOrUpdate_shouldCreateNewNote_whenIdIsNull() {
        NoteRequest request = buildRequest(null, "Title", "Text", Set.of(NoteTag.BUSINESS));
        Note saved = buildNote("123", "Title", "Text", Set.of(NoteTag.BUSINESS), CREATED_AT);

        when(noteRepository.save(any(Note.class))).thenReturn(saved);

        NoteDetailsResponse result = noteService.createOrUpdate(request);

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());

        Note toSave = captor.getValue();
        assertEquals("Title", toSave.getTitle());
        assertEquals("Text", toSave.getText());
        assertEquals(Set.of(NoteTag.BUSINESS), toSave.getTags());
        assertNotNull(toSave.getCreatedDate()); // createdDate має виставлятися

        assertEquals("123", result.getId());
        assertEquals("Title", result.getTitle());
        assertEquals("Text", result.getText());
        assertEquals(Set.of(NoteTag.BUSINESS), result.getTags());
    }

    @Test
    void createOrUpdate_shouldUseEmptyTags_whenRequestTagsNull() {
        NoteRequest request = buildRequest(null, "Title", "Text", null);
        Note saved = buildNote("123", "Title", "Text", Set.of(), CREATED_AT);

        when(noteRepository.save(any(Note.class))).thenReturn(saved);

        NoteDetailsResponse result = noteService.createOrUpdate(request);

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());

        Note toSave = captor.getValue();
        assertTrue(toSave.getTags().isEmpty());
        assertTrue(result.getTags().isEmpty());
    }

    @Test
    void createOrUpdate_shouldUpdateExistingNote_whenIdIsNotNull() {
        Note existing = buildNote("123", "Old", "Old text", Set.of(NoteTag.PERSONAL), CREATED_AT);
        NoteRequest request = buildRequest("123", "Updated", "Updated text", Set.of(NoteTag.IMPORTANT));

        when(noteRepository.findById("123")).thenReturn(Optional.of(existing));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        NoteDetailsResponse result = noteService.createOrUpdate(request);

        verify(noteRepository).findById("123");
        verify(noteRepository).save(existing);

        assertEquals("123", result.getId());
        assertEquals("Updated", result.getTitle());
        assertEquals("Updated text", result.getText());
        assertEquals(Set.of(NoteTag.IMPORTANT), result.getTags());
        assertEquals(CREATED_AT, existing.getCreatedDate());
    }

    @Test
    void createOrUpdate_shouldThrowNotFound_whenUpdatingMissingNote() {
        NoteRequest request = buildRequest("missing", "Title", "Text", null);

        when(noteRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> noteService.createOrUpdate(request));
    }

    @Test
    void delete_shouldDeleteNote_whenExists() {
        Note note = buildNote("123", "Title", null, null, CREATED_AT);

        when(noteRepository.findById("123")).thenReturn(Optional.of(note));

        noteService.delete("123");

        verify(noteRepository).findById("123");
        verify(noteRepository).delete(note);
    }

    @Test
    void delete_shouldThrowNotFound_whenMissing() {
        when(noteRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> noteService.delete("missing"));
    }

    @Test
    void getById_shouldReturnDetails_whenNoteExists() {
        Note note = buildNote("123", "Title", "Text", Set.of(NoteTag.PERSONAL), CREATED_AT);

        when(noteRepository.findById("123")).thenReturn(Optional.of(note));

        NoteDetailsResponse result = noteService.getById("123");

        verify(noteRepository).findById("123");
        assertEquals("123", result.getId());
        assertEquals("Title", result.getTitle());
        assertEquals("Text", result.getText());
        assertEquals(Set.of(NoteTag.PERSONAL), result.getTags());
        assertEquals(note.getCreatedDate(), result.getCreatedDate());
    }

    @Test
    void getById_shouldThrowNotFound_whenMissing() {
        when(noteRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> noteService.getById("missing"));
    }

    @Test
    void list_shouldUseFindAllFiltered_whenTagsNull() {
        Pageable pageable = PageRequest.of(0, 20);

        Note firstNote = buildNote(null, "First", null, null, CREATED_AT);
        Note secondNote = buildNote(null, "Second", null, null, CREATED_AT.plusSeconds(86400));

        Page<Note> page = new PageImpl<>(List.of(firstNote, secondNote), pageable, 2);

        when(noteRepository.findAllFiltered(null, pageable))
                .thenReturn(page);

        Page<NoteSummaryResponse> result = noteService.list(null, pageable);

        verify(noteRepository).findAllFiltered(null, pageable);
        assertEquals(2, result.getTotalElements());
        assertEquals("First", result.getContent().get(0).getTitle());
        assertEquals("Second", result.getContent().get(1).getTitle());
    }

    @Test
    void list_shouldUseFindAllFiltered_whenTagsProvided() {
        Set<NoteTag> tags = Set.of(NoteTag.BUSINESS);
        Pageable pageable = PageRequest.of(0, 10);

        Note note = buildNote(null, "Business note", null, tags, CREATED_AT);

        Page<Note> page = new PageImpl<>(List.of(note), pageable, 1);

        when(noteRepository.findAllFiltered(tags, pageable))
                .thenReturn(page);

        Page<NoteSummaryResponse> result = noteService.list(tags, pageable);

        verify(noteRepository).findAllFiltered(tags, pageable);
        assertEquals(1, result.getTotalElements());
        assertEquals("Business note", result.getContent().get(0).getTitle());
    }

    @Test
    void getStats_shouldReturnWordCountsSorted() {
        Note note = buildNote("1", null, "note is just a note!", null, null);

        when(noteRepository.findById("1")).thenReturn(Optional.of(note));

        NoteWordStatsResponse noteWordStatsResponse = noteService.getStats("1");
        Map<String, Long> stats = noteWordStatsResponse.getWordStats();

        List<String> keys = new ArrayList<>(stats.keySet());
        assertEquals(List.of("note", "a", "is", "just"), keys);
        assertEquals(2L, stats.get("note"));
        assertEquals(1L, stats.get("a"));
        assertEquals(1L, stats.get("is"));
        assertEquals(1L, stats.get("just"));
    }

    @Test
    void getStats_shouldThrowNotFound_whenNoteMissing() {
        when(noteRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> noteService.getStats("missing"));
    }

    private NoteRequest buildRequest(String id, String title, String text, Set<NoteTag> tags) {
        return NoteRequest.builder()
                .id(id)
                .title(title)
                .text(text)
                .tags(tags)
                .build();
    }

    private Note buildNote(String id, String title, String text, Set<NoteTag> tags, Instant createdDate) {
        return Note.builder()
                .id(id)
                .title(title)
                .text(text)
                .tags(tags)
                .createdDate(createdDate)
                .build();
    }

}
