package com.turboio.games.vampires.perimeter;

import com.jme3.math.Vector3f;
import com.turboio.games.vampires.controls.PlayerControl;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles the complex geometric calculations for capturing territory.
 * This class is stateless and its methods are pure functions.
 */
public class PerimeterManager {

    public Perimeter calculateNewPerimeter(Perimeter oldPerimeter, PlayerControl control, Vector3f enemyPosition) {
        List<Vector3f> drawingPath = control.getDrawingPath();
        if (drawingPath == null || drawingPath.size() < 2) {
            return oldPerimeter; // No change
        }

        int stepOffSegmentIndex = control.getStepOffSegmentIndex();
        int collidedSegmentIndex = control.collidedSegmentIndex;
        List<Vector3f> oldVertices = oldPerimeter.getVertices();

        // The old perimeter is split into two paths by the drawing line.
        // We will combine the drawing path with each of these two paths to form two new loops.

        // Path 1: The vertices from the intersection point back to the step-off point.
        List<Vector3f> path1 = new ArrayList<>();
        int currentIndex = (collidedSegmentIndex + 1) % oldVertices.size();
        while (currentIndex != (stepOffSegmentIndex + 1) % oldVertices.size()) {
            path1.add(oldVertices.get(currentIndex));
            currentIndex = (currentIndex + 1) % oldVertices.size();
        }

        // Path 2: The vertices from the step-off point back to the intersection point.
        List<Vector3f> path2 = new ArrayList<>();
        currentIndex = (stepOffSegmentIndex + 1) % oldVertices.size();
        while (currentIndex != (collidedSegmentIndex + 1) % oldVertices.size()) {
            path2.add(oldVertices.get(currentIndex));
            currentIndex = (currentIndex + 1) % oldVertices.size();
        }

        // Create the two new potential perimeters (loops).
        // Loop A = drawingPath + path1
        List<Vector3f> loopA = new ArrayList<>(drawingPath);
        loopA.addAll(path1);

        // Loop B = drawingPath (reversed) + path2
        List<Vector3f> reversedDrawingPath = new ArrayList<>(drawingPath);
        Collections.reverse(reversedDrawingPath);
        List<Vector3f> loopB = new ArrayList<>(reversedDrawingPath);
        loopB.addAll(path2);

        // Determine which loop contains the enemy.
        Polygon polyA = new Polygon();
        for (Vector3f v : loopA) polyA.addPoint((int) v.x, (int) v.y);

        Polygon polyB = new Polygon();
        for (Vector3f v : loopB) polyB.addPoint((int) v.x, (int) v.y);

        List<Vector3f> newPerimeterVertices;
        // The new perimeter is the one that contains the enemy.
        if (polyA.contains(enemyPosition.x, enemyPosition.y)) {
            newPerimeterVertices = loopA;
        } else if (polyB.contains(enemyPosition.x, enemyPosition.y)) {
            newPerimeterVertices = loopB;
        } else {
            // Failsafe: if the enemy isn't in either, something is wrong (e.g., enemy is on the line).
            // Keep the smaller of the two loops as a sensible default.
            newPerimeterVertices = loopA.size() < loopB.size() ? loopA : loopB;
        }

        return new Perimeter(newPerimeterVertices);
    }
}
