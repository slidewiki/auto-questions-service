package de.bonn.eis.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created by Ainuddin Faizan on 3/22/17.
 */
@Builder
@Data
public class SelectQuestion implements Question {
    private String answer;
    private String questionText;
    private List<String> distractors;
}
