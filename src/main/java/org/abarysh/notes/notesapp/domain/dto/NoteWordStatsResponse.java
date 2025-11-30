package org.abarysh.notes.notesapp.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteWordStatsResponse {

    private Map<String, Long> wordStats;

}
