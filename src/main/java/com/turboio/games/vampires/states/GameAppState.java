package com.turboio.games.vampires.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.jme3.util.BufferUtils;
import com.turboio.games.vampires.controls.PlayerControl;

import java.util.ArrayList;
import java.util.List;

public class GameAppState extends BaseAppState implements ActionListener {
    private SimpleApplication app;
    private Picture background;
    private Spatial player;
    private Geometry perimeterGeom;
    private Node shadingOverlay;
    private List<Vector3f> perimeter;
    private FilterPostProcessor fpp;

    private final String[] MAPPINGS = new String[]{"left", "right", "up", "down", "return"};

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;

        // Layer 0: Background
        background = new Picture("Background");
        background.setImage(app.getAssetManager(), "Textures/field.png", true);
        background.setWidth(app.getCamera().getWidth());
        background.setHeight(app.getCamera().getHeight());
        background.setLocalTranslation(0, 0, 0);

        // --- Data Setup ---
        perimeter = new ArrayList<>();
        float inset = 64f;
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        perimeter.add(new Vector3f(inset, inset, 0)); // Bottom-left
        perimeter.add(new Vector3f(screenWidth - inset, inset, 0)); // Bottom-right
        perimeter.add(new Vector3f(screenWidth - inset, screenHeight - inset, 0)); // Top-right
        perimeter.add(new Vector3f(inset, screenHeight - inset, 0)); // Top-left

        // Layer 1: Shading Overlay
        shadingOverlay = setupShadingOverlay(perimeter, screenWidth, screenHeight);

        // Layer 2: Perimeter Line
        perimeterGeom = createPerimeterLine(perimeter);

        // Layer 3: Player
        player = getSpatial("player");
        player.setUserData("alive", true);
        player.setLocalTranslation(perimeter.get(0).add(0, 0, 3));
        player.setQueueBucket(RenderQueue.Bucket.Gui);

        // Add control to the player
        player.addControl(new PlayerControl(perimeter));

        // Stencil buffer setup
        fpp = new FilterPostProcessor(app.getAssetManager());
        fpp.setFrameBufferDepthFormat(Image.Format.Depth24Stencil8);
    }

    private Geometry createPerimeterLine(List<Vector3f> vertices) {
        Mesh lineMesh = new Mesh();
        lineMesh.setMode(Mesh.Mode.LineLoop);
        lineMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices.toArray(new Vector3f[0])));
        lineMesh.setBuffer(VertexBuffer.Type.Index, 2, new short[]{0, 1, 2, 3});
        lineMesh.updateBound();

        Material lineMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        lineMat.setColor("Color", ColorRGBA.White);

        Geometry geom = new Geometry("Perimeter", lineMesh);
        geom.setMaterial(lineMat);
        geom.setQueueBucket(RenderQueue.Bucket.Gui);
        geom.setLocalTranslation(0, 0, 2);
        return geom;
    }

    private Node setupShadingOverlay(List<Vector3f> holeVertices, float width, float height) {
        Node overlayNode = new Node("ShadingOverlay");
        AssetManager assetManager = app.getAssetManager();

        // 1. Create the invisible mask (the "hole")
        Mesh maskMesh = new Mesh();
        maskMesh.setMode(Mesh.Mode.Triangles);
        maskMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(holeVertices.toArray(new Vector3f[0])));
        maskMesh.setBuffer(VertexBuffer.Type.Index, 3, new short[]{0, 1, 2, 0, 2, 3});
        maskMesh.updateBound();

        Geometry maskGeom = new Geometry("Mask", maskMesh);
        Material maskMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        RenderState maskState = maskMat.getAdditionalRenderState();
        maskState.setColorWrite(false);
        maskState.setDepthWrite(false);
        maskState.setStencil(true,
                RenderState.StencilOperation.Replace, RenderState.StencilOperation.Replace, RenderState.StencilOperation.Replace,
                RenderState.StencilOperation.Replace, RenderState.StencilOperation.Replace, RenderState.StencilOperation.Replace,
                RenderState.TestFunction.Always, RenderState.TestFunction.Always);
        maskState.setFrontStencilReference(1);
        maskState.setBackStencilReference(1);
        maskState.setFrontStencilMask(0xFF);
        maskState.setBackStencilMask(0xFF);
        maskGeom.setMaterial(maskMat);

        // 2. Create the nighttime overlay
        Quad overlayQuad = new Quad(width, height);
        Geometry overlayGeom = new Geometry("Overlay", overlayQuad);
        Material overlayMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture nightTexture = assetManager.loadTexture("Textures/field_night.png");
        overlayMat.setTexture("ColorMap", nightTexture);
        RenderState overlayState = overlayMat.getAdditionalRenderState();
        overlayState.setStencil(true,
                RenderState.StencilOperation.Keep, RenderState.StencilOperation.Keep, RenderState.StencilOperation.Keep,
                RenderState.StencilOperation.Keep, RenderState.StencilOperation.Keep, RenderState.StencilOperation.Keep,
                RenderState.TestFunction.NotEqual, RenderState.TestFunction.NotEqual);
        overlayState.setFrontStencilReference(1);
        overlayState.setBackStencilReference(1);
        overlayState.setFrontStencilMask(0xFF);
        overlayState.setBackStencilMask(0xFF);
        overlayGeom.setMaterial(overlayMat);

        // 3. Attach both to the node. The mask will be rendered first.
        overlayNode.attachChild(maskGeom);
        overlayNode.attachChild(overlayGeom);

        overlayNode.setQueueBucket(RenderQueue.Bucket.Gui);
        overlayNode.setLocalTranslation(0, 0, 1);

        return overlayNode;
    }

    @Override
    protected void onEnable() {
        app.getViewPort().setClearStencil(true);
        app.getViewPort().addProcessor(fpp);
        app.getGuiNode().attachChild(background);
        app.getGuiNode().attachChild(player);
        app.getGuiNode().attachChild(perimeterGeom);
        app.getGuiNode().attachChild(shadingOverlay);

        InputManager inputManager = app.getInputManager();
        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("return", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, MAPPINGS);
    }

    @Override
    protected void onDisable() {
        app.getViewPort().setClearStencil(false);
        app.getViewPort().removeProcessor(fpp);
        app.getGuiNode().detachChild(background);
        app.getGuiNode().detachChild(player);
        app.getGuiNode().detachChild(perimeterGeom);
        app.getGuiNode().detachChild(shadingOverlay);

        InputManager inputManager = app.getInputManager();
        for (String mapping : MAPPINGS) {
            if (inputManager.hasMapping(mapping)) {
                inputManager.deleteMapping(mapping);
            }
        }
        inputManager.removeListener(this);
    }

    @Override
    public void update(float tpf) {}

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if ((Boolean) player.getUserData("alive")) {
            PlayerControl control = player.getControl(PlayerControl.class);
            if (control != null) {
                switch (name) {
                    case "up": control.up = isPressed; break;
                    case "down": control.down = isPressed; break;
                    case "left": control.left = isPressed; break;
                    case "right": control.right = isPressed; break;
                }
            }
        }
    }

    private Spatial getSpatial(String name) {
        Node node = new Node(name);
        Picture pic = new Picture(name);
        Texture2D tex = (Texture2D) app.getAssetManager().loadTexture("Textures/" + name + ".png");
        pic.setTexture(app.getAssetManager(), tex, true);

        float width = tex.getImage().getWidth();
        float height = tex.getImage().getHeight();
        pic.setWidth(width);
        pic.setHeight(height);
        pic.move(-width / 2f, -height / 2f, 0);

        Material picMat = new Material(app.getAssetManager(), "Common/MatDefs/Gui/Gui.j3md");
        picMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        node.setMaterial(picMat);

        node.setUserData("radius", width / 2);
        node.attachChild(pic);
        return node;
    }

    @Override
    protected void cleanup(Application app) {}
}
