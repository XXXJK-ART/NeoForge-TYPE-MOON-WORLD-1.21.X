package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class UbwChantRippleEntity extends net.minecraft.world.entity.Entity {
   private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(UbwChantRippleEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> TARGET_RADIUS = SynchedEntityData.defineId(UbwChantRippleEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> ALPHA = SynchedEntityData.defineId(UbwChantRippleEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(UbwChantRippleEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> COLLAPSING = SynchedEntityData.defineId(UbwChantRippleEntity.class, EntityDataSerializers.BOOLEAN);
   private UUID ownerUUID;

   public UbwChantRippleEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public UbwChantRippleEntity(Level level, ServerPlayer owner) {
      this(ModEntities.UBW_CHANT_RIPPLE.get(), level);
      this.ownerUUID = owner.getUUID();
      this.setPos(owner.getX(), owner.getY() + 0.02, owner.getZ());
      this.entityData.set(COLOR, MagicCircuitColorHelper.COLOR_SWORD);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(RADIUS, 0.8F);
      builder.define(TARGET_RADIUS, 6.0F);
      builder.define(ALPHA, 0.65F);
      builder.define(COLOR, MagicCircuitColorHelper.DEFAULT_COLOR);
      builder.define(COLLAPSING, false);
   }

   public float getRadius() {
      return this.entityData.get(RADIUS);
   }

   public float getAlphaStrength() {
      return this.entityData.get(ALPHA);
   }

   public int getColor() {
      return this.entityData.get(COLOR);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         if (!(this.level() instanceof ServerLevel serverLevel) || this.ownerUUID == null) {
            this.discard();
            return;
         }

         ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(this.ownerUUID);
         if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
         }

         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         this.entityData.set(COLOR, MagicCircuitColorHelper.COLOR_SWORD);
         if (this.entityData.get(COLLAPSING)) {
            float radius = this.entityData.get(RADIUS) * 0.78F;
            float alpha = this.entityData.get(ALPHA) * 0.82F;
            this.entityData.set(RADIUS, radius);
            this.entityData.set(ALPHA, alpha);
            if (radius <= 0.5F || alpha <= 0.05F) {
               this.discard();
            }
            return;
         }

         if (!vars.is_chanting_ubw || vars.is_in_ubw) {
            this.beginCollapse();
            return;
         }

         this.setPos(owner.getX(), owner.getY() + 0.02, owner.getZ());
         float targetRadius = 6.0F + Math.max(0, vars.ubw_chant_progress - 3) * 4.8F;
         this.entityData.set(TARGET_RADIUS, targetRadius);
         this.entityData.set(ALPHA, Math.min(0.9F, 0.52F + Math.max(0, vars.ubw_chant_progress - 3) * 0.05F));
         this.entityData.set(RADIUS, this.entityData.get(RADIUS) + (targetRadius - this.entityData.get(RADIUS)) * 0.22F);
         if (vars.ubw_chant_progress >= 9) {
            this.beginCollapse();
         }
      }
   }

   public void beginCollapse() {
      this.entityData.set(COLLAPSING, true);
      this.entityData.set(TARGET_RADIUS, 0.0F);
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) {
         this.ownerUUID = tag.getUUID("Owner");
      }
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerUUID != null) {
         tag.putUUID("Owner", this.ownerUUID);
      }
   }
}
