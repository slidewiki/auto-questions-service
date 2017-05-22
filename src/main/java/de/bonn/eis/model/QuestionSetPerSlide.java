package de.bonn.eis.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created by andy on 5/20/17.
 */
@Data
@Builder
public class QuestionSetPerSlide {
    private String slideId;
    private List<Question> questionSet;
}
