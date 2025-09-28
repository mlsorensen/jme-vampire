package com.turboio.games.vampires.perimeter;

import com.jme3.math.Vector3f;

import java.awt.Polygon;
import java.util.List;

/**
 * Represents the perimeter of the safe area. This is a pure data class that holds the vertices
 * and provides a method for point-in-polygon tests.
 */
public class Perimeter {

    private final List<Vector3f> vertices;
    private final Polygon polygon;

    public Perimeter(List<Vector3f> vertices) {
        this.vertices = vertices;
        this.polygon = new Polygon();
        for (Vector3f v : vertices) {
            this.polygon.addPoint((int) v.x, (int) v.y);
        }
    }

    public boolean contains(Vector3f point) {
        return polygon.contains(point.x, point.y);
    }

    public List<Vector3f> getVertices() {
        return vertices;
    }

    public double getArea() {
        double area = 0.0;
        int n = vertices.size();
        for (int i = 0; i < n; i++) {
            Vector3f p1 = vertices.get(i);
            Vector3f p2 = vertices.get((i + 1) % n);
            area += (p1.x * p2.y) - (p2.x * p1.y);
        }
        return Math.abs(area / 2.0);
    }
}
