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
        "slideId",
        "slideTitleAndText",
        "detectedLanguage",
        "tokens",
        "DBPediaSpotlight"
})
public class NlpProcessResults {

    @JsonProperty("slideId")
    private String slideId;
    @JsonProperty("slideTitleAndText")
    private String slideTitleAndText;
    @JsonProperty("detectedLanguage")
    private String detectedLanguage;
    @JsonProperty("tokens")
    private List<String> tokens = null;
    @JsonProperty("DBPediaSpotlight")
    private DBPediaSpotlightPOJO dBPediaSpotlight;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("slideId")
    public String getSlideId() {
        return slideId;
    }

    @JsonProperty("slideId")
    public void setSlideId(String slideId) {
        this.slideId = slideId;
    }

    @JsonProperty("slideTitleAndText")
    public String getSlideTitleAndText() {
        return slideTitleAndText;
    }

    @JsonProperty("slideTitleAndText")
    public void setSlideTitleAndText(String slideTitleAndText) {
        this.slideTitleAndText = slideTitleAndText;
    }

    @JsonProperty("detectedLanguage")
    public String getDetectedLanguage() {
        return detectedLanguage;
    }

    @JsonProperty("detectedLanguage")
    public void setDetectedLanguage(String detectedLanguage) {
        this.detectedLanguage = detectedLanguage;
    }

    @JsonProperty("tokens")
    public List<String> getTokens() {
        return tokens;
    }

    @JsonProperty("tokens")
    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }

    @JsonProperty("DBPediaSpotlight")
    public DBPediaSpotlightPOJO getDBPediaSpotlight() {
        return dBPediaSpotlight;
    }

    @JsonProperty("DBPediaSpotlight")
    public void setDBPediaSpotlight(DBPediaSpotlightPOJO dBPediaSpotlight) {
        this.dBPediaSpotlight = dBPediaSpotlight;
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