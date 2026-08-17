package net.xxxjk.TYPE_MOON_WORLD.chain.service;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EnumaPatternMath {
    private static final double GOLDEN_ANGLE = 2.399963229728653D;

    public static Vec3 gateOffset(long seed, int index, int count, double fieldRadius) {
        if (index < 0 || index >= count || count <= 0 || fieldRadius <= 1.0D) {
            throw new IllegalArgumentException("Invalid Enuma gate layout arguments");
        }
        double radius = Math.sqrt((index + 0.5D) / count) * (fieldRadius - 1.0D);
        double jitter = unitDouble(mix64(seed + index * 0x9E3779B97F4A7C15L));
        double angle = index * GOLDEN_ANGLE + (jitter - 0.5D) * 0.42D;
        radius = Math.max(1.0D, radius + (jitter - 0.5D) * 1.4D);
        return new Vec3(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
    }

    public static boolean segmentHits(AABB box, Vec3 from, Vec3 to) {
        return box.contains(from) || box.contains(to) || box.clip(from, to).isPresent();
    }

    public static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static double unitDouble(long value) {
        return (value >>> 11) * 0x1.0p-53;
    }

    private EnumaPatternMath() {
    }
}

