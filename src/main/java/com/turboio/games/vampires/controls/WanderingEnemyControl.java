package com.turboio.games.vampires.controls;

import com.jme3.math.Vector3f;
import com.jme3.scene.control.AbstractControl;
import com.turboio.games.vampires.perimeter.Perimeter;

import java.util.Random;

public class WanderingEnemyControl extends AbstractControl {

    private static final float DEFAULT_MIN_INTERVAL = 1.5f;
    private static final float DEFAULT_MAX_INTERVAL = 3.5f;

    private final Random random = new Random();

    private float speed;
    private float minInterval;
    private float maxInterval;
    private float directionTimer = 0f;
    private final Vector3f velocity = new Vector3f();
    private Perimeter perimeter;

    public WanderingEnemyControl(Perimeter perimeter, float speed) {
        this(perimeter, speed, DEFAULT_MIN_INTERVAL, DEFAULT_MAX_INTERVAL);
    }

    public WanderingEnemyControl(Perimeter perimeter, float speed, float minInterval, float maxInterval) {
        this.perimeter = perimeter;
        this.speed = speed;
        this.minInterval = minInterval;
        this.maxInterval = maxInterval;
        chooseNewDirection();
    }

    public void setPerimeter(Perimeter perimeter) {
        this.perimeter = perimeter;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setDirectionInterval(float minInterval, float maxInterval) {
        this.minInterval = minInterval;
        this.maxInterval = Math.max(minInterval, maxInterval);
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (!isEnabled() || spatial == null || perimeter == null) {
            return;
        }

        directionTimer -= tpf;
        if (directionTimer <= 0f) {
            chooseNewDirection();
        }

        Vector3f currentPos = spatial.getLocalTranslation();
        Vector3f step = velocity.mult(speed * tpf);
        Vector3f nextPos = currentPos.add(step);

        if (!perimeter.contains(nextPos)) {
            chooseNewDirection();
            return;
        }

        spatial.setLocalTranslation(nextPos);
    }

    @Override
    protected void controlRender(com.jme3.renderer.RenderManager rm, com.jme3.renderer.ViewPort vp) {}

    private void chooseNewDirection() {
        do {
            velocity.set(random.nextFloat() * 2f - 1f, random.nextFloat() * 2f - 1f, 0f);
        } while (velocity.lengthSquared() == 0f);
        velocity.normalizeLocal();
        directionTimer = minInterval + random.nextFloat() * (maxInterval - minInterval);
    }
}
