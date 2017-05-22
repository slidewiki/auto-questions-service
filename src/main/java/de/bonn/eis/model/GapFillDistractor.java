package de.bonn.eis.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created by andy on 5/20/17.
 */
@Data
@Builder
public class GapFillDistractor {
    private String resourceURI;
    private String surfaceForm;
    private String pluralSurfaceForm;
    private List<String> inTextDistractors;
    private List<String> externalDistractors;
}
