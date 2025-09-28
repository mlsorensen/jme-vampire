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
        List<Vector3f> drawingPathSource = control.getDrawingPath();
        if (drawingPathSource == null || drawingPathSource.size() < 2) {
            return oldPerimeter; // No change
        }

        List<Vector3f> drawingPath = new ArrayList<>();
        for (Vector3f v : drawingPathSource) {
            drawingPath.add(new Vector3f(v.x, v.y, 0));
        }

        Vector3f stepOffPoint = new Vector3f(control.getStepOffPoint().x, control.getStepOffPoint().y, 0);
        Vector3f intersectionPoint = new Vector3f(control.intersectionPoint.x, control.intersectionPoint.y, 0);

        // 1. Create a clean, unified list of the old perimeter's vertices, including the new connection points.
        List<Vector3f> unifiedPerimeter = createUnifiedPerimeter(oldPerimeter.getVertices(), control, stepOffPoint, intersectionPoint);

        // 2. Find the indices of the connection points within the new unified list.
        int startIndex = unifiedPerimeter.indexOf(stepOffPoint);
        int endIndex = unifiedPerimeter.indexOf(intersectionPoint);

        // 3. Build the two potential new loops by walking the unified perimeter.
        List<Vector3f> loopA = new ArrayList<>(drawingPath);
        int currentIndex = endIndex;
        while (currentIndex != startIndex) {
            loopA.add(unifiedPerimeter.get(currentIndex));
            currentIndex = (currentIndex + 1) % unifiedPerimeter.size();
        }

        List<Vector3f> loopB = new ArrayList<>();
        List<Vector3f> reversedDrawingPath = new ArrayList<>(drawingPath);
        Collections.reverse(reversedDrawingPath);
        loopB.addAll(reversedDrawingPath);
        currentIndex = startIndex;
        while (currentIndex != endIndex) {
            loopB.add(unifiedPerimeter.get(currentIndex));
            currentIndex = (currentIndex + 1) % unifiedPerimeter.size();
        }

        // 4. Determine which loop contains the enemy.
        Polygon polyA = new Polygon();
        for (Vector3f v : loopA) polyA.addPoint((int) v.x, (int) v.y);

        Polygon polyB = new Polygon();
        for (Vector3f v : loopB) polyB.addPoint((int) v.x, (int) v.y);

        List<Vector3f> newPerimeterVertices;
        if (polyA.contains(enemyPosition.x, enemyPosition.y)) {
            newPerimeterVertices = loopA; // Keep the loop WITH the enemy
        } else {
            newPerimeterVertices = loopB;
        }

        System.out.println("New Perimeter Vertices: " + newPerimeterVertices);

        return new Perimeter(newPerimeterVertices);
    }

    private List<Vector3f> createUnifiedPerimeter(List<Vector3f> oldPerimeter, PlayerControl control, Vector3f stepOffPoint, Vector3f intersectionPoint) {
        List<Vector3f> unified = new ArrayList<>(oldPerimeter);
        
        int stepOffSegmentIndex = control.getStepOffSegmentIndex();
        int collidedSegmentIndex = control.collidedSegmentIndex;

        // Insert points, making sure to handle the case where they are on the same segment
        if (stepOffSegmentIndex == collidedSegmentIndex) {
            Vector3f startOfSegment = unified.get(stepOffSegmentIndex);
            if (stepOffPoint.distanceSquared(startOfSegment) > intersectionPoint.distanceSquared(startOfSegment)) {
                unified.add(stepOffSegmentIndex + 1, stepOffPoint);
                unified.add(stepOffSegmentIndex + 2, intersectionPoint);
            } else {
                unified.add(stepOffSegmentIndex + 1, intersectionPoint);
                unified.add(stepOffSegmentIndex + 2, stepOffPoint);
            }
        } else {
            // Insert farther point first to not mess up index of the closer one
            if (stepOffSegmentIndex > collidedSegmentIndex) {
                unified.add(stepOffSegmentIndex + 1, stepOffPoint);
                unified.add(collidedSegmentIndex + 1, intersectionPoint);
            } else {
                unified.add(collidedSegmentIndex + 1, intersectionPoint);
                unified.add(stepOffSegmentIndex + 1, stepOffPoint);
            }
        }
        return unified;
    }
}
