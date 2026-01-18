package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.Item;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;

import net.minecraft.core.particles.ParticleTypes;

@SuppressWarnings("null")
public class RubyProjectileEntity extends ThrowableItemProjectile {
    public RubyProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public RubyProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.RUBY_PROJECTILE.get(), shooter, level);
    }

    public RubyProjectileEntity(Level level, double x, double y, double z) {
        super(ModEntities.RUBY_PROJECTILE.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CARVED_RUBY_FULL.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            // 参数说明：source, x, y, z, radius, fire, interaction
            // 将 fire 参数设置为 true 以产生火焰
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), MagicConstants.RUBY_EXPLOSION_RADIUS, true, Level.ExplosionInteraction.TNT);
            
            // Additional particle effects on hit
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(), 20, 1.0, 1.0, 1.0, 0.5);
                serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 30, 1.5, 1.5, 1.5, 0.1);
            }

            this.discard();
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }

        if (this.tickCount > MagicConstants.RUBY_LIFETIME_TICKS && !this.level().isClientSide) { // 10 seconds = 200 ticks
             this.level().explode(this, this.getX(), this.getY(), this.getZ(), MagicConstants.RUBY_EXPLOSION_RADIUS, true, Level.ExplosionInteraction.TNT);
             this.discard();
        }
    }
}
