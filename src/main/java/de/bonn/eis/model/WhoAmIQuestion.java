package de.bonn.eis.model;

import lombok.Builder;
import lombok.Data;

/**
 * Created by andy on 4/7/17.
 */
@Data
@Builder
public class WhoAmIQuestion {
    private String baseType;
    private String firstProp;
    private String firstHint;
    private String secondProp;
    private String secondHint;
    private String answer;
}
