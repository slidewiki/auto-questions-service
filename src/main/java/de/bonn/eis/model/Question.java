package de.bonn.eis.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Ainuddin Faizan on 2/13/17.
 */
@Builder
@Data // Needed for JSON serialization
public class Question implements Serializable {
    private String answer;
    private List<String> questions;
    private List<String> externalDistractors;
    private List<String> inTextDistractors;
}
