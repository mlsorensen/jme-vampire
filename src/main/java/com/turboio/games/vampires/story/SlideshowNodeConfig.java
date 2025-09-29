package com.turboio.games.vampires.story;

import java.util.List;

public class SlideshowNodeConfig extends StoryNodeConfig {
    private List<SlideConfig> slides;
    private String next;

    public List<SlideConfig> getSlides() {
        return slides;
    }

    public void setSlides(List<SlideConfig> slides) {
        this.slides = slides;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }
}
