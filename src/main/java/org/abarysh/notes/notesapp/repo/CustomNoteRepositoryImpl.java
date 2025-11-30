package org.abarysh.notes.notesapp.repo;

import lombok.RequiredArgsConstructor;
import org.abarysh.notes.notesapp.domain.entity.Note;
import org.abarysh.notes.notesapp.domain.enums.NoteTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class CustomNoteRepositoryImpl implements CustomNoteRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Note> findAllFiltered(Set<NoteTag> tags, Pageable pageable) {
        Query query = new Query()
                .with(pageable)
                .with(Sort.by(Sort.Direction.DESC, "createdDate"));

        if (tags != null && !tags.isEmpty()) {
            query.addCriteria(Criteria.where("tags").in(tags));
        }

        List<Note> content = mongoTemplate.find(query, Note.class);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Note.class);

        return new PageImpl<>(content, pageable, total);
    }

}
