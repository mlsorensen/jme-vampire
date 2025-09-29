package com.turboio.games.vampires.story;

import com.turboio.games.vampires.level.LevelConfig;
import com.turboio.games.vampires.level.LevelLoader;
import com.turboio.games.vampires.story.StoryConfig;
import com.turboio.games.vampires.story.StoryLoader;
import com.turboio.games.vampires.story.StoryNodeConfig;
import com.turboio.games.vampires.story.LevelNodeConfig;
import com.turboio.games.vampires.story.SlideshowNodeConfig;
import com.turboio.games.vampires.story.SlideConfig;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class StoryValidationTest {

    private static final Path RESOURCE_ROOT = Paths.get("src/main/resources");

    @TestFactory
    Collection<DynamicTest> validateAllStories() throws IOException {
        List<Path> storyFiles = Files.walk(RESOURCE_ROOT.resolve("storylines"))
                .filter(path -> path.getFileName().toString().equals("story.json"))
                .collect(Collectors.toList());

        List<DynamicTest> tests = new ArrayList<>();
        for (Path storyPath : storyFiles) {
            tests.add(DynamicTest.dynamicTest("Validate " + RESOURCE_ROOT.relativize(storyPath),
                    () -> validateStory(storyPath)));
        }
        return tests;
    }

    private void validateStory(Path storyPath) throws IOException {
        String storyResource = RESOURCE_ROOT.relativize(storyPath).toString().replace('\\', '/');
        StoryConfig story = StoryLoader.load(storyResource);
        assertNotNull(story, "Failed to load story: " + storyResource);

        for (Map.Entry<String, StoryNodeConfig> entry : story.getNodes().entrySet()) {
            StoryNodeConfig node = entry.getValue();
            if (node instanceof LevelNodeConfig levelNode) {
                validateLevelNode(storyResource, levelNode);
            } else if (node instanceof SlideshowNodeConfig slideshowNode) {
                validateSlideshowNode(slideshowNode);
            }
        }
    }

    private void validateLevelNode(String storyResource, LevelNodeConfig levelNode) throws IOException {
        String levelPath = resolveLevelPath(storyResource, levelNode.getLevel());
        LevelConfig level = LevelLoader.load(levelPath);
        assertNotNull(level, "Failed to load level: " + levelPath);

        assertResourceExists(level.getBackgroundImage(), "Level background missing: " + levelPath);
        assertResourceExists(level.getForegroundImage(), "Level foreground missing: " + levelPath);
        if (level.getMusic() != null) {
            assertResourceExists(level.getMusic(), "Music missing: " + levelPath);
        }
    }

    private void validateSlideshowNode(SlideshowNodeConfig slideshowNode) {
        for (SlideConfig slide : slideshowNode.getSlides()) {
            if (slide.getBackgroundImage() != null) {
                assertResourceExists(slide.getBackgroundImage(), "Slide background missing for asset " + slide.getAssetId());
            }
        }
    }

    private void assertResourceExists(String resourcePath, String message) {
        if (resourcePath == null) {
            return;
        }
        Path path = RESOURCE_ROOT.resolve(resourcePath);
        assertTrue(Files.exists(path), message + " -> " + path);
    }

    private String resolveLevelPath(String storyResource, String levelFile) {
        if (levelFile.startsWith("storylines/")) {
            return levelFile;
        }
        int idx = storyResource.lastIndexOf('/') + 1;
        String base = storyResource.substring(0, idx);
        return base + "levels/" + levelFile;
    }
}
