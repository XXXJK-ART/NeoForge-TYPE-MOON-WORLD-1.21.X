package net.xxxjk.TYPE_MOON_WORLD.utils;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

public class EntityUtils {
    private static final double RIGHT_HAND_CAST_FORWARD = 0.78D;
    private static final double RIGHT_HAND_CAST_RIGHT = 0.36D;
    private static final double RIGHT_HAND_CAST_UP = -0.22D;

    public static boolean isSpectatorPlayer(Entity entity) {
        return entity instanceof Player player && player.isSpectator();
    }

    public static boolean isImmunePlayerTarget(Entity entity) {
        return entity instanceof Player player && (player.isCreative() || player.isSpectator());
    }

    public static HitResult getRayTraceTarget(ServerPlayer player, double range) {
        float partialTicks = 1.0F;
        HitResult blockHit = player.pick(range, partialTicks, false);
        Vec3 eyePos = player.getEyePosition(partialTicks);
        double distToBlock = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation().distanceTo(eyePos) : range;

        Vec3 lookDir = player.getViewVector(partialTicks);
        Vec3 endPos = eyePos.add(lookDir.x * range, lookDir.y * range, lookDir.z * range);
        AABB searchBox = player.getBoundingBox().expandTowards(lookDir.scale(range)).inflate(1.0D);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eyePos, endPos, searchBox, (e) -> !e.isSpectator() && e.isPickable(), distToBlock * distToBlock);

        if (entityHit != null) {
            return entityHit;
        }
        return blockHit;
    }

    public static Vec3 getRightHandCastAnchor(LivingEntity caster) {
        if (caster instanceof Player player) {
            HumanoidArm rightArm = HumanoidArm.RIGHT;
            InteractionHand rightHand = player.getMainArm() == rightArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            return getHandCastAnchor(player, rightHand);
        }
        float yawRad = caster.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = forward.cross(up);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        return caster.getEyePosition()
                .add(forward.scale(RIGHT_HAND_CAST_FORWARD))
                .add(right.scale(RIGHT_HAND_CAST_RIGHT))
                .add(up.scale(RIGHT_HAND_CAST_UP));
    }

    public static boolean hasAnyEmptyHand(Player player) {
        return resolveEmptyCastingHand(player) != null;
    }

    public static InteractionHand resolveEmptyCastingHand(Player player) {
        boolean mainEmpty = player.getMainHandItem().isEmpty();
        boolean offEmpty = player.getOffhandItem().isEmpty();
        if (!mainEmpty && !offEmpty) {
            return null;
        }
        if (mainEmpty && !offEmpty) {
            return InteractionHand.MAIN_HAND;
        }
        if (!mainEmpty) {
            return InteractionHand.OFF_HAND;
        }
        // Both empty: prefer main hand as the active casting hand.
        return InteractionHand.MAIN_HAND;
    }

    public static HumanoidArm getArmForHand(Player player, InteractionHand hand) {
        HumanoidArm mainArm = player.getMainArm();
        return hand == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();
    }

    public static Vec3 getHandCastAnchor(Player player, InteractionHand hand) {
        float yawRad = player.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 sideBase = forward.cross(up);
        if (sideBase.lengthSqr() < 1.0E-6D) {
            sideBase = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            sideBase = sideBase.normalize();
        }
        HumanoidArm arm = getArmForHand(player, hand);
        double side = arm == HumanoidArm.RIGHT ? RIGHT_HAND_CAST_RIGHT : -RIGHT_HAND_CAST_RIGHT;
        return player.getEyePosition()
                .add(forward.scale(RIGHT_HAND_CAST_FORWARD))
                .add(sideBase.scale(side))
                .add(up.scale(RIGHT_HAND_CAST_UP));
    }

    public static HumanoidArm resolveEmptyCastingArm(Player player) {
        InteractionHand hand = resolveEmptyCastingHand(player);
        return hand == null ? null : getArmForHand(player, hand);
    }

    public static Vec3 getCurrentEmptyHandCastAnchor(Player player) {
        InteractionHand hand = resolveEmptyCastingHand(player);
        if (hand == null) {
            return getRightHandCastAnchor(player);
        }
        return getHandCastAnchor(player, hand);
    }

    public static void triggerSwarmAnger(Level level, LivingEntity attacker, LivingEntity target) {
        if (target == null || !target.isAlive() || attacker == null || level.isClientSide) return;
        if (attacker instanceof Player player && (player.isCreative() || player.isSpectator())) return;
        
        double range = 32.0D; // Alert range
        UUID attackerUUID = attacker.getUUID();
        
        // Zombified Piglin / Piglin
        if (target instanceof ZombifiedPiglin || target instanceof Piglin) {
            
            List<Monster> others = level.getEntitiesOfClass(
                Monster.class, 
                target.getBoundingBox().inflate(range),
                e -> e != target && e.isAlive() && (e instanceof ZombifiedPiglin || e instanceof Piglin)
            );
            
            for (Monster mob : others) {
                if (mob instanceof NeutralMob neutral) {
                    neutral.setPersistentAngerTarget(attackerUUID);
                    neutral.setRemainingPersistentAngerTime(400);
                    mob.setTarget(attacker);
                }
            }
        }
        // Wolves
        else if (target instanceof Wolf) {
             List<Wolf> wolves = level.getEntitiesOfClass(
                Wolf.class, 
                target.getBoundingBox().inflate(range),
                e -> e != target && e.isAlive() && !e.isTame()
            );
            for (Wolf wolf : wolves) {
                wolf.setPersistentAngerTarget(attackerUUID);
                wolf.setRemainingPersistentAngerTime(400);
                wolf.setTarget(attacker);
            }
        }
        // Bees
        else if (target instanceof Bee) {
             List<Bee> bees = level.getEntitiesOfClass(
                Bee.class, 
                target.getBoundingBox().inflate(range),
                e -> e != target && e.isAlive()
            );
            for (Bee bee : bees) {
                bee.setPersistentAngerTarget(attackerUUID);
                bee.setRemainingPersistentAngerTime(400);
                bee.setTarget(attacker);
            }
        }
        // Silverfish
        else if (target instanceof Silverfish) {
             List<Silverfish> fish = level.getEntitiesOfClass(
                Silverfish.class, 
                target.getBoundingBox().inflate(10.0D), // Smaller range usually
                e -> e != target && e.isAlive()
            );
            for (Silverfish s : fish) {
                s.setTarget(attacker);
            }
        }
    }
}
