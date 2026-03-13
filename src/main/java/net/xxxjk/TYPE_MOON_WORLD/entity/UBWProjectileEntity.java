package net.xxxjk.TYPE_MOON_WORLD.entity;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class UBWProjectileEntity extends ThrowableItemProjectile {
   private List<Entity> hitEntities = new ArrayList<>();

   public UBWProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public UBWProjectileEntity(Level level, LivingEntity shooter, ItemStack stack) {
      super(ModEntities.UBW_PROJECTILE.get(), shooter, level);
      this.setItem(stack);
   }

   public UBWProjectileEntity(Level level, double x, double y, double z) {
      super(ModEntities.UBW_PROJECTILE.get(), x, y, z, level);
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
         this.level().addParticle(ParticleTypes.ENCHANT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
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

                  if (this.getOwner() instanceof ServerPlayer player) {
                     ChantHandler.registerPlacedSword(player.getUUID(), placePos);
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
