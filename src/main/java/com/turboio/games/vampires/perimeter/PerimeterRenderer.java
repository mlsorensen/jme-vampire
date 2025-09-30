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
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
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
    private String foregroundTexture = "Textures/field.png";
    private static final float TRAIL_WIDTH = 5f;
    private static final float TRAIL_Z = 3f;
    private Texture2D trailTexture;

    public PerimeterRenderer(AssetManager assetManager, float screenWidth, float screenHeight) {
        this.assetManager = assetManager;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void setForegroundTexture(String path) {
        this.foregroundTexture = path;
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
        geom.setLocalTranslation(0, 0, 2f);
        return geom;
    }

    public Geometry createDayField(List<Vector3f> vertices) {
        Mesh dayMesh = triangulate(vertices);
        Geometry dayGeom = new Geometry("DayField", dayMesh);
        Material dayMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        dayMat.setTexture("ColorMap", assetManager.loadTexture(foregroundTexture));
        dayMat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        dayGeom.setMaterial(dayMat);
        dayGeom.setQueueBucket(RenderQueue.Bucket.Gui);
        dayGeom.setLocalTranslation(0, 0, 1f);
        return dayGeom;
    }

    public Geometry createDrawingPathLine() {
        Mesh lineMesh = new Mesh();
        lineMesh.setMode(Mesh.Mode.Triangles);
        lineMesh.setDynamic();
        lineMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(0));
        lineMesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(0));
        lineMesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(0));

        Material lineMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        lineMat.setTexture("ColorMap", getTrailTexture());
        lineMat.setColor("Color", ColorRGBA.White);
        lineMat.setColor("GlowColor", new ColorRGBA(1f, 0.15f, 0.15f, 1f));
        lineMat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        lineMat.getAdditionalRenderState().setDepthTest(false);
        lineMat.getAdditionalRenderState().setDepthWrite(false);
        lineMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Additive);

        Geometry geom = new Geometry("DrawingPath", lineMesh);
        geom.setMaterial(lineMat);
        geom.setQueueBucket(RenderQueue.Bucket.Gui);
        geom.setLocalTranslation(0, 0, 3f);
        return geom;
    }


    public void updateDrawingPathVisuals(Geometry drawingPathGeom, List<Vector3f> path) {
        Mesh mesh = drawingPathGeom.getMesh();
        if (path == null || path.size() < 2) {
            mesh.clearBuffer(VertexBuffer.Type.Position);
            mesh.clearBuffer(VertexBuffer.Type.TexCoord);
            mesh.clearBuffer(VertexBuffer.Type.Index);
            mesh.updateCounts();
            mesh.updateBound();
            return;
        }

        int size = path.size();
        Vector3f[] vertices = new Vector3f[size * 2];
        Vector2f[] uvs = new Vector2f[size * 2];
        float[] distances = new float[size];
        distances[0] = 0f;
        for (int i = 1; i < size; i++) {
            distances[i] = distances[i - 1] + path.get(i).distance(path.get(i - 1));
        }
        float totalLength = distances[size - 1];

        float halfWidth = TRAIL_WIDTH / 2f;
        for (int i = 0; i < size; i++) {
            Vector3f point = path.get(i);
            Vector3f prevPoint = i == 0 ? point : path.get(i - 1);
            Vector3f nextPoint = i == size - 1 ? point : path.get(i + 1);

            Vector3f dir = nextPoint.subtract(prevPoint);
            if (dir.lengthSquared() < 1e-4f) {
                dir = new Vector3f(0, 1, 0);
            } else {
                dir.normalizeLocal();
            }

            Vector3f normal = new Vector3f(-dir.y, dir.x, 0f);
            if (normal.lengthSquared() < 1e-4f) {
                normal = new Vector3f(0, 1, 0);
            } else {
                normal.normalizeLocal();
            }
            Vector3f offset = normal.mult(halfWidth);

            Vector3f left = new Vector3f(point.x + offset.x, point.y + offset.y, TRAIL_Z);
            Vector3f right = new Vector3f(point.x - offset.x, point.y - offset.y, TRAIL_Z);
            vertices[i * 2] = left;
            vertices[i * 2 + 1] = right;

            float v = totalLength > 0f ? distances[i] / totalLength : 0f;
            uvs[i * 2] = new Vector2f(0f, v);
            uvs[i * 2 + 1] = new Vector2f(1f, v);
        }

        int[] indices = new int[(size - 1) * 6];
        int indexPos = 0;
        for (int i = 0; i < size - 1; i++) {
            int left0 = i * 2;
            int right0 = left0 + 1;
            int left1 = left0 + 2;
            int right1 = left1 + 1;
            indices[indexPos++] = left0;
            indices[indexPos++] = right0;
            indices[indexPos++] = left1;
            indices[indexPos++] = left1;
            indices[indexPos++] = right0;
            indices[indexPos++] = right1;
        }

        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(uvs));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices));
        mesh.updateCounts();
        mesh.updateBound();
    }

    private Texture2D getTrailTexture() {
        if (trailTexture != null) {
            return trailTexture;
        }

        int width = 64;
        int height = 4;
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float u = width > 1 ? (float) x / (float) (width - 1) : 0f;
                float distance = Math.abs(u - 0.5f) * 2f;
                distance = Math.min(1f, distance);
                float falloff = 1f - distance;
                float alpha = (float) Math.pow(falloff, 0.25f);
                float r = 0.8f + 0.2f * falloff;
                float g = 0.1f + 0.3f * falloff;
                float b = 0.1f + 0.2f * falloff;
                buffer.put((byte) (255 * r));
                buffer.put((byte) (255 * g));
                buffer.put((byte) (255 * b));
                buffer.put((byte) (255 * alpha));
            }
        }
        buffer.flip();

        Image image = new Image(Image.Format.RGBA8, width, height, buffer);
        trailTexture = new Texture2D(image);
        trailTexture.setMagFilter(Texture.MagFilter.Bilinear);
        trailTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        return trailTexture;
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
            // In case of failure, return a simple fallback mesh (single triangle)
            return createFallbackMesh();
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

    private Mesh createFallbackMesh() {
        // Create a simple single triangle mesh that won't crash
        Vector3f[] vertices = {
            new Vector3f(0, 0, 0),
            new Vector3f(100, 0, 0),
            new Vector3f(50, 100, 0)
        };
        
        Vector2f[] uvs = {
            new Vector2f(0, 0),
            new Vector2f(1, 0),
            new Vector2f(0.5f, 1)
        };
        
        int[] indices = {0, 1, 2};
        
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(uvs));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices));
        mesh.updateBound();
        return mesh;
    }
}
