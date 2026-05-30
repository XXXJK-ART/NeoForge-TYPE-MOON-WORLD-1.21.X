package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.LinkedList;
import java.util.List;
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
      RULE_BREAKER(1);

      private final int id;

      Mode(int id) {
         this.id = id;
      }

      public int id() {
         return this.id;
      }

      public static Mode fromId(int id) {
         return id == 1 ? RULE_BREAKER : BOLT;
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

   @Override
   protected boolean canHitEntity(Entity entity) {
      return entity != this.getOwner() && entity != null && !EntityUtils.isImmunePlayerTarget(entity) && super.canHitEntity(entity);
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
         this.level().addParticle(BOLT_CORE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         this.level().addParticle(BOLT_ACCENT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
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
      if (owner != null) {
         EntityUtils.triggerSwarmAnger(this.level(), owner, living);
      }
      this.spawnImpactFx(this.position());
      this.discard();
   }

   @Override
   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!this.level().isClientSide()) {
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
         serverLevel.sendParticles(BOLT_CORE, position.x, position.y, position.z, 8, 0.18, 0.18, 0.18, 0.0);
         serverLevel.sendParticles(BOLT_ACCENT, position.x, position.y, position.z, 8, 0.16, 0.16, 0.16, 0.0);
         serverLevel.sendParticles(ParticleTypes.END_ROD, position.x, position.y, position.z, 6, 0.12, 0.12, 0.12, 0.02);
      }
   }
}
