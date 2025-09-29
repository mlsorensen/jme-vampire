package com.turboio.games.vampires.perimeter;

import com.jme3.math.Vector3f;
import com.turboio.games.vampires.controls.PlayerControl;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.polygonize.Polygonizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Handles territory capture calculations using JTS computational geometry library.
 * This approach is much more robust than manual polygon manipulation.
 */
public class PerimeterManager {
    
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    public Perimeter calculateNewPerimeter(Perimeter oldPerimeter, PlayerControl control, Vector3f enemyPosition) {
        List<Vector3f> drawingPath = control.getDrawingPath();
        if (drawingPath == null || drawingPath.size() < 2) {
            return oldPerimeter; // No change
        }

        System.out.println("--- JTS PerimeterManager ---");
        System.out.println("Old perimeter has " + oldPerimeter.getVertices().size() + " vertices");
        System.out.println("Drawing path has " + drawingPath.size() + " vertices");

        try {
            Polygon oldJtsPerimeter = createJTSPolygon(oldPerimeter.getVertices());
            LineString jtsDrawingPath = createJTSLineString(drawingPath);

            // Union the perimeter boundary with the new drawing path
            Geometry unioned = oldJtsPerimeter.getBoundary().union(jtsDrawingPath);

            // Use Polygonizer to find all new polygons formed by the union
            Polygonizer polygonizer = new Polygonizer();
            polygonizer.add(unioned);
            Collection<Polygon> newPolygons = polygonizer.getPolygons();

            System.out.println("Polygonizer found " + newPolygons.size() + " new polygons.");

            // Find the correct polygon to keep (the one without the enemy)
            Point enemyPoint = geometryFactory.createPoint(new Coordinate(enemyPosition.x, enemyPosition.y));
            Polygon newPerimeterPolygon = null;

            if (newPolygons.size() > 1) {
                // More than one polygon, choose the one containing the enemy
                for (Polygon p : newPolygons) {
                    if (p.contains(enemyPoint)) {
                        newPerimeterPolygon = p;
                        break; // Found our polygon
                    }
                }
            } else if (!newPolygons.isEmpty()) {
                // Only one polygon was formed, this is our new perimeter
                newPerimeterPolygon = newPolygons.iterator().next();
            }

            if (newPerimeterPolygon != null) {
                System.out.println("Selected new perimeter with area: " + newPerimeterPolygon.getArea());
                return new Perimeter(convertJTSToVertices(newPerimeterPolygon));
            } else {
                System.err.println("Could not determine new perimeter, fallback to old one.");
                return oldPerimeter;
            }

        } catch (Exception e) {
            System.err.println("JTS calculation failed: " + e.getMessage());
            e.printStackTrace();
            return oldPerimeter; // Fallback to old perimeter
        }
    }
    
    public boolean isOnBoundary(Vector3f point, Perimeter perimeter) {
        Polygon polygon = createJTSPolygon(perimeter.getVertices());
        Point jtsPoint = geometryFactory.createPoint(new Coordinate(point.x, point.y));
        // Use a small tolerance for boundary checks to handle floating point issues
        return polygon.getBoundary().distance(jtsPoint) < 0.01;
    }

    public Vector3f getIntersection(LineString line, Perimeter perimeter) {
        Polygon polygon = createJTSPolygon(perimeter.getVertices());
        Geometry intersection = polygon.getBoundary().intersection(line);
        if (!intersection.isEmpty()) {
            Coordinate intersectionCoord = intersection.getCoordinate();
            return new Vector3f((float) intersectionCoord.x, (float) intersectionCoord.y, 0);
        }
        return null;
    }

    private Polygon createJTSPolygon(List<Vector3f> vertices) {
        Coordinate[] coords = new Coordinate[vertices.size() + 1]; // +1 to close the ring
        for (int i = 0; i < vertices.size(); i++) {
            Vector3f v = vertices.get(i);
            coords[i] = new Coordinate(v.x, v.y);
        }
        // Close the ring by repeating the first coordinate
        coords[vertices.size()] = new Coordinate(vertices.get(0).x, vertices.get(0).y);
        
        LinearRing ring = geometryFactory.createLinearRing(coords);
        return geometryFactory.createPolygon(ring);
    }
    
    private LineString createJTSLineString(List<Vector3f> path) {
        Coordinate[] coords = new Coordinate[path.size()];
        for (int i = 0; i < path.size(); i++) {
            Vector3f v = path.get(i);
            coords[i] = new Coordinate(v.x, v.y);
        }
        return geometryFactory.createLineString(coords);
    }
    
    private List<Vector3f> convertJTSToVertices(Polygon polygon) {
        Coordinate[] coords = polygon.getExteriorRing().getCoordinates();
        List<Vector3f> vertices = new ArrayList<>();
        
        // Skip the last coordinate as it's a duplicate of the first (ring closure)
        for (int i = 0; i < coords.length - 1; i++) {
            vertices.add(new Vector3f((float)coords[i].x, (float)coords[i].y, 0f));
        }
        
        return vertices;
    }
}