package de.bonn.eis.model;

/**
 * Created by Ainuddin Faizan on 3/13/17.
 */
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "spotlightEntity",
        "tfidf"
})
public class TFIDFEntity {

    @JsonProperty("spotlightEntity")
    private String spotlightEntity;
    @JsonProperty("tfidf")
    private Double tfidf;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("spotlightEntity")
    public String getSpotlightEntity() {
        return spotlightEntity;
    }

    @JsonProperty("spotlightEntity")
    public void setSpotlightEntity(String spotlightEntity) {
        this.spotlightEntity = spotlightEntity;
    }

    @JsonProperty("tfidf")
    public Double getTfidf() {
        return tfidf;
    }

    @JsonProperty("tfidf")
    public void setTfidf(Double tfidf) {
        this.tfidf = tfidf;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
