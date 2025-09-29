package com.turboio.games.vampires.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.turboio.games.vampires.level.LevelConfig;
import com.turboio.games.vampires.level.LevelLoader;
import com.turboio.games.vampires.story.EndingNodeConfig;
import com.turboio.games.vampires.story.LevelNodeConfig;
import com.turboio.games.vampires.story.SlideConfig;
import com.turboio.games.vampires.story.SlideshowNodeConfig;
import com.turboio.games.vampires.story.StoryConfig;
import com.turboio.games.vampires.story.StoryLoader;
import com.turboio.games.vampires.story.StoryNodeConfig;
import com.turboio.games.vampires.states.LevelAppState.LevelOutcome;

import java.io.IOException;
import java.util.List;

public class StoryAppState extends BaseAppState implements LevelAppState.LevelResultListener, SlideshowAppState.SlideshowListener {

    private final String storyPath;
    private StoryConfig storyConfig;
    private StoryNodeConfig currentNode;

    private LevelAppState activeLevel;
    private SlideshowAppState activeSlideshow;
    private String pendingNextNodeId;

    public StoryAppState(String storyPath) {
        this.storyPath = storyPath;
    }

    @Override
    protected void initialize(Application app) {
        try {
            storyConfig = StoryLoader.load(storyPath);
            currentNode = storyConfig.getNodes().get(storyConfig.getStart());
            advance();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load story: " + storyPath, e);
        }
    }

    private void advance() {
        if (currentNode == null) {
            return;
        }
        if (currentNode instanceof SlideshowNodeConfig) {
            startSlideshow((SlideshowNodeConfig) currentNode);
        } else if (currentNode instanceof LevelNodeConfig) {
            startLevel((LevelNodeConfig) currentNode);
        } else if (currentNode instanceof EndingNodeConfig) {
            startEnding((EndingNodeConfig) currentNode);
        }
    }

    private void startSlideshow(SlideshowNodeConfig config) {
        List<SlideConfig> slides = config.getSlides();
        activeSlideshow = new SlideshowAppState(slides, this);
        getStateManager().attach(activeSlideshow);
    }

    private void startLevel(LevelNodeConfig config) {
        try {
            LevelConfig levelConfig = LevelLoader.load(resolveLevelPath(config.getLevel()));
            activeLevel = new LevelAppState(levelConfig, this);
            getStateManager().attach(activeLevel);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load level: " + config.getLevel(), e);
        }
    }

    private String resolveLevelPath(String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith("storylines/")) {
            return path;
        }
        int lastSlash = storyPath.lastIndexOf('/');
        if (lastSlash < 0) {
            return path;
        }
        String base = storyPath.substring(0, lastSlash + 1);
        return base + "levels/" + path;
    }

    private void startEnding(EndingNodeConfig config) {
        String title = "gameOver".equalsIgnoreCase(config.getEndingType()) ? "THE END" : config.getEndingType();
        getStateManager().attach(new LevelSummaryAppState(title, 0, 0, false, this::returnToStartScreen));
    }

    private void transitionTo(String nextId) {
        if (nextId == null) {
            currentNode = null;
            return;
        }
        currentNode = storyConfig.getNodes().get(nextId);
        advance();
    }

    @Override
    protected void cleanup(Application app) {}

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}

    @Override
    public void onLevelCompleted(LevelOutcome outcome, double score, double percentage) {
        if (!(currentNode instanceof LevelNodeConfig)) {
            return;
        }
        LevelNodeConfig levelNode = (LevelNodeConfig) currentNode;
        String title = outcome == LevelOutcome.WIN ? "YOU WIN" : "YOU LOSE";
        pendingNextNodeId = outcome == LevelOutcome.WIN ? levelNode.getOnWin() : levelNode.getOnLose();
        getStateManager().attach(new LevelSummaryAppState(title, score, percentage, true, this::onEndScreenDismissed));
    }

    private void onEndScreenDismissed() {
        if (pendingNextNodeId != null) {
            transitionTo(pendingNextNodeId);
            pendingNextNodeId = null;
        }
    }

    private void returnToStartScreen() {
        getStateManager().detach(this);
        getStateManager().attach(new StartScreenAppState());
    }

    @Override
    public void onSlideshowFinished() {
        if (!(currentNode instanceof SlideshowNodeConfig)) {
            return;
        }
        SlideshowNodeConfig slideshowNode = (SlideshowNodeConfig) currentNode;
        transitionTo(slideshowNode.getNext());
    }
}
