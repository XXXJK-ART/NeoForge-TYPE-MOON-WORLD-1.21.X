package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.player.MercurySwordMagicAmplifier;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class ElementalMagicProjectileEntity extends ThrowableItemProjectile {
   public static final int ELEMENT_FIRE = 0;
   public static final int ELEMENT_WATER = 1;
   public static final int ELEMENT_WIND = 2;
   public static final int ELEMENT_EARTH = 3;
   public static final int FORM_BASIC = 0;
   public static final int FORM_HIGH = 1;
   public static final int FORM_ULTIMATE = 2;
   private static final EntityDataAccessor<Integer> ELEMENT = SynchedEntityData.defineId(ElementalMagicProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(ElementalMagicProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> VISUAL_SCALE = SynchedEntityData.defineId(ElementalMagicProjectileEntity.class, EntityDataSerializers.FLOAT);
   public final List<Vec3> tracePos = new LinkedList<>();
   private float damage = 4.0F;
   private float radius = 0.0F;
   private float knockback = 0.0F;
   private int igniteSeconds = 0;
   private int slowTicks = 0;
   private float slowPercent = 0.0F;
   private double maxRange = 16.0;
   private Vec3 originPos = Vec3.ZERO;
   private boolean pierceArmor = false;
   private boolean cutLowHardnessBlocks = false;

   public ElementalMagicProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public ElementalMagicProjectileEntity(Level level, LivingEntity shooter) {
      super(ModEntities.ELEMENTAL_MAGIC_PROJECTILE.get(), shooter, level);
      this.originPos = shooter.position();
      this.setPos(EntityUtils.getRightHandCastAnchor(shooter));
   }

   public ElementalMagicProjectileEntity(Level level, double x, double y, double z) {
      super(ModEntities.ELEMENTAL_MAGIC_PROJECTILE.get(), x, y, z, level);
      this.originPos = new Vec3(x, y, z);
   }

   protected Item getDefaultItem() {
      return ModItems.MAGIC_FRAGMENTS.get();
   }

   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ELEMENT, ELEMENT_FIRE);
      builder.define(FORM, FORM_BASIC);
      builder.define(VISUAL_SCALE, 0.65F);
   }

   public void configure(int element, int form, float damage, float radius, float knockback, int igniteSeconds, int slowTicks, float slowPercent, double maxRange, boolean pierceArmor, boolean cutLowHardnessBlocks, float visualScale) {
      this.entityData.set(ELEMENT, Math.max(ELEMENT_FIRE, Math.min(ELEMENT_EARTH, element)));
      this.entityData.set(FORM, Math.max(FORM_BASIC, Math.min(FORM_ULTIMATE, form)));
      this.entityData.set(VISUAL_SCALE, Math.max(0.2F, visualScale));
      this.damage = Math.max(0.0F, damage);
      this.radius = Math.max(0.0F, radius);
      this.knockback = Math.max(0.0F, knockback);
      this.igniteSeconds = Math.max(0, igniteSeconds);
      this.slowTicks = Math.max(0, slowTicks);
      this.slowPercent = Math.max(0.0F, slowPercent);
      this.maxRange = Math.max(4.0, maxRange);
      this.pierceArmor = pierceArmor;
      this.cutLowHardnessBlocks = cutLowHardnessBlocks;
      this.originPos = this.position();
   }

   public int getElement() {
      return this.entityData.get(ELEMENT);
   }

   public int getForm() {
      return this.entityData.get(FORM);
   }

   public float getVisualScale() {
      return this.entityData.get(VISUAL_SCALE);
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide || this.tickCount % 2 == 0) {
         spawnTrailParticles();
      }
      if (!this.level().isClientSide && (this.tickCount > 100 || this.position().distanceToSqr(this.originPos) > this.maxRange * this.maxRange)) {
         this.discard();
      }
      if (this.level().isClientSide) {
         Vec3 pos = this.position();
         if (this.tracePos.isEmpty() || pos.distanceToSqr(this.tracePos.get(this.tracePos.size() - 1)) >= 0.01) {
            this.tracePos.add(pos);
            if (this.tracePos.size() > 32) {
               this.tracePos.remove(0);
            }
         }
      }
   }

   protected boolean canHitEntity(Entity entity) {
      return entity != null && entity != this.getOwner() && super.canHitEntity(entity);
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide) {
         if (result.getEntity() instanceof LivingEntity living && !EntityUtils.isImmunePlayerTarget(living)) {
            if (this.radius > 0.0F) {
               applyAreaImpact(result.getLocation());
            } else {
               applyToTarget(living, result.getLocation());
            }
         }
         spawnImpactParticles(result.getLocation());
         this.discard();
      }
   }

   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!this.level().isClientSide && !this.isRemoved()) {
         if (result instanceof BlockHitResult blockHit && this.cutLowHardnessBlocks) {
            tryCutBlock(blockHit.getBlockPos());
         }
         if (this.radius > 0.0F) {
            applyAreaImpact(result.getLocation());
         }
         spawnImpactParticles(result.getLocation());
         this.discard();
      }
   }

   private void applyAreaImpact(Vec3 center) {
      AABB box = new AABB(center, center).inflate(this.radius);
      for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this.getOwner() && e.isAlive() && !EntityUtils.isImmunePlayerTarget(e))) {
         if (living.position().distanceToSqr(center) <= this.radius * this.radius) {
            applyToTarget(living, center);
         }
      }
   }

   private void applyToTarget(LivingEntity target, Vec3 center) {
      float finalDamage = this.damage;
      if (this.getOwner() instanceof LivingEntity owner) {
         finalDamage = MercurySwordMagicAmplifier.amplifyElementalDamage(owner, this.getElement(), finalDamage);
      }
      if (!this.pierceArmor) {
         finalDamage = MagicResistanceHelper.applyMagicDamageReduction(target, this.damageSources().magic(), finalDamage);
      }
      target.invulnerableTime = 0;
      target.hurt(this.damageSources().magic(), finalDamage);
      target.invulnerableTime = 0;
      if (this.igniteSeconds > 0) {
         target.igniteForSeconds(this.igniteSeconds);
      }
      if (this.slowTicks > 0) {
         int amplifier = this.slowPercent >= 0.5F ? 2 : this.slowPercent >= 0.2F ? 0 : 0;
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MagicResistanceHelper.applyDebuffResistance(target, this.slowTicks), amplifier, false, true, true));
      }
      if (this.knockback > 0.0F) {
         Vec3 away = target.position().subtract(center);
         if (away.lengthSqr() < 1.0E-6) {
            away = this.getDeltaMovement();
         }
         if (away.lengthSqr() > 1.0E-6) {
            target.setDeltaMovement(target.getDeltaMovement().add(away.normalize().scale(this.knockback)));
            target.hurtMarked = true;
         }
      }
   }

   private void tryCutBlock(BlockPos pos) {
      BlockState state = this.level().getBlockState(pos);
      float hardness = state.getDestroySpeed(this.level(), pos);
      if (hardness >= 0.0F && hardness <= 1.5F && !state.isAir() && !state.is(Blocks.BEDROCK)) {
         this.level().destroyBlock(pos, true, this.getOwner());
      }
   }

   private void spawnTrailParticles() {
      ParticleOptions particle = particleForElement(this.getElement());
      if (this.level() instanceof ServerLevel level) {
         level.sendParticles(particle, this.getX(), this.getY(), this.getZ(), 2, 0.04, 0.04, 0.04, 0.0);
      } else {
         this.level().addParticle(particle, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      }
   }

   private void spawnImpactParticles(Vec3 pos) {
      if (this.level() instanceof ServerLevel level) {
         level.sendParticles(particleForElement(this.getElement()), pos.x, pos.y, pos.z, this.radius > 0.0F ? 48 : 18, this.radius, this.radius * 0.45, this.radius, 0.08);
         level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 8, 0.22, 0.22, 0.22, 0.02);
      }
   }

   private static ParticleOptions particleForElement(int element) {
      return switch (element) {
         case ELEMENT_WATER -> ParticleTypes.SPLASH;
         case ELEMENT_WIND -> ParticleTypes.CLOUD;
         case ELEMENT_EARTH -> ParticleTypes.POOF;
         default -> ParticleTypes.FLAME;
      };
   }
}
