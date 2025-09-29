package com.turboio.games.vampires.level;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class LevelLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static LevelConfig load(String path) throws IOException {
        try (InputStream stream = LevelLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Level config not found: " + path);
            }
            return MAPPER.readValue(stream, LevelConfig.class);
        }
    }
}
