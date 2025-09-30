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

    public enum DrawingState {
        NOT_DRAWING,
        STARTING_DRAW,
        DRAWING,
        ENDING_DRAW
    }

    private static final float SPATIAL_Z_OFFSET = 3f;
    private static final float SUB_STEP_DISTANCE = 5f; // For anti-tunneling
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING_SINGLE));

    // Movement
    public boolean up, down, left, right;
    private final float speed = 280f;
    private Vector3f lastDirection;

    // Game Data
    private Perimeter perimeter;
    private PerimeterManager perimeterManager;
    private List<Vector3f> playerPath;
    private boolean collisionDetected = false;
    private DrawingState drawingState = DrawingState.NOT_DRAWING;
    public Vector3f intersectionPoint;

    public PlayerControl(Perimeter perimeter, PerimeterManager perimeterManager) {
        setPerimeter(perimeter);
        this.perimeterManager = perimeterManager;
        this.playerPath = new ArrayList<>();
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
        updateAnimatedSprite(desiredDirection);

        if (desiredDirection.lengthSquared() == 0) {
            return; // No input
        }

        boolean isCurrentlyDrawing = (drawingState == DrawingState.DRAWING || drawingState == DrawingState.STARTING_DRAW);

        if (isCurrentlyDrawing && lastDirection.lengthSquared() > 0 && !desiredDirection.equals(lastDirection)) {
            playerPath.add(new Vector3f(spatial.getLocalTranslation().x, spatial.getLocalTranslation().y, 0));
        }
        lastDirection.set(desiredDirection);

        if (isCurrentlyDrawing) {
            // When drawing, move freely and check for collision
            Vector3f currentPos = spatial.getLocalTranslation();
            Vector3f nextPos = currentPos.add(desiredDirection.mult(speed * tpf));
            if (!perimeter.contains(nextPos)) {
                checkForPerimeterCollision(currentPos, nextPos);
            } else {
                spatial.setLocalTranslation(nextPos);
            }
        } else {
            // When not drawing, use sub-steps to prevent tunneling over gaps
            Vector3f totalMovement = desiredDirection.mult(speed * tpf);
            float totalDistance = totalMovement.length();

            if (totalDistance <= 0) {
                return;
            }

            Vector3f moveDirection = totalMovement.normalize();

            // Start from the player's current 2D position
            Vector3f currentSubStepPos = spatial.getLocalTranslation().clone();
            currentSubStepPos.z = 0;

            float distanceMoved = 0;
            while (distanceMoved < totalDistance) {
                float step = Math.min(totalDistance - distanceMoved, SUB_STEP_DISTANCE);
                Vector3f nextSubStepAttempt = currentSubStepPos.add(moveDirection.mult(step));

                // Snap the sub-step position to the perimeter
                currentSubStepPos = perimeterManager.getClosestPointOnPerimeter(nextSubStepAttempt, perimeter);

                distanceMoved += step;
            }

            // Set the final position after all sub-steps
            spatial.setLocalTranslation(currentSubStepPos.add(0, 0, SPATIAL_Z_OFFSET));
        }
    }

    public void toggleDrawing() {
        // Only allow starting a draw if we are not already in the middle of one.
        if (drawingState == DrawingState.NOT_DRAWING || drawingState == DrawingState.ENDING_DRAW) {
            drawingState = DrawingState.STARTING_DRAW;
            playerPath.clear();
            playerPath.add(new Vector3f(spatial.getLocalTranslation().x, spatial.getLocalTranslation().y, 0));
        }
    }

    private void checkForPerimeterCollision(Vector3f currentPos, Vector3f nextPos) {
        LineString movementLine = geometryFactory.createLineString(new Coordinate[]{
            new Coordinate(currentPos.x, currentPos.y),
            new Coordinate(nextPos.x, nextPos.y)
        });

        Vector3f intersection = perimeterManager.getIntersection(movementLine, perimeter);

        if (intersection != null) {
            if (playerPath.size() >= 1) {
                this.intersectionPoint = intersection;
                this.collisionDetected = true;
                spatial.setLocalTranslation(intersection.add(0, 0, SPATIAL_Z_OFFSET));
                playerPath.add(new Vector3f(intersection.x, intersection.y, 0));
            } else {
                if (!playerPath.isEmpty()) {
                    Vector3f startPos = playerPath.get(0);
                    spatial.setLocalTranslation(startPos.add(0, 0, SPATIAL_Z_OFFSET));
                    playerPath.clear();
                    drawingState = DrawingState.NOT_DRAWING;
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
        this.drawingState = DrawingState.ENDING_DRAW;
        this.playerPath.clear();
        spatial.setLocalTranslation(this.intersectionPoint.add(0, 0, SPATIAL_Z_OFFSET));
    }

    public List<Vector3f> getDrawingPath() {
        return playerPath;
    }

    public List<Vector3f> getVisualDrawingPath() {
        if (!isDrawing() || playerPath.isEmpty()) {
            return null;
        }
        List<Vector3f> visualPath = new ArrayList<>(playerPath);
        visualPath.add(new Vector3f(spatial.getLocalTranslation().x, spatial.getLocalTranslation().y, 0));
        return visualPath;
    }

    public DrawingState getDrawingState() {
        return drawingState;
    }

    public void advanceDrawingState() {
        if (drawingState == DrawingState.STARTING_DRAW) {
            drawingState = DrawingState.DRAWING;
        } else if (drawingState == DrawingState.ENDING_DRAW) {
            drawingState = DrawingState.NOT_DRAWING;
        }
    }

    public boolean isDrawing() {
        return drawingState == DrawingState.DRAWING || drawingState == DrawingState.STARTING_DRAW;
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}

    public void reset() {
        up = down = left = right = false;
        collisionDetected = false;
        drawingState = DrawingState.NOT_DRAWING;
        playerPath.clear();
    }
}
