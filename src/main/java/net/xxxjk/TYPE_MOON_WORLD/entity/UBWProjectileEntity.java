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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.UBWWeaponBlock;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.UBWWeaponBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class UBWProjectileEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Integer> VISUAL_COLOR = SynchedEntityData.defineId(UBWProjectileEntity.class, EntityDataSerializers.INT);
   private List<Entity> hitEntities = new ArrayList<>();
   private boolean stainUbwTerrainOnImpact;
   private float miniBrokenPhantasmDamage;
   public final List<net.minecraft.world.phys.Vec3> tracePos = new LinkedList<>();

   public UBWProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public UBWProjectileEntity(Level level, LivingEntity shooter, ItemStack stack) {
      super(ModEntities.UBW_PROJECTILE.get(), shooter, level);
      this.setItem(stack);
      this.setVisualColorRgb(MagicCircuitColorHelper.ensureColor(shooter));
   }

   public UBWProjectileEntity(Level level, double x, double y, double z) {
      super(ModEntities.UBW_PROJECTILE.get(), x, y, z, level);
   }

   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(VISUAL_COLOR, MagicCircuitColorHelper.DEFAULT_COLOR);
   }

   public void setVisualColorRgb(int color) {
      this.entityData.set(VISUAL_COLOR, color);
   }

   public int getVisualColorRgb() {
      return (Integer)this.entityData.get(VISUAL_COLOR);
   }

   public void setStainUbwTerrainOnImpact(boolean stainUbwTerrainOnImpact) {
      this.stainUbwTerrainOnImpact = stainUbwTerrainOnImpact;
   }

   public void setMiniBrokenPhantasmDamage(float damage) {
      this.miniBrokenPhantasmDamage = Math.max(0.0F, damage);
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

   public void tick() {
      super.tick();
      if (this.level().isClientSide) {
         net.xxxjk.TYPE_MOON_WORLD.client.renderer.ProjectileVisualEffectHelper.captureTrace(this.tracePos, this, 80);
      } else if (this.level() instanceof ServerLevel serverLevel) {
         if (this.tickCount % 2 == 0) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY(), this.getZ(), 2, 0.04, 0.04, 0.04, 0.01);
            serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 1, 0.03, 0.03, 0.03, 0.02);
         }
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

   protected void onHit(HitResult result) {
      if (!this.level().isClientSide) {
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
               target.invulnerableTime = 0;
               if (this.getOwner() instanceof ServerPlayer serverPlayer && this.level() instanceof ServerLevel serverLevel) {
                  FakePlayer fakePlayer = new FakePlayer(serverLevel, new GameProfile(UUID.randomUUID(), "[UBW_Proxy]")) {
                     public float getAttackStrengthScale(float adjustTicks) {
                        return 1.0F;
                     }
                  };
                  fakePlayer.setPos(this.getX(), this.getY(), this.getZ());
                  fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, this.getItem());
                  fakePlayer.attack(target);
                  if (target instanceof LivingEntity livingTarget) {
                     livingTarget.setLastHurtByMob(serverPlayer);
                     if (!serverPlayer.isCreative()) {
                        if (livingTarget instanceof Mob mob) {
                           mob.setTarget(serverPlayer);
                        }

                        EntityUtils.triggerSwarmAnger(this.level(), serverPlayer, livingTarget);
                     }
                  }

                  fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                  fakePlayer.discard();
               } else {
                  float damage = this.calculateDamage();
                  target.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
               }

               target.invulnerableTime = 0;
               this.hitEntities.add(target);
               if (this.miniBrokenPhantasmDamage > 0.0F) {
                  triggerMiniBrokenPhantasm(entityHit.getLocation());
                  this.discard();
               }
            }

            return;
         }

         if (result.getType() == Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult)result;
            BlockPos hitPos = blockHit.getBlockPos();
            BlockState state = this.level().getBlockState(hitPos);
            if (!state.getCollisionShape(this.level(), hitPos).isEmpty() && state.canOcclude()) {
               if (state.getBlock() instanceof UBWWeaponBlock) {
                  this.discard();
                  return;
               }

               BlockPos placePos = hitPos.relative(blockHit.getDirection());
               BlockState placeState = this.level().getBlockState(placePos);
               if (!placeState.canBeReplaced()) {
                  this.discard();
                  return;
               }

               Direction facing = Direction.fromYRot(this.getYRot()).getOpposite();
               if (facing == Direction.UP || facing == Direction.DOWN) {
                  facing = Direction.NORTH;
               }

               BlockState newState = (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((UBWWeaponBlock)ModBlocks.UBW_WEAPON_BLOCK
                                          .get())
                                       .defaultBlockState()
                                       .setValue(UBWWeaponBlock.FACING, facing))
                                    .setValue(UBWWeaponBlock.ROTATION_A, this.random.nextBoolean()))
                                 .setValue(UBWWeaponBlock.ROTATION_B, this.random.nextBoolean()))
                              .setValue(UBWWeaponBlock.ROTATION_C, this.random.nextBoolean()))
                           .setValue(UBWWeaponBlock.ROTATION_D, this.random.nextBoolean()))
                        .setValue(UBWWeaponBlock.ROTATION_E, this.random.nextBoolean()))
                     .setValue(UBWWeaponBlock.ROTATION_F, this.random.nextBoolean()))
                  .setValue(UBWWeaponBlock.ROTATION_G, this.random.nextBoolean());
               if (this.level().setBlock(placePos, newState, 3)) {
                  if (this.level().getBlockEntity(placePos) instanceof UBWWeaponBlockEntity tile) {
                     tile.setStoredItem(this.getItem());
                  }

                  if (this.stainUbwTerrainOnImpact && this.level() instanceof ServerLevel serverLevel) {
                     EmiyaArcherCombatHelper.markIronSwordImpactTerrain(serverLevel, this.getOwner(), hitPos);
                  }

                  if (this.getOwner() instanceof ServerPlayer player) {
                     ChantHandler.registerPlacedSword(player.getUUID(), placePos);
                  }
               }

               if (this.miniBrokenPhantasmDamage > 0.0F) {
                  triggerMiniBrokenPhantasm(result.getLocation());
               }

               this.discard();
               return;
            }

            return;
         }

         this.discard();
      }
   }

   private void triggerMiniBrokenPhantasm(net.minecraft.world.phys.Vec3 center) {
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }
      double radius = 3.25;
      Entity owner = this.getOwner();
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         new net.minecraft.world.phys.AABB(center, center).inflate(radius),
         e -> e.isAlive() && e != owner && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         double distance = Math.sqrt(living.distanceToSqr(center.x, center.y, center.z));
         if (distance > radius) {
            continue;
         }
         float damage = (float)Math.max(8.0, this.miniBrokenPhantasmDamage * (1.0 - distance / (radius * 1.45)));
         living.invulnerableTime = 0;
         living.hurt(this.damageSources().explosion(this, owner), damage);
         living.invulnerableTime = 0;
         net.minecraft.world.phys.Vec3 push = living.position().subtract(center).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() > 1.0E-4) {
            push = push.normalize();
            living.push(push.x * 0.45, 0.18, push.z * 0.45);
            living.hurtMarked = true;
         }
      }
      level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 2, 0.35, 0.18, 0.35, 0.0);
      level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.1, center.z, 18, 0.55, 0.25, 0.55, 0.06);
      level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.25, center.z, 12, 0.45, 0.25, 0.45, 0.08);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.85F, 1.45F);
   }
}
