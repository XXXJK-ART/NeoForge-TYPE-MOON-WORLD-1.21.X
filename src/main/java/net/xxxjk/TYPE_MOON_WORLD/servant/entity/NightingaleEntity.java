package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.HumanNpcEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NightingaleGunItem;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantEngagementService;
import net.xxxjk.TYPE_MOON_WORLD.servant.nightingale.NightingaleRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.nightingale.NightingaleSupportService;
import net.xxxjk.TYPE_MOON_WORLD.servant.skill.ServantNoblePhantasmResourceService;

public final class NightingaleEntity extends ServantEntity {
   public static final String SERVANT_KEY = "nightingale";
   private static final String LAST_NURSING = "NightingaleLastNursing";
   private static final String LAST_ANGEL_CRY = "NightingaleLastAngelCry";
   private static final String LAST_SHOT = "NightingaleLastShot";
   private static final String LAST_MELEE = "NightingaleLastMelee";
   private static final String NEXT_MELEE = "NightingaleNextMelee";
   private static final double MELEE_MODE_RANGE = 6.0;
   private static final String LAST_NP = "NightingaleLastNp";
   private static final String NP_CAST_END = "NightingaleNpCastEnd";
   private static final String NP_OVERDRAFT = "NightingaleNpOverdraft";
   private static final String COMBAT_OPENED = "NightingaleCombatOpened";
   private static final String LAST_INNOCENT_ATTACK_PREFIX = "NightingaleAggressor.";

   public NightingaleEntity(EntityType<? extends NightingaleEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
      this.setPathfindingMalus(PathType.DAMAGE_FIRE, 16.0F);
      this.setPathfindingMalus(PathType.DANGER_FIRE, 16.0F);
      this.setPathfindingMalus(PathType.STICKY_HONEY, 8.0F);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide && this.tickCount <= 2 && !this.getMainHandItem().is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.NIGHTINGALE_GUN.get())) {
         this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.NIGHTINGALE_GUN.get()));
      }
      if (this.level() instanceof ServerLevel level && this.isAlive()) tickNightingale(level);
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      boolean hurt = super.hurt(source, amount);
      if (!this.level().isClientSide && hurt && amount > 0.0F) {
         if (source.getEntity() instanceof LivingEntity attacker) markAggressor(attacker);
      }
      return hurt;
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      if (!(target instanceof LivingEntity living) || !canAttackTarget(living)
         || this.distanceToSqr(living) > MELEE_MODE_RANGE * MELEE_MODE_RANGE) return false;
      this.triggerAttackSwing();
      ServantVoiceHelper.tryPlayAttack(this);
      float damage = (float)this.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
      return living.hurt(this.damageSources().mobAttack(this), damage);
   }

   public boolean canAttackTarget(LivingEntity target) {
      if (target == null || !target.isAlive() || this.isAlliedTo(target) || target.isAlliedTo(this)) return false;
      if (!isNormallyInnocent(target)) return true;
      return NightingaleRules.canAttackNormallyInnocent(
         this.level().getGameTime(), this.getPersistentData().getLong(LAST_INNOCENT_ATTACK_PREFIX + target.getUUID())
      );
   }

   public void markAggressor(LivingEntity attacker) {
      if (attacker != null) this.getPersistentData().putLong(LAST_INNOCENT_ATTACK_PREFIX + attacker.getUUID(), this.level().getGameTime());
   }

   public boolean isNoblePhantasmCasting() {
      return this.getPersistentData().contains(NP_CAST_END);
   }

   private void tickNightingale(ServerLevel level) {
      long now = level.getGameTime();
      NightingaleSupportService.tickBuffs(this, now);
      if (isNoblePhantasmCasting()) {
         this.getNavigation().stop();
         this.setDeltaMovement(Vec3.ZERO);
         if (now >= this.getPersistentData().getLong(NP_CAST_END)) {
            finishNoblePhantasm(level, now);
         }
         return;
      }

      LivingEntity target = this.getTarget();
      if (target != null && !canAttackTarget(target)) {
         this.setTarget(null);
         target = null;
      }

      if (shouldStartNoblePhantasm() && canUseNoblePhantasm(now)) {
         startNoblePhantasm(now);
         return;
      }

      LivingEntity nursing = NightingaleSupportService.findSteelNursingTarget(this);
      if (nursing == null) nursing = NightingaleSupportService.findPaleRiderTarget(this);
      if (nursing != null && cooldownReady(LAST_NURSING, now, NightingaleRules.STEEL_NURSING_COOLDOWN)
         && this.getCurrentMp() >= NightingaleRules.STEEL_NURSING_COST) {
         this.setCurrentMp(this.getCurrentMp() - NightingaleRules.STEEL_NURSING_COST);
         this.getPersistentData().putLong(LAST_NURSING, now);
         this.faceToward(nursing.position());
         this.triggerNamedActionAnimation("heal");
         NightingaleSupportService.applyHealing(this, nursing, NightingaleRules.STEEL_NURSING_AMOUNT);
         return;
      }

      if (target != null) {
         if (!this.getPersistentData().getBoolean(COMBAT_OPENED)
            && cooldownReady(LAST_ANGEL_CRY, now, NightingaleRules.ANGEL_CRY_COOLDOWN)
            && this.getCurrentMp() >= NightingaleRules.ANGEL_CRY_COST) {
            useAngelCry(now);
            this.getPersistentData().putBoolean(COMBAT_OPENED, true);
            return;
         }
         maintainCombat(target, now);
      } else {
         this.getPersistentData().putBoolean(COMBAT_OPENED, false);
         this.getPersistentData().putBoolean(NEXT_MELEE, false);
         if (now % 20L == 0L && isDirtyPosition(this.blockPosition())) {
            BlockPos clean = findCleanPosition();
            if (clean != null) this.getNavigation().moveTo(clean.getX() + 0.5, clean.getY(), clean.getZ() + 0.5, 1.0);
         }
      }
   }

   private void maintainCombat(LivingEntity target, long now) {
      this.faceToward(target.getEyePosition());
      double distance = this.distanceTo(target);
      boolean meleeTurn = this.getPersistentData().getBoolean(NEXT_MELEE);
      if (meleeTurn) {
         if (distance > MELEE_MODE_RANGE) {
            this.getNavigation().moveTo(target, 1.35);
            return;
         }
         if (cooldownReady(LAST_MELEE, now, adjustedMeleeCooldown())) {
            this.getPersistentData().putLong(LAST_MELEE, now);
            this.getPersistentData().putBoolean(NEXT_MELEE, false);
            this.getNavigation().stop();
            this.doHurtTarget(target);
         }
         return;
      }
      if (distance > 3.0) ServantEngagementService.maintainRangedPosition(this, target, now, 7.0, 11.0, 16.0, 1.1, "NightingaleRanged");
      if (distance <= 32.0 && cooldownReady(LAST_SHOT, now, adjustedShotCooldown())) {
         this.getPersistentData().putLong(LAST_SHOT, now);
         this.getPersistentData().putBoolean(NEXT_MELEE, true);
         this.triggerNamedActionAnimation("shoot");
         ServantVoiceHelper.tryPlayAttack(this);
         Vec3 aim = target.getEyePosition().subtract(this.getEyePosition()).normalize();
         NightingaleGunItem.fire(this, aim, 4.8F);
      }
   }

   private int adjustedShotCooldown() {
      return NightingaleSupportService.adjustActionTicks(this, 20, this.level().getGameTime());
   }

   private int adjustedMeleeCooldown() {
      return NightingaleSupportService.adjustActionTicks(this, 16, this.level().getGameTime());
   }

   private void useAngelCry(long now) {
      LivingEntity ally = NightingaleSupportService.findAngelCryTarget(this, now);
      this.setCurrentMp(this.getCurrentMp() - NightingaleRules.ANGEL_CRY_COST);
      this.getPersistentData().putLong(LAST_ANGEL_CRY, now);
      this.faceToward(ally.position());
      this.triggerNamedActionAnimation("buff");
      NightingaleSupportService.applyAngelCry(this, ally, now);
   }

   private boolean shouldStartNoblePhantasm() {
      float selfRatio = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
      float lowest = 1.0F;
      int half = 0;
      for (LivingEntity ally : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(15.0),
         entity -> entity.isAlive() && NightingaleSupportService.isAlly(this, entity) && !NightingaleSupportService.isPaleRider(entity))) {
         float ratio = ally.getHealth() / Math.max(1.0F, ally.getMaxHealth());
         lowest = Math.min(lowest, ratio);
         if (ratio <= 0.5F) half++;
      }
      return NightingaleRules.shouldUseNoblePhantasm(selfRatio, lowest, half);
   }

   private boolean canUseNoblePhantasm(long now) {
      return this.hasMasterNoblePhantasmPermission()
         && ServantNoblePhantasmResourceService.canAttemptNpcCast(this, NightingaleRules.NOBLE_PHANTASM_COST)
         && cooldownReady(LAST_NP, now, this.getPersistentData().getBoolean(NP_OVERDRAFT)
            ? NightingaleRules.NOBLE_PHANTASM_COOLDOWN * 2 : NightingaleRules.NOBLE_PHANTASM_COOLDOWN);
   }

   private boolean cooldownReady(String key, long now, int cooldown) {
      return !this.getPersistentData().contains(key) || now - this.getPersistentData().getLong(key) >= cooldown;
   }

   private void startNoblePhantasm(long now) {
      ServantNoblePhantasmResourceService.CastDecision resource =
         ServantNoblePhantasmResourceService.evaluateNpcCast(this, NightingaleRules.NOBLE_PHANTASM_COST);
      if (!resource.allowed() || ServantNoblePhantasmResourceService.isOverdraftWeak(this)) {
         return;
      }
      ServantNoblePhantasmResourceService.commitNpcCast(this, resource);
      this.getPersistentData().putBoolean(NP_OVERDRAFT, resource.overdraft());
      this.getPersistentData().putLong(NP_CAST_END, now + NightingaleRules.NOBLE_PHANTASM_WINDUP);
      this.triggerNamedActionAnimation("noble_phantasm");
      ServantVoiceHelper.tryPlayNightingaleNp(this);
   }

   private void finishNoblePhantasm(ServerLevel level, long now) {
      this.getPersistentData().remove(NP_CAST_END);
      this.getPersistentData().putLong(LAST_NP, now);
      NightingaleSupportService.createSafetyCircle(this, level, this.position(), now);
   }

   private static boolean isNormallyInnocent(LivingEntity target) {
      return target instanceof Player || target instanceof Villager || target instanceof Animal || target instanceof HumanNpcEntity;
   }

   private boolean isDirtyPosition(BlockPos position) {
      var state = this.level().getBlockState(position);
      var below = this.level().getBlockState(position.below());
      return state.is(Blocks.FIRE) || state.is(Blocks.COBWEB) || state.is(Blocks.SOUL_SAND) || below.is(Blocks.MUD) || below.is(Blocks.SOUL_SAND);
   }

   private BlockPos findCleanPosition() {
      BlockPos center = this.blockPosition();
      for (int attempt = 0; attempt < 18; attempt++) {
         BlockPos candidate = center.offset(this.random.nextIntBetweenInclusive(-6, 6), 0, this.random.nextIntBetweenInclusive(-6, 6));
         if (!isDirtyPosition(candidate) && this.level().getBlockState(candidate).isAir()
            && this.level().getBlockState(candidate.below()).isSolidRender(this.level(), candidate.below())) return candidate;
      }
      return null;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
   }
}
