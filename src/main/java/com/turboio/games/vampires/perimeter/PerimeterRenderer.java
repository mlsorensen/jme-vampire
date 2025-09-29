package com.turboio.games.vampires.perimeter;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.util.BufferUtils;
import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PerimeterRenderer {

    private final AssetManager assetManager;
    private final float screenWidth;
    private final float screenHeight;

    public PerimeterRenderer(AssetManager assetManager, float screenWidth, float screenHeight) {
        this.assetManager = assetManager;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public Geometry createPerimeterLine(List<Vector3f> vertices) {
        Mesh lineMesh = new Mesh();
        lineMesh.setMode(Mesh.Mode.LineLoop);
        lineMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices.toArray(new Vector3f[0])));

        short[] indices = new short[vertices.size()];
        for (short i = 0; i < vertices.size(); i++) {
            indices[i] = i;
        }
        lineMesh.setBuffer(VertexBuffer.Type.Index, 1, indices);
        lineMesh.updateBound();

        Material lineMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        lineMat.setColor("Color", ColorRGBA.White);

        Geometry geom = new Geometry("Perimeter", lineMesh);
        geom.setMaterial(lineMat);
        geom.setQueueBucket(RenderQueue.Bucket.Gui);
        geom.setLocalTranslation(0, 0, 2);
        return geom;
    }

    public Geometry createDayField(List<Vector3f> vertices) {
        Mesh dayMesh = triangulate(vertices);
        Geometry dayGeom = new Geometry("DayField", dayMesh);
        Material dayMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        dayMat.setTexture("ColorMap", assetManager.loadTexture("Textures/field.png"));
        dayMat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        dayGeom.setMaterial(dayMat);
        dayGeom.setQueueBucket(RenderQueue.Bucket.Gui);
        dayGeom.setLocalTranslation(0, 0, 1);
        return dayGeom;
    }

    public Geometry createDrawingPathLine() {
        Mesh lineMesh = new Mesh();
        lineMesh.setMode(Mesh.Mode.LineStrip);
        Material lineMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        lineMat.setColor("Color", ColorRGBA.Yellow);
        Geometry geom = new Geometry("DrawingPath", lineMesh);
        geom.setMaterial(lineMat);
        geom.setQueueBucket(RenderQueue.Bucket.Gui);
        geom.setLocalTranslation(0, 0, 2);
        return geom;
    }

    public void updateDrawingPathVisuals(Geometry drawingPathGeom, List<Vector3f> path) {
        Mesh mesh = drawingPathGeom.getMesh();
        if (path != null && path.size() > 1) {
            mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(path.toArray(new Vector3f[0])));
            short[] indices = new short[path.size()];
            for (short i = 0; i < path.size(); i++) {
                indices[i] = i;
            }
            mesh.setBuffer(VertexBuffer.Type.Index, 1, indices);
            mesh.updateBound();
        } else {
            mesh.clearBuffer(VertexBuffer.Type.Index);
            mesh.clearBuffer(VertexBuffer.Type.Position);
        }
    }

    private Mesh triangulate(List<Vector3f> polygonVerts) {
        if (polygonVerts == null || polygonVerts.size() < 3) {
            return new Mesh();
        }

        List<PolygonPoint> points = new ArrayList<>();
        for (Vector3f v : polygonVerts) {
            points.add(new PolygonPoint(v.x, v.y));
        }

        Polygon polygon = new Polygon(points);
        try {
            Poly2Tri.triangulate(polygon);
        } catch (Exception e) {
            System.err.println("Triangulation failed: " + e.getMessage());
            // In case of failure, return an empty mesh. The old method is too complex to maintain.
            return new Mesh();
        }

        List<DelaunayTriangle> triangles = polygon.getTriangles();
        List<Vector3f> uniqueVerts = new ArrayList<>();
        List<Vector2f> uniqueUVs = new ArrayList<>();
        List<Integer> newIndices = new ArrayList<>();
        Map<TriangulationPoint, Integer> vertexMap = new LinkedHashMap<>();
        int nextIndex = 0;

        for (DelaunayTriangle t : triangles) {
            for (TriangulationPoint p : t.points) {
                if (!vertexMap.containsKey(p)) {
                    vertexMap.put(p, nextIndex++);
                    uniqueVerts.add(new Vector3f((float) p.getX(), (float) p.getY(), 0f));
                    
                    // Generate UV coordinates by mapping screen coordinates to 0-1 range
                    float u = (float) p.getX() / screenWidth;
                    float v = (float) p.getY() / screenHeight;
                    uniqueUVs.add(new Vector2f(u, v));
                }
                newIndices.add(vertexMap.get(p));
            }
        }

        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(uniqueVerts.toArray(new Vector3f[0])));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(uniqueUVs.toArray(new Vector2f[0])));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(newIndices.stream().mapToInt(i -> i).toArray()));
        mesh.updateBound();
        return mesh;
    }
}
