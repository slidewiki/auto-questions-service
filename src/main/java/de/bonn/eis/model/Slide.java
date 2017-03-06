package de.bonn.eis.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ainuddin Faizan on 3/6/17.
 */

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "title",
        "content",
        "speakernotes",
        "user",
        "id",
        "type"
})
public class Slide implements Serializable {

    private final static long serialVersionUID = 3273068580799385327L;
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

}
