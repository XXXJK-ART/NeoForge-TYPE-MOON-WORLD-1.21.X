package net.xxxjk.TYPE_MOON_WORLD.entity;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.SwordBarrelBlock;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.SwordBarrelBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ProjectileVisualEffectHelper;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm.UBWBrokenPhantasmExplosion;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class SwordBarrelProjectileEntity extends ThrowableItemProjectile {
   private List<Entity> hitEntities = new ArrayList<>();
   private static final EntityDataAccessor<Integer> HOVER_TICKS = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> IS_HOVERING = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Float> TARGET_X = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> TARGET_Y = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> TARGET_Z = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> HAS_TARGET = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> IS_BROKEN_PHANTASM = SynchedEntityData.defineId(
      SwordBarrelProjectileEntity.class, EntityDataSerializers.BOOLEAN
   );
   private static final EntityDataAccessor<Integer> TARGET_ENTITY_ID = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> VISUAL_COLOR = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> SPAWN_PHASE = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.INT);
   private boolean isMode1Tracking = false;
   private boolean isMode2Tracking = false;
   private Vec3 hoverOffset = null;
   public final List<Vec3> tracePos = new LinkedList<>();

   public SwordBarrelProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public SwordBarrelProjectileEntity(Level level, LivingEntity shooter, ItemStack stack) {
      super(ModEntities.SWORD_BARREL_PROJECTILE.get(), shooter, level);
      this.setItem(stack);
      this.setVisualColorRgb(MagicCircuitColorHelper.ensureColor(shooter));
   }

   public SwordBarrelProjectileEntity(Level level, double x, double y, double z) {
      super(ModEntities.SWORD_BARREL_PROJECTILE.get(), x, y, z, level);
   }

   public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
      return new ClientboundAddEntityPacket(this, serverEntity);
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      return true;
   }

   public boolean isPushedByFluid() {
      return false;
   }

   protected Item getDefaultItem() {
      return Items.IRON_SWORD;
   }

   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(HOVER_TICKS, 0);
      builder.define(IS_HOVERING, false);
      builder.define(TARGET_X, 0.0F);
      builder.define(TARGET_Y, 0.0F);
      builder.define(TARGET_Z, 0.0F);
      builder.define(HAS_TARGET, false);
      builder.define(IS_BROKEN_PHANTASM, false);
      builder.define(TARGET_ENTITY_ID, -1);
      builder.define(VISUAL_COLOR, MagicCircuitColorHelper.DEFAULT_COLOR);
      builder.define(SPAWN_PHASE, 10);
   }

   public void setVisualColorRgb(int color) {
      this.entityData.set(VISUAL_COLOR, color);
   }

   public int getVisualColorRgb() {
      return (Integer)this.entityData.get(VISUAL_COLOR);
   }

   public int getSpawnPhase() {
      return (Integer)this.entityData.get(SPAWN_PHASE);
   }

   public void setTargetEntity(int entityId) {
      this.entityData.set(TARGET_ENTITY_ID, entityId);
   }

   public void setBrokenPhantasm(boolean isBrokenPhantasm) {
      this.entityData.set(IS_BROKEN_PHANTASM, isBrokenPhantasm);
   }

   public boolean isBrokenPhantasm() {
      return (Boolean)this.entityData.get(IS_BROKEN_PHANTASM);
   }

   public void setHover(int ticks, Vec3 target) {
      this.entityData.set(HOVER_TICKS, ticks);
      this.entityData.set(IS_HOVERING, true);
      if (target != null) {
         this.entityData.set(HAS_TARGET, true);
         this.entityData.set(TARGET_X, (float)target.x);
         this.entityData.set(TARGET_Y, (float)target.y);
         this.entityData.set(TARGET_Z, (float)target.z);
      } else {
         this.entityData.set(HAS_TARGET, false);
      }

      this.setNoGravity(true);
      this.setDeltaMovement(Vec3.ZERO);
   }

   public void setMode1Tracking(boolean tracking) {
      this.isMode1Tracking = tracking;
   }

   public void setMode2Tracking(boolean tracking) {
      this.isMode2Tracking = tracking;
   }

   public void setHoverOffset(Vec3 offset) {
      this.hoverOffset = offset;
   }

   public boolean isHovering() {
      return (Boolean)this.entityData.get(IS_HOVERING);
   }

   public void tick() {
      if ((Boolean)this.entityData.get(IS_HOVERING)) {
         int ticks = (Integer)this.entityData.get(HOVER_TICKS);
         if (this.level().isClientSide && (Boolean)this.entityData.get(HAS_TARGET)) {
            Vec3 target = new Vec3(
               ((Float)this.entityData.get(TARGET_X)).floatValue(),
               ((Float)this.entityData.get(TARGET_Y)).floatValue(),
               ((Float)this.entityData.get(TARGET_Z)).floatValue()
            );
            Vec3 dir = target.subtract(this.position()).normalize();
            float targetXRot = (float)Math.toDegrees(Math.asin(dir.y));
            double safeZ = Math.abs(dir.z) < 1.0E-5 ? (dir.z >= 0.0 ? 1.0E-5 : -1.0E-5) : dir.z;
            float targetYRot = (float)Math.toDegrees(Math.atan2(dir.x, safeZ));
            this.setXRot(targetXRot);
            this.setYRot(targetYRot);
            this.xRotO = this.getXRot();
            this.yRotO = this.getYRot();
         }

         if (ticks > 0) {
            this.entityData.set(HOVER_TICKS, ticks - 1);
            this.setDeltaMovement(Vec3.ZERO);
            if (this.level().isClientSide) {
               this.tracePos.clear();
            }
            if (!this.level().isClientSide) {
               if (this.isMode2Tracking && this.hoverOffset != null && this.getOwner() instanceof LivingEntity livingOwner) {
                  float yawRad = (float) Math.toRadians(livingOwner.getYRot());
                  double fwdX = -Math.sin(yawRad);
                  double fwdZ = Math.cos(yawRad);
                  double rightX = fwdZ;
                  double rightZ = -fwdX;
                  double localRight = this.hoverOffset.x;
                  double localUp = this.hoverOffset.y;
                  double localForward = this.hoverOffset.z;
                  Vec3 newPos = livingOwner.position()
                     .add(rightX * localRight, localUp, rightZ * localRight)
                     .add(fwdX * localForward, 0.0, fwdZ * localForward);
                  this.setPos(newPos.x, newPos.y, newPos.z);
               }
               int targetId = (Integer)this.entityData.get(TARGET_ENTITY_ID);
               boolean locked = false;
               if (targetId != -1) {
                  Entity target = this.level().getEntity(targetId);
                  if (target != null && target.isAlive()) {
                     this.entityData.set(TARGET_X, (float)target.getX());
                     this.entityData.set(TARGET_Y, (float)(target.getY() + target.getBbHeight() * 0.5));
                     this.entityData.set(TARGET_Z, (float)target.getZ());
                     this.entityData.set(HAS_TARGET, true);
                     locked = true;
                  } else {
                     this.entityData.set(TARGET_ENTITY_ID, -1);
                  }
               }

               if (!locked && this.getOwner() instanceof LivingEntity livingOwner) {
                  double range = 40.0;
                  Vec3 eyePos = livingOwner.getEyePosition();
                  Vec3 lookVec = livingOwner.getLookAngle();
                  Vec3 endPos = eyePos.add(lookVec.scale(range));
                  HitResult hit = this.level().clip(new ClipContext(eyePos, endPos, Block.COLLIDER, Fluid.NONE, livingOwner));
                  Vec3 newTarget = hit.getType() != Type.MISS ? hit.getLocation() : endPos;
                  this.entityData.set(TARGET_X, (float)newTarget.x);
                  this.entityData.set(TARGET_Y, (float)newTarget.y);
                  this.entityData.set(TARGET_Z, (float)newTarget.z);
               }

               if ((Boolean)this.entityData.get(HAS_TARGET)) {
                  Vec3 target = new Vec3(
                     ((Float)this.entityData.get(TARGET_X)).floatValue(),
                     ((Float)this.entityData.get(TARGET_Y)).floatValue(),
                     ((Float)this.entityData.get(TARGET_Z)).floatValue()
                  );
                  Vec3 dir = target.subtract(this.position()).normalize();
                  float targetXRot = (float)Math.toDegrees(Math.asin(dir.y));
                  double safeZ = Math.abs(dir.z) < 1.0E-5 ? (dir.z >= 0.0 ? 1.0E-5 : -1.0E-5) : dir.z;
                  float targetYRot = (float)Math.toDegrees(Math.atan2(dir.x, safeZ));
                  this.setXRot(targetXRot);
                  this.setYRot(targetYRot);
               }
            }

            this.setPos(this.getX(), this.getY(), this.getZ());
            return;
         }

         this.entityData.set(IS_HOVERING, false);
         this.setNoGravity(false);
         if ((Boolean)this.entityData.get(HAS_TARGET)) {
            Vec3 target = new Vec3(
               ((Float)this.entityData.get(TARGET_X)).floatValue(),
               ((Float)this.entityData.get(TARGET_Y)).floatValue(),
               ((Float)this.entityData.get(TARGET_Z)).floatValue()
            );
            Vec3 dir = target.subtract(this.position()).normalize();
            double speed = 2.0;
            this.setDeltaMovement(dir.scale(speed));
            this.level().playSound(null, this.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.0F);
            this.setXRot((float)Math.toDegrees(Math.asin(-dir.y)));
            double safeZ = Math.abs(dir.z) < 1.0E-5 ? (dir.z >= 0.0 ? 1.0E-5 : -1.0E-5) : dir.z;
            this.setYRot((float)Math.toDegrees(Math.atan2(-dir.x, safeZ)));
            this.xRotO = this.getXRot();
            this.yRotO = this.getYRot();
         }
      } else {
         if ((this.isMode1Tracking || this.isMode2Tracking) && !this.level().isClientSide) {
            Vec3 targetPos = null;
            int targetIdx = (Integer)this.entityData.get(TARGET_ENTITY_ID);
            if (targetIdx != -1) {
               Entity target = this.level().getEntity(targetIdx);
               if (target != null && target.isAlive()) {
                  targetPos = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
               }
            }

            if (targetPos == null && this.getOwner() instanceof LivingEntity livingOwner) {
               double range = 64.0;
               Vec3 eyePos = livingOwner.getEyePosition();
               Vec3 lookVec = livingOwner.getLookAngle();
               Vec3 endPos = eyePos.add(lookVec.scale(range));
               HitResult hit = this.level().clip(new ClipContext(eyePos, endPos, Block.COLLIDER, Fluid.NONE, livingOwner));
               targetPos = hit.getType() != Type.MISS ? hit.getLocation() : endPos;
            }

            if (targetPos != null) {
               Vec3 currentVel = this.getDeltaMovement();
               double speed = currentVel.length();
               if (speed > 0.05) {
                  Vec3 desiredDir = targetPos.subtract(this.position()).normalize();
                  double turnRate = this.isMode2Tracking ? 0.15 : 0.1;
                  Vec3 newDir = currentVel.normalize().lerp(desiredDir, turnRate).normalize();
                  this.setDeltaMovement(newDir.scale(speed));
               }
            }
         }

         Vec3 motion = this.getDeltaMovement();
         if (motion.lengthSqr() > 0.01) {
            double vH = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            this.setXRot((float)Math.toDegrees(Math.atan2(motion.y, vH)));
            double safeZ = Math.abs(motion.z) < 1.0E-5 ? (motion.z >= 0.0 ? 1.0E-5 : -1.0E-5) : motion.z;
            this.setYRot((float)Math.toDegrees(Math.atan2(motion.x, safeZ)));
            this.xRotO = this.getXRot();
            this.yRotO = this.getYRot();
         } else if ((Boolean)this.entityData.get(HAS_TARGET)) {
            Vec3 target = new Vec3(
               ((Float)this.entityData.get(TARGET_X)).floatValue(),
               ((Float)this.entityData.get(TARGET_Y)).floatValue(),
               ((Float)this.entityData.get(TARGET_Z)).floatValue()
            );
            Vec3 dir = target.subtract(this.position()).normalize();
            this.setXRot((float)Math.toDegrees(Math.asin(-dir.y)));
            double safeZ = Math.abs(dir.z) < 1.0E-5 ? (dir.z >= 0.0 ? 1.0E-5 : -1.0E-5) : dir.z;
            this.setYRot((float)Math.toDegrees(Math.atan2(-dir.x, safeZ)));
            this.xRotO = this.getXRot();
            this.yRotO = this.getYRot();
         }
      }

      super.tick();
      if (!this.level().isClientSide && this.tickCount > 100) {
         this.discard();
      }

      if (this.level().isClientSide) {
         ProjectileVisualEffectHelper.captureTrace(this.tracePos, this, 80);
      }
   }

   private float calculateDamage() {
      ItemStack stack = this.getItem();
      if (stack.isEmpty()) {
         return 4.0F;
      } else {
         ItemAttributeModifiers modifiers = (ItemAttributeModifiers)stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
         double damage = modifiers.compute(1.0, EquipmentSlot.MAINHAND);
         return (float)Math.max(4.0, damage);
      }
   }

   private void triggerExplosion() {
      UBWBrokenPhantasmExplosion.explode(this.level(), this, this.getOwner(), this.getItem(), this.position());
      this.discard();
   }

   protected void onHit(HitResult result) {
      if (!this.level().isClientSide) {
         if (this.isBrokenPhantasm()) {
            this.triggerExplosion();
            return;
         }

         if (result.getType() == Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult)result;
            Entity target = entityHit.getEntity();
            if (target.equals(this.getOwner())) {
               return;
            }

            if (EntityUtils.isImmunePlayerTarget(target)) {
               return;
            }

            if (!this.hitEntities.contains(target)) {
               if (this.getOwner() instanceof ServerPlayer serverPlayer && this.level() instanceof ServerLevel serverLevel) {
                  FakePlayer fakePlayer = new FakePlayer(serverLevel, new GameProfile(UUID.randomUUID(), "[UBW_Proxy]")) {
                     public float getAttackStrengthScale(float adjustTicks) {
                        return 1.0F;
                     }
                  };
                  fakePlayer.setPos(this.getX(), this.getY(), this.getZ());
                  fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, this.getItem());
                  target.invulnerableTime = 0;
                  fakePlayer.attack(target);
                  target.invulnerableTime = 0;
                  if (target instanceof LivingEntity livingTarget) {
                     livingTarget.setLastHurtByMob(serverPlayer);
                     if (!serverPlayer.isCreative()) {
                        if (livingTarget instanceof Mob mob) {
                           mob.setTarget(serverPlayer);
                        }

                        EntityUtils.triggerSwarmAnger(this.level(), serverPlayer, livingTarget);
                     }
                  }

                  target.invulnerableTime = 0;
                  fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                  fakePlayer.discard();
               } else {
                  float damage = this.calculateDamage();
                  target.invulnerableTime = 0;
                  target.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
                  target.invulnerableTime = 0;
               }

               this.hitEntities.add(target);
            }

            return;
         }

         if (result.getType() == Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult)result;
            BlockPos hitPos = blockHit.getBlockPos();
            BlockState state = this.level().getBlockState(hitPos);
            if (!state.getCollisionShape(this.level(), hitPos).isEmpty() && state.canOcclude()) {
               if (state.getBlock() instanceof SwordBarrelBlock) {
                  this.discard();
                  return;
               }

               BlockPos placePos = hitPos.relative(blockHit.getDirection());
               BlockState placeState = this.level().getBlockState(placePos);
               if (!placeState.canBeReplaced()) {
                  this.discard();
                  return;
               }

               Direction hitFace = blockHit.getDirection();
               Direction facing = hitFace.getOpposite();
               if (this.random.nextFloat() > 0.5F) {
                  this.discard();
                  return;
               }

               BlockState newState = (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((SwordBarrelBlock)ModBlocks.SWORD_BARREL_BLOCK
                                          .get())
                                       .defaultBlockState()
                                       .setValue(SwordBarrelBlock.FACING, facing))
                                    .setValue(SwordBarrelBlock.ROTATION_A, this.random.nextBoolean()))
                                 .setValue(SwordBarrelBlock.ROTATION_B, this.random.nextBoolean()))
                              .setValue(SwordBarrelBlock.ROTATION_C, this.random.nextBoolean()))
                           .setValue(SwordBarrelBlock.ROTATION_D, this.random.nextBoolean()))
                        .setValue(SwordBarrelBlock.ROTATION_E, this.random.nextBoolean()))
                     .setValue(SwordBarrelBlock.ROTATION_F, this.random.nextBoolean()))
                  .setValue(SwordBarrelBlock.ROTATION_G, this.random.nextBoolean());
               if (this.level().setBlock(placePos, newState, 3)) {
                  if (this.level().getBlockEntity(placePos) instanceof SwordBarrelBlockEntity tile) {
                     tile.setStoredItem(this.getItem());
                     tile.setCustomRotation(this.getXRot(), this.getYRot());
                  }

                  if (this.getOwner() instanceof ServerPlayer serverPlayer) {
                     ChantHandler.registerPlacedSword(serverPlayer.getUUID(), placePos);
                  }
               }

               this.discard();
               return;
            }

            return;
         }

         this.discard();
      }
   }
}
