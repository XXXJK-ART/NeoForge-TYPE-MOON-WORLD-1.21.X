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
   private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ACTION = net.minecraft.network.syncher.SynchedEntityData.defineId(
      DiarmuidUaDuibhneEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
   private static final net.minecraft.network.syncher.EntityDataAccessor<Long> ACTION_START = net.minecraft.network.syncher.SynchedEntityData.defineId(
      DiarmuidUaDuibhneEntity.class, net.minecraft.network.syncher.EntityDataSerializers.LONG);
   private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DOUBLE_SPEAR_STANCE = net.minecraft.network.syncher.SynchedEntityData.defineId(
      DiarmuidUaDuibhneEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

   public DiarmuidUaDuibhneEntity(EntityType<? extends DiarmuidUaDuibhneEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ACTION, -1);
      builder.define(ACTION_START, 0L);
      builder.define(DOUBLE_SPEAR_STANCE, false);
   }

   public void setDoubleSpearStance(boolean active) { entityData.set(DOUBLE_SPEAR_STANCE, active); }
   public boolean isDoubleSpearStance() { return entityData.get(DOUBLE_SPEAR_STANCE); }

   public void triggerDiarmuidAction(net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAction action) {
      entityData.set(ACTION, action == null ? -1 : action.networkId());
      entityData.set(ACTION_START, level().getGameTime());
   }

   public net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAction getDiarmuidAction() {
      return net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAction.byNetworkId(entityData.get(ACTION));
   }

   public float getDiarmuidActionAge(float partialTick) { return level().getGameTime() - entityData.get(ACTION_START) + partialTick; }

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
      }
   }

   @Override
   protected void customServerAiStep() {
      if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) {
         super.customServerAiStep();
         return;
      }
      super.customServerAiStep();
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      if (!(target instanceof LivingEntity living) || living == this || !living.isAlive() || this.isAlliedTo(living) || living.isAlliedTo(this)) {
         return false;
      }
      this.faceToward(living.position());
      boolean red = net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAbilityService.distance(this, living) > 3
         || !net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAbilityService.hasSpear(this, false);
      return net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAbilityService.basicSpear(this, red);
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
      // Equipment is restored by vanilla; do not regenerate broken or unequipped spears on reload.
      if (tag.contains("HandItems", net.minecraft.nbt.Tag.TAG_LIST)) {
         var hands = tag.getList("HandItems", net.minecraft.nbt.Tag.TAG_COMPOUND);
         if (!hands.isEmpty() && hands.getCompound(0).isEmpty()) this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      }
      for (var hand : InteractionHand.values()) {
         if (isSpear(getItemInHand(hand), DiarmuidSpearItem.SpearType.GAE_DEARG) && !DiarmuidCombatHelper.isRedActive(this)
            || isSpear(getItemInHand(hand), DiarmuidSpearItem.SpearType.GAE_BUIDHE) && !DiarmuidCombatHelper.isYellowActive(this)) setItemInHand(hand, ItemStack.EMPTY);
      }
   }

   @Override
   public void ensureDefaultNpcLoadout(boolean forceClientSync) {
      // The spawn path equips both spears once; refreshes must preserve absent and broken weapons.
   }

   private void ensureDiarmuidLoadout() {
      if (DiarmuidCombatHelper.isRedActive(this) && !isSpear(this.getMainHandItem(), DiarmuidSpearItem.SpearType.GAE_DEARG)
         && !isSpear(this.getMainHandItem(), DiarmuidSpearItem.SpearType.GAE_BUIDHE)) {
         this.setItemInHand(InteractionHand.MAIN_HAND, DiarmuidCombatHelper.createSpearStack(
            DiarmuidSpearItem.SpearType.GAE_DEARG, DiarmuidCombatHelper.redDurability(this)));
      }
      if (DiarmuidCombatHelper.isYellowActive(this) && !isSpear(this.getOffhandItem(), DiarmuidSpearItem.SpearType.GAE_DEARG)
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
