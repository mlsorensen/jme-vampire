package com.turboio.games.vampires.story;

import java.util.List;

public class SlideConfig {
    private String text;
    private float duration = 3f;
    private String backgroundColor = "#000000";
    private String backgroundImage;
    private String assetId;
    private String targetPath;
    private List<String> characterFocus;
    private String mood;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public List<String> getCharacterFocus() {
        return characterFocus;
    }

    public void setCharacterFocus(List<String> characterFocus) {
        this.characterFocus = characterFocus;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }
}
