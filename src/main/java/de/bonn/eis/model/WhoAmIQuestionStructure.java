package de.bonn.eis.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created by andy on 4/7/17.
 */
@Data
@Builder
public class WhoAmIQuestionStructure implements Question {
    private String question;
    private String baseType;
    private String answer;
    private String firstSubject;
    private String firstPredicate;
    private String firstObject;
    private String secondSubject;
    private String secondPredicate;
    private String secondObject;
    private List<String> distractors;
}
