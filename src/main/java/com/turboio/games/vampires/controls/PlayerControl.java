package com.turboio.games.vampires.controls;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.turboio.games.vampires.perimeter.Perimeter;
import com.turboio.games.vampires.perimeter.PerimeterManager;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.ArrayList;
import java.util.List;

public class PlayerControl extends AbstractControl {

    private static final float SPATIAL_Z_OFFSET = 3f;
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING_SINGLE));

    // Movement
    public boolean up, down, left, right;
    private final float speed = 400f;
    private Vector3f lastDirection;

    // Game Data
    private Perimeter perimeter;
    private PerimeterManager perimeterManager;
    private List<Vector3f> playerPath;
    private boolean collisionDetected = false;
    private boolean isDrawing = false; // Track if we're currently drawing a cut
    public Vector3f intersectionPoint;

    public PlayerControl(Perimeter perimeter, PerimeterManager perimeterManager) {
        setPerimeter(perimeter);
        this.perimeterManager = perimeterManager;
        this.playerPath = new ArrayList<>();
        this.collisionDetected = false;
        this.isDrawing = false;
        this.lastDirection = new Vector3f();
    }

    public final void setPerimeter(Perimeter perimeter) {
        this.perimeter = perimeter;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (collisionDetected) {
            return; // Wait for game to handle collision
        }

        Vector3f desiredDirection = getDesiredDirection();
        
        // Update animated sprite direction if available
        updateAnimatedSprite(desiredDirection);
        
        if (desiredDirection.lengthSquared() == 0) {
            return; // No input
        }

        if (isDrawing && lastDirection.lengthSquared() > 0 && !desiredDirection.equals(lastDirection)) {
            playerPath.add(new Vector3f(spatial.getLocalTranslation().x, spatial.getLocalTranslation().y, 0));
        }
        lastDirection.set(desiredDirection);

        Vector3f currentPos = spatial.getLocalTranslation();
        Vector3f nextPos = currentPos.add(desiredDirection.mult(speed * tpf));

        // Check if next position is within the perimeter
        if (perimeter.contains(nextPos)) {
            // Check if we should allow this move
            boolean wasOnBoundary = perimeterManager.isOnBoundary(currentPos, perimeter);
            boolean willBeOnBoundary = perimeterManager.isOnBoundary(nextPos, perimeter);
            
            if (!isDrawing) {
                // If not drawing, only allow movement along the boundary
                if (!wasOnBoundary && !willBeOnBoundary) {
                    // Both positions are in interior - don't allow free movement in interior
                    System.out.println("Blocking interior movement - must stay on boundary or start cut");
                    return;
                } else if (wasOnBoundary && !willBeOnBoundary) {
                    // Stepping off boundary - start drawing
                    System.out.println("Starting cut - stepped off boundary");
                    isDrawing = true;
                    playerPath.clear();
                    // Add starting point on boundary
                    playerPath.add(new Vector3f(currentPos.x, currentPos.y, 0));
                }
            }
            
            // Valid move - update position
            spatial.setLocalTranslation(nextPos);
            
            // When drawing, we only track start and end points
            // The path will be a simple straight line from start to collision point
        } else {
            // Hit boundary - check if we've created a valid cut
            if (isDrawing) {
                checkForPerimeterCollision(currentPos, nextPos);
            }
            // If not drawing, just block the move (stay on perimeter)
        }
    }

    private void checkForPerimeterCollision(Vector3f currentPos, Vector3f nextPos) {
        LineString movementLine = geometryFactory.createLineString(new Coordinate[]{
            new Coordinate(currentPos.x, currentPos.y),
            new Coordinate(nextPos.x, nextPos.y)
        });

        Vector3f intersection = perimeterManager.getIntersection(movementLine, perimeter);

        if (intersection != null) {
            System.out.println("PlayerControl: Collision detected with perimeter.");
            System.out.println("  Path has " + playerPath.size() + " vertices before adding collision point.");
            // Found intersection with perimeter boundary
            if (playerPath.size() >= 1) { // Need at least a starting point for a valid cut
                this.intersectionPoint = intersection;
                this.collisionDetected = true;
                
                // Position player at intersection point
                spatial.setLocalTranslation(intersection.add(0, 0, SPATIAL_Z_OFFSET));
                playerPath.add(new Vector3f(intersection.x, intersection.y, 0));
            } else {
                System.out.println("  Path too short, warping player back to start.");
                // Path too short - reset to start of path
                if (!playerPath.isEmpty()) {
                    Vector3f startPos = playerPath.get(0);
                    spatial.setLocalTranslation(startPos.add(0, 0, SPATIAL_Z_OFFSET));
                    playerPath.clear();
                    isDrawing = false; // Stop drawing
                }
            }
        }
    }

    private Vector3f getDesiredDirection() {
        Vector3f desiredDirection = new Vector3f();
        if (up) desiredDirection.y = 1;
        if (down) desiredDirection.y = -1;
        if (left) desiredDirection.x = -1;
        if (right) desiredDirection.x = 1;
        return desiredDirection.normalizeLocal();
    }
    
    private void updateAnimatedSprite(Vector3f direction) {
        AnimatedSpriteControl animControl = spatial.getControl(AnimatedSpriteControl.class);
        if (animControl == null) {
            return; // Not an animated sprite
        }
        
        if (direction.lengthSquared() == 0) {
            animControl.setDirection(AnimatedSpriteControl.Direction.IDLE);
            return;
        }
        
        // Determine primary direction based on movement vector
        if (Math.abs(direction.y) > Math.abs(direction.x)) {
            // Vertical movement dominates
            if (direction.y > 0) {
                animControl.setDirection(AnimatedSpriteControl.Direction.UP);
            } else {
                animControl.setDirection(AnimatedSpriteControl.Direction.DOWN);
            }
        } else {
            // Horizontal movement dominates
            if (direction.x > 0) {
                animControl.setDirection(AnimatedSpriteControl.Direction.RIGHT);
            } else {
                animControl.setDirection(AnimatedSpriteControl.Direction.LEFT);
            }
        }
    }



    public boolean wasCollisionDetected() {
        return collisionDetected;
    }

    public void finalizeCollision(Perimeter newPerimeter) {
        setPerimeter(newPerimeter);
        this.collisionDetected = false;
        this.isDrawing = false;
        this.playerPath.clear();
        
        // Position player at intersection point
        spatial.setLocalTranslation(this.intersectionPoint.add(0, 0, SPATIAL_Z_OFFSET));
    }

    public List<Vector3f> getDrawingPath() {
        return playerPath;
    }

    public List<Vector3f> getVisualDrawingPath() {
        if (!isDrawing || playerPath.isEmpty()) {
            return null;
        }
        // Create a temporary list for visualization
        List<Vector3f> visualPath = new ArrayList<>(playerPath);
        // Add the player's current position to the end of the list
        visualPath.add(new Vector3f(spatial.getLocalTranslation().x, spatial.getLocalTranslation().y, 0));
        return visualPath;
    }

    public boolean isDrawing() {
        return isDrawing;
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}

    public void reset() {
        up = down = left = right = false;
        collisionDetected = false;
        isDrawing = false;
        playerPath.clear();
    }

}

