package de.bonn.eis.model; /**
 * Created by Ainuddin Faizan on 12/29/16.
 */

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "@URI",
        "@support",
        "@types",
        "@surfaceForm",
        "@offset",
        "@similarityScore",
        "@percentageOfSecondRank"
})
public class DBPediaResource implements Serializable, Comparable {

    private final static long serialVersionUID = -6649043481394421196L;
    @JsonProperty("@URI")
    private String uRI;
    @JsonProperty("@support")
    private String support;
    @JsonProperty("@types")
    private String types;
    @JsonProperty("@surfaceForm")
    private String surfaceForm;
    @JsonProperty("@offset")
    private String offset;
    @JsonProperty("@similarityScore")
    private String similarityScore;
    @JsonProperty("@percentageOfSecondRank")
    private String percentageOfSecondRank;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @Override
    public int compareTo(Object o) {
        return 0;
    }

}