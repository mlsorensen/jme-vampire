package com.turboio.games.vampires.level;

public class EnemyConfig {
    private String sprite;
    private float radius;
    private float spawnX;
    private float spawnY;
    private String movementClass;
    private float speed;

    public String getSprite() {
        return sprite;
    }

    public void setSprite(String sprite) {
        this.sprite = sprite;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public float getSpawnX() {
        return spawnX;
    }

    public void setSpawnX(float spawnX) {
        this.spawnX = spawnX;
    }

    public float getSpawnY() {
        return spawnY;
    }

    public void setSpawnY(float spawnY) {
        this.spawnY = spawnY;
    }

    public String getMovementClass() {
        return movementClass;
    }

    public void setMovementClass(String movementClass) {
        this.movementClass = movementClass;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }
}
