package de.bonn.eis.model;

/**
 * Created by Ainuddin Faizan on 3/13/17.
 */
 import java.util.HashMap;
        import java.util.List;
        import java.util.Map;
        import com.fasterxml.jackson.annotation.JsonAnyGetter;
        import com.fasterxml.jackson.annotation.JsonAnySetter;
        import com.fasterxml.jackson.annotation.JsonIgnore;
        import com.fasterxml.jackson.annotation.JsonInclude;
        import com.fasterxml.jackson.annotation.JsonProperty;
        import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "deckId",
        "children",
        "nlpProcessResults"
})
public class NLP {

    @JsonProperty("deckId")
    private Integer deckId;
    @JsonProperty("children")
    private List<NlpProcessResults> children = null;
    @JsonProperty("nlpProcessResults")
    private NlpProcessResultsForDeck nlpProcessResults;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("deckId")
    public Integer getDeckId() {
        return deckId;
    }

    @JsonProperty("deckId")
    public void setDeckId(Integer deckId) {
        this.deckId = deckId;
    }

    @JsonProperty("children")
    public List<NlpProcessResults> getChildren() {
        return children;
    }

    @JsonProperty("children")
    public void setChildren(List<NlpProcessResults> children) {
        this.children = children;
    }

    @JsonProperty("nlpProcessResults")
    public NlpProcessResultsForDeck getNlpProcessResults() {
        return nlpProcessResults;
    }

    @JsonProperty("nlpProcessResults")
    public void setNlpProcessResults(NlpProcessResultsForDeck nlpProcessResults) {
        this.nlpProcessResults = nlpProcessResults;
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