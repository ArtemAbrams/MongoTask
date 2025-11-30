package org.abarysh.notes.notesapp;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.abarysh.notes.notesapp.api.NoteController;
import org.abarysh.notes.notesapp.domain.dto.NoteDetailsResponse;
import org.abarysh.notes.notesapp.domain.dto.NoteRequest;
import org.abarysh.notes.notesapp.domain.dto.NoteSummaryResponse;
import org.abarysh.notes.notesapp.domain.enums.NoteTag;
import org.abarysh.notes.notesapp.exсeptions.NotFoundException;
import org.abarysh.notes.notesapp.exсeptions.handler.GlobalExceptionHandler;
import org.abarysh.notes.notesapp.service.NoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NoteController.class)
@Import(GlobalExceptionHandler.class)
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoteService noteService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Instant CREATED_AT = Instant.parse("2025-02-27T00:00:00Z");

    @Test
    void saveNote_shouldReturnNoteDetails() throws Exception {
        NoteRequest request = buildRequest("Test title", "Some text");

        NoteDetailsResponse response = buildDetails("Test title", "Some text");

        when(noteService.createOrUpdate(any(NoteRequest.class))).thenReturn(response);

        performSave(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.title").value("Test title"))
                .andExpect(jsonPath("$.text").value("Some text"))
                .andExpect(jsonPath("$.createdDate").exists());
    }

    @Test
    void saveNote_shouldReturnBadRequest_onValidationError() throws Exception {
        NoteRequest request = buildRequest("", "Some text");

        performSave(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("title: title must not be blank")))
                .andExpect(jsonPath("$.path").value("/api/notes"));
    }

    @Test
    void saveNote_shouldReturnBadRequest_whenTextBlank() throws Exception {
        NoteRequest request = buildRequest("Test title", "");

        performSave(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("text: text must not be blank")))
                .andExpect(jsonPath("$.path").value("/api/notes"));
    }


    @Test
    void getById_shouldReturnNoteDetails() throws Exception {
        NoteDetailsResponse response = buildDetails("Complete the test task", "Be careful and smart!");

        when(noteService.getById("123")).thenReturn(response);

        mockMvc.perform(get("/api/notes/{id}", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.title").value("Complete the test task"))
                .andExpect(jsonPath("$.text").value("Be careful and smart!"))
                .andExpect(jsonPath("$.createdDate").exists());
    }

    @Test
    void getById_shouldReturnNotFound_whenNoteIdMissing() throws Exception {
        when(noteService.getById("missing")).thenThrow(new NotFoundException("Note with id missing not found"));

        mockMvc.perform(get("/api/notes/{id}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Note with id missing not found"))
                .andExpect(jsonPath("$.path").value("/api/notes/missing"));
    }

    @Test
    void list_shouldReturnPageOfSummaries() throws Exception {
        NoteSummaryResponse noteSummaryFirst = buildSummary("My first note", CREATED_AT);
        NoteSummaryResponse noteSummarySecond = buildSummary("My second note", CREATED_AT.plusSeconds(86400));

        Page<NoteSummaryResponse> page = new PageImpl<>(List.of(noteSummaryFirst, noteSummarySecond), PageRequest.of(0, 20), 2);

        when(noteService.list(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/notes")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("My first note"))
                .andExpect(jsonPath("$.content[0].createdDate").exists())
                .andExpect(jsonPath("$.content[1].title").value("My second note"))
                .andExpect(jsonPath("$.content[1].createdDate").exists())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void list_shouldReturnPageOfSummariesFilterByTags() throws Exception {
        NoteSummaryResponse noteSummaryResponse = NoteSummaryResponse.builder()
                .title("First NOTE")
                .createdDate(Instant.parse("2025-02-27T00:00:00Z"))
                .build();

        Page<NoteSummaryResponse> page = new PageImpl<>(List.of(noteSummaryResponse), PageRequest.of(0, 10), 1);

        when(noteService.list(eq(Set.of(NoteTag.BUSINESS, NoteTag.IMPORTANT)), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/notes")
                        .param("tags", "BUSINESS", "IMPORTANT")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("First NOTE"))
                .andExpect(jsonPath("$.content[0].createdDate").exists())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void delete_shouldCallServiceAndReturnOk() throws Exception {
        doNothing().when(noteService).delete("123");

        mockMvc.perform(delete("/api/notes/{id}", "123"))
                .andExpect(status().isOk());

        verify(noteService).delete("123");
    }

    @Test
    void getStats_shouldReturnStatsMap() throws Exception {
        LinkedHashMap<String, Long> stats = new LinkedHashMap<>();
        stats.put("test", 2L);
        stats.put("task", 1L);

        when(noteService.getStats("123")).thenReturn(stats);

        mockMvc.perform(get("/api/notes/{id}/stats", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.test").value(2))
                .andExpect(jsonPath("$.task").value(1));
    }

    @Test
    void getById_shouldReturnInternalServerError() throws Exception {
        when(noteService.getById("123")).thenThrow(new RuntimeException("Unexpected error occurred"));

        mockMvc.perform(get("/api/notes/{id}", "123"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/api/notes/123"));
    }

    private NoteRequest buildRequest(String title, String text) {
        return NoteRequest.builder()
                .title(title)
                .text(text)
                .tags(Set.of(NoteTag.PERSONAL))
                .build();
    }

    private NoteSummaryResponse buildSummary(String title, Instant createdDate) {
        return NoteSummaryResponse.builder()
                .title(title)
                .createdDate(createdDate)
                .build();
    }

    private NoteDetailsResponse buildDetails(String title, String text) {
        return NoteDetailsResponse.builder()
                .id("123")
                .title(title)
                .text(text)
                .tags(Set.of(NoteTag.PERSONAL))
                .createdDate(CREATED_AT)
                .build();
    }

    private ResultActions performSave(NoteRequest request) throws Exception {
        return mockMvc.perform(post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

}