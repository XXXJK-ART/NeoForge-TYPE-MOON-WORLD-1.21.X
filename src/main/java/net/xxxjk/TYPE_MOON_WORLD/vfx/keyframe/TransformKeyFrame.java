package net.xxxjk.TYPE_MOON_WORLD.vfx.keyframe;

import net.xxxjk.TYPE_MOON_WORLD.vfx.Easing;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public record TransformKeyFrame(float t, Vector3f position, Quaternionf rotation, Vector3f scale, Easing easing) {
}
