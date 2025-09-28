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

import java.util.ArrayList;
import java.util.List;

public class PerimeterRenderer {

    private final AssetManager assetManager;

    public PerimeterRenderer(AssetManager assetManager) {
        this.assetManager = assetManager;
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

    public Node setupShadingOverlay(List<Vector3f> holeVertices, float width, float height) {
        Node overlayNode = new Node("ShadingOverlay");

        Mesh maskMesh = new Mesh();
        maskMesh.setMode(Mesh.Mode.Triangles);
        maskMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(holeVertices.toArray(new Vector3f[0])));

        short[] indices = triangulate(holeVertices);
        maskMesh.setBuffer(VertexBuffer.Type.Index, 3, indices);
        maskMesh.updateBound();

        Geometry maskGeom = new Geometry("Mask", maskMesh);
        Material maskMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        RenderState maskState = maskMat.getAdditionalRenderState();
        maskState.setColorWrite(false);
        maskState.setDepthWrite(false);
        maskState.setStencil(true, RenderState.StencilOperation.Replace, RenderState.StencilOperation.Replace, RenderState.StencilOperation.Replace, RenderState.StencilOperation.Replace, RenderState.StencilOperation.Replace, RenderState.StencilOperation.Replace, RenderState.TestFunction.Always, RenderState.TestFunction.Always);
        maskState.setFrontStencilReference(1);
        maskState.setBackStencilReference(1);
        maskState.setFrontStencilMask(0xFF);
        maskState.setBackStencilMask(0xFF);
        maskGeom.setMaterial(maskMat);

        Quad overlayQuad = new Quad(width, height);
        Geometry overlayGeom = new Geometry("Overlay", overlayQuad);
        Material overlayMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        overlayMat.setTexture("ColorMap", assetManager.loadTexture("Textures/field_night.png"));
        RenderState overlayState = overlayMat.getAdditionalRenderState();
        overlayState.setStencil(true, RenderState.StencilOperation.Keep, RenderState.StencilOperation.Keep, RenderState.StencilOperation.Keep, RenderState.StencilOperation.Keep, RenderState.StencilOperation.Keep, RenderState.StencilOperation.Keep, RenderState.TestFunction.NotEqual, RenderState.TestFunction.NotEqual);
        overlayState.setFrontStencilReference(1);
        overlayState.setBackStencilReference(1);
        overlayState.setFrontStencilMask(0xFF);
        overlayState.setBackStencilMask(0xFF);
        overlayGeom.setMaterial(overlayMat);

        overlayNode.attachChild(maskGeom);
        overlayNode.attachChild(overlayGeom);
        overlayNode.setQueueBucket(RenderQueue.Bucket.Gui);
        overlayNode.setLocalTranslation(0, 0, 1);

        return overlayNode;
    }

    private short[] triangulate(List<Vector3f> polygon) {
        List<Short> indices = new ArrayList<>();
        List<Vector2f> vertices = new ArrayList<>();
        for (Vector3f v : polygon) {
            vertices.add(new Vector2f(v.x, v.y));
        }

        int n = vertices.size();
        if (n < 3) return new short[0];

        List<Integer> V = new ArrayList<>();
        for (int j = 0; j < n; j++) V.add(j);

        int nv = n;
        int count = 2 * nv;
        for (int m = 0, v = nv - 1; nv > 2; ) {
            if ((count--) <= 0) return new short[0]; // Failsafe against infinite loop

            int u = v;
            if (nv <= u) u = 0;
            v = u + 1;
            if (nv <= v) v = 0;
            int w = v + 1;
            if (nv <= w) w = 0;

            if (isEar(u, v, w, vertices, V)) {
                indices.add(V.get(u).shortValue());
                indices.add(V.get(v).shortValue());
                indices.add(V.get(w).shortValue());
                m++;
                V.remove(v);
                nv--;
                count = 2 * nv;
            }
        }

        short[] result = new short[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            result[i] = indices.get(i);
        }
        return result;
    }

    private boolean isEar(int u, int v, int w, List<Vector2f> vertices, List<Integer> V) {
        Vector2f p1 = vertices.get(V.get(u));
        Vector2f p2 = vertices.get(V.get(v));
        Vector2f p3 = vertices.get(V.get(w));

        // Check if the triangle is wound counter-clockwise (i.e., the corner is convex)
        if ((p2.x - p1.x) * (p3.y - p2.y) - (p2.y - p1.y) * (p3.x - p2.x) < 0) {
            return false;
        }

        // Check if any other vertex is inside the triangle
        for (int i = 0; i < V.size(); i++) {
            int p = V.get(i);
            if (p == V.get(u) || p == V.get(v) || p == V.get(w)) continue;
            Vector2f pt = vertices.get(p);
            if (isInsideTriangle(p1, p2, p3, pt)) {
                return false;
            }
        }
        return true;
    }

    private boolean isInsideTriangle(Vector2f p1, Vector2f p2, Vector2f p3, Vector2f pt) {
        float d1 = sign(pt, p1, p2);
        float d2 = sign(pt, p2, p3);
        float d3 = sign(pt, p3, p1);

        boolean has_neg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean has_pos = (d1 > 0) || (d2 > 0) || (d3 > 0);

        return !(has_neg && has_pos);
    }

    private float sign(Vector2f p1, Vector2f p2, Vector2f p3) {
        return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y);
    }
}
