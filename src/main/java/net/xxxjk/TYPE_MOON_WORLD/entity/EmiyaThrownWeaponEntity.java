package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class EmiyaThrownWeaponEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Float> FIXED_DAMAGE = SynchedEntityData.defineId(EmiyaThrownWeaponEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> BREAK_LOW_HARDNESS = SynchedEntityData.defineId(EmiyaThrownWeaponEntity.class, EntityDataSerializers.BOOLEAN);
   private final Set<Integer> hitEntities = new HashSet<>();

   public EmiyaThrownWeaponEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public EmiyaThrownWeaponEntity(Level level, LivingEntity shooter, ItemStack stack) {
      super(ModEntities.EMIYA_THROWN_WEAPON.get(), shooter, level);
      this.setItem(stack);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FIXED_DAMAGE, 24.0F);
      builder.define(BREAK_LOW_HARDNESS, false);
   }

   public void setFixedDamage(float damage) {
      this.entityData.set(FIXED_DAMAGE, damage);
   }

   public void setBreakLowHardnessBlocks(boolean enabled) {
      this.entityData.set(BREAK_LOW_HARDNESS, enabled);
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      return entity != null && entity != this.getOwner() && !EntityUtils.isImmunePlayerTarget(entity) && super.canHitEntity(entity);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.IRON_SWORD;
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.entityData.get(BREAK_LOW_HARDNESS)) {
         destroySoftBlocksAhead();
      }
      if (this.tickCount > 100) {
         this.discard();
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!(result.getEntity() instanceof LivingEntity living) || this.level().isClientSide()) {
         return;
      }
      if (this.hitEntities.add(living.getId())) {
         DamageSource source = this.getOwner() instanceof LivingEntity owner ? this.damageSources().mobProjectile(this, owner) : this.damageSources().thrown(this, this);
         living.invulnerableTime = 0;
         living.hurt(source, this.damageForCurrentItem());
         living.invulnerableTime = 0;
      }
      this.discard();
   }

   private float damageForCurrentItem() {
      float fixed = this.entityData.get(FIXED_DAMAGE);
      if (fixed > 0.0F) {
         return fixed;
      }
      ItemStack stack = this.getItem();
      if (stack.isEmpty()) {
         return 8.0F;
      }
      ItemAttributeModifiers modifiers = (ItemAttributeModifiers)stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      return (float)Math.max(8.0, modifiers.compute(1.0, EquipmentSlot.MAINHAND));
   }

   private void destroySoftBlocksAhead() {
      if (!(this.level() instanceof ServerLevel serverLevel)) {
         return;
      }
      if (this.getDeltaMovement().lengthSqr() < 1.0E-4) {
         return;
      }
      for (double step = 0.2; step <= 1.4; step += 0.2) {
         BlockPos pos = BlockPos.containing(this.position().add(this.getDeltaMovement().normalize().scale(step)));
         BlockState state = serverLevel.getBlockState(pos);
         float hardness = state.getDestroySpeed(serverLevel, pos);
         if (!state.isAir() && !state.is(Blocks.BEDROCK) && hardness >= 0.0F && hardness <= 8.0F) {
            serverLevel.destroyBlock(pos, false, this.getOwner() instanceof LivingEntity living ? living : null);
         }
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(FIXED_DAMAGE, tag.getFloat("FixedDamage"));
      this.entityData.set(BREAK_LOW_HARDNESS, tag.getBoolean("BreakLowHardness"));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      tag.putFloat("FixedDamage", this.entityData.get(FIXED_DAMAGE));
      tag.putBoolean("BreakLowHardness", this.entityData.get(BREAK_LOW_HARDNESS));
   }
}
