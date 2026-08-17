package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DiarmuidSpearItem;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidCombatAi;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidCombatHelper;
import org.jetbrains.annotations.Nullable;

public class DiarmuidUaDuibhneEntity extends ServantEntity {
   public static final String SERVANT_KEY = "diarmuid_ua_duibhne";
   public static final String TAG_RETREAT_UNTIL = "DiarmuidRetreatUntil";

   public DiarmuidUaDuibhneEntity(EntityType<? extends DiarmuidUaDuibhneEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                       @Nullable SpawnGroupData spawnData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
      this.ensureDiarmuidLoadout();
      DiarmuidCombatHelper.ensureDurability(this);
      return result;
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         DiarmuidCombatHelper.ensureDurability(this);
         if (this.tickCount % 40 == 0) this.ensureDiarmuidLoadout();
      }
   }

   @Override
   protected void customServerAiStep() {
      if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) {
         super.customServerAiStep();
         return;
      }
      super.customServerAiStep();
      DiarmuidCombatAi.tick(this, level);
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      if (!(target instanceof LivingEntity living) || living == this || !living.isAlive() || this.isAlliedTo(living) || living.isAlliedTo(this)) {
         return false;
      }
      boolean useRed = DiarmuidCombatAi.consumePreferredRedRose(this, living);
      ItemStack originalMain = this.getMainHandItem();
      ItemStack originalOff = this.getOffhandItem();
      boolean mainAlreadyPreferred = isSpear(originalMain, useRed ? DiarmuidSpearItem.SpearType.GAE_DEARG : DiarmuidSpearItem.SpearType.GAE_BUIDHE);
      if (!mainAlreadyPreferred) {
         this.setItemInHand(InteractionHand.MAIN_HAND, originalOff);
         this.setItemInHand(InteractionHand.OFF_HAND, originalMain);
      }
      boolean hit = super.doHurtTarget(target);
      DiarmuidCombatHelper.syncSpearItemToOwner(this, useRed ? DiarmuidSpearItem.SpearType.GAE_DEARG : DiarmuidSpearItem.SpearType.GAE_BUIDHE);
      if (!mainAlreadyPreferred) {
         ItemStack currentMain = this.getMainHandItem();
         ItemStack currentOff = this.getOffhandItem();
         this.setItemInHand(InteractionHand.MAIN_HAND, currentOff);
         this.setItemInHand(InteractionHand.OFF_HAND, currentMain);
      }
      if (hit) {
         ServantVoiceHelper.tryPlayAttack(this);
      }
      return hit;
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (amount > 0.0F && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         amount = MagicResistanceHelper.applyMagicDamageReduction(this, source, amount);
         if (amount <= 0.0F) return false;
      }
      return super.hurt(source, amount);
   }

   @Override
   public void die(DamageSource cause) {
      DiarmuidCombatHelper.clearCursesFromOwner(this);
      super.die(cause);
   }

   @Override
   public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putLong(TAG_RETREAT_UNTIL, this.getPersistentData().getLong(TAG_RETREAT_UNTIL));
      tag.putInt(DiarmuidCombatHelper.RED_SPEAR_DURABILITY_TAG, DiarmuidCombatHelper.redDurability(this));
      tag.putInt(DiarmuidCombatHelper.YELLOW_SPEAR_DURABILITY_TAG, DiarmuidCombatHelper.yellowDurability(this));
   }

   @Override
   public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.getPersistentData().putLong(TAG_RETREAT_UNTIL, tag.getLong(TAG_RETREAT_UNTIL));
      this.getPersistentData().putInt(DiarmuidCombatHelper.RED_SPEAR_DURABILITY_TAG,
         tag.contains(DiarmuidCombatHelper.RED_SPEAR_DURABILITY_TAG) ? tag.getInt(DiarmuidCombatHelper.RED_SPEAR_DURABILITY_TAG) : DiarmuidCombatHelper.SPEAR_MAX_DURABILITY);
      this.getPersistentData().putInt(DiarmuidCombatHelper.YELLOW_SPEAR_DURABILITY_TAG,
         tag.contains(DiarmuidCombatHelper.YELLOW_SPEAR_DURABILITY_TAG) ? tag.getInt(DiarmuidCombatHelper.YELLOW_SPEAR_DURABILITY_TAG) : DiarmuidCombatHelper.SPEAR_MAX_DURABILITY);
      this.ensureDiarmuidLoadout();
   }

   private void ensureDiarmuidLoadout() {
      if (!isSpear(this.getMainHandItem(), DiarmuidSpearItem.SpearType.GAE_DEARG)
         && !isSpear(this.getMainHandItem(), DiarmuidSpearItem.SpearType.GAE_BUIDHE)) {
         this.setItemInHand(InteractionHand.MAIN_HAND, DiarmuidCombatHelper.createSpearStack(
            DiarmuidSpearItem.SpearType.GAE_DEARG, DiarmuidCombatHelper.redDurability(this)));
      }
      if (!isSpear(this.getOffhandItem(), DiarmuidSpearItem.SpearType.GAE_DEARG)
         && !isSpear(this.getOffhandItem(), DiarmuidSpearItem.SpearType.GAE_BUIDHE)) {
         this.setItemInHand(InteractionHand.OFF_HAND, DiarmuidCombatHelper.createSpearStack(
            DiarmuidSpearItem.SpearType.GAE_BUIDHE, DiarmuidCombatHelper.yellowDurability(this)));
      }
      DiarmuidCombatHelper.syncSpearItemToOwner(this, DiarmuidSpearItem.SpearType.GAE_DEARG);
      DiarmuidCombatHelper.syncSpearItemToOwner(this, DiarmuidSpearItem.SpearType.GAE_BUIDHE);
      this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
      this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.SERVANT_CARD_DIARMUID_UA_DUIBHNE_CHEST.get()));
      this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.SERVANT_CARD_DIARMUID_UA_DUIBHNE_LEGS.get()));
      this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.SERVANT_CARD_DIARMUID_UA_DUIBHNE_FEET.get()));
      this.setDropChance(EquipmentSlot.HEAD, 0.0F);
      this.setDropChance(EquipmentSlot.CHEST, 0.0F);
      this.setDropChance(EquipmentSlot.LEGS, 0.0F);
      this.setDropChance(EquipmentSlot.FEET, 0.0F);
   }

   private static boolean isSpear(ItemStack stack, DiarmuidSpearItem.SpearType type) {
      return stack.getItem() instanceof DiarmuidSpearItem spear && spear.spearType() == type;
   }
}
