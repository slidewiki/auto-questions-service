package de.bonn.eis.model; /**
 * Created by Ainuddin Faizan on 12/28/16.
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
import org.apache.commons.lang.builder.ToStringBuilder;

//TODO Shorten using Project Lombok

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "@text",
        "@confidence",
        "@support",
        "@types",
        "@sparql",
        "@policy",
        "Resources"
})
public class DBPediaSpotlightPOJO implements Serializable
{

    @JsonProperty("@text")
    private String text;
    @JsonProperty("@confidence")
    private String confidence;
    @JsonProperty("@support")
    private String support;
    @JsonProperty("@types")
    private String types;
    @JsonProperty("@sparql")
    private String sparql;
    @JsonProperty("@policy")
    private String policy;
    @JsonProperty("Resources")
    private List<DBPediaResource> DBPediaResources = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = -4700003125831217985L;

    /**
     * No args constructor for use in serialization
     *
     */
    public DBPediaSpotlightPOJO() {
    }

    /**
     *
     * @param DBPediaResources
     * @param text
     * @param support
     * @param policy
     * @param confidence
     * @param types
     * @param sparql
     */
    public DBPediaSpotlightPOJO(String text, String confidence, String support, String types, String sparql, String policy, List<DBPediaResource> DBPediaResources) {
        super();
        this.text = text;
        this.confidence = confidence;
        this.support = support;
        this.types = types;
        this.sparql = sparql;
        this.policy = policy;
        this.DBPediaResources = DBPediaResources;
    }

    @JsonProperty("@text")
    public String getText() {
        return text;
    }

    @JsonProperty("@text")
    public void setText(String text) {
        this.text = text;
    }

    @JsonProperty("@confidence")
    public String getConfidence() {
        return confidence;
    }

    @JsonProperty("@confidence")
    public void setConfidence(String confidence) {
        this.confidence = confidence;
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

    @JsonProperty("@sparql")
    public String getSparql() {
        return sparql;
    }

    @JsonProperty("@sparql")
    public void setSparql(String sparql) {
        this.sparql = sparql;
    }

    @JsonProperty("@policy")
    public String getPolicy() {
        return policy;
    }

    @JsonProperty("@policy")
    public void setPolicy(String policy) {
        this.policy = policy;
    }

    @JsonProperty("Resources")
    public List<DBPediaResource> getDBPediaResources() {
        return DBPediaResources;
    }

    @JsonProperty("Resources")
    public void setDBPediaResources(List<DBPediaResource> DBPediaResources) {
        this.DBPediaResources = DBPediaResources;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
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