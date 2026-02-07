package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

import java.util.List;

@SuppressWarnings("null")
public class TopazProjectileEntity extends ThrowableItemProjectile {
    public TopazProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public TopazProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.TOPAZ_PROJECTILE.get(), shooter, level);
    }

    public TopazProjectileEntity(Level level, double x, double y, double z) {
        super(ModEntities.TOPAZ_PROJECTILE.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CARVED_TOPAZ_FULL.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            Level level = this.level();
            double radius = MagicConstants.TOPAZ_EFFECT_RADIUS;

            // Spawn Particles (Fireworks/Sparkle)
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FIREWORK, this.getX(), this.getY(), this.getZ(), 50, 2.0, 2.0, 2.0, 0.1);
                serverLevel.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY(), this.getZ(), 5, 1.0, 1.0, 1.0, 0);
            }

            // Apply Debuffs
            AABB aabb = this.getBoundingBox().inflate(radius);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb);
            Entity owner = this.getOwner();
            for (LivingEntity entity : entities) {
                if (entity != owner) {
                    entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, MagicConstants.TOPAZ_DEBUFF_DURATION, 0));
                    entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, MagicConstants.TOPAZ_DEBUFF_DURATION, 0));
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MagicConstants.TOPAZ_DEBUFF_DURATION, 1));
                    
                    if (owner instanceof LivingEntity livingOwner) {
                        EntityUtils.triggerSwarmAnger(level, livingOwner, entity);
                        if (entity instanceof net.minecraft.world.entity.Mob mob) {
                            mob.setTarget(livingOwner);
                        }
                    }
                }
            }

            this.discard();
        }
    }
}
