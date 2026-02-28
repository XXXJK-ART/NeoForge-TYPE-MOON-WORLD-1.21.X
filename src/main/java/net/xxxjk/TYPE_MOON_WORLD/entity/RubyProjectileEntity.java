package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.Item;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.gravity.GemGravityFieldMagic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedList;
import java.util.List;

import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public class RubyProjectileEntity extends ThrowableItemProjectile {
    public final List<Vec3> tracePos = new LinkedList<>();
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> GEM_TYPE = net.minecraft.network.syncher.SynchedEntityData.defineId(RubyProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> VISUAL_SCALE = net.minecraft.network.syncher.SynchedEntityData.defineId(RubyProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> VISUAL_END_X = net.minecraft.network.syncher.SynchedEntityData.defineId(RubyProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> VISUAL_END_Y = net.minecraft.network.syncher.SynchedEntityData.defineId(RubyProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> VISUAL_END_Z = net.minecraft.network.syncher.SynchedEntityData.defineId(RubyProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> HAS_VISUAL_END = net.minecraft.network.syncher.SynchedEntityData.defineId(RubyProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    public RubyProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GEM_TYPE, 0); // 0: Ruby, 1: Sapphire, 2: Emerald, 3: Topaz, 4: Cyan, 5: White, 6: Black
        builder.define(VISUAL_SCALE, 1.0f);
        builder.define(VISUAL_END_X, 0.0f);
        builder.define(VISUAL_END_Y, 0.0f);
        builder.define(VISUAL_END_Z, 0.0f);
        builder.define(HAS_VISUAL_END, false);
    }
    
    public void setVisualScale(float scale) {
        this.entityData.set(VISUAL_SCALE, scale);
    }
    
    public float getVisualScale() {
        return this.entityData.get(VISUAL_SCALE);
    }
    
    public void setVisualEnd(Vec3 end) {
        this.entityData.set(VISUAL_END_X, (float)end.x);
        this.entityData.set(VISUAL_END_Y, (float)end.y);
        this.entityData.set(VISUAL_END_Z, (float)end.z);
        this.entityData.set(HAS_VISUAL_END, true);
    }
    
    public Vec3 getVisualEnd() {
        if (!this.entityData.get(HAS_VISUAL_END)) return null;
        return new Vec3(this.entityData.get(VISUAL_END_X), this.entityData.get(VISUAL_END_Y), this.entityData.get(VISUAL_END_Z));
    }
    
    public void setGemType(int type) {
        this.entityData.set(GEM_TYPE, type);
    }
    
    public int getGemType() {
        return this.entityData.get(GEM_TYPE);
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
    protected boolean canHitEntity(Entity entity) {
        if (this.getGemType() == 99) return false; // Visual slash passes through entities
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            // Visual Only Mode (Slash Effect)
            if (this.getGemType() == 99) {
                this.discard();
                return;
            }

            boolean isRandomMode = false;
            ItemStack stack = this.getItem();
            net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            net.minecraft.nbt.CompoundTag customTag = null;
            if (customData != null) {
                customTag = customData.copyTag();
                if (customTag.getBoolean("IsRandomMode")) {
                    isRandomMode = true;
                }
            }

            if (GemGravityFieldMagic.tryHandleProjectileImpact(this, stack)) {
                this.discard();
                return;
            }

            if (this.getGemType() == 4 && !isRandomMode) {
                 float cyanMultiplier = 1.0f;
                 if (stack.getItem() instanceof FullManaCarvedGemItem gemItem) {
                      cyanMultiplier = gemItem.getQuality().getEffectMultiplier();
                 }
                 
                 if (customTag != null && customTag.getBoolean("IsCyanTornado")
                         && result.getType() == HitResult.Type.BLOCK) {
                     float radius = MagicConstants.CYAN_WIND_RADIUS * cyanMultiplier;
                     if (customTag.contains("CyanRadius")) {
                         radius = customTag.getFloat("CyanRadius");
                     }
                     int duration = MagicConstants.CYAN_WIND_DURATION;
                     if (customTag.contains("CyanDuration")) {
                         duration = customTag.getInt("CyanDuration");
                     }
                     net.xxxjk.TYPE_MOON_WORLD.entity.CyanWindFieldEntity wind = new net.xxxjk.TYPE_MOON_WORLD.entity.CyanWindFieldEntity(this.level(), this.getX(), this.getY(), this.getZ(), radius, duration, (this.getOwner() instanceof LivingEntity l) ? l : null);
                     wind.setTornadoMode(true);
                     this.level().addFreshEntity(wind);
                     this.discard();
                     return;
                 }

                 if (customTag != null && customTag.contains("ExplosionPowerMultiplier")) {
                     cyanMultiplier *= customTag.getFloat("ExplosionPowerMultiplier");
                 }

                 net.xxxjk.TYPE_MOON_WORLD.entity.CyanWindFieldEntity wind = new net.xxxjk.TYPE_MOON_WORLD.entity.CyanWindFieldEntity(this.level(), this.getX(), this.getY(), this.getZ(), MagicConstants.CYAN_WIND_RADIUS * cyanMultiplier, MagicConstants.CYAN_WIND_DURATION, (this.getOwner() instanceof LivingEntity l) ? l : null);
                 this.level().addFreshEntity(wind);
                 this.discard();
                 return;
            }

            float multiplier = 1.0f;
            if (stack.getItem() instanceof FullManaCarvedGemItem gemItem) {
                 multiplier = gemItem.getQuality().getEffectMultiplier();
            }
            
            if (customTag != null) {
                if (customTag.contains("ExplosionPowerMultiplier")) {
                    multiplier *= customTag.getFloat("ExplosionPowerMultiplier");
                }
            }
            
            float radius = MagicConstants.RUBY_EXPLOSION_RADIUS * multiplier;

            // 随机投掷模式：基于玩家魔力量决定爆炸半径，仅造成范围与伤害与特效，不破坏地形
            if (isRandomMode) {
                int gemType = this.getGemType();
                
                // 以玩家当前魔力量占比进行缩放：半径 = 基础半径 * 质量/附加倍率 * (0.8 + 1.2 * manaRatio) * typeWeight
                float manaScale = 1.0f;
                if (this.getOwner() instanceof net.minecraft.world.entity.player.Player p) {
                    TypeMoonWorldModVariables.PlayerVariables vars = p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    double maxMana = Math.max(vars.player_max_mana, 1.0);
                    double manaRatio = Math.max(0.0, Math.min(vars.player_mana / maxMana, 1.0));
                    manaScale = (float)(0.8 + 1.2 * manaRatio); // 0.8x ~ 2.0x
                }
                
                // 不点燃、且不破坏方块
                this.level().explode(this, this.getX(), this.getY(), this.getZ(), radius, false, Level.ExplosionInteraction.NONE);
                
                // 额外的范围爆炸特效，随半径提升粒子数量
                if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    int particleCount = Math.max(6, (int)(8 + radius * 2));
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                    serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), particleCount, radius * 0.4, radius * 0.3, radius * 0.4, 0.02);
                }
            } else {
                // 标准红宝石投掷保持原有行为（可点燃、可破坏地形）
                // 参数说明：source, x, y, z, radius, fire, interaction
                this.level().explode(this, this.getX(), this.getY(), this.getZ(), radius, true, Level.ExplosionInteraction.TNT);
            }
            
            if (this.getOwner() instanceof LivingEntity owner) {
                 java.util.List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius));
                 for (LivingEntity t : targets) {
                     EntityUtils.triggerSwarmAnger(this.level(), owner, t);
                 }
            }
            
            // Additional particle effects on hit
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(), 20, 1.0, 1.0, 1.0, 0.5);
                serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 30, 1.5, 1.5, 1.5, 0.1);
            }

            this.discard();
        }
    }
    
    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (HAS_VISUAL_END.equals(key) && this.level().isClientSide) {
             if (this.entityData.get(HAS_VISUAL_END)) {
                // Update trace immediately when data arrives
                tracePos.clear();
                Vec3 start = this.position();
                Vec3 end = getVisualEnd();
                if (end != null) {
                    int steps = 20;
                    for (int i = 0; i <= steps; i++) {
                        tracePos.add(start.lerp(end, (float)i / steps));
                    }
                }
             }
        }
    }
    
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        if (this.getGemType() == 99) {
            // Always render visual slashes if within reasonable distance (e.g. 64 blocks = 4096 sqr)
            // Or just return true if it's not too far (e.g. 256 blocks)
            return distance < 65536.0D; // 256 blocks
        }
        return super.shouldRenderAtSqrDistance(distance);
    }

    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        if (this.getGemType() == 99 && this.entityData.get(HAS_VISUAL_END)) {
            Vec3 end = getVisualEnd();
            if (end != null) {
                return this.getBoundingBox().minmax(new net.minecraft.world.phys.AABB(end, end).inflate(1.0));
            }
        }
        return super.getBoundingBoxForCulling();
    }

    @Override
    public void tick() {
        super.tick();
        
        // Cleanup visual entities
        // Lasts for 2s (40 ticks) as requested
        if (!this.level().isClientSide && this.getGemType() == 99 && this.tickCount > 40) {
            this.discard();
            return;
        }
        
        if (this.level().isClientSide) {
            boolean isRandomMode = false;
            ItemStack stack = this.getItem();
            net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (customData != null && customData.copyTag().getBoolean("IsRandomMode")) {
                isRandomMode = true;
            }

            int type = getGemType();
            // Default Ruby particles
            if (type == 0) {
                 this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            } else if (type == 1) { // Sapphire
                 this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            } else if (type == 2) { // Emerald
                 this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            } else if (type == 3) { // Topaz
                 this.level().addParticle(ParticleTypes.WAX_ON, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            } else if (type == 4) { // Cyan
                 this.level().addParticle(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            } else if (type == 5) { // White
                 this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            } else if (type == 6) { // Black
                 this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            } else {
                 this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
            
            Vec3 pos = position();
            if (this.getGemType() == 99 && this.entityData.get(HAS_VISUAL_END)) {
                // Static slash logic: Always keep the trace full between Start (Current Pos) and End
                tracePos.clear();
                Vec3 start = this.position();
                Vec3 end = getVisualEnd();
                int steps = 20;
                for (int i = 0; i <= steps; i++) {
                    tracePos.add(start.lerp(end, (float)i / steps));
                }
            } else {
                boolean addPos = true;
                if (!tracePos.isEmpty()) {
                    Vec3 last = tracePos.get(tracePos.size() - 1);
                    if (last.distanceToSqr(pos) < 0.05) {
                        addPos = false;
                    }
                }
                
                if (addPos) {
                    tracePos.add(pos);
                    if (tracePos.size() > 560) {
                        tracePos.remove(0);
                    }
                }
            }
        }

        if (this.tickCount > MagicConstants.RUBY_LIFETIME_TICKS && !this.level().isClientSide) { // 10 seconds = 200 ticks
             float multiplier = 1.0f;
             ItemStack stack = this.getItem();
             if (stack.getItem() instanceof FullManaCarvedGemItem gemItem) {
                  multiplier = gemItem.getQuality().getEffectMultiplier();
             }
             
             // Check for custom power multiplier
             net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
             if (customData != null) {
                 net.minecraft.nbt.CompoundTag tag = customData.copyTag();
                 if (tag.contains("ExplosionPowerMultiplier")) {
                     multiplier *= tag.getFloat("ExplosionPowerMultiplier");
                 }
             }
             
             float radius = MagicConstants.RUBY_EXPLOSION_RADIUS * multiplier;

             this.level().explode(this, this.getX(), this.getY(), this.getZ(), radius, true, Level.ExplosionInteraction.TNT);
             
             if (this.getOwner() instanceof LivingEntity owner) {
                 java.util.List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius));
                 for (LivingEntity t : targets) {
                     EntityUtils.triggerSwarmAnger(this.level(), owner, t);
                 }
             }
             
             this.discard();
        }
    }
}
