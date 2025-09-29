package com.turboio.games.vampires.story;

public class LevelNodeConfig extends StoryNodeConfig {
    private String level;
    private String onWin;
    private String onLose;

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getOnWin() {
        return onWin;
    }

    public void setOnWin(String onWin) {
        this.onWin = onWin;
    }

    public String getOnLose() {
        return onLose;
    }

    public void setOnLose(String onLose) {
        this.onLose = onLose;
    }
}
