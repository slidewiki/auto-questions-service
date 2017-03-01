package de.bonn.eis.model;

import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Ainuddin Faizan on 2/13/17.
 */
@Builder
@Data
public class Question implements Serializable{
    private String questionText;
    private String answer;
    private List<String> externalDistractors;
    private List<String> inTextDistractors;
}
