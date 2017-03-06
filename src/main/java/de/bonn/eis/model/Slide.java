package de.bonn.eis.model;

import com.fasterxml.jackson.annotation.*;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ainuddin Faizan on 3/6/17.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "title",
        "content",
        "speakernotes",
        "user",
        "id",
        "type"
})
public class Slide implements Serializable
{

    @JsonProperty("title")
    private String title;
    @JsonProperty("content")
    private String content;
    @JsonProperty("speakernotes")
    private String speakernotes;
    @JsonProperty("user")
    private String user;
    @JsonProperty("id")
    private String id;
    @JsonProperty("type")
    private String type;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();
    private final static long serialVersionUID = 3273068580799385327L;

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("content")
    public String getContent() {
        return content;
    }

    @JsonProperty("content")
    public void setContent(String content) {
        this.content = content;
    }

    @JsonProperty("speakernotes")
    public String getSpeakernotes() {
        return speakernotes;
    }

    @JsonProperty("speakernotes")
    public void setSpeakernotes(String speakernotes) {
        this.speakernotes = speakernotes;
    }

    @JsonProperty("user")
    public String getUser() {
        return user;
    }

    @JsonProperty("user")
    public void setUser(String user) {
        this.user = user;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(title).append(content).append(speakernotes).append(user).append(id).append(type).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Slide)) {
            return false;
        }
        Slide rhs = ((Slide) other);
        return new EqualsBuilder().append(title, rhs.title).append(content, rhs.content).append(speakernotes, rhs.speakernotes).append(user, rhs.user).append(id, rhs.id).append(type, rhs.type).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}
