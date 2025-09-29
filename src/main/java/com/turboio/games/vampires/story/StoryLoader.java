package com.turboio.games.vampires.story;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class StoryLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static StoryConfig load(String path) throws IOException {
        try (InputStream stream = StoryLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Story config not found: " + path);
            }
            return MAPPER.readValue(stream, StoryConfig.class);
        }
    }
}
