package com.turboio.games.vampires.story;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LevelNodeConfig.class, name = "level"),
        @JsonSubTypes.Type(value = SlideshowNodeConfig.class, name = "slideshow"),
        @JsonSubTypes.Type(value = EndingNodeConfig.class, name = "ending")
})
public abstract class StoryNodeConfig {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
