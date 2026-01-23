package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public class SapphireProjectileEntity extends ThrowableItemProjectile {
    public SapphireProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public SapphireProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.SAPPHIRE_PROJECTILE.get(), shooter, level);
    }

    public SapphireProjectileEntity(Level level, double x, double y, double z) {
        super(ModEntities.SAPPHIRE_PROJECTILE.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CARVED_SAPPHIRE_FULL.get();
    }

    @Override
    public void tick() {
        // Check for water collision manually before movement
        if (!this.level().isClientSide) {
             Vec3 start = this.position();
             Vec3 end = start.add(this.getDeltaMovement());
             HitResult raytrace = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, this));
             if (raytrace.getType() == HitResult.Type.BLOCK) {
                 this.onHit(raytrace);
                 // If onHit discards the entity, we should stop here.
                 if (this.isRemoved()) return;
             }
        }
        super.tick();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            Level level = this.level();
            BlockPos center = this.blockPosition();
            int radius = MagicConstants.SAPPHIRE_ICE_RADIUS;
            List<BlockPos> placedBlocks = new ArrayList<>();
            RandomSource random = level.getRandom();

            // Create Ice Sphere
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x * x + y * y + z * z <= radius * radius) {
                            BlockPos pos = center.offset(x, y, z);
                            if (level.getBlockState(pos).isAir()) {
                                BlockState iceState;
                                int roll = random.nextInt(100);
                                if (roll < 60) {
                                    iceState = Blocks.ICE.defaultBlockState();
                                } else if (roll < 90) {
                                    iceState = Blocks.PACKED_ICE.defaultBlockState();
                                } else {
                                    iceState = Blocks.BLUE_ICE.defaultBlockState();
                                }
                                
                                if (level.setBlock(pos, iceState, 3)) {
                                    placedBlocks.add(pos);
                                }
                            }
                        }
                    }
                }
            }

            // Spawn Particles (Snow)
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 50, 3.0, 3.0, 3.0, 0.1);
            }

            // Apply Debuff
            AABB aabb = new AABB(center).inflate(radius);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb);
            Entity owner = this.getOwner();
            for (LivingEntity entity : entities) {
                if (entity != owner) {
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MagicConstants.SAPPHIRE_DEBUFF_DURATION, 1));
                }
            }

            // Schedule Cleanup
            if (!placedBlocks.isEmpty()) {
                // Random duration for each block or group? 
                // The requirement says "every block break becomes 5-8s".
                // Let's iterate and schedule individual destruction.
                
                for (BlockPos pos : placedBlocks) {
                    int duration = 100 + random.nextInt(60); // 5s (100 ticks) + 0-3s (0-60 ticks) = 5-8s
                    TYPE_MOON_WORLD.queueServerWork(duration, () -> {
                         // Only remove if it's still one of our ice types
                        BlockState state = level.getBlockState(pos);
                        if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)) {
                            level.destroyBlock(pos, false);
                        }
                    });
                }
            }

            this.discard();
        }
    }
}
