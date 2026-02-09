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
import java.util.LinkedList;
import net.minecraft.world.phys.Vec3;

import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public class TopazProjectileEntity extends ThrowableItemProjectile {
    public final List<Vec3> tracePos = new LinkedList<>();

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
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
             Vec3 pos = position();
             boolean addPos = true;
             if (tracePos.size() > 0) {
                 Vec3 lastPos = tracePos.get(tracePos.size() - 1);
                 addPos = pos.distanceToSqr(lastPos) >= 0.01;
             }
             if (addPos) {
                 tracePos.add(pos);
                 if (tracePos.size() > 560) {
                     tracePos.remove(0);
                 }
             }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            Level level = this.level();
            double radius = MagicConstants.TOPAZ_EFFECT_RADIUS;
            
            float multiplier = 1.0f;
            ItemStack stack = this.getItem();
            if (stack.getItem() instanceof FullManaCarvedGemItem gemItem) {
                 multiplier = gemItem.getQuality().getEffectMultiplier();
            }
            int duration = Math.round(MagicConstants.TOPAZ_DEBUFF_DURATION * multiplier);

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
                    entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0));
                    entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 1));
                    
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
