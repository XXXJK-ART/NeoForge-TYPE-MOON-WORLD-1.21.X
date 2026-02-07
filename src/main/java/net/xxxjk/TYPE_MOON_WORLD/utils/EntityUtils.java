package net.xxxjk.TYPE_MOON_WORLD.utils;

import net.minecraft.world.entity.LivingEntity;
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

public class EntityUtils {

    public static void triggerSwarmAnger(Level level, LivingEntity attacker, LivingEntity target) {
        if (target == null || !target.isAlive() || attacker == null || level.isClientSide) return;
        
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
                // If Zombified Piglin, set anger
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
