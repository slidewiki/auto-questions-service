package de.bonn.eis.model;

import lombok.Data;

import java.util.List;

/**
 * Created by Ainuddin Faizan on 2/13/17.
 */
@Data
public class Question {
    private String questionText;
    private String answer;
    private List<String> externalDistractors;
    private List<String> inTextDistractors;
}
