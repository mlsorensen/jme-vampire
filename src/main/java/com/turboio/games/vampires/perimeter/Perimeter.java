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
}
