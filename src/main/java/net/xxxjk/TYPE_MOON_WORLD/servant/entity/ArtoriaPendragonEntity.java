package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem;

public class ArtoriaPendragonEntity extends ServantEntity {
   public static final String SERVANT_KEY = "artoria_pendragon";
   private static final EntityDataAccessor<Boolean> EXCALIBUR_VISIBLE = SynchedEntityData.defineId(ArtoriaPendragonEntity.class, EntityDataSerializers.BOOLEAN);

   public ArtoriaPendragonEntity(EntityType<ArtoriaPendragonEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(EXCALIBUR_VISIBLE, false);
   }

   @Override
   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      CompoundTag data = this.getPersistentData();

      if (stack.is(ModItems.AVALON.get()) && player.isShiftKeyDown() && !ArtoriaPendragonCombatHelper.hasAvalon(this)) {
         if (!this.level().isClientSide()) {
            data.putBoolean(ArtoriaPendragonCombatHelper.TAG_HAS_AVALON, true);
            ArtoriaPendragonCombatHelper.syncExcaliburVisibility(this);
            if (!player.getAbilities().instabuild) {
               stack.shrink(1);
            }
            player.displayClientMessage(Component.translatable("entity.typemoonworld.artoria_pendragon.avalon_given"), true);
            ArtoriaPendragonCombatHelper.spawnAvalonFx(this);
         }
         return InteractionResult.sidedSuccess(this.level().isClientSide());
      }

      if (stack.is(ModItems.AVALON.get()) && !player.isShiftKeyDown()) {
         if (!this.level().isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            AvalonItem.activateFor(serverPlayer, stack, this);
            player.displayClientMessage(Component.translatable("item.typemoonworld.avalon.active"), true);
         }
         return InteractionResult.sidedSuccess(this.level().isClientSide());
      }

      if (player.isShiftKeyDown() && stack.isEmpty() && ArtoriaPendragonCombatHelper.hasAvalon(this)) {
         if (!this.level().isClientSide()) {
            data.putBoolean(ArtoriaPendragonCombatHelper.TAG_HAS_AVALON, false);
            ArtoriaPendragonCombatHelper.syncExcaliburVisibility(this);
            ItemStack avalon = new ItemStack(ModItems.AVALON.get());
            if (!player.addItem(avalon)) {
               player.drop(avalon, false);
            }
            player.displayClientMessage(Component.translatable("entity.typemoonworld.artoria_pendragon.avalon_taken"), true);
            ArtoriaPendragonCombatHelper.spawnAvalonFx(this);
         }
         return InteractionResult.sidedSuccess(this.level().isClientSide());
      }

      return super.mobInteract(player, hand);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         ArtoriaPendragonCombatHelper.syncExcaliburVisibility(this);
      }
   }

   public boolean isExcaliburVisible() {
      return this.entityData.get(EXCALIBUR_VISIBLE);
   }

   public void setExcaliburVisible(boolean visible) {
      this.entityData.set(EXCALIBUR_VISIBLE, visible);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("ArtoriaExcaliburVisible", this.isExcaliburVisible());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setExcaliburVisible(tag.getBoolean("ArtoriaExcaliburVisible"));
   }

   @Override
   public void die(DamageSource cause) {
      boolean dropAvalon = ArtoriaPendragonCombatHelper.hasAvalon(this) && this.level() instanceof ServerLevel;
      if (dropAvalon) {
         this.getPersistentData().putBoolean(ArtoriaPendragonCombatHelper.TAG_HAS_AVALON, false);
      }
      super.die(cause);
      if (dropAvalon) {
         this.spawnAtLocation(new ItemStack(ModItems.AVALON.get()));
      }
   }
}
