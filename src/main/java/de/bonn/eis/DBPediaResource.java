package de.bonn.eis; /**
 * Created by Ainuddin Faizan on 12/29/16.
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
import org.apache.commons.lang.builder.ToStringBuilder;

import static org.apache.commons.lang.builder.ToStringStyle.MULTI_LINE_STYLE;

//TODO Shorten using Project Lombok

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "@URI",
        "@support",
        "@types",
        "@surfaceForm",
        "@offset",
        "@similarityScore",
        "@percentageOfSecondRank"
})
public class DBPediaResource implements Serializable, Comparable
{

    @JsonProperty("@URI")
    private String uRI;
    @JsonProperty("@support")
    private String support;
    @JsonProperty("@types")
    private String types;
    @JsonProperty("@surfaceForm")
    private String surfaceForm;
    @JsonProperty("@offset")
    private String offset;
    @JsonProperty("@similarityScore")
    private String similarityScore;
    @JsonProperty("@percentageOfSecondRank")
    private String percentageOfSecondRank;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = -6649043481394421196L;

    /**
     * No args constructor for use in serialization
     *
     */
    public DBPediaResource() {
    }

    /**
     *
     * @param support
     * @param percentageOfSecondRank
     * @param uRI
     * @param surfaceForm
     * @param offset
     * @param types
     * @param similarityScore
     */
    public DBPediaResource(String uRI, String support, String types, String surfaceForm, String offset, String similarityScore, String percentageOfSecondRank) {
        super();
        this.uRI = uRI;
        this.support = support;
        this.types = types;
        this.surfaceForm = surfaceForm;
        this.offset = offset;
        this.similarityScore = similarityScore;
        this.percentageOfSecondRank = percentageOfSecondRank;
    }

    @JsonProperty("@URI")
    public String getURI() {
        return uRI;
    }

    @JsonProperty("@URI")
    public void setURI(String uRI) {
        this.uRI = uRI;
    }

    @JsonProperty("@support")
    public String getSupport() {
        return support;
    }

    @JsonProperty("@support")
    public void setSupport(String support) {
        this.support = support;
    }

    @JsonProperty("@types")
    public String getTypes() {
        return types;
    }

    @JsonProperty("@types")
    public void setTypes(String types) {
        this.types = types;
    }

    @JsonProperty("@surfaceForm")
    public String getSurfaceForm() {
        return surfaceForm;
    }

    @JsonProperty("@surfaceForm")
    public void setSurfaceForm(String surfaceForm) {
        this.surfaceForm = surfaceForm;
    }

    @JsonProperty("@offset")
    public String getOffset() {
        return offset;
    }

    @JsonProperty("@offset")
    public void setOffset(String offset) {
        this.offset = offset;
    }

    @JsonProperty("@similarityScore")
    public String getSimilarityScore() {
        return similarityScore;
    }

    @JsonProperty("@similarityScore")
    public void setSimilarityScore(String similarityScore) {
        this.similarityScore = similarityScore;
    }

    @JsonProperty("@percentageOfSecondRank")
    public String getPercentageOfSecondRank() {
        return percentageOfSecondRank;
    }

    @JsonProperty("@percentageOfSecondRank")
    public void setPercentageOfSecondRank(String percentageOfSecondRank) {
        this.percentageOfSecondRank = percentageOfSecondRank;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, MULTI_LINE_STYLE);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof DBPediaResource){
            DBPediaResource otherObj = (DBPediaResource) obj;
            return otherObj.getURI().equals(this.getURI()) && otherObj.getSurfaceForm().equals(this.getSurfaceForm());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getURI().hashCode();
    }
}