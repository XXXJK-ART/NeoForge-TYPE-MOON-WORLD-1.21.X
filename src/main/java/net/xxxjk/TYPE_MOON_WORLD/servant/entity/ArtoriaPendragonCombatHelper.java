package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.Map;
import java.util.WeakHashMap;
import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaStateService;
import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaAbilityService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.AttackOrigin;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.CombatDamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ExpandingRingEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class ArtoriaPendragonCombatHelper {
   public static final String TAG_INVISIBLE_AIR_ACTIVE = "ArtoriaInvisibleAirActive";
   public static final String TAG_MANA_BURST_UNTIL = "ArtoriaManaBurstUntil";
   public static final String TAG_MANA_BURST_ACTIVE = "ArtoriaManaBurstActive";
   public static final String TAG_CHARISMA_UNTIL = "ArtoriaCharismaUntil";
   public static final String TAG_EXCALIBUR_WINDUP_UNTIL = "ArtoriaExcaliburWindupUntil";
   public static final String TAG_EXCALIBUR_RELEASE_UNTIL = "ArtoriaExcaliburReleaseUntil";
   public static final String TAG_HAS_AVALON = "ArtoriaHasAvalon";
   public static final String TAG_INVISIBLE_AIR_RELEASED = "ArtoriaInvisibleAirReleased";
   public static final String TAG_INVISIBLE_AIR_DAMAGE_BYPASS_UNTIL = "ArtoriaInvisibleAirDamageBypassUntil";
   public static final String TAG_WIND_REGATHER_UNTIL = "ArtoriaWindRegatherUntil";
   public static final String TAG_MANA_BURST_REOPEN_AT = "ArtoriaManaBurstReopenAt";
   public static final String TAG_INSTINCT_CHARGES = "ArtoriaInstinctCharges";
   public static final String TAG_INSTINCT_RECOVERY_UNTIL = "ArtoriaInstinctRecoveryUntil";
   public static final String TAG_EXCALIBUR_VISIBLE_UNTIL = "ArtoriaExcaliburVisibleUntil";

   private static final String TAG_LAST_ROYAL_COMBO = "ArtoriaLastRoyalCombo";

   private static final int EXCALIBUR_COOLDOWN = 3600;
   private static final ResourceLocation RIDING_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "artoria_riding_speed");
   private static final ResourceLocation RIDING_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "artoria_riding_armor");
   private static final ResourceLocation MANA_BURST_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "artoria_mana_burst_attack");
   private static final ResourceLocation MANA_BURST_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "artoria_mana_burst_speed");
   private static final ResourceLocation EXCALIBUR_REVEALED_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "artoria_revealed_excalibur_attack");
   private static final ResourceLocation CHARISMA_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "artoria_charisma_attack");
   private static final String TAG_LAST_PERSISTENT_TICK = "ArtoriaLastPersistentStateTick";
   private static final String TAG_PERSISTENT_BUSY = "ArtoriaPersistentStateBusy";
   private static final Map<DamageSource, LivingEntity> PENDING_BURST_HITS = new WeakHashMap<>();

   private ArtoriaPendragonCombatHelper() {
   }

   public static boolean tick(ArtoriaPendragonEntity entity, ServantAiContext context) {
      if (!(entity.level() instanceof ServerLevel level)) return false;
      if (tickPersistentState(entity)) return true;
      LivingEntity target = context.target();
      if (!ArtoriaAbilityService.enemy(entity, target) || ServantCombatSystem.skillsSuppressed(entity)
         || ServantCombatSystem.cannotAct(entity)) return false;
      if (ArtoriaAbilityService.actionLocked(entity)) return true;
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      entity.getLookControl().setLookAt(target, 40, 90);
      double distance = entity.distanceTo(target);
      if (entity.getHealth() <= entity.getMaxHealth() * .35F)
         ServantCombatSystem.forcePhaseAtLeast(entity, ServantCombatPhase.DECISIVE);
      var phase = ServantCombatSystem.getPhase(entity);
      if (ArtoriaStateService.tryStartNpcWindDefense(entity, level)) {
         entity.getNavigation().stop();
         return true;
      }
      if (phase.id() >= ServantCombatPhase.NORMAL.id() && tryManaBurst(entity, level, data, now)) return true;
      boolean windHammerRange = distance > 5.8 && distance <= 12.0;
      if (windHammerRange && ArtoriaRules.npcPrioritizesWindHammer(phase, entity.getRandom().nextFloat())
         && tryInvisibleAir(entity, target, level, data, now)) return true;
      if (phase == ServantCombatPhase.DECISIVE && tryStartExcalibur(entity, target, level, data, now)) return true;
      if (distance >= 3.8 && distance <= 5.8
         && ArtoriaAbilityService.tryNpcSkill(entity, ArtoriaRules.INTERCEPT_THRUST)) return true;
      if (distance <= 5.8) {
         if (entity.getLastHurtByMob() == target && entity.tickCount - entity.getLastHurtByMobTimestamp() <= 8
            && ArtoriaAbilityService.tryNpcSkill(entity, ArtoriaRules.GUARD_COUNTER)) return true;
         if (phase == ServantCombatPhase.DECISIVE && ArtoriaAbilityService.tryNpcSkill(entity, ArtoriaRules.KNIGHT_HEAVY_SLASH)) return true;
         if (tryNpcSwordSkill(entity, distance)) return true;
      }
      if (distance <= 5.5 && tryInvisibleAirRelease(entity, target, level, data, now)) return true;
      if (phase == ServantCombatPhase.DECISIVE && distance > 10.5 && distance <= ArtoriaRules.EXCALIBUR_SHORT_RANGE && entity.hasLineOfSight(target)
          && entity.getCurrentMp() >= ArtoriaRules.EXCALIBUR_SHORT_MP
          && now >= data.getLong("ArtoriaSkillReady:" + ArtoriaRules.EXCALIBUR_SHORT_SKILL)) {
         faceExcaliburTarget(entity, target);
         if (ArtoriaAbilityService.tryNpcLightCannon(entity)) return true;
      }
      return tryArtoriaSmallSkill(entity, target, level, data, now, distance);
   }

   /** Try the whole close-range rotation so one cooldown cannot make the NPC idle. */
   private static boolean tryNpcSwordSkill(ArtoriaPendragonEntity entity, double distance) {
      if (distance > 5.0) return false;
      String[] rotation = {ArtoriaRules.KNIGHT_TRIPLE_SLASH, ArtoriaRules.SPINNING_SLASH, ArtoriaRules.UPWARD_SLASH};
      int start = Math.floorMod(entity.tickCount / 20, rotation.length);
      for (int offset = 0; offset < rotation.length; offset++) {
         if (ArtoriaAbilityService.tryNpcSkill(entity, rotation[(start + offset) % rotation.length])) return true;
      }
      return false;
   }

   public static boolean hasAvalon(LivingEntity entity) {
      if (entity instanceof ArtoriaPendragonEntity && entity.getPersistentData().getBoolean(TAG_HAS_AVALON)) {
         return true;
      }
      return entity instanceof Player player && hasActiveAvalonInInventory(player);
   }

   private static boolean hasActiveAvalonInInventory(Player player) {
      for (ItemStack stack : player.getInventory().items) {
         if (isActiveAvalonFor(player, stack)) {
            return true;
         }
      }
      for (ItemStack stack : player.getInventory().offhand) {
         if (isActiveAvalonFor(player, stack)) {
            return true;
         }
      }
      return false;
   }

   private static boolean isActiveAvalonFor(Player player, ItemStack stack) {
      if (!stack.is(ModItems.AVALON.get())) {
         return false;
      }
      if (AvalonItem.isAvalonActivated(stack)) {
         return true;
      }
      if (player instanceof ServerPlayer serverPlayer) {
         TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.servant_card_transformed && "artoria_pendragon".equals(vars.servant_card_id);
      }
      return false;
   }

   public static boolean tickPersistentState(ArtoriaPendragonEntity entity) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) return false;
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (data.contains(TAG_LAST_PERSISTENT_TICK) && data.getLong(TAG_LAST_PERSISTENT_TICK) == now) {
         return data.getBoolean(TAG_PERSISTENT_BUSY);
      }
      data.putLong(TAG_LAST_PERSISTENT_TICK, now);
      tickAvalon(entity, level);
      tickLakeProtection(entity);
      syncRidingModifiers(entity, entity.isPassenger());
      tickTimedModifiers(entity, now);
      tickInstinctRecovery(entity, now);
      tickInvisibleAirCleanup(data, now);
      syncExcaliburVisibility(entity);
      tickInvisibleAirWrapVfx(entity, level, data, now);
      boolean busy = tickExcaliburState(entity, entity.getTarget(), level, data, now);
      data.putBoolean(TAG_PERSISTENT_BUSY, busy);
      return busy;
   }

   public static boolean isInvisibleAirActive(ArtoriaPendragonEntity entity) {
      if (entity == null) {
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      return data.getBoolean(TAG_INVISIBLE_AIR_ACTIVE) && data.getLong(TAG_INVISIBLE_AIR_ACTIVE + "Until") > entity.level().getGameTime();
   }

   public static boolean isInvisibleAirRevealed(LivingEntity entity) {
      if (entity == null) {
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      return data.getBoolean(TAG_INVISIBLE_AIR_ACTIVE) && data.getLong(TAG_INVISIBLE_AIR_ACTIVE + "Until") > entity.level().getGameTime();
   }

   public static boolean hasReleasedInvisibleAir(ArtoriaPendragonEntity entity) {
      return entity != null && entity.getPersistentData().getBoolean(TAG_INVISIBLE_AIR_RELEASED);
   }

   public static void syncExcaliburVisibility(ArtoriaPendragonEntity entity) {
      if (entity == null || entity.level().isClientSide()) return;
      long now = entity.level().getGameTime();
      boolean visible = shouldRenderExcalibur(entity, entity.getPersistentData(), now);
      entity.setExcaliburVisible(visible);
      if (!entity.getMainHandItem().is(ModItems.EXCALIBUR.get())) {
         entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.EXCALIBUR.get()));
         entity.setDropChance(EquipmentSlot.MAINHAND, 0);
      }
      long until = visible ? Math.max(now + 2, entity.getPersistentData().getLong(TAG_EXCALIBUR_VISIBLE_UNTIL)) : 0;
      ArtoriaStateService.syncSword(entity.getMainHandItem(), visible, until, isManaBurstActive(entity), ArtoriaStateService.isWindDefenseActive(entity));
   }

   private static boolean shouldRenderExcalibur(ArtoriaPendragonEntity entity, CompoundTag data, long now) {
      if (ArtoriaStateService.isWindDefenseActive(entity)) return false;
      return !ArtoriaStateService.isWindBarrierActive(entity)
         || ArtoriaStateService.busy(entity) || data.getLong(TAG_EXCALIBUR_VISIBLE_UNTIL) > now;
   }

   public static boolean isManaBurstActive(LivingEntity entity) {
      return entity != null && (entity.getPersistentData().getBoolean(TAG_MANA_BURST_ACTIVE)
         || entity.getPersistentData().getLong(TAG_MANA_BURST_UNTIL) > entity.level().getGameTime());
   }

   public static void syncManaBurstModifiers(LivingEntity entity) {
      if (entity == null || entity.level().isClientSide()) return;
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), MANA_BURST_ATTACK_ID, 0.0,
         AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.ATTACK_SPEED), MANA_BURST_SPEED_ID, 0.0,
         AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), MANA_BURST_SPEED_ID,
         isManaBurstActive(entity) ? 0.15 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), EXCALIBUR_REVEALED_ATTACK_ID, 0.0,
         AttributeModifier.Operation.ADD_VALUE);
   }

   public static float applyManaBurstDefense(LivingEntity target, DamageSource source, float amount) {
      if (!ArtoriaStateService.isArtoria(target) || source == null || amount <= 0 || !isManaBurstActive(target)
         || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
         || source.is(net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes.BYPASSES_DEFENSES)
         || source instanceof CombatDamageSource hit && hit.bypassesMagicDefense()
         || target.getPersistentData().getLong(net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidCombatHelper.MAGIC_DEFENSE_BROKEN_TAG) > target.level().getGameTime()) return amount;
      if (!(source instanceof CombatDamageSource) && source.getDirectEntity() instanceof LivingEntity attacker
         && attacker.getMainHandItem().getItem() instanceof net.xxxjk.TYPE_MOON_WORLD.item.custom.DiarmuidSpearItem spear
         && spear.spearType() == net.xxxjk.TYPE_MOON_WORLD.item.custom.DiarmuidSpearItem.SpearType.GAE_DEARG
         && net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidCombatHelper.isRedActive(attacker)) return amount;
      return amount * (float)ArtoriaRules.BURST_DEFENSE_MULTIPLIER;
   }

   public static boolean isExcaliburWindingOrReleasing(LivingEntity entity) {
      return entity != null && ArtoriaStateService.isArtoria(entity) && ArtoriaStateService.busy(entity);
   }

   public static boolean tryNegateCertainHitOrDeath(LivingEntity target, String reason) {
      return target != null && ArtoriaStateService.negateSpecial(target, reason + ":" + target.level().getGameTime());
   }
   public static boolean tryNegateCertainHitOrDeath(LivingEntity target, String reason, Object attackToken) {
      return target != null && ArtoriaStateService.negateSpecial(target, attackToken);
   }

   public static boolean tryDodgeProjectileWithInstinct(ArtoriaPendragonEntity artoria, Projectile projectile) {
      return false; // All ordinary hits use the shared counted dodge transaction.
   }

   public static void tickInstinctRecovery(LivingEntity entity, long now) {
      ArtoriaStateService.syncInstinct(entity);
   }

   public static float applyAvalonDamageReduction(LivingEntity target, DamageSource source, float amount) {
      if (amount <= 0.0F || !hasAvalon(target)) {
         return amount;
      }
      tryProtectWithAvalon(target);
      return 0.0F;
   }

   public static boolean tryProtectWithAvalon(LivingEntity target) {
      if (target == null || !hasAvalon(target)) {
         return false;
      }
      CompoundTag data = target.getPersistentData();
      data.remove("CausalSevered");
      data.remove("MasterLossForcedDeath");
      data.remove("MasterLossDecayDamage");
      target.clearFire();
      target.setHealth(target.getMaxHealth());
      spawnAvalonFx(target);
      return true;
   }

   public static boolean tryNegateMedeaSmallMagic(ArtoriaPendragonEntity entity, DamageSource source, float amount) {
      return false; // Magic Resistance A belongs to the shared damage pipeline.
   }

   public static float applyManaBurstOutgoing(LivingEntity attacker, float amount) {
      return applyManaBurstOutgoing(attacker, null, amount);
   }

   public static float applyManaBurstOutgoing(LivingEntity attacker, DamageSource source, float amount) {
      return applyManaBurstOutgoing(attacker, source, amount, null);
   }

   /** Production damage path: defer the 20 MP debit until the hit survives all defenses. */
   public static float applyManaBurstOutgoing(LivingEntity attacker, LivingIncomingDamageEvent event) {
      return applyManaBurstOutgoing(attacker, event.getSource(), event.getAmount(), event.getSource());
   }

   private static float applyManaBurstOutgoing(LivingEntity attacker, DamageSource source, float amount,
                                                DamageSource pendingKey) {
      if (!ArtoriaStateService.isArtoria(attacker) || !isManaBurstActive(attacker) || source == null || amount <= 0) return amount;
      String skill = AttackOrigin.of(source).skill();
      boolean swordHit = source.getDirectEntity() == attacker && ("artoria_small_combo".equals(skill)
         || ArtoriaRules.KNIGHT_TRIPLE_SLASH.equals(skill) || ArtoriaRules.INTERCEPT_THRUST.equals(skill)
         || ArtoriaRules.SPINNING_SLASH.equals(skill) || ArtoriaRules.UPWARD_SLASH.equals(skill)
         || ArtoriaRules.GUARD_COUNTER.equals(skill) || ArtoriaRules.KNIGHT_HEAVY_SLASH.equals(skill)
         || "basic".equals(skill) && attacker.getMainHandItem().is(ModItems.EXCALIBUR.get()));
      if (!swordHit || !isExcaliburVisible(attacker)) return amount;
      if (!ArtoriaStateService.canConsume(attacker, ArtoriaRules.BURST_SWORD_HIT_COST)) {
         ArtoriaStateService.stopBurst(attacker);
         return amount;
      }
      if (pendingKey == null) {
         if (!ArtoriaStateService.consume(attacker, ArtoriaRules.BURST_SWORD_HIT_COST)) {
            ArtoriaStateService.stopBurst(attacker);
            return amount;
         }
      } else {
         PENDING_BURST_HITS.put(pendingKey, attacker);
      }
      return amount * (float)ArtoriaRules.BURST_SWORD_MULTIPLIER;
   }

   @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST, receiveCanceled = true)
   public static void clearCanceledBurstHit(LivingIncomingDamageEvent event) {
      if (event.isCanceled() || event.getAmount() <= 0.0F) PENDING_BURST_HITS.remove(event.getSource());
   }

   @SubscribeEvent
   public static void debitBurstHit(LivingDamageEvent.Post event) {
      LivingEntity attacker = PENDING_BURST_HITS.remove(event.getSource());
      if (attacker == null || event.getNewDamage() <= 0.0F) return;
      if (!ArtoriaStateService.consume(attacker, ArtoriaRules.BURST_SWORD_HIT_COST)) ArtoriaStateService.stopBurst(attacker);
   }

   public static float applyRevealedSwordBonus(LivingEntity attacker, DamageSource source, float amount) {
      // Excalibur's exposed blade no longer adds a separate damage component;
      // the tenfold multiplier is owned by Mana Burst in applyManaBurstOutgoing.
      return amount;
   }

   public static boolean isExcaliburVisible(LivingEntity entity) {
      if (entity == null) return false;
      return !ArtoriaStateService.isWindBarrierActive(entity)
         || ArtoriaStateService.busy(entity)
         || entity.getPersistentData().getLong(TAG_EXCALIBUR_VISIBLE_UNTIL) > entity.level().getGameTime()
         || entity instanceof ServerPlayer player && net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper.isArtoriaExcaliburRevealed(player);
   }

   public static void tickLakeProtection(LivingEntity entity) {
      if (entity == null || entity.level().isClientSide() || entity.isShiftKeyDown() || entity.isPassenger()) {
         return;
      }
      BlockPos feet = BlockPos.containing(entity.getX(), entity.getY() - 0.08, entity.getZ());
      BlockPos below = feet.below();
      boolean waterAtFeet = entity.level().getFluidState(feet).is(FluidTags.WATER);
      boolean waterBelow = entity.level().getFluidState(below).is(FluidTags.WATER);
      if (!waterAtFeet && !waterBelow) {
         return;
      }
      double surfaceY = (waterAtFeet ? feet.getY() : below.getY()) + 1.02;
      double lift = Mth.clamp((surfaceY - entity.getY()) * 0.42, -0.04, 0.18);
      Vec3 motion = entity.getDeltaMovement();
      entity.setDeltaMovement(motion.x, Math.max(motion.y, lift), motion.z);
      entity.setOnGround(true);
      entity.fallDistance = 0.0F;
      if (entity.tickCount % 10 == 0 && entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SPLASH, entity.getX(), surfaceY - 0.05, entity.getZ(), fxCount(4), 0.28, 0.02, 0.28, 0.02);
         level.sendParticles(ParticleTypes.END_ROD, entity.getX(), surfaceY + 0.02, entity.getZ(), fxCount(2), 0.22, 0.02, 0.22, 0.005);
      }
   }

   public static void tickSharedBuffCleanup(LivingEntity entity) {
      if (entity == null || entity.level().isClientSide()) {
         return;
      }
      long charismaUntil = entity.getPersistentData().getLong(TAG_CHARISMA_UNTIL);
      if (charismaUntil > 0L && charismaUntil <= entity.level().getGameTime()) {
         removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), CHARISMA_ATTACK_ID);
         entity.getPersistentData().remove(TAG_CHARISMA_UNTIL);
      }
   }

   public static void spawnAvalonFx(LivingEntity entity) {
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.64, entity.getZ(), fxCount(12), 0.22, 0.28, 0.22, 0.02);
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + entity.getBbHeight() * 0.48, entity.getZ(), fxCount(6), 0.22, 0.26, 0.22, 0.03);
         level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.85F, 1.45F);
      }
   }

   private static void tickAvalon(ArtoriaPendragonEntity entity, ServerLevel level) {
      if (!hasAvalon(entity) || !entity.isAlive()) {
         return;
      }
      entity.heal(entity.getMaxHealth() * 0.05F);
      spawnAvalonPassiveFx(entity, level);
   }

   private static void spawnAvalonPassiveFx(ArtoriaPendragonEntity entity, ServerLevel level) {
      double time = entity.tickCount * 0.32;
      double cx = entity.getX();
      double cy = entity.getY();
      double cz = entity.getZ();
      double height = entity.getBbHeight();

      level.sendParticles(ParticleTypes.CLOUD, cx, cy + height * 0.54, cz, fxCount(3), 0.32, 0.26, 0.32, 0.015);
      level.sendParticles(ParticleTypes.END_ROD, cx, cy + height * 0.66, cz, fxCount(2), 0.24, 0.28, 0.24, 0.015);
      level.sendParticles(ParticleTypes.END_ROD, cx, cy + 0.18, cz, fxCount(3), 0.42, 0.06, 0.42, 0.03);

      for (int i = 0; i < fxCount(8); i++) {
         double angle = time + i * Math.PI * 0.5;
         double radius = 0.55 + 0.12 * Math.sin(time * 0.7 + i);
         double y = cy + 0.25 + (i % 4) * height * 0.23;
         double x = cx + Math.cos(angle) * radius;
         double z = cz + Math.sin(angle) * radius;
         double swirlX = -Math.sin(angle) * 0.018;
         double swirlZ = Math.cos(angle) * 0.018;
         level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.015, 0.015, 0.015, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, x, y + 0.05, z, 1, swirlX, 0.022, swirlZ, 0.0);
      }

      if (entity.tickCount % 16 == 0) {
         level.sendParticles(ParticleTypes.ENCHANT, cx, cy + height * 0.6, cz, fxCount(6), 0.45, 0.32, 0.45, 0.025);
      }
      if (entity.tickCount % 28 == 0) {
         level.addFreshEntity(new ExpandingRingEffectEntity(level, cx, cy + 0.16, cz, 0.18F, 1.65F, 0.04F, 14, 0xEFFFF8, 0.18F, 0.014F));
         level.addFreshEntity(new ExpandingRingEffectEntity(level, cx, cy + height * 0.52, cz, 0.12F, 1.0F, 0.03F, 12, 0xCFFFEF, 0.12F, 0.01F, 82.0F, (entity.tickCount * 7) % 360));
      }
   }

   public static void syncRidingModifiers(LivingEntity entity, boolean riding) {
      updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), RIDING_SPEED_ID, riding ? 0.20 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.ARMOR), RIDING_ARMOR_ID, riding ? 0.10 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
   }

   private static void tickTimedModifiers(ArtoriaPendragonEntity entity, long now) {
      ArtoriaStateService.tick(entity);
      if (isManaBurstActive(entity) && entity.level() instanceof ServerLevel level) {
         tickManaBurstJetMovement(entity, level);
      }
   }

   private static void tickManaBurstJetMovement(ArtoriaPendragonEntity entity, ServerLevel level) {
      LivingEntity target = entity.getTarget();
      Vec3 desired = Vec3.ZERO;
      boolean lift = entity.horizontalCollision;
      if (ArtoriaAbilityService.enemy(entity, target)) {
         Vec3 offset = target.position().subtract(entity.position());
         if (offset.horizontalDistance() > 2.35) desired = offset.multiply(1, 0, 1);
         lift |= offset.y > .75 && offset.horizontalDistance() < 13;
      }
      if (!ArtoriaStateService.jet(entity, desired, lift, false) || entity.tickCount % 4 != 0) return;
      Vec3 exhaust = desired.lengthSqr() > 1.0E-4
         ? desired.normalize().scale(-1.0)
         : lift ? new Vec3(0.0, -1.0, 0.0) : entity.getDeltaMovement().multiply(-1.0, 0.0, -1.0);
      if (exhaust.lengthSqr() < 1.0E-4) exhaust = new Vec3(0.0, -1.0, 0.0);
      VFXServerEffects.spawnOrientedScaled(level, "artoria_mana_burst_jet",
         entity.position().add(0.0, 0.18, 0.0), exhaust, 64.0, 1.0F);
   }

   private static Vec3 clampHorizontal(Vec3 motion, double cap) {
      double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
      if (horizontal <= cap || horizontal <= 1.0E-6) {
         return motion;
      }
      double scale = cap / horizontal;
      return new Vec3(motion.x * scale, motion.y, motion.z * scale);
   }

   private static void tickInvisibleAirCleanup(CompoundTag data, long now) {
      if (data.getLong(TAG_INVISIBLE_AIR_ACTIVE + "Until") <= now) {
         data.putBoolean(TAG_INVISIBLE_AIR_ACTIVE, false);
      }
   }

   private static boolean tickExcaliburState(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      if (!ArtoriaStateService.busy(entity)) {
         data.remove(TAG_EXCALIBUR_WINDUP_UNTIL); data.remove(TAG_EXCALIBUR_RELEASE_UNTIL); return false;
      }
      entity.getNavigation().stop();
      entity.setDeltaMovement(entity.getDeltaMovement().multiply(.35, 1, .35));
      if (ArtoriaAbilityService.enemy(entity, target)) faceExcaliburTarget(entity, target);
      if (NoblePhantasmCastService.chanting(entity) && now % 32 == 0)
         VFXServerEffects.spawn(level, "artoria_excalibur_charge", entity, 128);
      return true;
   }

   private static boolean tryStartExcalibur(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      if (now < data.getLong(ArtoriaStateService.NP_READY) || entity.getCurrentMp() < ArtoriaRules.EXCALIBUR_MP
         || entity.distanceTo(target) > 150 || !entity.hasLineOfSight(target)) return false;
      var opportunity = net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantCombatAssessment.opportunity(entity, target);
      if (!opportunity.favorsNoblePhantasm(entity.distanceTo(target), 152)) return false;
      Vec3 aim = target.getBoundingBox().getCenter().subtract(entity.getEyePosition()).normalize();
      for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(150),
         e -> e != entity && e.isAlive() && !ArtoriaAbilityService.enemy(entity, e))) {
         Vec3 rel = ally.getBoundingBox().getCenter().subtract(entity.getEyePosition());
         double along = rel.dot(aim);
         if (along > 0 && along < 150 && rel.subtract(aim.scale(along)).lengthSqr() < 225) return false;
      }
      if (!ArtoriaRules.npcMayUseExcalibur(ServantCombatSystem.getPhase(entity), entity.getRandom().nextFloat())) return false;
      if (!ArtoriaStateService.consume(entity, ArtoriaRules.EXCALIBUR_MP)) return false;
      NoblePhantasmCastService.noteNpcPayment(entity, ArtoriaRules.EXCALIBUR_MP);
      data.putLong(ArtoriaStateService.NP_READY, now + ArtoriaRules.EXCALIBUR_COOLDOWN);
      NoblePhantasmCastService.begin(entity, "artoria_excalibur", ArtoriaRules.EXCALIBUR_COOLDOWN,
         ticks -> data.putLong(ArtoriaStateService.NP_READY, entity.level().getGameTime() + ticks),
         () -> { data.remove(TAG_EXCALIBUR_WINDUP_UNTIL); data.remove(TAG_EXCALIBUR_VISIBLE_UNTIL); });
      int windup = (int)(NoblePhantasmCastService.releaseAt(entity) - now);
      data.putLong(TAG_EXCALIBUR_WINDUP_UNTIL, now + windup);
      ArtoriaStateService.autoReleaseWindBarrier(entity);
      TYPE_MOON_WORLD.queueServerWork(20, () -> {
         if (NoblePhantasmCastService.chanting(entity)) ArtoriaStateService.autoReleaseWindBarrier(entity);
      });
      ArtoriaAbilityService.reveal(entity, windup);
      net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaExcaliburAnimationService.play(
         entity, net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaExcaliburAction.FULL_NP);
      entity.triggerChargeAnimation(windup);
      ServantCombatSystem.broadcastNoblePhantasmWindup(entity, target, windup, true);
      NoblePhantasmCastService.deferEffect(entity, () -> releaseExcalibur(entity, entity.getTarget(), level, data, level.getGameTime()));
      return true;
   }

   private static void releaseExcalibur(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      data.remove(TAG_EXCALIBUR_WINDUP_UNTIL);
      if (!entity.isAlive()) return;
      data.putLong(TAG_EXCALIBUR_RELEASE_UNTIL, now + ArtoriaRules.EXCALIBUR_TICKS);
      ArtoriaAbilityService.reveal(entity, ArtoriaRules.EXCALIBUR_TICKS + ArtoriaRules.REVEAL_AFTER_NP);
      Vec3 start = entity.position().add(0, entity.getBbHeight() * .66, 0).add(excaliburLook(entity).scale(1.2));
      ArtoriaExcaliburBeamEntity beam = new ArtoriaExcaliburBeamEntity(level, entity, start, ArtoriaRules.EXCALIBUR_TICKS, 0, 1);
      data.putUUID(ArtoriaStateService.NP_BEAM, beam.getUUID()); level.addFreshEntity(beam);
      VFXServerEffects.spawn(level, "artoria_excalibur_beam", entity, 192);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.5F, .85F);
   }

   private static boolean tryArtoriaSmallSkill(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now, double distance) {
      if (distance > 16 || ArtoriaAbilityService.comboTarget(entity) == null
         || !consumeSmallSkill(entity, data, now, TAG_LAST_ROYAL_COMBO, 30, 16)) return false;
       entity.faceToward(target.position());
       ArtoriaAbilityService.combo(entity);
       ServantVoiceHelper.tryPlayArtoriaSkill(entity);
       return true;
   }

   private static boolean tryManaBurst(ArtoriaPendragonEntity entity, ServerLevel level, CompoundTag data, long now) {
      if (isManaBurstActive(entity) || now < data.getLong(TAG_MANA_BURST_REOPEN_AT)
         || !ArtoriaStateService.consume(entity, ArtoriaRules.BURST_START_MP)) return false;
       ArtoriaStateService.startBurst(entity);
       spawnManaBurstActivationFx(entity, level);
       ServantVoiceHelper.tryPlayArtoriaSkill(entity);
       return true;
   }

   private static boolean tryInvisibleAir(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      if (!ArtoriaAbilityService.canWind(entity)
         || now < data.getLong(ArtoriaStateService.WIND_HAMMER_READY)
         || !entity.hasLineOfSight(target) || !ArtoriaStateService.consume(entity, 60)) return false;
      entity.faceToward(target.position());
      if (!ArtoriaAbilityService.hammer(entity)) {
         entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + 60.0));
         return false;
      }
      ServantVoiceHelper.tryPlayArtoriaInvisibleAir(entity);
      return true;
   }

   private static boolean tryInvisibleAirRelease(ArtoriaPendragonEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      if (!ArtoriaAbilityService.canWind(entity)
         || now < data.getLong(ArtoriaStateService.WIND_RELEASE_READY)
         || !entity.hasLineOfSight(target) || entity.getRandom().nextFloat() > .34F
         || !ArtoriaStateService.consume(entity, 28)) return false;
      if (!ArtoriaAbilityService.windRelease(entity)) return false;
      ServantVoiceHelper.tryPlayArtoriaInvisibleAir(entity);
      return true;
   }

   private static void tickInvisibleAirWrapVfx(ArtoriaPendragonEntity entity, ServerLevel level, CompoundTag data, long now) {
      // The held-item renderer draws translucent wind in the sword pose.
   }

   private static void spawnManaBurstActivationFx(ArtoriaPendragonEntity entity, ServerLevel level) {
      VFXServerEffects.spawn(level, "artoria_mana_burst_aura", entity, 96);
   }

   private static boolean consumeSmallSkill(ArtoriaPendragonEntity entity, CompoundTag data, long now, String tag, int cooldown, double mpCost) {
      if (entity.getCurrentMp() < mpCost || data.contains(tag) && now - data.getLong(tag) < cooldown) {
         return false;
      }
      data.putLong(tag, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - mpCost));
      return true;
   }

   private static Vec3 horizontalLook(LivingEntity entity) {
      Vec3 look = entity.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      return horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }

   public static Vec3 excaliburLook(LivingEntity entity) {
      Vec3 look = entity.getLookAngle();
      if (look.lengthSqr() < 1.0E-4) {
         return new Vec3(0.0, 0.0, 1.0);
      }
      return look.normalize();
   }

   private static void faceExcaliburTarget(ArtoriaPendragonEntity entity, LivingEntity target) {
      Vec3 dir = target.getBoundingBox().getCenter().subtract(entity.getEyePosition()).normalize();
      float yaw = (float)Math.toDegrees(Math.atan2(-dir.x, dir.z));
      float pitch = (float)-Math.toDegrees(Math.asin(Math.clamp(dir.y, -1, 1)));
      entity.setYRot(yaw); entity.setYHeadRot(yaw); entity.setXRot(pitch);
   }

   private static int fxCount(int count) {
      return Math.max(1, Math.round(count * 0.67F));
   }

   private static void spawnInstinctFx(ArtoriaPendragonEntity entity, boolean avalon) {
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(avalon ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.CRIT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ(), avalon ? 22 : 12, 0.35, 0.35, 0.35, 0.03);
         level.playSound(null, entity.blockPosition(), avalon ? SoundEvents.SHIELD_BLOCK : SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.9F, avalon ? 1.45F : 1.8F);
      }
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
      if (attribute == null) {
         return;
      }
      if (Math.abs(amount) < 1.0E-6) {
         removeModifier(attribute, id);
         return;
      }
      AttributeModifier existing = attribute.getModifier(id);
      if (existing != null && existing.operation() == operation && Math.abs(existing.amount() - amount) < 1.0E-6) {
         return;
      }
      if (existing != null) {
         attribute.removeModifier(id);
      }
      attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null && attribute.getModifier(id) != null) {
         attribute.removeModifier(id);
      }
   }
}
