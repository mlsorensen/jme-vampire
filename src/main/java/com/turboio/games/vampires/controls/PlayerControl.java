package com.turboio.games.vampires.controls;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

import java.util.List;

public class PlayerControl extends AbstractControl {

    // is the player currently moving?
    public boolean up, down, left, right;

    // speed of the player
    private final float speed = 800f;

    // Perimeter data
    private final List<Vector3f> perimeter;
    private int currentSegmentIndex;
    private Vector3f currentDirection;

    public PlayerControl(List<Vector3f> perimeter) {
        this.perimeter = perimeter;
        this.currentSegmentIndex = 0;
        updateCurrentDirection();
    }

    private void updateCurrentDirection() {
        Vector3f start = perimeter.get(currentSegmentIndex);
        Vector3f end = perimeter.get((currentSegmentIndex + 1) % perimeter.size());
        this.currentDirection = end.subtract(start).normalizeLocal();
    }

    @Override
    protected void controlUpdate(float tpf) {
        // 1. Determine player's desired direction from input
        Vector3f desiredDirection = new Vector3f();
        if (up) desiredDirection.y = 1;
        if (down) desiredDirection.y = -1;
        if (left) desiredDirection.x = -1;
        if (right) desiredDirection.x = 1;

        if (desiredDirection.lengthSquared() == 0) {
            return; // No input, do nothing
        }
        desiredDirection.normalizeLocal();

        // 2. Check if the desired direction is aligned with the current path
        float dot = desiredDirection.dot(currentDirection);
        float moveDistance;

        if (dot > 0.9f) { // Moving forward along the segment
            moveDistance = speed * tpf;
        } else if (dot < -0.9f) { // Moving backward along the segment
            moveDistance = -speed * tpf;
        } else {
            return; // Input is perpendicular to the path, do nothing for now
        }

        // 3. Move the player
        spatial.move(currentDirection.mult(moveDistance));

        // 4. Clamp the player to the current segment and check for corners
        Vector3f start = perimeter.get(currentSegmentIndex);
        Vector3f end = perimeter.get((currentSegmentIndex + 1) % perimeter.size());

        // Project the player's position onto the line segment to see how far along they are
        Vector3f playerOffset = spatial.getLocalTranslation().subtract(start);
        float projection = playerOffset.dot(currentDirection);
        float segmentLength = end.distance(start);

        if (projection >= segmentLength) { // Reached or passed the end vertex
            spatial.setLocalTranslation(end);
            currentSegmentIndex = (currentSegmentIndex + 1) % perimeter.size();
            updateCurrentDirection();
        } else if (projection <= 0 && moveDistance < 0) { // Reached or passed the start vertex while moving backward
            spatial.setLocalTranslation(start);
            currentSegmentIndex = (currentSegmentIndex - 1 + perimeter.size()) % perimeter.size();
            updateCurrentDirection();
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // Not needed for this control
    }

    // reset the moving values (i.e. for spawning)
    public void reset() {
        up = false;
        down = false;
        left = false;
        right = false;
    }
}
