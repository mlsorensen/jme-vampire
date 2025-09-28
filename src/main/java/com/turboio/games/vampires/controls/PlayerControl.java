package com.turboio.games.vampires.controls;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

public class PlayerControl extends AbstractControl {
    private final int screenWidth;
    private final int screenHeight;

    // is the player currently moving?
    public boolean up,down,left,right;

    // speed of the player
    private final float speed = 800f;

    // lastRotation of the player
    private float lastRotation;

    public PlayerControl(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    @Override
    protected void controlUpdate(float tpf) {
        // move the player in a certain direction if they are not out of the screen
        if (up) {
            if (spatial.getLocalTranslation().y < screenHeight - (Float)spatial.getUserData("radius") - 200) {
                spatial.move(0,tpf*speed,0);
            }
        } else if (down) {
            if (spatial.getLocalTranslation().y > (Float)spatial.getUserData("radius")) {
                spatial.move(0,tpf*-speed,0);
            }
        } else if (left) {
            if (spatial.getLocalTranslation().x  > (Float)spatial.getUserData("radius")) {
                spatial.move(tpf*-speed,0,0);
            }
        } else if (right) {
            if (spatial.getLocalTranslation().x < screenWidth - (Float)spatial.getUserData("radius")) {
                spatial.move(tpf*speed,0,0);
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    // reset the moving values (i.e. for spawning)
    public void reset() {
        up = false;
        down = false;
        left = false;
        right = false;
    }
}
