package com.turboio.games.vampires.story;

public class SlideConfig {
    private String text;
    private float duration = 3f;
    private String backgroundColor = "#000000";
    private String backgroundImage;

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
}
