package com.turboio.games.vampires.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.turboio.games.vampires.controls.PlayerControl;

import java.util.ArrayList;
import java.util.List;

public class GameAppState extends BaseAppState implements ActionListener {
    private Picture background;
    private SimpleApplication app;

    private Spatial player;
    private final String[] MAPPINGS = new String[]{"left", "right", "up", "down", "return"};

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        background = new Picture("Background");
        background.setImage(app.getAssetManager(), "Textures/field.png", true);
        background.setWidth(app.getCamera().getWidth());
        background.setHeight(app.getCamera().getHeight());

        player = getSpatial("player");
        player.setUserData("alive", true);

        // Define the initial perimeter
        List<Vector3f> perimeter = new ArrayList<>();
        float inset = 64f;
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        perimeter.add(new Vector3f(inset, inset, 0)); // Bottom-left
        perimeter.add(new Vector3f(screenWidth - inset, inset, 0)); // Bottom-right
        perimeter.add(new Vector3f(screenWidth - inset, screenHeight - inset, 0)); // Top-right
        perimeter.add(new Vector3f(inset, screenHeight - inset, 0)); // Top-left

        // Set player's initial position to the start of the perimeter
        player.setLocalTranslation(perimeter.get(0));

        // add control to the player, passing the perimeter data
        player.addControl(new PlayerControl(perimeter));
    }

    @Override
    protected void cleanup(Application app) {
        // This is called when the state is permanently removed.
        // You can do final cleanup here if necessary.
    }

    @Override
    protected void onEnable() {
        // Attach the GUI elements
        app.getGuiNode().attachChild(background);
        app.getGuiNode().attachChild(player);

        // Add input mappings and listeners when the state becomes active
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
        // Detach the GUI elements
        app.getGuiNode().detachChild(background);
        app.getGuiNode().detachChild(player);

        // Remove input mappings and listeners when the state becomes inactive
        InputManager inputManager = app.getInputManager();
        for (String mapping : MAPPINGS) {
            if (inputManager.hasMapping(mapping)) {
                inputManager.deleteMapping(mapping);
            }
        }
        inputManager.removeListener(this);
    }

    @Override
    public void update(float tpf) {
        // Game logic goes here
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if ((Boolean) player.getUserData("alive")) {
            PlayerControl control = player.getControl(PlayerControl.class);
            if (control != null) {
                switch (name) {
                    case "up":
                        control.up = isPressed;
                        break;
                    case "down":
                        control.down = isPressed;
                        break;
                    case "left":
                        control.left = isPressed;
                        break;
                    case "right":
                        control.right = isPressed;
                        break;
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
}
