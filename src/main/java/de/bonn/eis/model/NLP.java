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
        "_id",
        "deckId",
        "deckTitle",
        "children",
        "numberOfSlides",
        "numberOfSlidesWithText",
        "detectedLanguage",
        "NER",
        "DBPediaSpotlight",
        "frequencyOfMostFrequentWord",
        "wordFrequenciesExclStopwords",
        "NERFrequencies",
        "DBPediaSpotlightURIFrequencies"
})
public class NLP implements Serializable {

    @JsonProperty("_id")
    private String id;
    @JsonProperty("deckId")
    private String deckId;
    @JsonProperty("deckTitle")
    private String deckTitle;
    @JsonProperty("children")
    private List<NlpProcessResults> children = null;
    @JsonProperty("numberOfSlides")
    private Integer numberOfSlides;
    @JsonProperty("numberOfSlidesWithText")
    private Integer numberOfSlidesWithText;
    @JsonProperty("detectedLanguage")
    private String detectedLanguage;
    @JsonProperty("NER")
    private List<NER> nER = null;
    @JsonProperty("DBPediaSpotlight")
    private DBPediaSpotlightResult dBPediaSpotlight;
    @JsonProperty("frequencyOfMostFrequentWord")
    private Integer frequencyOfMostFrequentWord;
    @JsonProperty("wordFrequenciesExclStopwords")
    private List<Frequency> wordFrequenciesExclStopwords = null;
    @JsonProperty("NERFrequencies")
    private List<Object> nERFrequencies = null;
    @JsonProperty("DBPediaSpotlightURIFrequencies")
    private List<Object> dBPediaSpotlightURIFrequencies = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("_id")
    public String getId() {
        return id;
    }

    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("deckId")
    public String getDeckId() {
        return deckId;
    }

    @JsonProperty("deckId")
    public void setDeckId(String deckId) {
        this.deckId = deckId;
    }

    @JsonProperty("deckTitle")
    public String getDeckTitle() {
        return deckTitle;
    }

    @JsonProperty("deckTitle")
    public void setDeckTitle(String deckTitle) {
        this.deckTitle = deckTitle;
    }

    @JsonProperty("children")
    public List<NlpProcessResults> getChildren() {
        return children;
    }

    @JsonProperty("children")
    public void setChildren(List<NlpProcessResults> children) {
        this.children = children;
    }

    @JsonProperty("numberOfSlides")
    public Integer getNumberOfSlides() {
        return numberOfSlides;
    }

    @JsonProperty("numberOfSlides")
    public void setNumberOfSlides(Integer numberOfSlides) {
        this.numberOfSlides = numberOfSlides;
    }

    @JsonProperty("numberOfSlidesWithText")
    public Integer getNumberOfSlidesWithText() {
        return numberOfSlidesWithText;
    }

    @JsonProperty("numberOfSlidesWithText")
    public void setNumberOfSlidesWithText(Integer numberOfSlidesWithText) {
        this.numberOfSlidesWithText = numberOfSlidesWithText;
    }

    @JsonProperty("detectedLanguage")
    public String getDetectedLanguage() {
        return detectedLanguage;
    }

    @JsonProperty("detectedLanguage")
    public void setDetectedLanguage(String detectedLanguage) {
        this.detectedLanguage = detectedLanguage;
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

    @JsonProperty("frequencyOfMostFrequentWord")
    public Integer getFrequencyOfMostFrequentWord() {
        return frequencyOfMostFrequentWord;
    }

    @JsonProperty("frequencyOfMostFrequentWord")
    public void setFrequencyOfMostFrequentWord(Integer frequencyOfMostFrequentWord) {
        this.frequencyOfMostFrequentWord = frequencyOfMostFrequentWord;
    }

    @JsonProperty("wordFrequenciesExclStopwords")
    public List<Frequency> getWordFrequenciesExclStopwords() {
        return wordFrequenciesExclStopwords;
    }

    @JsonProperty("wordFrequenciesExclStopwords")
    public void setWordFrequenciesExclStopwords(List<Frequency> wordFrequenciesExclStopwords) {
        this.wordFrequenciesExclStopwords = wordFrequenciesExclStopwords;
    }

    @JsonProperty("NERFrequencies")
    public List<Object> getNERFrequencies() {
        return nERFrequencies;
    }

    @JsonProperty("NERFrequencies")
    public void setNERFrequencies(List<Object> nERFrequencies) {
        this.nERFrequencies = nERFrequencies;
    }

    @JsonProperty("DBPediaSpotlightURIFrequencies")
    public List<Object> getDBPediaSpotlightURIFrequencies() {
        return dBPediaSpotlightURIFrequencies;
    }

    @JsonProperty("DBPediaSpotlightURIFrequencies")
    public void setDBPediaSpotlightURIFrequencies(List<Object> dBPediaSpotlightURIFrequencies) {
        this.dBPediaSpotlightURIFrequencies = dBPediaSpotlightURIFrequencies;
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