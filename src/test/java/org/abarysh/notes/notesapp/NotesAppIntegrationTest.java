package org.abarysh.notes.notesapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.abarysh.notes.notesapp.domain.dto.NoteDetailsResponse;
import org.abarysh.notes.notesapp.domain.dto.NoteRequest;
import org.abarysh.notes.notesapp.domain.enums.NoteTag;
import org.abarysh.notes.notesapp.exсeptions.ApiError;
import org.abarysh.notes.notesapp.repo.NoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class NotesAppIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NoteRepository noteRepository;

    private final static ObjectMapper objectMapper = new ObjectMapper();

    private String baseUrl;

    @BeforeEach
    void setUp() {
        this.baseUrl = "http://localhost:" + port + "/api/notes";
    }

    @AfterEach
    void tearDown() {
        noteRepository.deleteAll();
    }

    @Test
    void createNote_andGetById_shouldWorkEndToEnd() {
        NoteDetailsResponse created = createNote("Integration note", "Some integration text", Set.of(NoteTag.PERSONAL));

        assertNotNull(created.getId());
        assertEquals("Integration note", created.getTitle());
        assertEquals("Some integration text", created.getText());
        assertNotNull(created.getCreatedDate());
        assertEquals(Set.of(NoteTag.PERSONAL), created.getTags());

        String getUrl = baseUrl + "/" + created.getId();
        ResponseEntity<NoteDetailsResponse> getResponse = restTemplate.getForEntity(getUrl, NoteDetailsResponse.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        NoteDetailsResponse fetched = getResponse.getBody();

        assertEquals(created.getId(), fetched.getId());
        assertEquals("Integration note", fetched.getTitle());
        assertEquals("Some integration text", fetched.getText());
        assertEquals(Set.of(NoteTag.PERSONAL), fetched.getTags());
    }

    @Test
    void listNotes_shouldReturnPage() throws Exception {
        createNote("First note", "First text", Set.of(NoteTag.BUSINESS));
        createNote("Second note", "Second text", Set.of(NoteTag.PERSONAL));
        String url = baseUrl + "?page=0&size=10";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode root = objectMapper.readTree(response.getBody());
        int total = root.get("totalElements").asInt();
        assertEquals(2, total);

        JsonNode content = root.get("content");
        assertTrue(content.isArray());
        assertEquals(2, content.size());

        JsonNode first = content.get(0);
        assertTrue(first.hasNonNull("title"));
        assertTrue(first.hasNonNull("createdDate"));
    }

    @Test
    void listNotes_shouldFilterByTags() throws Exception {
        createNote("Business note", "biz text", Set.of(NoteTag.BUSINESS));
        createNote("Personal note", "pers text", Set.of(NoteTag.PERSONAL));

        String url = baseUrl + "?tags=BUSINESS&page=0&size=10";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode root = objectMapper.readTree(response.getBody());
        int total = root.get("totalElements").asInt();
        assertEquals(1, total);

        JsonNode content = root.get("content");
        assertEquals(1, content.size());
        assertEquals("Business note", content.get(0).get("title").asText());
    }

    @Test
    void deleteNote_shouldRemoveNote() {
        NoteDetailsResponse created = createNote("To delete", "Delete me", Set.of(NoteTag.PERSONAL));

        String deleteUrl = baseUrl + "/" + created.getId();
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(deleteUrl, HttpMethod.DELETE, null, Void.class);

        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());

        String getUrl = baseUrl + "/" + created.getId();
        ResponseEntity<ApiError> getResponse = restTemplate.getForEntity(getUrl, ApiError.class);

        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(404, getResponse.getBody().getStatus());
    }

    @Test
    void stats_shouldReturnWordCounts() throws Exception {
        NoteDetailsResponse created = createNote("Stats note", "note is just a note", Set.of(NoteTag.PERSONAL));

        String statsUrl = baseUrl + "/" + created.getId() + "/stats";

        ResponseEntity<String> response = restTemplate.getForEntity(statsUrl, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode root = objectMapper.readTree(response.getBody());

        assertEquals(2, root.get("note").asInt());
        assertEquals(1, root.get("is").asInt());
        assertEquals(1, root.get("just").asInt());
        assertEquals(1, root.get("a").asInt());
    }

    @Test
    void createNote_shouldReturnBadRequest_whenTitleAndTextBlank() {
        NoteRequest request = NoteRequest.builder()
                .title("")
                .text("")
                .tags(Set.of(NoteTag.PERSONAL))
                .build();

        ResponseEntity<ApiError> response = restTemplate.postForEntity(baseUrl, jsonEntity(request), ApiError.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        ApiError body = response.getBody();

        assertEquals(400, body.getStatus());
        assertEquals("Bad Request", body.getError());
        assertTrue(body.getMessage().contains("title: title must not be blank"));
        assertTrue(body.getMessage().contains("text: text must not be blank"));
        assertEquals("/api/notes", body.getPath());
    }

    @Test
    void getById_shouldReturnNotFound_whenNoteMissing() {
        String url = baseUrl + "/missing-id";

        ResponseEntity<ApiError> response = restTemplate.getForEntity(url, ApiError.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        ApiError body = response.getBody();

        assertEquals(404, body.getStatus());
        assertEquals("Not Found", body.getError());
        assertEquals("Note with id missing-id not found", body.getMessage());
        assertEquals("/api/notes/missing-id", body.getPath());
    }

    private NoteDetailsResponse createNote(String title, String text, Set<NoteTag> tags) {
        NoteRequest request = NoteRequest.builder()
                .title(title)
                .text(text)
                .tags(tags)
                .build();

        HttpEntity<NoteRequest> entity = new HttpEntity<>(request, new HttpHeaders());

        ResponseEntity<NoteDetailsResponse> response = restTemplate.postForEntity(baseUrl, entity, NoteDetailsResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        return response.getBody();
    }

    private HttpEntity<NoteRequest> jsonEntity(NoteRequest request) {
        HttpHeaders headers = new HttpHeaders();
        return new HttpEntity<>(request, headers);
    }

}
