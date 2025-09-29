package com.turboio.games.vampires.level;

import java.util.List;

public class LevelConfig {
    private String backgroundImage;
    private String foregroundImage;
    private String music;
    private List<Vector2> perimeterVertices;
    private List<EnemyConfig> enemies;

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public String getForegroundImage() {
        return foregroundImage;
    }

    public void setForegroundImage(String foregroundImage) {
        this.foregroundImage = foregroundImage;
    }

    public String getMusic() {
        return music;
    }

    public void setMusic(String music) {
        this.music = music;
    }

    public List<Vector2> getPerimeterVertices() {
        return perimeterVertices;
    }

    public void setPerimeterVertices(List<Vector2> perimeterVertices) {
        this.perimeterVertices = perimeterVertices;
    }

    public List<EnemyConfig> getEnemies() {
        return enemies;
    }

    public void setEnemies(List<EnemyConfig> enemies) {
        this.enemies = enemies;
    }
}
