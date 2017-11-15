package de.bonn.eis.model;

/**
 * Created by Ainuddin Faizan on 3/13/17.
 */

import com.fasterxml.jackson.annotation.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "slideId",
        "slideTitleAndText",
        "detectedLanguage",
        "tokens",
        "NER",
        "DBPediaSpotlight"
})
public class NlpProcessResults implements Serializable {

    @JsonProperty("slideId")
    private String slideId;
    @JsonProperty("slideTitleAndText")
    private String slideTitleAndText;
    @JsonProperty("detectedLanguage")
    private String detectedLanguage;
    @JsonProperty("tokens")
    private List<String> tokens = null;
    @JsonProperty("NER")
    private List<NER> nER = null;
    @JsonProperty("DBPediaSpotlight")
    private DBPediaSpotlightResult dBPediaSpotlight;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = 8193220172692345640L;

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

    @JsonProperty("NER")
    public List<NER> getNER() {
        return nER;
    }

    @JsonProperty("NER")
    public void setNER(List<NER> nER) {
        this.nER = nER;
    }

    @JsonProperty("DBPediaSpotlight")
    public DBPediaSpotlightResult getDBPediaSpotlight() {
        return dBPediaSpotlight;
    }

    @JsonProperty("DBPediaSpotlight")
    public void setDBPediaSpotlight(DBPediaSpotlightResult dBPediaSpotlight) {
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