package org.abarysh.notes.notesapp.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.abarysh.notes.notesapp.domain.dto.NoteDetailsResponse;
import org.abarysh.notes.notesapp.domain.dto.NoteRequest;
import org.abarysh.notes.notesapp.domain.dto.NoteSummaryResponse;
import org.abarysh.notes.notesapp.domain.dto.NoteWordStatsResponse;
import org.abarysh.notes.notesapp.domain.enums.NoteTag;
import org.abarysh.notes.notesapp.service.NoteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    @Operation(summary = "Create or update note",
            description = "If id is null, creates a new note, otherwise updates the existing one."
    )
    public ResponseEntity<NoteDetailsResponse> saveNote(@Valid @RequestBody NoteRequest request) {
        return ResponseEntity.ok(noteService.createOrUpdate(request));
    }

    @GetMapping
    @Operation(summary = "List notes",
            description = "Returns paginated list of notes with optional filtering by tags."
    )
    public ResponseEntity<Page<NoteSummaryResponse>> list(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) Set<NoteTag> tags) {
        return ResponseEntity.ok(noteService.list(tags, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get note details",
            description = "Returns full details of a single note by id."
    )
    public ResponseEntity<NoteDetailsResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(noteService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete note",
            description = "Deletes note by id."
    )
    public ResponseEntity<Void> delete(@PathVariable String id) {
        noteService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get note text statistics",
            description = "Returns unique word counts for the note text."
    )
    public ResponseEntity<NoteWordStatsResponse> getStats(@PathVariable String id) {
        return ResponseEntity.ok(noteService.getStats(id));
    }

}
