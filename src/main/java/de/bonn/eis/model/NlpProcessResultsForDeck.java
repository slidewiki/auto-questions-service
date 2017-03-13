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
        "languageDetectedWholeDeck",
        "DBPediaSpotlight_perDeck",
        "TFIDF"
})
public class NlpProcessResultsForDeck {

    @JsonProperty("languageDetectedWholeDeck")
    private String languageDetectedWholeDeck;
    @JsonProperty("DBPediaSpotlight_perDeck")
    private DBPediaSpotlightPOJO dBPediaSpotlightPerDeck;
    @JsonProperty("TFIDF")
    private TFIDF tFIDF;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("languageDetectedWholeDeck")
    public String getLanguageDetectedWholeDeck() {
        return languageDetectedWholeDeck;
    }

    @JsonProperty("languageDetectedWholeDeck")
    public void setLanguageDetectedWholeDeck(String languageDetectedWholeDeck) {
        this.languageDetectedWholeDeck = languageDetectedWholeDeck;
    }

    @JsonProperty("DBPediaSpotlight_perDeck")
    public DBPediaSpotlightPOJO getDBPediaSpotlightPerDeck() {
        return dBPediaSpotlightPerDeck;
    }

    @JsonProperty("DBPediaSpotlight_perDeck")
    public void setDBPediaSpotlightPerDeck(DBPediaSpotlightPOJO dBPediaSpotlightPerDeck) {
        this.dBPediaSpotlightPerDeck = dBPediaSpotlightPerDeck;
    }

    @JsonProperty("TFIDF")
    public TFIDF getTFIDF() {
        return tFIDF;
    }

    @JsonProperty("TFIDF")
    public void setTFIDF(TFIDF tFIDF) {
        this.tFIDF = tFIDF;
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