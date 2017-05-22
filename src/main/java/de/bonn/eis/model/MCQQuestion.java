package de.bonn.eis.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Ainuddin Faizan on 2/13/17.
 */
@Data
@Builder
public class MCQQuestion implements Serializable, Question {
    private String questionText;
    private String answer;
    private List<String> inTextDistractors;
    private List<String> externalDistractors;
}
