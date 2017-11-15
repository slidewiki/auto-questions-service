package de.bonn.eis.model; /**
 * Created by Ainuddin Faizan on 12/28/16.
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
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "@text",
        "@confidence",
        "@support",
        "@types",
        "@sparql",
        "@policy",
        "Resources"
})
public class DBPediaSpotlightResult implements Serializable {

    @JsonProperty("@text")
    private String text;
    @JsonProperty("@confidence")
    private String confidence;
    @JsonProperty("@support")
    private String support;
    @JsonProperty("@types")
    private String types;
    @JsonProperty("@sparql")
    private String sparql;
    @JsonProperty("@policy")
    private String policy;
    @JsonProperty("Resources")
    private List<DBPediaResource> DBPediaResources = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

}