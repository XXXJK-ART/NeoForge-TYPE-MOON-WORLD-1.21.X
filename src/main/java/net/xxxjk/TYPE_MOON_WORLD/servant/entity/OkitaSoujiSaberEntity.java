package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SwordActionAnimationService;
import org.jetbrains.annotations.Nullable;

public final class OkitaSoujiSaberEntity extends ServantEntity {
   public static final String SERVANT_KEY = "okita_souji_saber";
   private long swordActionUntil;
   private int basicSwordAction;

   public OkitaSoujiSaberEntity(EntityType<? extends OkitaSoujiSaberEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                       @Nullable SpawnGroupData groupData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, groupData);
      this.ensureOkitaDefaultLoadout(false);
      return result;
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         OkitaSoujiSaberCombatHelper.tickPersistentState(this);
         LivingEntity target = this.getTarget();
         if (target != null && target.isAlive() && !this.isRemoved()) {
            // Navigation can rotate a melee mob toward its retreat/chase path.
            // Okita's sword combat always presents her front to the target.
            this.faceToward(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
         }
      }
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      SasakiKojiroAction action = switch (basicSwordAction++ & 3) {
         case 1 -> SasakiKojiroAction.UPWARD_SLASH;
         case 2 -> SasakiKojiroAction.HORIZONTAL_SLASH;
         case 3 -> SasakiKojiroAction.DOWNWARD_SLASH;
         default -> SasakiKojiroAction.VERTICAL_SLASH;
      };
      // Start the sword motion on the attack attempt, matching Sasaki's NPC
      // technique presentation even when the target's hurt call is rejected.
      SwordActionAnimationService.play(this, action);
      boolean hit;
      if (target instanceof LivingEntity living) {
         hit = living.hurt(this.damageSources().indirectMagic(this, this), (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE));
      } else {
         hit = super.doHurtTarget(target);
      }
      if (hit) {
         this.swing(InteractionHand.MAIN_HAND);
         this.triggerSlashAnimation();
      }
      return hit;
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      OkitaSoujiSaberTechniqueService.cancel(this);
      swordActionUntil = 0;
      this.ensureOkitaDefaultLoadout(false);
   }

   public void ensureOkitaDefaultLoadout(boolean forceClientSync) {
      if (this.level() instanceof ServerLevel) {
         this.ensureDefaultNpcLoadout(forceClientSync);
         this.equipKikuIchimonji();
         OkitaSoujiSaberCombatHelper.ensureHaoriActive(this);
      }
   }

   @Override
   public void ensureDefaultNpcLoadout(boolean forceClientSync) {
      super.ensureDefaultNpcLoadout(forceClientSync);
      this.equipKikuIchimonji();
      OkitaSoujiSaberCombatHelper.ensureHaoriActive(this);
   }

   public void onShinsengumiKilled() {
      OkitaSoujiSaberCombatHelper.decrementFlagPool(this);
   }

   public void onShinsengumiKilled(int formationIndex) {
      OkitaSoujiSaberCombatHelper.decrementFlagPool(this, formationIndex);
   }

   public boolean canUseMumyoudanZuki() {
      return !OkitaSoujiSaberCombatHelper.isWeakConstitutionActive(this);
   }

   public void beginSwordAction(int ticks) {
      swordActionUntil = this.level().getGameTime() + ticks;
      this.getNavigation().stop();
   }

   @Override
   public boolean isPerformingAction() {
      return super.isPerformingAction() || this.level().getGameTime() < swordActionUntil;
   }

   private void equipKikuIchimonji() {
      if (this.level() instanceof ServerLevel && !this.getMainHandItem().is(ModItems.KIKU_ICHIMONJI_NORIMUNE.get())) {
         this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KIKU_ICHIMONJI_NORIMUNE.get()));
         this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
      }
   }
}
