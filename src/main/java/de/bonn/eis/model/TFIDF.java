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
        "TFIDF_forSpotlightEntitiesRetrievedPerSlide_docFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent",
        "TFIDF_forSpotlightEntitiesRetrievedPerDeck_docFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent"
})
public class TFIDF {

    @JsonProperty("TFIDF_forSpotlightEntitiesRetrievedPerSlide_docFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent")
    private List<TFIDFEntity> TFIDFPerSlide = null;
    @JsonProperty("TFIDF_forSpotlightEntitiesRetrievedPerDeck_docFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent")
    private List<TFIDFEntity> TFIDFPerDeck = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("TFIDF_forSpotlightEntitiesRetrievedPerSlide_docFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent")
    public List<TFIDFEntity> getTFIDFPerSlide() {
        return TFIDFPerSlide;
    }

    @JsonProperty("TFIDF_forSpotlightEntitiesRetrievedPerSlide_docFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent")
    public void setTFIDFPerSlide(List<TFIDFEntity> TFIDFPerSlide) {
        this.TFIDFPerSlide = TFIDFPerSlide;
    }

    @JsonProperty("TFIDF_forSpotlightEntitiesRetrievedPerDeck_docFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent")
    public List<TFIDFEntity> getTFIDFPerDeck() {
        return TFIDFPerDeck;
    }

    @JsonProperty("TFIDF_forSpotlightEntitiesRetrievedPerDeck_docFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent")
    public void setTFIDFPerDeck(List<TFIDFEntity> TFIDFPerDeck) {
        this.TFIDFPerDeck = TFIDFPerDeck;
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