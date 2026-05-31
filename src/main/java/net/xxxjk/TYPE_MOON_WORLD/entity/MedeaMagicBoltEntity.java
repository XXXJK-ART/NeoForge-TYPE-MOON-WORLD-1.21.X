package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

public class MedeaMagicBoltEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(MedeaMagicBoltEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(MedeaMagicBoltEntity.class, EntityDataSerializers.FLOAT);
   private static final DustParticleOptions BOLT_CORE = new DustParticleOptions(new Vector3f(0.52F, 0.72F, 1.0F), 1.1F);
   private static final DustParticleOptions BOLT_ACCENT = new DustParticleOptions(new Vector3f(0.72F, 0.28F, 1.0F), 1.25F);
   public final List<Vec3> tracePos = new LinkedList<>();

   public MedeaMagicBoltEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public MedeaMagicBoltEntity(Level level, LivingEntity shooter) {
      super(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.MEDEA_MAGIC_BOLT.get(), shooter, level);
      this.setNoGravity(true);
   }

   public enum Mode {
      BOLT(0),
      RULE_BREAKER(1),
      SUPER_BOLT(2),
      FIRE_BOLT(3),
      FROST_BOLT(4);

      private final int id;

      Mode(int id) {
         this.id = id;
      }

      public int id() {
         return this.id;
      }

      public static Mode fromId(int id) {
         return switch (id) {
            case 1 -> RULE_BREAKER;
            case 2 -> SUPER_BOLT;
            case 3 -> FIRE_BOLT;
            case 4 -> FROST_BOLT;
            default -> BOLT;
         };
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(MODE, Mode.BOLT.id());
      builder.define(DAMAGE, 25.0F);
   }

   @Override
   protected Item getDefaultItem() {
      return ModItems.GANDER.get();
   }

   public void setMode(Mode mode) {
      this.entityData.set(MODE, mode.id());
   }

   public Mode getMode() {
      return Mode.fromId(this.entityData.get(MODE));
   }

   public void setMagicDamage(float damage) {
      this.entityData.set(DAMAGE, damage);
   }

   public float getMagicDamage() {
      return this.entityData.get(DAMAGE);
   }

   public boolean isLargeMagicBolt() {
      return this.getMode() == Mode.SUPER_BOLT;
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      LivingEntity owner = this.getOwner() instanceof LivingEntity livingOwner ? livingOwner : null;
      return entity != this.getOwner()
         && entity != null
         && !EntityUtils.isImmunePlayerTarget(entity)
         && (owner == null || !entity.isAlliedTo(owner))
         && super.canHitEntity(entity);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         Vec3 position = this.position();
         if (this.tracePos.isEmpty() || this.tracePos.get(this.tracePos.size() - 1).distanceToSqr(position) >= 0.01) {
            this.tracePos.add(position);
            while (this.tracePos.size() > 90) {
               this.tracePos.remove(0);
            }
         }
         spawnClientTrail();
      } else if (this.tickCount > 60) {
         this.discard();
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!(result.getEntity() instanceof LivingEntity living)) {
         return;
      }
      LivingEntity owner = this.getOwner() instanceof LivingEntity livingOwner ? livingOwner : null;
      DamageSource source = owner != null ? this.damageSources().mobProjectile(this, owner) : this.damageSources().magic();
      living.invulnerableTime = 0;
      if (this.getMode() == Mode.RULE_BREAKER) {
         MedeaCombatHelper.applyRuleBreakerHit(living, owner);
      }
      living.hurt(source, this.getMagicDamage());
      living.invulnerableTime = 0;
      applyModeHitEffects(living);
      if (owner != null) {
         EntityUtils.triggerSwarmAnger(this.level(), owner, living);
      }
      if (this.getMode() == Mode.SUPER_BOLT) {
         destroyBreakableBlocks(BlockPos.containing(result.getLocation()), 1);
      }
      this.spawnImpactFx(this.position());
      this.discard();
   }

   @Override
   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!this.level().isClientSide()) {
         if (this.getMode() == Mode.SUPER_BOLT) {
            destroyBreakableBlocks(BlockPos.containing(result.getLocation()), 1);
         }
         this.spawnImpactFx(result.getLocation());
         this.discard();
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("MedeaBoltMode", this.getMode().id());
      tag.putFloat("MedeaBoltDamage", this.getMagicDamage());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.entityData.set(MODE, tag.getInt("MedeaBoltMode"));
      this.entityData.set(DAMAGE, tag.getFloat("MedeaBoltDamage"));
   }

   private void spawnImpactFx(Vec3 position) {
      if (this.level() instanceof ServerLevel serverLevel) {
         switch (this.getMode()) {
            case SUPER_BOLT -> {
               serverLevel.sendParticles(BOLT_CORE, position.x, position.y, position.z, 24, 0.45, 0.45, 0.45, 0.0);
               serverLevel.sendParticles(ParticleTypes.FLASH, position.x, position.y, position.z, 2, 0.1, 0.1, 0.1, 0.0);
               serverLevel.sendParticles(ParticleTypes.EXPLOSION, position.x, position.y, position.z, 6, 0.4, 0.4, 0.4, 0.0);
               serverLevel.sendParticles(ParticleTypes.END_ROD, position.x, position.y, position.z, 20, 0.32, 0.32, 0.32, 0.04);
            }
            case FIRE_BOLT -> {
               serverLevel.sendParticles(ParticleTypes.FLAME, position.x, position.y, position.z, 18, 0.25, 0.25, 0.25, 0.03);
               serverLevel.sendParticles(ParticleTypes.LAVA, position.x, position.y, position.z, 8, 0.18, 0.18, 0.18, 0.0);
               serverLevel.sendParticles(ParticleTypes.SMOKE, position.x, position.y, position.z, 10, 0.22, 0.22, 0.22, 0.02);
            }
            case FROST_BOLT -> {
               serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, position.x, position.y, position.z, 22, 0.3, 0.25, 0.3, 0.02);
               serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL, position.x, position.y, position.z, 14, 0.22, 0.22, 0.22, 0.01);
               serverLevel.sendParticles(BOLT_CORE, position.x, position.y, position.z, 8, 0.15, 0.15, 0.15, 0.0);
            }
            default -> {
               serverLevel.sendParticles(BOLT_CORE, position.x, position.y, position.z, 8, 0.18, 0.18, 0.18, 0.0);
               serverLevel.sendParticles(BOLT_ACCENT, position.x, position.y, position.z, 8, 0.16, 0.16, 0.16, 0.0);
               serverLevel.sendParticles(ParticleTypes.END_ROD, position.x, position.y, position.z, 6, 0.12, 0.12, 0.12, 0.02);
            }
         }
      }
   }

   private void spawnClientTrail() {
      switch (this.getMode()) {
         case SUPER_BOLT -> {
            this.level().addParticle(BOLT_CORE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(BOLT_CORE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.ENCHANT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         }
         case FIRE_BOLT -> {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.SMALL_FLAME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         }
         case FROST_BOLT -> {
            this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.ITEM_SNOWBALL, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(BOLT_CORE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         }
         default -> {
            this.level().addParticle(BOLT_CORE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(BOLT_ACCENT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         }
      }
   }

   private void applyModeHitEffects(LivingEntity living) {
      switch (this.getMode()) {
         case FIRE_BOLT -> {
            living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 120));
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 40, 0, false, true, true));
         }
         case FROST_BOLT -> {
            living.setTicksFrozen(Math.max(living.getTicksFrozen(), 120));
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 3, false, true, true));
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 60, 0, false, true, true));
         }
         case SUPER_BOLT -> living.knockback(1.2, this.getX() - living.getX(), this.getZ() - living.getZ());
         default -> {
         }
      }
   }

   private void destroyBreakableBlocks(BlockPos center, int radius) {
      if (!(this.level() instanceof ServerLevel serverLevel)) {
         return;
      }
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
         BlockState state = serverLevel.getBlockState(pos);
         float hardness = state.getDestroySpeed(serverLevel, pos);
         if (state.isAir() || state.is(Blocks.BEDROCK) || hardness < 0.0F || hardness >= 45.0F) {
            continue;
         }
         serverLevel.destroyBlock(pos, false, this.getOwner() instanceof LivingEntity living ? living : null);
      }
   }
}
