package org.abarysh.notes.notesapp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.abarysh.notes.notesapp.domain.dto.NoteDetailsResponse;
import org.abarysh.notes.notesapp.domain.dto.NoteRequest;
import org.abarysh.notes.notesapp.domain.dto.NoteSummaryResponse;
import org.abarysh.notes.notesapp.domain.entity.Note;
import org.abarysh.notes.notesapp.domain.enums.NoteTag;
import org.abarysh.notes.notesapp.exсeptions.NotFoundException;
import org.abarysh.notes.notesapp.mapper.NoteMapper;
import org.abarysh.notes.notesapp.repo.NoteRepository;
import org.abarysh.notes.notesapp.service.NoteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultNoteService implements NoteService {

    private final NoteRepository noteRepository;

    @Override
    public NoteDetailsResponse createOrUpdate(NoteRequest request) {
        Note note;
        if (request.getId() == null) {
            log.info("Creating new note with title='{}'", request.getTitle());
            note = Note.builder()
                    .createdDate(Instant.now())
                    .build();
        } else {
            log.info("Updating note id='{}' with title='{}'", request.getId(), request.getTitle());
            note = findByIdOrThrow(request.getId());
        }

        note.setTitle(request.getTitle());
        note.setText(request.getText());
        note.setTags(Optional.ofNullable(request.getTags()).orElse(Set.of()));

        Note saved = noteRepository.save(note);
        log.debug("Note saved id='{}'", saved.getId());
        return NoteMapper.toDetails(saved);
    }

    @Override
    public void delete(String id) {
        log.info("Deleting note id='{}'", id);
        Note note = findByIdOrThrow(id);
        noteRepository.delete(note);
    }

    @Override
    public NoteDetailsResponse getById(String id) {
        log.debug("Fetching note details id='{}'", id);
        Note note = findByIdOrThrow(id);
        return NoteMapper.toDetails(note);
    }

    @Override
    public Page<NoteSummaryResponse> list(Set<NoteTag> tags, Pageable pageable) {
        log.debug("Listing notes with tags={} page={} size={}", tags, pageable.getPageNumber(), pageable.getPageSize());
        return noteRepository.findAllFiltered(tags, pageable)
                .map(NoteMapper::toSummary);
    }

    @Override
    public LinkedHashMap<String, Long> getStats(String id) {
        log.debug("Calculating stats for note id='{}'", id);
        Note note = findByIdOrThrow(id);
        String text = Optional.ofNullable(note.getText()).orElse("");

        Map<String, Long> counts = Arrays.stream(text.split("\\s+"))
                .filter(s -> !s.isBlank())
                .map(s -> s.replaceAll("\\p{Punct}", "").toLowerCase())
                .filter(s -> !s.isBlank())
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));

        return counts.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                                .thenComparing(Map.Entry.comparingByKey())
                )
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private Note findByIdOrThrow(String id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Note with id '{}' not found", id);
                    return new NotFoundException("Note with id %s not found".formatted(id));
                });
    }

}
