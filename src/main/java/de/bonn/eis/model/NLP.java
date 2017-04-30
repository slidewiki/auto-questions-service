package de.bonn.eis.model;

/**
 * Created by Ainuddin Faizan on 3/13/17.
 */
import java.io.Serializable;
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
        "detectedLanguage",
        "NER",
        "DBPediaSpotlight",
        "frequencyOfMostFrequentWord",
        "wordFrequenciesExclStopwords",
        "NERFrequencies",
        "DBPediaSpotlightURIFrequencies",
        "TFIDF"
})
public class NLP implements Serializable
{

    @JsonProperty("deckId")
    private String deckId;
    @JsonProperty("children")
    private List<NlpProcessResults> children = null;
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
    private List<Frequency> nERFrequencies = null;
    @JsonProperty("DBPediaSpotlightURIFrequencies")
    private List<Frequency> dBPediaSpotlightURIFrequencies = null;
    @JsonProperty("TFIDF")
    private List<TFIDF> tFIDF = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonProperty("deckId")
    public String getDeckId() {
        return deckId;
    }

    @JsonProperty("deckId")
    public void setDeckId(String deckId) {
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
    public List<Frequency> getNERFrequencies() {
        return nERFrequencies;
    }

    @JsonProperty("NERFrequencies")
    public void setNERFrequencies(List<Frequency> nERFrequencies) {
        this.nERFrequencies = nERFrequencies;
    }

    @JsonProperty("DBPediaSpotlightURIFrequencies")
    public List<Frequency> getDBPediaSpotlightURIFrequencies() {
        return dBPediaSpotlightURIFrequencies;
    }

    @JsonProperty("DBPediaSpotlightURIFrequencies")
    public void setDBPediaSpotlightURIFrequencies(List<Frequency> dBPediaSpotlightURIFrequencies) {
        this.dBPediaSpotlightURIFrequencies = dBPediaSpotlightURIFrequencies;
    }

    @JsonProperty("TFIDF")
    public List<TFIDF> getTFIDF() {
        return tFIDF;
    }

    @JsonProperty("TFIDF")
    public void setTFIDF(List<TFIDF> tFIDF) {
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