package com.turboio.games.vampires.story;

import java.util.Map;

public class StoryConfig {
    private String id;
    private String start;
    private Map<String, StoryNodeConfig> nodes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public Map<String, StoryNodeConfig> getNodes() {
        return nodes;
    }

    public void setNodes(Map<String, StoryNodeConfig> nodes) {
        this.nodes = nodes;
    }
}
