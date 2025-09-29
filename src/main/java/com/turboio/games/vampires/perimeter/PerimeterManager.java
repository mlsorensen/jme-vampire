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
            // Convert current perimeter to JTS Polygon
            Polygon currentArea = createJTSPolygon(oldPerimeter.getVertices());
            
            // Convert drawing path to JTS LineString
            LineString playerLine = createJTSLineString(drawingPath);
            
            // Split the polygon using the line
            Collection<Polygon> splitPolygons = splitPolygon(currentArea, playerLine);
            
            if (splitPolygons.size() != 2) {
                System.out.println("Split resulted in " + splitPolygons.size() + " polygons, expected 2");
                return oldPerimeter; // Fallback
            }
            
            // Convert enemy position to JTS Point
            Point enemyPoint = geometryFactory.createPoint(new Coordinate(enemyPosition.x, enemyPosition.y));
            
            // Find which polygon contains the enemy
            Polygon keepPolygon = null;
            for (Polygon poly : splitPolygons) {
                if (poly.contains(enemyPoint)) {
                    keepPolygon = poly;
                    break;
                }
            }
            
            if (keepPolygon == null) {
                System.out.println("No polygon contains enemy, keeping larger one");
                // Fallback: keep the larger polygon
                keepPolygon = splitPolygons.stream()
                    .max((p1, p2) -> Double.compare(p1.getArea(), p2.getArea()))
                    .orElse(currentArea);
            }
            
            // Convert back to our Perimeter format
            List<Vector3f> newVertices = convertJTSToVertices(keepPolygon);
            System.out.println("New perimeter has " + newVertices.size() + " vertices");
            System.out.println("--- JTS End ---");
            
            return new Perimeter(newVertices);
            
        } catch (Exception e) {
            System.err.println("JTS calculation failed: " + e.getMessage());
            e.printStackTrace();
            return oldPerimeter; // Fallback to old perimeter
        }
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
    
    private Collection<Polygon> splitPolygon(Polygon polygon, LineString line) {
        try {
            // Create a very thin buffer around the line to make it a polygon
            Geometry lineBuffer = line.buffer(0.1); // Very small buffer
            
            // Subtract the line buffer from the original polygon
            Geometry result = polygon.difference(lineBuffer);
            
            List<Polygon> polygons = new ArrayList<>();
            
            if (result instanceof Polygon) {
                polygons.add((Polygon) result);
            } else if (result instanceof MultiPolygon) {
                MultiPolygon multiPoly = (MultiPolygon) result;
                for (int i = 0; i < multiPoly.getNumGeometries(); i++) {
                    Geometry geom = multiPoly.getGeometryN(i);
                    if (geom instanceof Polygon) {
                        polygons.add((Polygon) geom);
                    }
                }
            }
            
            System.out.println("Difference operation produced " + polygons.size() + " polygons");
            return polygons;
            
        } catch (Exception e) {
            System.err.println("Split operation failed: " + e.getMessage());
            return new ArrayList<>();
        }
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