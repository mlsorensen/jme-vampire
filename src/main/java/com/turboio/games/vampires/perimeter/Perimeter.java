package com.turboio.games.vampires.perimeter;

import com.jme3.math.Vector3f;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Represents the perimeter of the safe area. This is a pure data class that holds the vertices
 * and provides a method for point-in-polygon tests.
 */
public class Perimeter {

    private static final float EPSILON = 0.0005f;

    private final List<Vector3f> vertices;
    private final Path2D.Float path;

    public Perimeter(List<Vector3f> vertices) {
        requireNonNull(vertices, "vertices");
        List<Vector3f> sanitized = sanitizeVertices(vertices);
        if (sanitized.size() < 3) {
            throw new IllegalArgumentException("Perimeter requires at least 3 distinct vertices");
        }
        this.vertices = Collections.unmodifiableList(sanitized);
        this.path = buildPath(this.vertices);
    }

    public boolean contains(Vector3f point) {
        if (point == null) {
            return false;
        }
        if (path.contains(point.x, point.y)) {
            return true;
        }
        return isOnBoundary(point);
    }

    public List<Vector3f> getVertices() {
        return vertices;
    }

    public double getArea() {
        return Math.abs(computeSignedArea(vertices));
    }

    private List<Vector3f> sanitizeVertices(List<Vector3f> input) {
        List<Vector3f> result = new ArrayList<>();
        Vector3f previous = null;
        for (Vector3f vertex : input) {
            if (vertex == null) {
                continue;
            }
            Vector3f copy = new Vector3f(vertex.x, vertex.y, 0f);
            if (previous != null && areClose(previous, copy)) {
                continue;
            }
            result.add(copy);
            previous = copy;
        }
        if (!result.isEmpty() && areClose(result.get(0), result.get(result.size() - 1))) {
            result.remove(result.size() - 1);
        }
        if (computeSignedArea(result) < 0) {
            Collections.reverse(result);
        }
        return result;
    }

    private Path2D.Float buildPath(List<Vector3f> verts) {
        Path2D.Float path2D = new Path2D.Float();
        boolean first = true;
        for (Vector3f vertex : verts) {
            if (first) {
                path2D.moveTo(vertex.x, vertex.y);
                first = false;
            } else {
                path2D.lineTo(vertex.x, vertex.y);
            }
        }
        path2D.closePath();
        return path2D;
    }

    private boolean isOnBoundary(Vector3f point) {
        int n = vertices.size();
        for (int i = 0; i < n; i++) {
            Vector3f a = vertices.get(i);
            Vector3f b = vertices.get((i + 1) % n);
            if (isPointOnSegment(point, a, b)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPointOnSegment(Vector3f p, Vector3f a, Vector3f b) {
        float cross = (p.y - a.y) * (b.x - a.x) - (p.x - a.x) * (b.y - a.y);
        if (Math.abs(cross) > EPSILON) {
            return false;
        }
        float dot = (p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y);
        if (dot < -EPSILON) {
            return false;
        }
        float squaredLength = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y);
        return dot <= squaredLength + EPSILON;
    }

    private double computeSignedArea(List<Vector3f> verts) {
        double area = 0.0;
        int n = verts.size();
        for (int i = 0; i < n; i++) {
            Vector3f p1 = verts.get(i);
            Vector3f p2 = verts.get((i + 1) % n);
            area += (double) p1.x * p2.y - (double) p2.x * p1.y;
        }
        return area / 2.0;
    }

    private boolean areClose(Vector3f a, Vector3f b) {
        return a.distanceSquared(b) <= EPSILON * EPSILON;
    }
}
