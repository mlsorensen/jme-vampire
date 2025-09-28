package com.turboio.games.vampires.controls;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.turboio.games.vampires.perimeter.Perimeter;

import java.util.ArrayList;
import java.util.List;

public class PlayerControl extends AbstractControl {

    public enum State {
        ON_PERIMETER,
        DRAWING,
        COLLISION_DETECTED
    }

    private static final float SPATIAL_Z_OFFSET = 3f;

    // State
    private State currentState = State.ON_PERIMETER;
    public boolean up, down, left, right;
    private final float speed = 800f;

    // Perimeter Data
    private Perimeter perimeter;
    private int currentSegmentIndex;
    private Vector3f currentDirection;

    // Drawing Data
    private List<Vector3f> drawingPath;
    private Vector3f lastDrawingDirection;
    public Vector3f intersectionPoint;
    public int collidedSegmentIndex;
    private Vector3f stepOffPoint;
    private int stepOffSegmentIndex;

    public PlayerControl(Perimeter perimeter) {
        setPerimeter(perimeter);
        this.currentSegmentIndex = 0;
        updateCurrentDirection();
    }

    public final void setPerimeter(Perimeter perimeter) {
        this.perimeter = perimeter;
    }

    private void updateCurrentDirection() {
        List<Vector3f> vertices = perimeter.getVertices();
        if (vertices.isEmpty() || currentSegmentIndex >= vertices.size()) {
            // Cannot update direction if vertices are not set
            return;
        }
        Vector3f start = vertices.get(currentSegmentIndex);
        Vector3f end = vertices.get((currentSegmentIndex + 1) % vertices.size());
        this.currentDirection = end.subtract(start).normalizeLocal();
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (currentState == State.ON_PERIMETER) {
            updateOnPerimeter(tpf);
        } else if (currentState == State.DRAWING) {
            updateDrawing(tpf);
        }
    }

    private void updateOnPerimeter(float tpf) {
        Vector3f desiredDirection = getDesiredDirection();
        if (desiredDirection.lengthSquared() == 0) return;

        float dot = desiredDirection.dot(currentDirection);
        float moveDistance;

        if (dot > 0.9f) { // Moving forward
            moveDistance = speed * tpf;
        } else if (dot < -0.9f) { // Moving backward
            moveDistance = -speed * tpf;
        } else { // Perpendicular move attempt
            handleStepOff(desiredDirection, tpf);
            return;
        }

        spatial.move(currentDirection.mult(moveDistance));
        clampToSegment(moveDistance);
    }

    private void handleStepOff(Vector3f desiredDirection, float tpf) {
        Vector3f currentPos = spatial.getLocalTranslation();
        Vector3f nextPos = currentPos.add(desiredDirection.mult(speed * tpf));

        if (perimeter.contains(nextPos)) {
            this.stepOffPoint = new Vector3f(currentPos.x, currentPos.y, 0);
            this.stepOffSegmentIndex = this.currentSegmentIndex;
            currentState = State.DRAWING;
            drawingPath = new ArrayList<>();
            drawingPath.add(this.stepOffPoint.clone());
            lastDrawingDirection = desiredDirection.clone();
            spatial.move(desiredDirection.mult(speed * tpf));
        }
    }

    private void updateDrawing(float tpf) {
        Vector3f desiredDirection = getDesiredDirection();
        if (desiredDirection.lengthSquared() == 0) return;

        // Add a new vertex (corner) if the direction has changed significantly.
        if (desiredDirection.dot(lastDrawingDirection) < 0.999f) {
            drawingPath.add(new Vector3f(spatial.getLocalTranslation().x, spatial.getLocalTranslation().y, 0));
            lastDrawingDirection = desiredDirection.clone();
        }

        Vector3f oldPos = spatial.getLocalTranslation().clone();
        spatial.move(desiredDirection.mult(speed * tpf));
        Vector3f newPos = spatial.getLocalTranslation();

        List<Vector3f> vertices = perimeter.getVertices();
        for (int i = 0; i < vertices.size(); i++) {
            Vector3f p1 = vertices.get(i);
            Vector3f p2 = vertices.get((i + 1) % vertices.size());
            Vector3f intersection = getLineIntersection(oldPos, newPos, p1, p2);

            if (intersection != null) {
                drawingPath.add(intersection.clone());
                spatial.setLocalTranslation(intersection.add(0, 0, SPATIAL_Z_OFFSET));

                this.intersectionPoint = intersection;
                this.collidedSegmentIndex = i;
                this.currentState = State.COLLISION_DETECTED;
                return;
            }
        }
    }

    private Vector3f getLineIntersection(Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4) {
        float s1_x = p2.x - p1.x;
        float s1_y = p2.y - p1.y;
        float s2_x = p4.x - p3.x;
        float s2_y = p4.y - p3.y;

        float denominator = (-s2_x * s1_y + s1_x * s2_y);
        if (Math.abs(denominator) < 0.0001f) return null; // Parallel lines

        float s = (-s1_y * (p1.x - p3.x) + s1_x * (p1.y - p3.y)) / denominator;
        float t = (s2_x * (p1.y - p3.y) - s2_y * (p1.x - p3.x)) / denominator;

        if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
            return new Vector3f(p1.x + (t * s1_x), p1.y + (t * s1_y), 0);
        }

        return null; // No collision
    }

    private Vector3f getDesiredDirection() {
        Vector3f desiredDirection = new Vector3f();
        if (up) desiredDirection.y = 1;
        if (down) desiredDirection.y = -1;
        if (left) desiredDirection.x = -1;
        if (right) desiredDirection.x = 1;
        return desiredDirection.normalizeLocal();
    }

    private void clampToSegment(float moveDistance) {
        List<Vector3f> vertices = perimeter.getVertices();
        Vector3f start = vertices.get(currentSegmentIndex);
        Vector3f end = vertices.get((currentSegmentIndex + 1) % vertices.size());

        Vector3f playerOffset = spatial.getLocalTranslation().subtract(start.add(0, 0, SPATIAL_Z_OFFSET));
        float projection = playerOffset.dot(currentDirection);
        float segmentLength = end.distance(start);

        if (projection >= segmentLength) {
            spatial.setLocalTranslation(end.add(0, 0, SPATIAL_Z_OFFSET));
            currentSegmentIndex = (currentSegmentIndex + 1) % vertices.size();
            updateCurrentDirection();
        } else if (projection <= 0 && moveDistance < 0) {
            spatial.setLocalTranslation(start.add(0, 0, SPATIAL_Z_OFFSET));
            currentSegmentIndex = (currentSegmentIndex - 1 + vertices.size()) % vertices.size();
            updateCurrentDirection();
        }
    }

    public boolean wasCollisionDetected() {
        return currentState == State.COLLISION_DETECTED;
    }

    public void finalizeCollision(Perimeter newPerimeter) {
        setPerimeter(newPerimeter);
        this.currentState = State.ON_PERIMETER;
        this.drawingPath = null;

        // The player is now at the intersection point on the new perimeter.
        // Find the index of this vertex to correctly set the current segment.
        int intersectionVertexIndex = newPerimeter.getVertices().indexOf(this.intersectionPoint);

        if (intersectionVertexIndex != -1) {
            // The player is at the start of the segment from the intersection point
            this.currentSegmentIndex = intersectionVertexIndex;
        } else {
            // This indicates a problem with the new perimeter's vertex list, as it should contain the intersection point.
            // As a fallback, we'll just start at the beginning of the new perimeter.
            this.currentSegmentIndex = 0;
            // Also move the player to that point to avoid being in a weird state.
            if (!newPerimeter.getVertices().isEmpty()) {
                Vector3f fallbackPosition = newPerimeter.getVertices().get(0);
                spatial.setLocalTranslation(fallbackPosition.add(0, 0, SPATIAL_Z_OFFSET));
            }
        }
        updateCurrentDirection();
    }

    public List<Vector3f> getDrawingPath() {
        return drawingPath;
    }

    public List<Vector3f> getVisualDrawingPath() {
        if (currentState != State.DRAWING || drawingPath == null) {
            return null;
        }
        // Create a temporary list for visualization
        List<Vector3f> visualPath = new ArrayList<>(drawingPath);
        // Add the player's current position to the end of the list
        visualPath.add(new Vector3f(spatial.getLocalTranslation().x, spatial.getLocalTranslation().y, 0));
        return visualPath;
    }

    public Vector3f getStepOffPoint() {
        return stepOffPoint;
    }

    public int getStepOffSegmentIndex() {
        return stepOffSegmentIndex;
    }

    public State getCurrentState() {
        return currentState;
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}

    public void reset() {
        up = down = left = right = false;
        currentState = State.ON_PERIMETER;
        drawingPath = null;
    }
}
