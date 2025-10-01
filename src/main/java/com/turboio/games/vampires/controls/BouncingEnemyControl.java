package com.turboio.games.vampires.controls;

import com.jme3.math.Vector3f;
import com.jme3.scene.control.AbstractControl;
import com.turboio.games.vampires.perimeter.Perimeter;

import java.util.List;
import java.util.Random;

public class BouncingEnemyControl extends AbstractControl implements EnemyMovementControl {

    private final Random random = new Random();

    private float speed;
    private final Vector3f velocity = new Vector3f();
    private Perimeter perimeter;

    public BouncingEnemyControl(Perimeter perimeter, float speed) {
        this.perimeter = perimeter;
        this.speed = speed;
        chooseNewDirection();
    }

    @Override
    public void setPerimeter(Perimeter perimeter) {
        this.perimeter = perimeter;
    }

    @Override
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (!isEnabled() || spatial == null || perimeter == null) {
            return;
        }

        Vector3f currentPos = spatial.getLocalTranslation();
        Vector3f step = velocity.mult(speed * tpf);
        Vector3f nextPos = currentPos.add(step);

        float radius = ((Number) spatial.getUserData("radius")).floatValue();

        List<Vector3f> vertices = perimeter.getVertices();
        int n = vertices.size();
        Vector3f wallToReflect = null;
        boolean collision = false;

        // A simple check to see if we are about to collide.
        // This is not perfect, as it doesn't find the exact collision point,
        // but it's a good starting point for the bouncing behavior.
        if (!perimeter.contains(nextPos)) {
            collision = true;
        }

        for (int i = 0; i < n; i++) {
            Vector3f a = vertices.get(i);
            Vector3f b = vertices.get((i + 1) % n);
            if (distanceToSegment(nextPos, a, b) < radius) {
                wallToReflect = b.subtract(a);
                collision = true;
                break;
            }
        }

        if (collision) {
            // If we don't know which wall we hit (e.g. from contains=false), find the closest one.
            if (wallToReflect == null) {
                float minDistance = Float.MAX_VALUE;
                 for (int i = 0; i < n; i++) {
                    Vector3f a = vertices.get(i);
                    Vector3f b = vertices.get((i + 1) % n);
                    float dist = distanceToSegment(nextPos, a, b);
                    if (dist < minDistance) {
                        minDistance = dist;
                        wallToReflect = b.subtract(a);
                    }
                }
            }

            if (wallToReflect != null) {
                Vector3f wallNormal = new Vector3f(-wallToReflect.y, wallToReflect.x, 0).normalizeLocal();

                // Reflect velocity
                float dot = velocity.dot(wallNormal);
                velocity.subtractLocal(wallNormal.mult(2 * dot));

                // Move spatial along new vector for the remainder of the frame to avoid "sticky" walls
                // This is a simplification. A more accurate way involves calculating time of impact.
                // For now, we just apply the new velocity to the original position.
                Vector3f correctedStep = velocity.mult(speed * tpf);
                nextPos = currentPos.add(correctedStep);
            }
        }

        spatial.setLocalTranslation(nextPos);
    }

    @Override
    protected void controlRender(com.jme3.renderer.RenderManager rm, com.jme3.renderer.ViewPort vp) {}

    private void chooseNewDirection() {
        do {
            velocity.set(random.nextFloat() * 2f - 1f, random.nextFloat() * 2f - 1f, 0f);
        } while (velocity.lengthSquared() == 0f);
        velocity.normalizeLocal();
    }

    private float distanceToSegment(Vector3f point, Vector3f start, Vector3f end) {
        Vector3f seg = end.subtract(start);
        Vector3f toPoint = point.subtract(start);
        float segLenSq = seg.x * seg.x + seg.y * seg.y;
        if (segLenSq == 0f) {
            return point.distance(start);
        }
        float t = Math.max(0f, Math.min(1f, (toPoint.x * seg.x + toPoint.y * seg.y) / segLenSq));
        Vector3f projection = start.add(seg.mult(t));
        return projection.distance(point);
    }
}
