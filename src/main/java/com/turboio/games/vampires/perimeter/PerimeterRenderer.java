package com.turboio.games.vampires.perimeter;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.util.BufferUtils;
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

        short[] indices = new short[(holeVertices.size() - 2) * 3];
        for (int i = 0; i < holeVertices.size() - 2; i++) {
            indices[i * 3] = 0;
            indices[i * 3 + 1] = (short) (i + 1);
            indices[i * 3 + 2] = (short) (i + 2);
        }
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
}
