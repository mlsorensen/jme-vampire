package com.turboio.games.vampires.controls;

import com.jme3.scene.control.Control;
import com.turboio.games.vampires.perimeter.Perimeter;

public interface EnemyMovementControl extends Control {
    void setPerimeter(Perimeter perimeter);
    void setSpeed(float speed);
    void setEnabled(boolean enabled);
}
