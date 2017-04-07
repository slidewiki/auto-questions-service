package de.bonn.eis.model;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * Created by andy on 4/7/17.
 */
@Data
@Builder
public class LinkSUMResultRow {
    private String subject;
    private String predicate;
    private String object;
    private float vRank;
}
