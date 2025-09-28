package com.turboio.games.vampires.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.InputListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.turboio.games.vampires.controls.PlayerControl;

public class GameAppState extends BaseAppState implements InputListener, ActionListener {
    private Picture background;
    private SimpleApplication app;

    Spatial player;

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        background = new Picture("Background");
        background.setImage(app.getAssetManager(), "Textures/field.png", true);
        background.setWidth(app.getCamera().getWidth());
        background.setHeight(app.getCamera().getHeight());

        player = getSpatial("player");
        player.setUserData("alive", true);
        player.move((float) app.getCamera().getWidth() /2, (float) app.getCamera().getHeight() /2, 0);


        // add key listeners
        app.getInputManager().addMapping("left", new KeyTrigger(KeyInput.KEY_A));
        app.getInputManager().addMapping("right", new KeyTrigger(KeyInput.KEY_D));
        app.getInputManager().addMapping("up", new KeyTrigger(KeyInput.KEY_W));
        app.getInputManager().addMapping("down", new KeyTrigger(KeyInput.KEY_S));
        app.getInputManager().addMapping("return", new KeyTrigger(KeyInput.KEY_RETURN));
        app.getInputManager().addListener(this, "left");
        app.getInputManager().addListener(this, "right");
        app.getInputManager().addListener(this, "up");
        app.getInputManager().addListener(this, "down");
        app.getInputManager().addListener(this, "return");

        // add control to the player
        player.addControl(new PlayerControl(app.getCamera().getWidth(), app.getCamera().getHeight()));
    }

    @Override
    protected void cleanup(Application app) {
        // TODO: Implement cleanup logic
    }

    @Override
    protected void onEnable() {
        app.getGuiNode().attachChild(background);
        app.getGuiNode().attachChild(player);
    }

    @Override
    protected void onDisable() {
        app.getGuiNode().detachChild(background);
        app.getGuiNode().detachChild(player);
    }

    @Override
    public void update(float tpf) {
        // Called every frame
        if ((Boolean) player.getUserData("alive")) {

        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if ((Boolean) player.getUserData("alive")) {
            if (name.equals("up")) {
                player.getControl(PlayerControl.class).up = isPressed;
            } else if (name.equals("down")) {
                player.getControl(PlayerControl.class).down = isPressed;
            } else if (name.equals("left")) {
                player.getControl(PlayerControl.class).left = isPressed;
            } else if (name.equals("right")) {
                player.getControl(PlayerControl.class).right = isPressed;
            }
        }
    }

    /**
     * getSpatial creates a node, adds texture and material, sets radius and returns it
     */
    private Spatial getSpatial(String name) {
        Node node = new Node(name);

        // load picture, note the convention of name matching png filename
        Picture pic = new Picture(name);
        Texture2D tex = (Texture2D) app.getAssetManager().loadTexture("Textures/" + name + ".png");
        pic.setTexture(app.getAssetManager(), tex, true);

        // adjust picture
        float width = tex.getImage().getWidth();
        float height = tex.getImage().getHeight();
        pic.setWidth(width);
        pic.setHeight(height);
        pic.move(-width / 2f, -height / 2f, 0);

        // add a material to the picture
        Material picMat = new Material(app.getAssetManager(), "Common/MatDefs/Gui/Gui.j3md");
        picMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        node.setMaterial(picMat);

        // set the radius of the spatial
        // (using width only as a simple approximation)
        node.setUserData("radius", width / 2);

        // attach the picture to the node and return it
        node.attachChild(pic);
        return node;
    }
}


