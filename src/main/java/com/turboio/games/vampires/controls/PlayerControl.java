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

        if (dot > 0.5f) { // Moving forward (relaxed threshold)
            moveDistance = speed * tpf;
            spatial.move(currentDirection.mult(moveDistance));
            clampToSegment(moveDistance);
        } else if (dot < -0.5f) { // Moving backward (relaxed threshold)
            moveDistance = -speed * tpf;
            spatial.move(currentDirection.mult(moveDistance));
            clampToSegment(moveDistance);
        } else {
            // Check if we can move along an adjacent segment instead
            if (tryMoveAlongAdjacentSegment(desiredDirection, tpf)) {
                return; // Successfully moved along adjacent segment
            }
            
            // If no adjacent segment works, treat as perpendicular move attempt
            
            handleStepOff(desiredDirection, tpf);
        }
    }

    private void handleStepOff(Vector3f desiredDirection, float tpf) {
        Vector3f currentPos = spatial.getLocalTranslation();
        Vector3f nextPos = currentPos.add(desiredDirection.mult(speed * tpf));

        // Simple check: just ensure next position is inside the perimeter
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
        Vector3f newPos = oldPos.add(desiredDirection.mult(speed * tpf));

        // Self-intersection check
        if (drawingPath.size() >= 4) {
            for (int i = 0; i < drawingPath.size() - 3; i++) {
                Vector3f p1 = drawingPath.get(i);
                Vector3f p2 = drawingPath.get(i + 1);
                Vector3f intersection = getLineIntersection(oldPos, newPos, p1, p2);
                if (intersection != null) {
                    return; // Block the move
                }
            }
        }

        spatial.setLocalTranslation(newPos);

        List<Vector3f> vertices = perimeter.getVertices();
        for (int i = 0; i < vertices.size(); i++) {
            Vector3f p1 = vertices.get(i);
            Vector3f p2 = vertices.get((i + 1) % vertices.size());
            Vector3f intersection = getLineIntersection(oldPos, newPos, p1, p2);

            if (intersection != null) {
                // A collision with the perimeter occurred.
                if (i == this.stepOffSegmentIndex && drawingPath.size() < 4) {
                    // The collision is with the starting segment and the path is too short.
                    // This is considered a "cancel" action.
                    cancelDrawing();
                    return;
                } else {
                    // This is a valid collision to complete a new perimeter.
                    drawingPath.add(intersection.clone());
                    spatial.setLocalTranslation(intersection.add(0, 0, SPATIAL_Z_OFFSET));

                    this.intersectionPoint = intersection;
                    this.collidedSegmentIndex = i;
                    this.currentState = State.COLLISION_DETECTED;
                    return;
                }
            }
        }
    }

    private void cancelDrawing() {
        this.currentState = State.ON_PERIMETER;
        this.drawingPath = null;
        // Move the player back to where they stepped off.
        spatial.setLocalTranslation(this.stepOffPoint.add(0, 0, SPATIAL_Z_OFFSET));
        this.currentSegmentIndex = this.stepOffSegmentIndex;
        updateCurrentDirection();
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

        // Add a small epsilon to the bounds check to allow for connecting near vertices.
        float epsilon = 0.001f;
        if (s >= -epsilon && s <= 1 + epsilon && t > epsilon && t < 1 - epsilon) {
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


        // Find which segment the intersection point is actually on
        int intersectionSegmentIndex = findSegmentContainingPoint(newPerimeter.getVertices(), this.intersectionPoint);

        if (intersectionSegmentIndex != -1) {
            // Position the player at the intersection point and set the segment
            this.currentSegmentIndex = intersectionSegmentIndex;
            spatial.setLocalTranslation(this.intersectionPoint.add(0, 0, SPATIAL_Z_OFFSET));
            
        } else {
            // Fallback: find closest vertex
            int intersectionVertexIndex = findClosestVertexIndex(newPerimeter.getVertices(), this.intersectionPoint);
            if (intersectionVertexIndex != -1) {
                this.currentSegmentIndex = intersectionVertexIndex;
                Vector3f perimeterPosition = newPerimeter.getVertices().get(intersectionVertexIndex);
                spatial.setLocalTranslation(perimeterPosition.add(0, 0, SPATIAL_Z_OFFSET));
            } else {
                // Ultimate fallback: start at the beginning
                this.currentSegmentIndex = 0;
                if (!newPerimeter.getVertices().isEmpty()) {
                    Vector3f fallbackPosition = newPerimeter.getVertices().get(0);
                    spatial.setLocalTranslation(fallbackPosition.add(0, 0, SPATIAL_Z_OFFSET));
                }
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

    private int findSegmentContainingPoint(List<Vector3f> vertices, Vector3f point) {
        if (vertices.isEmpty() || point == null) {
            return -1;
        }

        float threshold = 1.0f; // Point must be within 1 unit of the segment
        
        for (int i = 0; i < vertices.size(); i++) {
            Vector3f a = vertices.get(i);
            Vector3f b = vertices.get((i + 1) % vertices.size());
            
            float distance = distanceToLineSegment(point, a, b);
            if (distance < threshold) {
                // Also check if the point is actually between a and b (not beyond the endpoints)
                Vector3f ab = b.subtract(a);
                Vector3f ap = point.subtract(a);
                
                float abSquared = ab.lengthSquared();
                if (abSquared == 0) continue; // Skip degenerate segments
                
                float t = ap.dot(ab) / abSquared;
                if (t >= -0.01f && t <= 1.01f) { // Small tolerance for floating point errors
                    // If t is very close to 1.0, the point is at the end vertex of this segment,
                    // which is also the start of the next segment. In this case, prefer the next segment
                    // so the player can move forward along the perimeter.
                    if (t > 0.95f) {
                        int nextSegment = (i + 1) % vertices.size();
                        return nextSegment;
                    }
                    
                    return i;
                }
            }
        }
        
        return -1; // Point not found on any segment
    }

    private int findClosestVertexIndex(List<Vector3f> vertices, Vector3f targetPoint) {
        if (vertices.isEmpty() || targetPoint == null) {
            return -1;
        }

        int closestIndex = 0;
        float closestDistance = vertices.get(0).distanceSquared(targetPoint);

        for (int i = 1; i < vertices.size(); i++) {
            float distance = vertices.get(i).distanceSquared(targetPoint);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestIndex = i;
            }
        }

        // Only return the index if it's reasonably close (within 1 unit)
        return closestDistance < 1.0f ? closestIndex : -1;
    }


    private boolean tryMoveAlongAdjacentSegment(Vector3f desiredDirection, float tpf) {
        Vector3f currentPos = spatial.getLocalTranslation();
        List<Vector3f> vertices = perimeter.getVertices();
        
        
        // Check if we're very close to a vertex (ignoring Z coordinate)
        for (int i = 0; i < vertices.size(); i++) {
            Vector3f vertex = vertices.get(i);
            Vector3f currentPos2D = new Vector3f(currentPos.x, currentPos.y, 0);
            Vector3f vertex2D = new Vector3f(vertex.x, vertex.y, 0);
            float distance = currentPos2D.distance(vertex2D);
            
            if (distance < 2.0f) { // Within 2 units of a vertex
                // Check the two segments connected to this vertex
                int prevSegment = (i - 1 + vertices.size()) % vertices.size();
                int nextSegment = i;
                
                // Calculate directions for both segments, forward and backward
                Vector3f prevStart = vertices.get(prevSegment);
                Vector3f prevForward = vertex.subtract(prevStart).normalizeLocal();
                Vector3f prevBackward = prevForward.negate();
                
                Vector3f nextEnd = vertices.get((i + 1) % vertices.size());
                Vector3f nextForward = nextEnd.subtract(vertex).normalizeLocal();
                Vector3f nextBackward = nextForward.negate();
                
                float prevForwardDot = desiredDirection.dot(prevForward);
                float prevBackwardDot = desiredDirection.dot(prevBackward);
                float nextForwardDot = desiredDirection.dot(nextForward);
                float nextBackwardDot = desiredDirection.dot(nextBackward);
                
                
                // Find the best alignment among all directions
                float bestDot = Math.max(Math.max(prevForwardDot, prevBackwardDot), Math.max(nextForwardDot, nextBackwardDot));
                
                // If we're at a vertex and no direction is very good (< 0.8), allow step-off instead
                // This prevents getting stuck in segment switching loops
                if (bestDot < 0.8f) {
                    return false; // Let handleStepOff handle it
                }
                
                if (bestDot > 0.5f) {
                    if (bestDot == prevForwardDot) {
                        this.currentSegmentIndex = prevSegment;
                        updateCurrentDirection();
                        spatial.move(prevForward.mult(speed * tpf));
                        clampToSegment(speed * tpf);
                        return true;
                    } else if (bestDot == prevBackwardDot) {
                        this.currentSegmentIndex = prevSegment;
                        updateCurrentDirection();
                        spatial.move(prevBackward.mult(speed * tpf));
                        clampToSegment(-speed * tpf); // Negative for backward movement
                        return true;
                    } else if (bestDot == nextForwardDot) {
                        this.currentSegmentIndex = nextSegment;
                        updateCurrentDirection();
                        spatial.move(nextForward.mult(speed * tpf));
                        clampToSegment(speed * tpf);
                        return true;
                    } else if (bestDot == nextBackwardDot) {
                        this.currentSegmentIndex = nextSegment;
                        updateCurrentDirection();
                        spatial.move(nextBackward.mult(speed * tpf));
                        clampToSegment(-speed * tpf); // Negative for backward movement
                        return true;
                    }
                }
            }
        }
        
        return false; // No suitable adjacent segment found
    }

    private float distanceToLineSegment(Vector3f point, Vector3f a, Vector3f b) {
        Vector3f ab = b.subtract(a);
        Vector3f ap = point.subtract(a);
        
        float abSquared = ab.lengthSquared();
        if (abSquared == 0) return ap.length(); // a == b case
        
        float t = Math.max(0, Math.min(1, ap.dot(ab) / abSquared));
        Vector3f projection = a.add(ab.mult(t));
        return point.distance(projection);
    }
}
