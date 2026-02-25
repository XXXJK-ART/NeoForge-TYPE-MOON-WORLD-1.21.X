package net.xxxjk.TYPE_MOON_WORLD.utils;

import net.minecraft.world.entity.LivingEntity;
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

public class EntityUtils {

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
