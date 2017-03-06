package de.bonn.eis.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ainuddin Faizan on 3/6/17.
 */


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "title",
        "id",
        "type",
        "user",
        "children"
})
public class Deck implements Serializable {

    private final static long serialVersionUID = -7706608184172017565L;
    @JsonProperty("title")
    private String title;
    @JsonProperty("id")
    private String id;
    @JsonProperty("type")
    private String type;
    @JsonProperty("user")
    private String user;
    @JsonProperty("children")
    private List<Slide> slides = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

}
