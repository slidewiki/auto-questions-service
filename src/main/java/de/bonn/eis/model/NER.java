package de.bonn.eis.model;

/**
 * Created by andy on 4/30/17.
 */
import java.io.Serializable;
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
        "name",
        "type",
        "sourceName",
        "probability",
        "tokenSpanBegin",
        "tokenSpanEnd",
        "link"
})

public class NER implements Serializable
{

    @JsonProperty("name")
    private String name;
    @JsonProperty("type")
    private String type;
    @JsonProperty("sourceName")
    private String sourceName;
    @JsonProperty("probability")
    private Double probability;
    @JsonProperty("tokenSpanBegin")
    private Integer tokenSpanBegin;
    @JsonProperty("tokenSpanEnd")
    private Integer tokenSpanEnd;
    @JsonProperty("link")
    private String link;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = -1681038655164560742L;

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("sourceName")
    public String getSourceName() {
        return sourceName;
    }

    @JsonProperty("sourceName")
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    @JsonProperty("probability")
    public Double getProbability() {
        return probability;
    }

    @JsonProperty("probability")
    public void setProbability(Double probability) {
        this.probability = probability;
    }

    @JsonProperty("tokenSpanBegin")
    public Integer getTokenSpanBegin() {
        return tokenSpanBegin;
    }

    @JsonProperty("tokenSpanBegin")
    public void setTokenSpanBegin(Integer tokenSpanBegin) {
        this.tokenSpanBegin = tokenSpanBegin;
    }

    @JsonProperty("tokenSpanEnd")
    public Integer getTokenSpanEnd() {
        return tokenSpanEnd;
    }

    @JsonProperty("tokenSpanEnd")
    public void setTokenSpanEnd(Integer tokenSpanEnd) {
        this.tokenSpanEnd = tokenSpanEnd;
    }

    @JsonProperty("link")
    public String getLink() {
        return link;
    }

    @JsonProperty("link")
    public void setLink(String link) {
        this.link = link;
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
