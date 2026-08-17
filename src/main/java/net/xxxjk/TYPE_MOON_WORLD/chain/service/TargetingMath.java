package net.xxxjk.TYPE_MOON_WORLD.chain.service;

import net.minecraft.world.phys.Vec3;

public final class TargetingMath {
    public static boolean isInsideForwardHemisphere(Vec3 origin, Vec3 look, Vec3 target, double radiusSqr) {
        Vec3 delta = target.subtract(origin);
        double distanceSqr = delta.lengthSqr();
        if (distanceSqr <= 1.0E-10D || distanceSqr > radiusSqr || look.lengthSqr() <= 1.0E-10D) {
            return false;
        }
        return delta.normalize().dot(look.normalize()) >= 0.0D;
    }

    public static int roundRobinIndex(int targetIndex, int chainCount) {
        if (targetIndex < 0 || chainCount <= 0) {
            throw new IllegalArgumentException("targetIndex must be nonnegative and chainCount must be positive");
        }
        return targetIndex % chainCount;
    }

    public static float applyDivinityOutputSuppression(float damage, int divinityLevel) {
        if (damage <= 0.0F || divinityLevel <= 0) {
            return damage;
        }
        return (float)(damage / Math.scalb(1.0D, Math.min(30, divinityLevel)));
    }

    public static Vec3 steerToward(Vec3 currentVelocity, Vec3 desiredDirection, double speed, double maxRadians) {
        if (speed <= 0.0D || maxRadians < 0.0D || desiredDirection.lengthSqr() <= 1.0E-10D) {
            throw new IllegalArgumentException("speed and maxRadians must be nonnegative and direction must be nonzero");
        }
        Vec3 desired = desiredDirection.normalize();
        if (currentVelocity.lengthSqr() <= 1.0E-10D) {
            return desired.scale(speed);
        }
        Vec3 current = currentVelocity.normalize();
        double dot = Math.clamp(current.dot(desired), -1.0D, 1.0D);
        double angle = Math.acos(dot);
        if (angle <= maxRadians || angle <= 1.0E-10D) {
            return desired.scale(speed);
        }

        Vec3 axis = current.cross(desired);
        if (axis.lengthSqr() <= 1.0E-10D) {
            axis = current.cross(Math.abs(current.y) < 0.9D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : new Vec3(1.0D, 0.0D, 0.0D));
        }
        axis = axis.normalize();
        double cosine = Math.cos(maxRadians);
        double sine = Math.sin(maxRadians);
        Vec3 turned = current.scale(cosine)
            .add(axis.cross(current).scale(sine))
            .add(axis.scale(axis.dot(current) * (1.0D - cosine)));
        return turned.normalize().scale(speed);
    }

    private TargetingMath() {
    }
}

