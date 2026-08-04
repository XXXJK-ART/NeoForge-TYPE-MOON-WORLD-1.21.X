package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.ChainsOfHeavenBindingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EnkiduEarthWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantFlightHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatFormulas;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;

public final class EnkiduCombatHelper {
   private static final int VOLLEY_EFFECT_STRIDE = 4;
   private static final String TAG_MODE = "EnkiduMode";
   private static final String TAG_LAST_TRANSFIGURATION = "EnkiduLastTransfiguration";
   private static final String TAG_LAST_PRESENCE = "EnkiduLastPresence";
   private static final String TAG_LAST_PERFECT_FORM = "EnkiduLastPerfectForm";
   private static final String TAG_REGEN_UNTIL = "EnkiduPerfectFormUntil";
   private static final String TAG_NEXT_REGEN = "EnkiduNextRegen";
   private static final String TAG_LAST_CHAIN = "EnkiduLastChain";
   private static final String TAG_LAST_SMALL_WEAPON = "EnkiduLastSmallWeapon";
   private static final String TAG_LAST_BIG_VOLLEY = "EnkiduLastBigVolley";
   private static final String TAG_LAST_MEGA_VOLLEY = "EnkiduLastMegaVolley";
   private static final String TAG_SMALL_WEAPON_TARGET = "EnkiduSmallWeaponTarget";
   private static final String TAG_SMALL_WEAPON_TOKEN = "EnkiduSmallWeaponToken";
   private static final String TAG_BIG_VOLLEY_TARGET = "EnkiduBigVolleyTarget";
   private static final String TAG_BIG_VOLLEY_TOKEN = "EnkiduBigVolleyToken";
   private static final String TAG_LAST_CHAIN_LASH = "EnkiduLastChainLash";
   private static final String TAG_LAST_EARTH_SPIKE = "EnkiduLastEarthSpike";
   private static final String TAG_LAST_SKY_SPEAR_SWEEP = "EnkiduLastSkySpearSweep";
   private static final String TAG_LAST_EARTH_WEDGE = "EnkiduLastEarthWedge";
   private static final String TAG_LAST_CLAY_BULWARK = "EnkiduLastClayBulwark";
   private static final String TAG_LAST_STARDUST_STEP = "EnkiduLastStardustStep";
   private static final String TAG_LAST_NATURE_PULSE = "EnkiduLastNaturePulse";
   private static final String TAG_LAST_MELEE_BASIC = "EnkiduLastMeleeBasic";
   private static final String TAG_LAST_BOUND_ASSAULT = "EnkiduLastBoundAssault";
   private static final String TAG_LAST_BOUND_BARRAGE = "EnkiduLastBoundBarrage";
   private static final String TAG_LAST_MELEE_LUNGE = "EnkiduLastMeleeLunge";
   private static final String TAG_LAST_CLAY_BODY = "EnkiduLastClayBody";
   private static final String TAG_MELEE_COMBO_STEP = "EnkiduMeleeComboStep";
   private static final String TAG_LAST_ENUMA = "EnkiduLastEnuma";
   private static final String TAG_ENUMA_RELEASE = "EnkiduEnumaRelease";
   private static final String TAG_ENUMA_FINISH = "EnkiduEnumaFinish";
   private static final String TAG_ENUMA_TARGET = "EnkiduEnumaTarget";
   private static final String TAG_ENUMA_DAMAGE_DONE = "EnkiduEnumaDamageDone";
   private static final String TAG_ENUMA_INVISIBLE = "EnkiduEnumaInvisible";
   private static final String TAG_ENUMA_PREV_INVISIBLE = "EnkiduEnumaPrevInvisible";
   private static final String TAG_ENUMA_START_X = "EnkiduEnumaStartX";
   private static final String TAG_ENUMA_START_Y = "EnkiduEnumaStartY";
   private static final String TAG_ENUMA_START_Z = "EnkiduEnumaStartZ";
   private static final String TAG_ENUMA_BIND_STEP = "EnkiduEnumaBindStep";
   private static final String TAG_ENUMA_NEXT_BIND = "EnkiduEnumaNextBind";
   private static final String TAG_ENUMA_LAST_FLIGHT_FX = "EnkiduEnumaLastFlightFx";
   private static final String TAG_FLIGHT_UNTIL = "EnkiduFlightUntil";
   private static final String TAG_LAND_UNTIL = "EnkiduLandUntil";
   private static final String TAG_FLIGHT_WAS_AIRBORNE = "EnkiduFlightWasAirborne";
   private static final String TAG_FLIGHT_LAST_CONTACT = "EnkiduFlightLastContact";
   private static final String TAG_NEXT_FLIGHT_TOGGLE = "EnkiduNextFlightToggle";
   private static final String TAG_UNREACHABLE_TICKS = "EnkiduUnreachableTicks";
   private static final String TAG_ENUMA_IMPACT_X = "EnkiduEnumaImpactX";
   private static final String TAG_ENUMA_IMPACT_Y = "EnkiduEnumaImpactY";
   private static final String TAG_ENUMA_IMPACT_Z = "EnkiduEnumaImpactZ";
   private static final String TAG_ENUMA_STAGE = "EnkiduEnumaStage";
   private static final String TAG_ENUMA_GROUND_X = "EnkiduEnumaGroundX";
   private static final String TAG_ENUMA_GROUND_Y = "EnkiduEnumaGroundY";
   private static final String TAG_ENUMA_GROUND_Z = "EnkiduEnumaGroundZ";
   private static final String TAG_ENUMA_DIR_X = "EnkiduEnumaDirX";
   private static final String TAG_ENUMA_DIR_Y = "EnkiduEnumaDirY";
   private static final String TAG_ENUMA_DIR_Z = "EnkiduEnumaDirZ";
   private static final String TAG_ENUMA_DUEL_FINALE = "EnkiduEnumaGilgameshFinale";
   private static final String TAG_BOUND_UNTIL = "EnkiduBoundUntil";
   private static final String TAG_BOUND_OWNER = "EnkiduBoundOwner";
   private static final String TAG_BOUND_PREV_NO_AI = "EnkiduBoundPrevNoAi";
   private static final String TAG_BOUND_X = "EnkiduBoundX";
   private static final String TAG_BOUND_Y = "EnkiduBoundY";
   private static final String TAG_BOUND_Z = "EnkiduBoundZ";
   private static final String TAG_MATCHLOCK_INTERCEPT_RESERVED = "EnkiduMatchlockInterceptReservedUntil";

   private static final int TRANSFIGURATION_COOLDOWN = 10 * 20;
   private static final int PRESENCE_COOLDOWN = 15 * 20;
   private static final int PERFECT_FORM_COOLDOWN = 30 * 20;
   private static final int CHAIN_COOLDOWN = 20 * 20;
   private static final int BIG_VOLLEY_COOLDOWN = 18 * 20;
   private static final int MEGA_VOLLEY_COOLDOWN = 55 * 20;
   private static final int CHAIN_LASH_COOLDOWN = 4 * 20;
   private static final int EARTH_SPIKE_COOLDOWN = 6 * 20;
   private static final int SKY_SPEAR_SWEEP_COOLDOWN = 9 * 20;
   private static final int EARTH_WEDGE_COOLDOWN = 12 * 20;
   private static final int CLAY_BULWARK_COOLDOWN = 16 * 20;
   private static final int STARDUST_STEP_COOLDOWN = 7 * 20;
   private static final int NATURE_PULSE_COOLDOWN = 9 * 20;
   private static final int MELEE_BASIC_COOLDOWN = 18;
   private static final int MELEE_LUNGE_COOLDOWN = 5 * 20;
   private static final int CLAY_BODY_COOLDOWN = 30 * 20;
   private static final int ENUMA_COOLDOWN = 60 * 20;
   private static final int ENUMA_WINDUP = 10 * 20;
   private static final int ENUMA_RELEASE_VISUAL = 5 * 20;
   private static final int ENUMA_BIND_COUNT = 3;
   private static final int ENUMA_BIND_DURATION = 58;
   private static final double ENUMA_GROUND_EXPLOSION_RADIUS = 60.0;

   private static final ResourceLocation MODE_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "enkidu_mode_attack");
   private static final ResourceLocation MODE_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "enkidu_mode_armor");
   private static final ResourceLocation MODE_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "enkidu_mode_speed");
   private static final ResourceLocation MODE_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "enkidu_mode_health");

   private static final ItemStack[] EARTH_WEAPONS = new ItemStack[] {
      new ItemStack(Items.IRON_SWORD), new ItemStack(Items.IRON_AXE), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_SHOVEL),
      new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_AXE), new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.TRIDENT),
      new ItemStack(Items.NETHERITE_SWORD), new ItemStack(Items.NETHERITE_AXE), new ItemStack(Items.NETHERITE_PICKAXE),
      new ItemStack(Items.BOW), new ItemStack(Items.CROSSBOW)
   };

   private EnkiduCombatHelper() {
   }

   public static void tick(EnkiduEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }

      long now = level.getGameTime();
      tickBoundTargets(entity, level, now);
      tickPerfectFormRegen(entity, level, now);
      tickFireImmunity(entity, level, now);
      tickPassivePresence(entity, level, now);
      tickGroundManaRegen(entity, level, now);
      tickGroundCombatResourceBoost(entity, now);
      EnkiduTemporaryPlantHelper.cleanupExpired(level, now);
      tickNatureDropCleanup(entity, level, now);
      tickEnumaWindup(entity, level, now);
      if (GilgameshDuelState.tickEnkidu(entity, level)) {
         return;
      }
      if (isEnumaActive(entity, now)) {
         return;
      }

      LivingEntity target = entity.getTarget();
      updateFlight(entity, target, now);

      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         entity.setTarget(null);
         clearWeaponBurstState(entity);
         return;
      }
      if (ServantCombatSystem.cannotAct(entity) || ServantCombatSystem.skillsSuppressed(entity)) {
         return;
      }

      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.55, 0.0));
      double distance = entity.distanceTo(target);
      interceptHostileProjectiles(entity, level, target, now);
      ServantCombatPhase phase = ServantCombatSystem.getPhase(entity);
      updateCombatMovement(entity, target, distance, now);
      if (tryBoundTargetAssault(entity, level, target, now, phase, distance)) {
         return;
      }
      if (tryPerfectForm(entity, level, now)) {
         return;
      }
      tryTransfiguration(entity, level, target, now);
      if (tryPresenceDetection(entity, level, now)) {
         return;
      }
      // Against Gilgamesh, keep the queued 100-weapon Age of Babylon rounds
      // together instead of letting another major skill consume the window.
      if (target instanceof GilgameshEntity
         && entity.getPersistentData().getLong(TAG_BIG_VOLLEY_TOKEN) > now) {
         return;
      }
      // This matchup uses the normal Enkidu projectile path, not the removed
      // projection-counter/door-pairing path.
      if (target instanceof GilgameshEntity
         && tryAgeOfBabylonVolley(entity, level, target, now, phase)) {
         return;
      }
      if (tryBeginEnumaElish(entity, level, target, now, phase)) {
         return;
      }
      if (tryChainOfHeaven(entity, level, target, now)) {
         return;
      }
      if (distance <= 5.0 && tryMeleeLunge(entity, level, target, now, phase, distance)) {
         return;
      }
      if (distance <= 5.0 && tryMeleeBasic(entity, level, target, now, phase, distance)) {
         return;
      }
      if (tryMegaAgeOfBabylonVolley(entity, level, target, now, phase)) {
         return;
      }
      if (!(target instanceof GilgameshEntity)
         && tryAgeOfBabylonVolley(entity, level, target, now, phase)) {
         return;
      }
      if (tryEnkiduSmallSkill(entity, level, target, now, phase, distance)) {
         return;
      }
      if (isFlying(entity) && tryFlyingBasicAttack(entity, level, target, now, distance)) {
         return;
      }
      if (isFlying(entity) && distance >= 8.0) {
         trySmallAgeOfBabylon(entity, level, target, now);
      }
   }

   public static boolean isFlying(EnkiduEntity entity) {
      return entity.getPersistentData().getLong(TAG_FLIGHT_UNTIL) > entity.level().getGameTime()
         && entity.getPersistentData().getLong(TAG_LAND_UNTIL) <= entity.level().getGameTime();
   }

   public static boolean isFireDamage(DamageSource source) {
      return source != null && source.is(DamageTypeTags.IS_FIRE);
   }

   public static void extinguishFire(EnkiduEntity entity) {
      if (entity != null && entity.isOnFire()) {
         entity.clearFire();
      }
   }

   public static boolean tryClayBodyOnHeavyDamage(EnkiduEntity entity, LivingIncomingDamageEvent event) {
      if (entity == null || event == null || !(entity.level() instanceof ServerLevel level) || event.isCanceled()) {
         return false;
      }
      if (isPerfectFormUndefendableDamage(event.getSource())) {
         return false;
      }
      if (event.getSource() != null && event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }
      float amount = event.getAmount();
      if (amount < Math.max(12.0F, entity.getHealth() * 0.30F)) {
         return false;
      }
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (now - data.getLong(TAG_LAST_CLAY_BODY) < CLAY_BODY_COOLDOWN) {
         return false;
      }
      data.putLong(TAG_LAST_CLAY_BODY, now);
      data.putLong("TypeMoonCombatInvulnUntil", now + 22L);
      data.putLong("TypeMoonCombatUntargetableUntil", now + 22L);
      data.putLong("TypeMoonCombatRecoveryUntil", now + 30L);
      event.setCanceled(true);
      event.setAmount(0.0F);

      LivingEntity target = entity.getTarget();
      Vec3 regroup = findClayRegroupPosition(entity, level, target);
      entity.getNavigation().stop();
      entity.setDeltaMovement(Vec3.ZERO);
      entity.setNoGravity(false);
      entity.triggerNamedActionAnimation("perfect_form");
      Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
      spawnClayBodyBreakFx(level, center, entity.getBbWidth(), entity.getBbHeight());
      level.playSound(null, entity.blockPosition(), SoundEvents.MUD_BREAK, SoundSource.HOSTILE, 1.2F, 0.68F);

      TYPE_MOON_WORLD.queueServerWork(20, () -> {
         if (!entity.isAlive() || !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
         }
         entity.teleportTo(regroup.x, regroup.y, regroup.z);
         entity.setDeltaMovement(Vec3.ZERO);
         entity.fallDistance = 0.0F;
         entity.heal(Math.max(35.0F, entity.getMaxHealth() * 0.08F));
         entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 45, 1, false, true, true));
         if (target != null && target.isAlive()) {
            entity.setTarget(target);
            entity.faceToward(target.position());
         }
         spawnClayBodyReformFx(serverLevel, entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0));
         serverLevel.playSound(null, entity.blockPosition(), SoundEvents.MUD_PLACE, SoundSource.HOSTILE, 1.15F, 1.2F);
      });
      return true;
   }

   public static float applyPerfectFormPassiveDamageReduction(EnkiduEntity entity, LivingIncomingDamageEvent event) {
      if (entity == null || event == null || event.isCanceled()) {
         return event == null ? 0.0F : event.getAmount();
      }
      float amount = event.getAmount();
      if (amount <= 0.0F || MagicResistanceHelper.isMagicDamage(event.getSource()) || isPerfectFormUndefendableDamage(event.getSource())) {
         return amount;
      }
      return amount * 0.7F;
   }

   public static boolean isPerfectFormUndefendableDamage(DamageSource source) {
      return source != null && source.is(DamageTypes.WITHER);
   }

   public static void cleanup(EnkiduEntity entity) {
      entity.setNoGravity(false);
      removeModeModifiers(entity);
      if (entity.level() instanceof ServerLevel level) {
         tickBoundTargets(entity, level, Long.MAX_VALUE / 4L);
      }
   }

   public static boolean tryRespondToNoblePhantasm(EnkiduEntity entity, ServantEntity caster, LivingEntity target, long now, boolean ranged) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return false;
      }
      LivingEntity counterTarget = caster != null && caster.isAlive() ? caster : target;
      if (counterTarget != null && counterTarget.isAlive() && tryBeginEnumaElish(entity, level, counterTarget, now, ServantCombatPhase.DECISIVE, true)) {
         return true;
      }
      if (entity.getCurrentMp() < 12.0) {
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      if (now - data.getLong(TAG_LAST_CLAY_BULWARK) < CLAY_BULWARK_COOLDOWN / 2L) {
         return false;
      }
      data.putLong(TAG_LAST_CLAY_BULWARK, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 12.0));
      entity.triggerNamedActionAnimation("perfect_form");
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ranged ? 100 : 70, 1, false, true, true));
      Vec3 threat = counterTarget != null && counterTarget.isAlive() ? counterTarget.position() : entity.position().add(entity.getLookAngle().scale(8.0));
      growPlantBulwark(entity, level, threat, true);
      return true;
   }

   public static boolean isEnumaElishActive(EnkiduEntity entity) {
      return entity != null && isEnumaActive(entity, entity.level().getGameTime());
   }

   private static void tickFireImmunity(EnkiduEntity entity, ServerLevel level, long now) {
      if (now % 20L != 0L || !entity.isOnFire()) {
         return;
      }
      entity.clearFire();
      level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.45, entity.getZ(), 8, 0.28, 0.35, 0.28, 0.025);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 4, 0.18, 0.22, 0.18, 0.025);
   }

   private static void tickPassivePresence(EnkiduEntity entity, ServerLevel level, long now) {
      if (now % 20L != 0L) {
         return;
      }
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(50.0),
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e))) {
         if (living.hasEffect(MobEffects.INVISIBILITY)) {
            living.removeEffect(MobEffects.INVISIBILITY);
         }
      }
   }

   private static void tickNatureDropCleanup(EnkiduEntity entity, ServerLevel level, long now) {
      if (now % 40L != 0L) {
         return;
      }
      for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(36.0), EnkiduCombatHelper::isMossOrGrassSeedDrop)) {
         item.discard();
      }
   }

   private static boolean isMossOrGrassSeedDrop(ItemEntity item) {
      if (item == null || !item.isAlive()) {
         return false;
      }
      ItemStack stack = item.getItem();
      return stack.is(Items.WHEAT_SEEDS) || stack.is(Blocks.MOSS_BLOCK.asItem()) || stack.is(Blocks.MOSS_CARPET.asItem());
   }

   private static boolean tryPresenceDetection(EnkiduEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      if (entity.getCurrentMp() < 15.0 || now - data.getLong(TAG_LAST_PRESENCE) < PRESENCE_COOLDOWN || entity.getRandom().nextFloat() > 0.08F) {
         return false;
      }
      data.putLong(TAG_LAST_PRESENCE, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 15.0));
      entity.triggerNamedActionAnimation("presence_detection");
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_presence_detection", entity, 0.65F);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(100.0),
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e))) {
         living.removeEffect(MobEffects.INVISIBILITY);
         living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 10 * 20, 0, false, true, true));
      }
      return true;
   }

   private static void tickGroundManaRegen(EnkiduEntity entity, ServerLevel level, long now) {
      if (now % 20L != 0L) {
         return;
      }
      boolean onEarth = isOnEarth(entity, level);
      entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + (onEarth ? 6.0 : 3.0)));
      if (entity.getHealth() < entity.getMaxHealth()) {
         entity.heal(onEarth ? 10.0F : 5.0F);
         int count = onEarth ? 10 : 5;
         double spread = onEarth ? 0.35 : 0.22;
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 0.35, entity.getZ(), count, spread, 0.16, spread, 0.035);
      }
   }

   private static boolean isOnEarth(EnkiduEntity entity, ServerLevel level) {
      if (!entity.onGround()) {
         return false;
      }
      BlockPos below = entity.blockPosition().below();
      BlockState state = level.getBlockState(below);
      return !state.isAir() && state.isSolidRender(level, below);
   }

   private static void tickGroundCombatResourceBoost(EnkiduEntity entity, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !isOnEarth(entity, level) || entity.tickCount % 5 != 0) {
         return;
      }
      CompoundTag data = entity.getPersistentData();
      if (entity.getDefinition() == null || entity.getDefinition().parameters() == null) {
         return;
      }
      double staminaMax = ServantCombatFormulas.staminaMax(entity.getDefinition().parameters());
      double poiseMax = ServantCombatFormulas.poiseMax(entity.getDefinition().parameters());
      data.putDouble("TypeMoonCombatStamina", Math.min(staminaMax, data.getDouble("TypeMoonCombatStamina") + ServantCombatFormulas.staminaRegenPerSecond(entity.getDefinition().parameters()) / 4.0));
      data.putDouble("TypeMoonCombatPoise", Math.min(poiseMax, data.getDouble("TypeMoonCombatPoise") + ServantCombatFormulas.poiseRegenPerSecond(entity.getDefinition().parameters()) / 4.0));
      if (data.getLong("TypeMoonCombatRecoveryUntil") > now && entity.tickCount % 20 == 0) {
         entity.heal((float)(entity.getMaxHealth() * ServantCombatFormulas.comboProtectionHealPercentPerSecond(entity.getDefinition().parameters())));
      }
   }

   private static boolean tryPerfectForm(EnkiduEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      if (entity.getHealth() > entity.getMaxHealth() * 0.4F
         || entity.getCurrentMp() < 50.0
         || now - data.getLong(TAG_LAST_PERFECT_FORM) < PERFECT_FORM_COOLDOWN) {
         return false;
      }
      data.putLong(TAG_LAST_PERFECT_FORM, now);
      data.putLong(TAG_REGEN_UNTIL, now + 200L);
      data.putLong(TAG_NEXT_REGEN, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 50.0));
      clearNegativeEffects(entity);
      entity.triggerNamedActionAnimation("perfect_form");
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_transfiguration", entity, 1.1F);
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2F, 1.45F);
      return true;
   }

   private static void tickPerfectFormRegen(EnkiduEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      if (data.getLong(TAG_REGEN_UNTIL) <= now) {
         return;
      }
      clearNegativeEffects(entity);
      if (now >= data.getLong(TAG_NEXT_REGEN)) {
         entity.heal(20.0F);
         data.putLong(TAG_NEXT_REGEN, now + 10L);
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 0.9, entity.getZ(), 20, 0.6, 0.8, 0.6, 0.05);
      }
   }

   private static void tryTransfiguration(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now) {
      CompoundTag data = entity.getPersistentData();
      if (entity.getCurrentMp() < 15.0 || now - data.getLong(TAG_LAST_TRANSFIGURATION) < TRANSFIGURATION_COOLDOWN) {
         return;
      }
      String desired = chooseMode(entity, target);
      if (desired.equals(data.getString(TAG_MODE)) && now - data.getLong(TAG_LAST_TRANSFIGURATION) < TRANSFIGURATION_COOLDOWN * 2L) {
         return;
      }

      data.putLong(TAG_LAST_TRANSFIGURATION, now);
      data.putString(TAG_MODE, desired);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 15.0));
      applyMode(entity, desired);
      entity.triggerNamedActionAnimation("transfiguration");
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_transfiguration", entity, 1.1F);
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.75F, 1.65F);
   }

   private static String chooseMode(EnkiduEntity entity, LivingEntity target) {
      double distance = entity.distanceTo(target);
      if (target.getMaxHealth() >= 120.0F || target.getArmorValue() >= 12 || hasTrait(target, ServantTraitTag.GIANT) || hasTrait(target, ServantTraitTag.BEAST)) {
         return "attack";
      }
      if (distance > 10.0 || target.hasEffect(MobEffects.MOVEMENT_SPEED) || target instanceof Player) {
         return "agility";
      }
      return "balanced";
   }

   private static void applyMode(EnkiduEntity entity, String mode) {
      removeModeModifiers(entity);
      if ("attack".equals(mode)) {
         addModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), MODE_ATTACK_ID, 10.0, AttributeModifier.Operation.ADD_VALUE);
         addModifier(entity.getAttribute(Attributes.ARMOR), MODE_ARMOR_ID, -4.0, AttributeModifier.Operation.ADD_VALUE);
         addModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), MODE_SPEED_ID, -0.04, AttributeModifier.Operation.ADD_VALUE);
         addModifier(entity.getAttribute(Attributes.MAX_HEALTH), MODE_HEALTH_ID, -100.0, AttributeModifier.Operation.ADD_VALUE);
      } else if ("agility".equals(mode)) {
         addModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), MODE_ATTACK_ID, -10.0, AttributeModifier.Operation.ADD_VALUE);
         addModifier(entity.getAttribute(Attributes.ARMOR), MODE_ARMOR_ID, -4.0, AttributeModifier.Operation.ADD_VALUE);
         addModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), MODE_SPEED_ID, 0.06, AttributeModifier.Operation.ADD_VALUE);
         addModifier(entity.getAttribute(Attributes.MAX_HEALTH), MODE_HEALTH_ID, -100.0, AttributeModifier.Operation.ADD_VALUE);
      }
      if (entity.getHealth() > entity.getMaxHealth()) {
         entity.setHealth(entity.getMaxHealth());
      }
   }

   private static void removeModeModifiers(EnkiduEntity entity) {
      removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), MODE_ATTACK_ID);
      removeModifier(entity.getAttribute(Attributes.ARMOR), MODE_ARMOR_ID);
      removeModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), MODE_SPEED_ID);
      removeModifier(entity.getAttribute(Attributes.MAX_HEALTH), MODE_HEALTH_ID);
   }

   private static void updateFlight(EnkiduEntity entity, LivingEntity target, long now) {
      CompoundTag data = entity.getPersistentData();
      if (data.getLong(TAG_FLIGHT_UNTIL) > now) {
         if (!entity.onGround()) {
            data.putBoolean(TAG_FLIGHT_WAS_AIRBORNE, true);
         } else if (data.getBoolean(TAG_FLIGHT_WAS_AIRBORNE)) {
            entity.setNoGravity(false);
            data.remove(TAG_FLIGHT_WAS_AIRBORNE);
            data.remove(TAG_FLIGHT_UNTIL);
            data.remove(TAG_NEXT_FLIGHT_TOGGLE);
            data.putLong(TAG_LAND_UNTIL, now + 80L);
            return;
         }
      }
      if (data.getLong(TAG_LAND_UNTIL) > now || target == null || !target.isAlive()) {
         entity.setNoGravity(false);
         data.remove(TAG_FLIGHT_WAS_AIRBORNE);
         data.remove(TAG_FLIGHT_UNTIL);
         data.remove(TAG_NEXT_FLIGHT_TOGGLE);
         data.remove(TAG_FLIGHT_LAST_CONTACT);
         return;
      }
      double distance = entity.distanceTo(target);
      if (!data.contains(TAG_FLIGHT_LAST_CONTACT)) {
         data.putLong(TAG_FLIGHT_LAST_CONTACT, now);
      }
      if (distance <= 12.0 || entity.getSensing().hasLineOfSight(target)) {
         data.putLong(TAG_FLIGHT_LAST_CONTACT, now);
      } else if (isFlying(entity) && now - data.getLong(TAG_FLIGHT_LAST_CONTACT) >= 8L * 20L) {
         entity.setNoGravity(false);
         data.remove(TAG_FLIGHT_WAS_AIRBORNE);
         data.remove(TAG_FLIGHT_UNTIL);
         data.putLong(TAG_LAND_UNTIL, now + 120L);
         data.putLong(TAG_NEXT_FLIGHT_TOGGLE, now + 200L);
         data.putLong(TAG_FLIGHT_LAST_CONTACT, now);
         ServantNavigationHelper.moveToTargetThrottled(
            entity, target, 1.25, now, 2, 0.2, "EnkiduFlightStalled");
         return;
      }
      long flightUntil = data.getLong(TAG_FLIGHT_UNTIL);
      boolean lowHealthNeedsEarth = entity.getHealth() <= entity.getMaxHealth() * 0.8F;
      double verticalGap = target.getY() - entity.getY();
      boolean closeMelee = distance <= 5.0;
      boolean targetClearlyHigh = verticalGap > 4.0 || (!target.onGround() && verticalGap > 1.75);
      boolean shouldFly = !lowHealthNeedsEarth && !closeMelee && targetClearlyHigh;
      if (closeMelee || !shouldFly || (target.onGround() && verticalGap <= 1.25)) {
         entity.setNoGravity(false);
         data.remove(TAG_FLIGHT_WAS_AIRBORNE);
         data.remove(TAG_FLIGHT_UNTIL);
         data.putLong(TAG_LAND_UNTIL, now + 80L);
         data.remove(TAG_NEXT_FLIGHT_TOGGLE);
         return;
      }
      if (!entity.getNavigation().isDone() || distance <= 6.0) {
         data.putLong(TAG_UNREACHABLE_TICKS, 0L);
      } else if (distance > 8.0) {
         data.putLong(TAG_UNREACHABLE_TICKS, data.getLong(TAG_UNREACHABLE_TICKS) + 1L);
      }
      if (shouldFly && flightUntil <= now) {
         data.putLong(TAG_FLIGHT_UNTIL, now + 70L + entity.getRandom().nextInt(50));
         data.putLong(TAG_NEXT_FLIGHT_TOGGLE, now + 70L + entity.getRandom().nextInt(40));
      } else if ((!shouldFly || distance < 7.0) && flightUntil > now) {
         data.putLong(TAG_FLIGHT_UNTIL, now + 5L);
         data.putLong(TAG_NEXT_FLIGHT_TOGGLE, now + 160L + entity.getRandom().nextInt(80));
      } else if (now >= data.getLong(TAG_NEXT_FLIGHT_TOGGLE)) {
         data.putLong(TAG_NEXT_FLIGHT_TOGGLE, now + 140L + entity.getRandom().nextInt(100));
         if (shouldFly) {
            data.putLong(TAG_FLIGHT_UNTIL, now + 60L + entity.getRandom().nextInt(60));
         } else {
            data.remove(TAG_FLIGHT_UNTIL);
         }
      }
      if (isFlying(entity)) {
         entity.getNavigation().stop();
         entity.setNoGravity(true);
         double hoverY = ServantFlightHelper.desiredHoverY(entity, target);
         Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(entity.getRandom().nextDouble() - 0.5, 0.0, entity.getRandom().nextDouble() - 0.5);
         }
         away = away.normalize();
         double radialDistance = Math.max(0.1, entity.distanceTo(target));
         double radial = radialDistance < 9.0 ? 0.16 : radialDistance > 15.0 ? -0.14 : 0.0;
         Vec3 orbit = new Vec3(-away.z, 0.0, away.x).scale(0.11);
         double yMotion = ServantFlightHelper.verticalVelocityToward(entity.getY(), hoverY, 0.12, 0.025, 0.18, 0.24);
         entity.setDeltaMovement(entity.getDeltaMovement().scale(0.65).add(away.scale(radial)).add(orbit).add(0.0, yMotion, 0.0));
      } else {
         entity.setNoGravity(false);
      }
   }

   private static void updateCombatMovement(EnkiduEntity entity, LivingEntity target, double distance, long now) {
      if (isFlying(entity) || ServantCombatSystem.cannotAct(entity)) {
         return;
      }
      if (distance > 16.0) {
         ServantNavigationHelper.moveToTargetThrottled(
            entity,
            target,
            1.18,
            now,
            ServantNavigationHelper.DEFAULT_REPATH_INTERVAL,
            1.0,
            "EnkiduChasePath"
         );
      } else if (distance > 8.0) {
         ServantNavigationHelper.moveToTargetThrottled(
            entity,
            target,
            1.02,
            now,
            ServantNavigationHelper.DEFAULT_REPATH_INTERVAL,
            1.0,
            "EnkiduPressurePath"
         );
         if (now % 12L == 0L) {
            float side = entity.getRandom().nextBoolean() ? 0.45F : -0.45F;
            entity.getMoveControl().strafe(0.18F, side);
         }
      } else if (distance > 3.2) {
         if (now % 10L == 0L) {
            float side = entity.getRandom().nextBoolean() ? 0.75F : -0.75F;
            entity.getMoveControl().strafe(0.12F, side);
         }
      } else {
         Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() > 1.0E-4) {
            away = away.normalize().scale(3.6);
            ServantNavigationHelper.moveToPositionThrottled(
               entity,
               new Vec3(entity.getX() + away.x, entity.getY(), entity.getZ() + away.z),
               1.1,
               now,
               ServantNavigationHelper.SHORT_REPATH_INTERVAL,
               1.0,
               "EnkiduAwayPath"
            );
         }
      }
   }

   private static boolean tryChainOfHeaven(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (isChainForbiddenTarget(entity, target)) return false;
      CompoundTag data = entity.getPersistentData();
      boolean divine = hasTrait(target, ServantTraitTag.DIVINE);
      if (entity.distanceTo(target) > 22.0 || entity.getCurrentMp() < 30.0 || now - data.getLong(TAG_LAST_CHAIN) < CHAIN_COOLDOWN) {
         return false;
      }
      if (!divine && entity.getRandom().nextFloat() > 0.28F) {
         return false;
      }

      int divinity = divinityLevel(target);
      int duration = divine ? (3 + Math.max(1, divinity)) * 20 : 20;
      data.putLong(TAG_LAST_CHAIN, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 30.0));
      bindTarget(entity, level, target, duration, divine);
      entity.triggerNamedActionAnimation("chain_of_heaven");
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_chain_of_heaven", target, Math.max(1.2F, duration / 20.0F));
      level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1.4F, divine ? 1.4F : 1.0F);
      return true;
   }

   private static void bindTarget(EnkiduEntity entity, ServerLevel level, LivingEntity target, int duration, boolean divine) {
      if (isChainForbiddenTarget(entity, target)) return;
      long now = level.getGameTime();
      CompoundTag data = target.getPersistentData();
      data.putLong(TAG_BOUND_UNTIL, now + duration);
      data.putUUID(TAG_BOUND_OWNER, entity.getUUID());
      data.putDouble(TAG_BOUND_X, target.getX());
      data.putDouble(TAG_BOUND_Y, target.getY());
      data.putDouble(TAG_BOUND_Z, target.getZ());
      if (target instanceof Mob mob) {
         data.putBoolean(TAG_BOUND_PREV_NO_AI, mob.isNoAi());
         mob.getNavigation().stop();
         mob.setNoAi(true);
      }
      target.setDeltaMovement(Vec3.ZERO);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration + 5, 10, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration + 5, 10, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration + 5, 10, false, true, true));
      level.addFreshEntity(new ChainsOfHeavenBindingEntity(level, entity, target, duration + 6, divine));
      TYPE_MOON_WORLD.queueServerWork(duration + 2, () -> restoreBoundTarget(target, entity.getUUID()));
   }

   private static boolean tryBoundTargetAssault(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase, double distance) {
      if (!isBoundByEnkidu(entity, target, now)) {
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      data.remove(TAG_FLIGHT_UNTIL);
      data.putLong(TAG_LAND_UNTIL, now + 100L);
      entity.setNoGravity(false);
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.55, 0.0));
      if (tryBoundAgeOfBabylonBarrage(entity, level, target, now, phase)) {
         return true;
      }
      if (distance > 5.0) {
         ServantNavigationHelper.moveToTargetThrottled(
            entity,
            target,
            1.35,
            now,
            ServantNavigationHelper.SHORT_REPATH_INTERVAL,
            0.8,
            "EnkiduBoundAssaultPath"
         );
         return true;
      }
      entity.getNavigation().stop();
      if (now - data.getLong(TAG_LAST_BOUND_ASSAULT) < 7L) {
         return true;
      }
      data.putLong(TAG_LAST_BOUND_ASSAULT, now);
      if (entity.getRandom().nextBoolean()) {
         Vec3 hit = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
         entity.triggerSlashAnimation();
         applyMorphingLimbBasic(entity, level, target, phase, hit);
      } else {
         entity.triggerHorizontalSwingAnimation();
         applyMeleeDamage(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (1.15 + phase.id() * 0.12) + 8.0F), 0.45, false);
         Vec3 hit = target.position().add(0.0, target.getBbHeight() * 0.52, 0.0);
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, hit.x, hit.y, hit.z, 2, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, hit.x, hit.y, hit.z, 14, 0.22, 0.25, 0.22, 0.07);
         level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.HOSTILE, 0.95F, 1.35F);
      }
      return true;
   }

   private static boolean isBoundByEnkidu(EnkiduEntity entity, LivingEntity target, long now) {
      CompoundTag targetData = target.getPersistentData();
      return targetData.hasUUID(TAG_BOUND_OWNER)
         && entity.getUUID().equals(targetData.getUUID(TAG_BOUND_OWNER))
         && targetData.getLong(TAG_BOUND_UNTIL) > now;
   }

   public static boolean isBoundByChainsOfHeaven(LivingEntity target) {
      if (target == null || target.level() == null) {
         return false;
      }
      CompoundTag targetData = target.getPersistentData();
      return targetData.hasUUID(TAG_BOUND_OWNER) && targetData.getLong(TAG_BOUND_UNTIL) > target.level().getGameTime();
   }

   private static boolean tryBoundAgeOfBabylonBarrage(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase) {
      CompoundTag data = entity.getPersistentData();
      long cooldown = phase == ServantCombatPhase.DECISIVE ? 24L : 36L;
      if (now - data.getLong(TAG_LAST_BOUND_BARRAGE) < cooldown) {
         return false;
      }
      data.putLong(TAG_LAST_BOUND_BARRAGE, now);
      data.putLong(TAG_LAST_BIG_VOLLEY, now);
      data.putUUID(TAG_BIG_VOLLEY_TARGET, target.getUUID());
      data.putLong(TAG_BIG_VOLLEY_TOKEN, now + 60L);
      entity.triggerNamedActionAnimation("age_of_babylon");
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_age_of_babylon", target, 1.8F);
      spawnEarthWeaponsAroundTarget(entity, level, target, phase == ServantCombatPhase.DECISIVE ? 72 : 48, 0, 3.0F, true);
      if (entity.distanceTo(target) <= 5.2) {
         TYPE_MOON_WORLD.queueServerWork(8, () -> {
            if (entity.isAlive() && target.isAlive() && isBoundByEnkidu(entity, target, entity.level().getGameTime()) && entity.level() instanceof ServerLevel serverLevel) {
               entity.triggerHorizontalSwingAnimation();
               applyMeleeDamage(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.95 + 8.0F), 0.22, false);
               Vec3 hit = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
               serverLevel.sendParticles(ParticleTypes.CRIT, hit.x, hit.y, hit.z, 12, 0.25, 0.25, 0.25, 0.08);
            }
         });
      }
      return true;
   }

   private static void tickBoundTargets(EnkiduEntity entity, ServerLevel level, long now) {
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(96.0), LivingEntity::isAlive)) {
         CompoundTag data = target.getPersistentData();
         if (!data.hasUUID(TAG_BOUND_OWNER) || !entity.getUUID().equals(data.getUUID(TAG_BOUND_OWNER))) {
            continue;
         }
         if (data.getLong(TAG_BOUND_UNTIL) <= now) {
            restoreBoundTarget(target, entity.getUUID());
            continue;
         }
         target.setDeltaMovement(Vec3.ZERO);
         target.hurtMarked = true;
         target.setPos(data.getDouble(TAG_BOUND_X), data.getDouble(TAG_BOUND_Y), data.getDouble(TAG_BOUND_Z));
         if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            mob.setNoAi(true);
         }
      }
   }

   private static void restoreBoundTarget(LivingEntity target, UUID owner) {
      if (target == null) {
         return;
      }
      CompoundTag data = target.getPersistentData();
      if (!data.hasUUID(TAG_BOUND_OWNER) || !owner.equals(data.getUUID(TAG_BOUND_OWNER))) {
         return;
      }
      if (target instanceof Mob mob) {
         mob.setNoAi(data.getBoolean(TAG_BOUND_PREV_NO_AI));
      }
      data.remove(TAG_BOUND_OWNER);
      data.remove(TAG_BOUND_UNTIL);
      data.remove(TAG_BOUND_PREV_NO_AI);
      data.remove(TAG_BOUND_X);
      data.remove(TAG_BOUND_Y);
      data.remove(TAG_BOUND_Z);
   }

   private static boolean trySmallAgeOfBabylon(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         clearWeaponBurstState(entity);
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      if (target instanceof GilgameshEntity
         && (data.getLong(TAG_BIG_VOLLEY_TOKEN) > now || entity.getRandom().nextFloat() >= 0.12F)) {
         return false;
      }
      long cooldown = isFlying(entity) ? 18L + entity.getRandom().nextInt(16) : 34L + entity.getRandom().nextInt(28);
      if (now - data.getLong(TAG_LAST_SMALL_WEAPON) < cooldown) {
         return false;
      }
      data.putLong(TAG_LAST_SMALL_WEAPON, now);
      data.putUUID(TAG_SMALL_WEAPON_TARGET, target.getUUID());
      data.putLong(TAG_SMALL_WEAPON_TOKEN, now + 24L);
      spawnEarthWeapons(entity, level, target, 1, 0, 2.0F, false);
      entity.triggerNamedActionAnimation("age_of_babylon");
      return true;
   }

   private static boolean tryAgeOfBabylonVolley(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase) {
      CompoundTag data = entity.getPersistentData();
      boolean counterDuel = isProjectionCounterDuelTarget(target);
      boolean gilgameshOpponent = target instanceof GilgameshEntity;
      boolean highThreat = counterDuel || gilgameshOpponent || target.getMaxHealth() >= 160.0F || hasTrait(target, ServantTraitTag.BEAST) || hasTrait(target, ServantTraitTag.HUMAN_THREAT) || target instanceof EnderDragon;
      int chance = gilgameshOpponent
         ? phase == ServantCombatPhase.DECISIVE ? 92 : phase == ServantCombatPhase.NORMAL ? 82 : 62
         : phase == ServantCombatPhase.DECISIVE ? 55 : phase == ServantCombatPhase.NORMAL ? 40 : 24;
      if ((!highThreat && phase != ServantCombatPhase.DECISIVE)
         || entity.getCurrentMp() < 30.0
         || now - data.getLong(TAG_LAST_BIG_VOLLEY) < phasedCooldown(BIG_VOLLEY_COOLDOWN, phase)
         || entity.getRandom().nextInt(100) >= chance) {
         return false;
      }
      data.putLong(TAG_LAST_BIG_VOLLEY, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 30.0));
      data.putUUID(TAG_BIG_VOLLEY_TARGET, target.getUUID());
      entity.triggerNamedActionAnimation("age_of_babylon");
      if (gilgameshOpponent) {
         int rounds = 2 + entity.getRandom().nextInt(2);
         data.putLong(TAG_BIG_VOLLEY_TOKEN, now + rounds * 52L + 80L);
         VFXServerEffects.spawnReplayable(level, "servant_enkidu_age_of_babylon", entity, rounds * 2.6F + 1.0F);
         for (int batch = 0; batch < rounds; batch++) {
            final int batchIndex = batch;
            TYPE_MOON_WORLD.queueServerWork(batch * 52 + 1, () -> {
               if (entity.isAlive() && entity.level() instanceof ServerLevel serverLevel) {
                  LivingEntity liveTarget = resolveAgeOfBabylonTarget(serverLevel, entity, data);
                  if (liveTarget instanceof GilgameshEntity) {
                     spawnEarthWeapons(entity, serverLevel, liveTarget, 100, batchIndex, 2.9F, true);
                  }
               }
            });
         }
         return true;
      }
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_age_of_babylon", entity, 2.2F);
      if (!counterDuel) {
         data.putLong(TAG_BIG_VOLLEY_TOKEN, now + 70L);
         spawnEarthWeapons(entity, level, target, 100 + entity.getRandom().nextInt(11), 0, 2.8F, true);
         return true;
      }

      int rounds = 12 + entity.getRandom().nextInt(5);
      data.putLong(TAG_BIG_VOLLEY_TOKEN, now + rounds * 10L + 30L);
      for (int batch = 0; batch < rounds; batch++) {
         final int batchIndex = batch;
         final int delay = batch * 10 + 1;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (entity.isAlive() && entity.level() instanceof ServerLevel serverLevel) {
               LivingEntity liveTarget = resolveAgeOfBabylonTarget(serverLevel, entity, data);
               if (liveTarget != null) {
                  spawnEarthWeapons(entity, serverLevel, liveTarget, 22, batchIndex, 2.9F, true);
                  triggerProjectionCounterVolley(liveTarget, entity, serverLevel, batchIndex, true);
               }
            }
         });
      }
      return true;
   }

   private static boolean tryMegaAgeOfBabylonVolley(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase) {
      // Gilgamesh matchups use the ordinary 100-weapon sequence above. Do not
      // replace it with the unrelated decisive/mega branch.
      if (target instanceof GilgameshEntity
         || phase != ServantCombatPhase.DECISIVE
         || target == null || !target.isAlive()) {
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      boolean extremeThreat = isProjectionCounterDuelTarget(target)
         || target.getMaxHealth() >= 320.0F
         || hasTrait(target, ServantTraitTag.BEAST)
         || hasTrait(target, ServantTraitTag.HUMAN_THREAT)
         || target instanceof EnderDragon;
      if (!extremeThreat
         || entity.getCurrentMp() < 80.0
         || now - data.getLong(TAG_LAST_MEGA_VOLLEY) < MEGA_VOLLEY_COOLDOWN
         || entity.getRandom().nextInt(100) >= 32) {
         return false;
      }
      boolean counterDuel = isProjectionCounterDuelTarget(target);
      int rounds = counterDuel ? 18 + entity.getRandom().nextInt(8) : 8 + entity.getRandom().nextInt(5);
      int perRound = counterDuel ? 56 : 72;
      data.putLong(TAG_LAST_MEGA_VOLLEY, now);
      data.putLong(TAG_LAST_BIG_VOLLEY, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 80.0));
      data.putUUID(TAG_BIG_VOLLEY_TARGET, target.getUUID());
      data.putLong(TAG_BIG_VOLLEY_TOKEN, now + rounds * 8L + 80L);
      entity.triggerNamedActionAnimation("age_of_babylon");
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_age_of_babylon", entity, 4.5F);
      for (int batch = 0; batch < rounds; batch++) {
         final int batchIndex = batch;
         final int delay = batch * 8 + 1;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (entity.isAlive() && entity.level() instanceof ServerLevel serverLevel) {
               LivingEntity liveTarget = resolveAgeOfBabylonTarget(serverLevel, entity, data);
               if (liveTarget != null) {
                  spawnEarthWeapons(entity, serverLevel, liveTarget, perRound, batchIndex, 3.05F, true);
                  triggerProjectionCounterVolley(liveTarget, entity, serverLevel, batchIndex, counterDuel);
               }
            }
         });
      }
      return true;
   }

   private static boolean tryEnkiduSmallSkill(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase, double distance) {
      if (tryClayBulwark(entity, level, now, phase)) {
         return true;
      }
      if (tryStardustStep(entity, level, target, now, phase, distance)) {
         return true;
      }
      if (tryNaturePulse(entity, level, now, phase)) {
         return true;
      }
      if (trySkySpearSweep(entity, level, target, now, phase, distance)) {
         return true;
      }
      if (tryEarthWedge(entity, level, target, now, phase, distance)) {
         return true;
      }
      if (tryEarthSpikeSprout(entity, level, target, now, phase, distance)) {
         return true;
      }
      return tryChainLash(entity, level, target, now, phase, distance, false);
   }

   private static boolean tryFlyingBasicAttack(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, double distance) {
      if (distance <= 13.5 && entity.getRandom().nextBoolean() && tryChainLash(entity, level, target, now, ServantCombatSystem.getPhase(entity), distance, true)) {
         return true;
      }
      return distance <= 20.0 && trySmallAgeOfBabylon(entity, level, target, now);
   }

   private static boolean tryMeleeLunge(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase, double distance) {
      CompoundTag data = entity.getPersistentData();
      if (distance < 2.4 || distance > 5.4 || now - data.getLong(TAG_LAST_MELEE_LUNGE) < phasedCooldown(MELEE_LUNGE_COOLDOWN, phase)) {
         return false;
      }
      if (entity.getRandom().nextInt(100) >= phaseChance(34, phase)) {
         return false;
      }
      data.putLong(TAG_LAST_MELEE_LUNGE, now);
      Vec3 dir = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
      entity.getNavigation().stop();
      entity.setDeltaMovement(dir.scale(0.82).add(0.0, 0.16, 0.0));
      entity.hurtMarked = true;
      entity.triggerChargeAnimation();
      Vec3 start = entity.position().add(0.0, entity.getBbHeight() * 0.52, 0.0);
      for (int i = 0; i < 7; i++) {
         Vec3 p = start.add(dir.scale(i * 0.48));
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.03);
         level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.02);
      }
      TYPE_MOON_WORLD.queueServerWork(5, () -> {
         if (entity.isAlive() && target.isAlive() && entity.distanceToSqr(target) <= 42.0 && entity.level() instanceof ServerLevel serverLevel) {
            entity.faceToward(target.position());
            entity.triggerSlashAnimation();
            applyMeleeDamage(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (1.0 + phase.id() * 0.12) + 6.0F), 0.5, false);
            Vec3 hit = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, hit.x, hit.y, hit.z, 2, 0.0, 0.0, 0.0, 0.0);
            spawnPlantPatch(serverLevel, target.position(), 2, 55, true);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.HOSTILE, 0.9F, 1.45F);
         }
      });
      return true;
   }

   private static boolean tryMeleeBasic(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase, double distance) {
      if (distance > 4.25 || ServantCombatSystem.cannotAct(entity)) {
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      int cooldown = Math.max(10, MELEE_BASIC_COOLDOWN - phase.id() * 3);
      if (now - data.getLong(TAG_LAST_MELEE_BASIC) < cooldown) {
         return false;
      }
      int variant = data.getInt(TAG_MELEE_COMBO_STEP) % 5;
      data.putInt(TAG_MELEE_COMBO_STEP, variant + 1);
      data.putLong(TAG_LAST_MELEE_BASIC, now);
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.55, 0.0));
      return switch (variant) {
         case 0 -> meleePalmStrike(entity, level, target, phase);
         case 1 -> meleeSweepingClayArm(entity, level, target, phase);
         case 2 -> meleeRisingRootUppercut(entity, level, target, phase);
         case 3 -> meleeRootSnareKick(entity, level, target, phase);
         default -> meleeShortChainPierce(entity, level, target, phase);
      };
   }

   private static boolean meleePalmStrike(EnkiduEntity entity, ServerLevel level, LivingEntity target, ServantCombatPhase phase) {
      entity.triggerSlashAnimation();
      Vec3 hit = target.position().add(0.0, target.getBbHeight() * 0.52, 0.0);
      applyMorphingLimbBasic(entity, level, target, phase, hit);
      return true;
   }

   private static void applyMorphingLimbBasic(EnkiduEntity entity, ServerLevel level, LivingEntity target, ServantCombatPhase phase, Vec3 hit) {
      int form = entity.getRandom().nextInt(4);
      float damage = (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.7 + phase.id() * 0.08));
      switch (form) {
         case 0 -> {
            applyMeleeDamage(entity, target, damage + 2.0F, 0.1, false);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 50 + phase.id() * 10, 0, false, true, true));
            level.sendParticles(ParticleTypes.CRIT, hit.x, hit.y, hit.z, 18, 0.2, 0.24, 0.2, 0.1);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.9F, 1.55F);
         }
         case 1 -> {
            applyMeleeDamage(entity, target, damage + 4.0F, 0.22, false);
            level.sendParticles(ParticleTypes.END_ROD, hit.x, hit.y, hit.z, 16, 0.15, 0.18, 0.15, 0.08);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.HOSTILE, 0.9F, 1.35F);
         }
         case 2 -> {
            applyMeleeDamage(entity, target, damage + 1.0F, 0.62, false);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.16, 0.0));
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.POOF, hit.x, hit.y, hit.z, 16, 0.28, 0.25, 0.28, 0.08);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.75F, 1.65F);
         }
         default -> {
            applyMeleeDamage(entity, target, damage * 0.86F, 0.2, true);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 38 + phase.id() * 10, 0, false, true, true));
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, hit.x, hit.y, hit.z, 20, 0.35, 0.2, 0.35, 0.08);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_STEP, SoundSource.HOSTILE, 0.9F, 1.5F);
         }
      }
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, hit.x, hit.y, hit.z, 12, 0.22, 0.24, 0.22, 0.045);
   }

   private static boolean meleeSweepingClayArm(EnkiduEntity entity, ServerLevel level, LivingEntity target, ServantCombatPhase phase) {
      entity.triggerSweepAnimation();
      Vec3 look = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (look.lengthSqr() < 1.0E-4) {
         look = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      }
      look = look.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
      AABB sweepBox = entity.getBoundingBox().inflate(3.2, 1.0, 3.2).move(look.scale(1.25));
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, sweepBox,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e))) {
         applyMeleeDamage(entity, living, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.58 + phase.id() * 0.08)), 0.28, false);
      }
      Vec3 fx = entity.position().add(look.scale(1.8)).add(0.0, entity.getBbHeight() * 0.48, 0.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, fx.x, fx.y, fx.z, 3, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, fx.x, fx.y - 0.25, fx.z, 18, 0.7, 0.18, 0.7, 0.04);
      spawnPlantPatch(level, fx, 2, 55, false);
      return true;
   }

   private static boolean meleeRisingRootUppercut(EnkiduEntity entity, ServerLevel level, LivingEntity target, ServantCombatPhase phase) {
      entity.triggerUppercutAnimation();
      applyMeleeDamage(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.78 + phase.id() * 0.1) + 3.0), 0.08, false);
      target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.38 + phase.id() * 0.08, 0.0));
      target.hurtMarked = true;
      Vec3 pos = target.position();
      spawnPlantPatch(level, pos, 2, 65, true);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 0.2, pos.z, 22, 0.35, 0.2, 0.35, 0.07);
      level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 0.75, pos.z, 10, 0.25, 0.35, 0.25, 0.1);
      level.playSound(null, target.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 0.9F, 1.05F);
      return true;
   }

   private static boolean meleeRootSnareKick(EnkiduEntity entity, ServerLevel level, LivingEntity target, ServantCombatPhase phase) {
      entity.triggerHorizontalSwingAnimation();
      applyMeleeDamage(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.62 + phase.id() * 0.08)), 0.16, true);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 34 + phase.id() * 12, 1, false, true, true));
      spawnPlantPatch(level, target.position(), 3, 70, true);
      Vec3 pos = target.position().add(0.0, 0.25, 0.0);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 26, 0.45, 0.18, 0.45, 0.05);
      level.playSound(null, target.blockPosition(), SoundEvents.GRASS_BREAK, SoundSource.HOSTILE, 1.0F, 0.72F);
      return true;
   }

   private static boolean meleeShortChainPierce(EnkiduEntity entity, ServerLevel level, LivingEntity target, ServantCombatPhase phase) {
      if (isChainForbiddenTarget(entity, target)) return false;
      entity.triggerNamedActionAnimation("chain_of_heaven");
      applyMeleeDamage(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.7 + phase.id() * 0.1) + (hasTrait(target, ServantTraitTag.DIVINE) ? 5.0 : 1.5)), 0.2, true);
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45 + phase.id() * 15, 0, false, true, true));
      VFXServerEffects.spawn(level, "servant_enkidu_chain_of_heaven", target, 96.0);
      Vec3 pos = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 10, 0.22, 0.22, 0.22, 0.06);
      level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 0.9F, 1.7F);
      return true;
   }

   private static void applyMeleeDamage(EnkiduEntity entity, LivingEntity target, float damage, double pushStrength, boolean pullToEnkidu) {
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), damage);
      target.invulnerableTime = 0;
      Vec3 direction = pullToEnkidu ? entity.position().subtract(target.position()) : target.position().subtract(entity.position());
      direction = direction.multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() > 1.0E-4) {
         direction = direction.normalize().scale(pushStrength);
         target.push(direction.x, 0.06, direction.z);
         target.hurtMarked = true;
      }
   }

   private static boolean tryChainLash(
      EnkiduEntity entity,
      ServerLevel level,
      LivingEntity target,
      long now,
      ServantCombatPhase phase,
      double distance,
      boolean airborneBasic
   ) {
      if (isChainForbiddenTarget(entity, target)) return false;
      CompoundTag data = entity.getPersistentData();
      int cooldown = airborneBasic ? 30 : phasedCooldown(CHAIN_LASH_COOLDOWN, phase);
      int chance = airborneBasic ? 70 : phaseChance(18, phase);
      if (distance > 13.5 || now - data.getLong(TAG_LAST_CHAIN_LASH) < cooldown || entity.getRandom().nextInt(100) >= chance) {
         return false;
      }
      data.putLong(TAG_LAST_CHAIN_LASH, now);
      entity.triggerNamedActionAnimation("chain_of_heaven");
      VFXServerEffects.spawn(level, "servant_enkidu_chain_of_heaven", target, 96.0);
      spawnPlantPatch(level, target.position(), 2, 70, true);
      float damage = (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (airborneBasic ? 0.46 : 0.58) + (hasTrait(target, ServantTraitTag.DIVINE) ? 8.0 : 3.0));
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), damage);
      target.invulnerableTime = 0;
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, airborneBasic ? 25 : 45, hasTrait(target, ServantTraitTag.DIVINE) ? 2 : 1, false, true, true));
      Vec3 pull = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (pull.lengthSqr() > 1.0E-4) {
         pull = pull.normalize().scale(airborneBasic ? 0.28 : 0.42);
         target.push(pull.x, 0.08, pull.z);
         target.hurtMarked = true;
      }
      level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 0.85F, 1.55F);
      return true;
   }

   private static boolean tryEarthSpikeSprout(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase, double distance) {
      CompoundTag data = entity.getPersistentData();
      if (distance > 18.0
         || entity.getCurrentMp() < 8.0
         || now - data.getLong(TAG_LAST_EARTH_SPIKE) < phasedCooldown(EARTH_SPIKE_COOLDOWN, phase)
         || entity.getRandom().nextInt(100) >= phaseChance(16, phase)) {
         return false;
      }
      data.putLong(TAG_LAST_EARTH_SPIKE, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 8.0));
      entity.triggerNamedActionAnimation("age_of_babylon");
      Vec3 center = target.position();
      spawnPlantPatch(level, center, 3, 95, true);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, target.getY() + 0.12, center.z, 26, 0.75, 0.08, 0.75, 0.05);
      level.sendParticles(ParticleTypes.CRIT, center.x, target.getY() + 0.35, center.z, 18, 0.5, 0.18, 0.5, 0.08);
      level.playSound(null, target.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.15F, 0.85F);
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.72 + 10.0));
      target.invulnerableTime = 0;
      target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.42, 0.0));
      target.hurtMarked = true;
      return true;
   }

   private static boolean trySkySpearSweep(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase, double distance) {
      CompoundTag data = entity.getPersistentData();
      if (distance > 8.5
         || now - data.getLong(TAG_LAST_SKY_SPEAR_SWEEP) < phasedCooldown(SKY_SPEAR_SWEEP_COOLDOWN, phase)
         || entity.getRandom().nextInt(100) >= phaseChance(20, phase)) {
         return false;
      }
      data.putLong(TAG_LAST_SKY_SPEAR_SWEEP, now);
      entity.triggerHorizontalSwingAnimation();
      Vec3 forward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 origin = entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
      AABB area = entity.getBoundingBox().inflate(6.5, 1.4, 6.5).move(forward.scale(2.4));
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 rel = living.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
         if (rel.lengthSqr() > 1.0E-4 && rel.normalize().dot(forward) < 0.15) {
            continue;
         }
         applyMeleeDamage(entity, living, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (1.25 + phase.id() * 0.13) + 8.0F), 0.7, false);
      }
      for (int i = -5; i <= 5; i++) {
         Vec3 fx = origin.add(forward.scale(2.2 + Math.abs(i) * 0.18)).add(side.scale(i * 0.58));
         level.sendParticles(ParticleTypes.END_ROD, fx.x, fx.y, fx.z, 6, 0.08, 0.08, 0.08, 0.035);
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, fx.x, fx.y, fx.z, 1, 0.0, 0.0, 0.0, 0.0);
      }
      spawnPlantPatch(level, entity.position().add(forward.scale(3.2)), 3, 75, true);
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.HOSTILE, 1.1F, 0.92F);
      return true;
   }

   private static boolean tryEarthWedge(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase, double distance) {
      CompoundTag data = entity.getPersistentData();
      if (distance > 15.0
         || now - data.getLong(TAG_LAST_EARTH_WEDGE) < phasedCooldown(EARTH_WEDGE_COOLDOWN, phase)
         || entity.getRandom().nextInt(100) >= phaseChance(18, phase)) {
         return false;
      }
      data.putLong(TAG_LAST_EARTH_WEDGE, now);
      entity.triggerUppercutAnimation();
      Vec3 forward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
      spawnEarthWedgePath(entity, level, forward, phase);
      Vec3 leap = entity.position().add(forward.scale(Math.min(7.5, Math.max(3.5, distance * 0.65))));
      BlockPos top = findSurface(level, BlockPos.containing(leap)).above(3);
      entity.getNavigation().stop();
      entity.setDeltaMovement(forward.scale(0.72).add(0.0, 0.52, 0.0));
      entity.hurtMarked = true;
      if (entity.distanceToSqr(Vec3.atCenterOf(top)) < 90.0) {
         TYPE_MOON_WORLD.queueServerWork(5, () -> {
            if (entity.isAlive()) {
               entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, 0.18, 0.0));
            }
         });
      }
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.DEEPSLATE_BREAK, SoundSource.HOSTILE, 1.25F, 0.65F);
      return true;
   }

   private static boolean tryClayBulwark(EnkiduEntity entity, ServerLevel level, long now, ServantCombatPhase phase) {
      CompoundTag data = entity.getPersistentData();
      if (entity.getHealth() > entity.getMaxHealth() * (phase == ServantCombatPhase.DECISIVE ? 0.72F : 0.55F)
         || entity.getCurrentMp() < 12.0
         || now - data.getLong(TAG_LAST_CLAY_BULWARK) < phasedCooldown(CLAY_BULWARK_COOLDOWN, phase)
         || entity.getRandom().nextInt(100) >= phaseChance(24, phase)) {
         return false;
      }
      data.putLong(TAG_LAST_CLAY_BULWARK, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 12.0));
      entity.triggerNamedActionAnimation("perfect_form");
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 90, 1, false, true, true));
      LivingEntity target = entity.getTarget();
      growPlantBulwark(entity, level, target != null && target.isAlive() ? target.position() : entity.position().add(entity.getLookAngle().scale(8.0)), false);
      return true;
   }

   private static boolean tryStardustStep(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase, double distance) {
      CompoundTag data = entity.getPersistentData();
      boolean badRange = distance < 4.0 || distance > 17.0;
      if (!badRange
         || entity.getCurrentMp() < 10.0
         || now - data.getLong(TAG_LAST_STARDUST_STEP) < phasedCooldown(STARDUST_STEP_COOLDOWN, phase)
         || entity.getRandom().nextInt(100) >= phaseChance(20, phase)) {
         return false;
      }
      data.putLong(TAG_LAST_STARDUST_STEP, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 10.0));
      Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-4) {
         away = entity.getLookAngle().multiply(-1.0, 0.0, -1.0);
      }
      away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
      Vec3 side = new Vec3(-away.z, 0.0, away.x).scale(entity.getRandom().nextBoolean() ? 1.0 : -1.0);
      Vec3 move = distance < 4.0 ? away.scale(2.6).add(side.scale(1.6)) : away.scale(-2.2).add(side.scale(2.4));
      Vec3 before = entity.position().add(0.0, entity.getBbHeight() * 0.45, 0.0);
      entity.setDeltaMovement(entity.getDeltaMovement().add(move.x * 0.32, 0.16, move.z * 0.32));
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 70, 1, false, true, true));
      entity.triggerNamedActionAnimation("transfiguration");
      level.sendParticles(ParticleTypes.END_ROD, before.x, before.y, before.z, 22, 0.28, 0.28, 0.28, 0.08);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, before.x, before.y, before.z, 18, 0.36, 0.36, 0.36, 0.06);
      spawnPlantPatch(level, entity.position().add(move.normalize().scale(1.2)), 2, 60, false);
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.8F, 1.75F);
      return true;
   }

   private static boolean tryNaturePulse(EnkiduEntity entity, ServerLevel level, long now, ServantCombatPhase phase) {
      CompoundTag data = entity.getPersistentData();
      List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(5.8),
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e));
      if (nearby.isEmpty()
         || entity.getCurrentMp() < 10.0
         || now - data.getLong(TAG_LAST_NATURE_PULSE) < phasedCooldown(NATURE_PULSE_COOLDOWN, phase)
         || entity.getRandom().nextInt(100) >= phaseChance(18 + nearby.size() * 4, phase)) {
         return false;
      }
      data.putLong(TAG_LAST_NATURE_PULSE, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 10.0));
      entity.triggerNamedActionAnimation("presence_detection");
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_presence_detection", entity, 0.65F);
      spawnPlantPatch(level, entity.position(), 5, 100, true);
      for (LivingEntity living : nearby) {
         living.invulnerableTime = 0;
         living.hurt(entity.damageSources().magic(), 10.0F + phase.id() * 3.0F);
         living.invulnerableTime = 0;
         living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true, true));
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 0, false, true, true));
      }
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GRASS_BREAK, SoundSource.HOSTILE, 1.1F, 0.65F);
      return true;
   }

   private static void growPlantBulwark(EnkiduEntity entity, ServerLevel level, Vec3 threat, boolean againstNoblePhantasm) {
      Vec3 forward = threat.subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      final Vec3 growthForward = forward;
      final Vec3 growthSide = side;
      BlockPos base = findSurface(level, BlockPos.containing(entity.position().add(forward.scale(3.0))));
      int radius = againstNoblePhantasm ? 14 : 9;
      int height = againstNoblePhantasm ? 15 : 11;
      int duration = againstNoblePhantasm ? 180 : 130;
      List<BlockPos> placed = new ArrayList<>();
      Set<BlockPos> seen = new HashSet<>();
      for (int y = 0; y <= height; y++) {
         final int layer = y;
         TYPE_MOON_WORLD.queueServerWork(layer * 2, () -> {
            if (!entity.isAlive()) {
               return;
            }
            double vertical = (double)layer / Math.max(1.0, height);
            double layerRadius = radius * Math.sqrt(Math.max(0.0, 1.0 - vertical * vertical * 0.82));
            int r = Math.max(1, (int)Math.ceil(layerRadius));
            for (int sx = -r; sx <= r; sx++) {
               for (int depth = -1; depth <= r; depth++) {
                  double normalized = (sx * sx) / Math.max(1.0, layerRadius * layerRadius) + (depth * depth) / Math.max(1.0, radius * radius);
                  double noise = blockNoise(level, base.offset(sx, layer, depth));
                  if (normalized > 1.05 + (noise - 0.5) * 0.32 || noise < 0.08) {
                     continue;
                  }
                  Vec3 offset = growthSide.scale(sx).add(growthForward.scale(depth));
                  BlockPos pos = BlockPos.containing(base.getX() + 0.5 + offset.x, base.getY() + layer, base.getZ() + 0.5 + offset.z);
                  if (!seen.add(pos) || !canGrowTemporaryPlant(level, pos)) {
                     continue;
                  }
                  if (layer > 1 && normalized < 0.22 && noise < 0.36) {
                     continue;
                  }
                  AxisChoice axis = bulwarkAxis(offset, layer, noise);
                  boolean core = layer <= 1 || normalized < 0.34 || noise > 0.82;
                  BlockState plant = temporaryBulwarkState(level, pos, core, layer, height, axis.axis());
                  level.setBlock(pos, plant, 3);
                  EnkiduTemporaryPlantHelper.register(level, pos, level.getGameTime() + duration);
                  placed.add(pos.immutable());
                  if (core && layer > 1 && noise > 0.86) {
                     BlockPos branch = pos.relative(axis.branchDirection());
                     if (seen.add(branch) && canGrowTemporaryPlant(level, branch)) {
                        level.setBlock(branch, orientedLogStateForGround(level, branch, noise, axis.axis()), 3);
                        EnkiduTemporaryPlantHelper.register(level, branch, level.getGameTime() + duration);
                        placed.add(branch.immutable());
                     }
                  }
                  if (layer > height * 0.55 && noise > 0.92) {
                     BlockPos crown = pos.above();
                     if (seen.add(crown) && canGrowTemporaryPlant(level, crown)) {
                        level.setBlock(crown, leafStateForGround(level, crown, noise), 3);
                        EnkiduTemporaryPlantHelper.register(level, crown, level.getGameTime() + duration);
                        placed.add(crown.immutable());
                     }
                  }
                  if (noise > 0.7 && layer > 0) {
                     level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.18, 0.18, 0.18, 0.03);
                  }
               }
            }
            Vec3 layerCenter = Vec3.atCenterOf(base.above(layer));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, layerCenter.x, layerCenter.y, layerCenter.z, againstNoblePhantasm ? 22 : 14, radius * 0.35, 0.18, radius * 0.35, 0.05);
            level.playSound(null, base, layer == 0 ? SoundEvents.ROOTED_DIRT_BREAK : SoundEvents.WOOD_PLACE, SoundSource.HOSTILE, againstNoblePhantasm ? 1.3F : 0.95F, 0.72F + layer * 0.06F);
         });
      }
      TYPE_MOON_WORLD.queueServerWork(duration, () -> clearTemporaryPlants(level, placed));
   }

   private static void spawnPlantPatch(ServerLevel level, Vec3 center, int radius, int duration, boolean lush) {
      BlockPos base = findSurface(level, BlockPos.containing(center));
      List<BlockPos> placed = new ArrayList<>();
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            BlockPos surface = findSurface(level, base.offset(dx, 0, dz));
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radius + 0.25 || blockNoise(level, surface) < 0.28) {
               continue;
            }
            BlockPos pos = surface.above();
            if (!canGrowTemporaryPlant(level, pos)) {
               continue;
            }
            double noise = blockNoise(level, pos);
            BlockState state = smallPlantState(level, pos);
            level.setBlock(pos, state, 3);
            EnkiduTemporaryPlantHelper.register(level, pos, level.getGameTime() + duration);
            placed.add(pos.immutable());
            if (lush && noise > 0.84) {
               int height = noise > 0.94 ? 3 : 2;
               for (int y = 1; y < height; y++) {
                  BlockPos trunk = pos.above(y);
                  if (!canGrowTemporaryPlant(level, trunk)) {
                     break;
                  }
                  level.setBlock(trunk, orientedLogStateForGround(level, trunk, noise, Direction.Axis.Y), 3);
                  EnkiduTemporaryPlantHelper.register(level, trunk, level.getGameTime() + duration);
                  placed.add(trunk.immutable());
               }
            }
         }
      }
      Vec3 fx = Vec3.atCenterOf(base);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, fx.x, fx.y + 0.45, fx.z, lush ? 28 : 16, radius * 0.45, 0.18, radius * 0.45, 0.04);
      TYPE_MOON_WORLD.queueServerWork(duration, () -> clearTemporaryPlants(level, placed));
   }

   private static void spawnEarthWedgePath(EnkiduEntity entity, ServerLevel level, Vec3 forward, ServantCombatPhase phase) {
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      List<BlockPos> placed = new ArrayList<>();
      Set<BlockPos> seen = new HashSet<>();
      int duration = 95 + phase.id() * 20;
      for (int step = 1; step <= 7; step++) {
         final int pathStep = step;
         TYPE_MOON_WORLD.queueServerWork(step, () -> {
            Vec3 center = entity.position().add(forward.scale(pathStep * 1.25));
            BlockPos surface = findSurface(level, BlockPos.containing(center));
            int height = Math.min(5, 1 + pathStep / 2 + phase.id());
            int halfWidth = pathStep >= 4 ? 1 : 0;
            for (int w = -halfWidth; w <= halfWidth; w++) {
               for (int y = 0; y < height; y++) {
                  BlockPos pos = BlockPos.containing(surface.getX() + 0.5 + side.x * w, surface.getY() + y, surface.getZ() + 0.5 + side.z * w);
                  if (!seen.add(pos) || !canGrowTemporaryPlant(level, pos)) {
                     continue;
                  }
                  double noise = blockNoise(level, pos);
                  BlockState state = y == height - 1 && noise > 0.55
                     ? Blocks.POINTED_DRIPSTONE.defaultBlockState()
                     : (noise > 0.62 ? Blocks.ROOTED_DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState());
                  level.setBlock(pos, state, 3);
                  EnkiduTemporaryPlantHelper.register(level, pos, level.getGameTime() + duration);
                  placed.add(pos.immutable());
               }
            }
            Vec3 fx = Vec3.atCenterOf(surface).add(0.0, height * 0.55, 0.0);
            level.sendParticles(ParticleTypes.POOF, fx.x, fx.y, fx.z, 12, 0.35, 0.45, 0.35, 0.08);
            level.sendParticles(ParticleTypes.CRIT, fx.x, fx.y + 0.25, fx.z, 8, 0.22, 0.28, 0.22, 0.08);
            AABB hitBox = new AABB(surface).inflate(1.2, height + 0.6, 1.2);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, hitBox,
               e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e))) {
               living.invulnerableTime = 0;
               living.hurt(entity.damageSources().mobAttack(entity), (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.55 + 9.0F + phase.id() * 3.0F));
               living.invulnerableTime = 0;
               living.setDeltaMovement(living.getDeltaMovement().add(forward.x * 0.22, 0.48, forward.z * 0.22));
               living.hurtMarked = true;
            }
         });
      }
      TYPE_MOON_WORLD.queueServerWork(duration, () -> clearTemporaryPlants(level, placed));
   }

   private static BlockPos findSurface(ServerLevel level, BlockPos start) {
      BlockPos pos = start;
      for (int i = 0; i < 8 && !level.getBlockState(pos.below()).isSolidRender(level, pos.below()); i++) {
         pos = pos.below();
      }
      for (int i = 0; i < 8 && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(); i++) {
         pos = pos.above();
      }
      return pos;
   }

   private static Vec3 findClayRegroupPosition(EnkiduEntity entity, ServerLevel level, LivingEntity target) {
      Vec3 anchor = target != null && target.isAlive() ? target.position() : entity.position();
      Vec3 away = entity.position().subtract(anchor).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-4) {
         away = new Vec3(entity.getRandom().nextDouble() - 0.5, 0.0, entity.getRandom().nextDouble() - 0.5);
      }
      away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
      Vec3 side = new Vec3(-away.z, 0.0, away.x);
      Vec3[] candidates = new Vec3[] {
         anchor.add(away.scale(3.2)),
         anchor.add(side.scale(3.0)),
         anchor.subtract(side.scale(3.0)),
         entity.position().add(away.scale(1.8)),
         entity.position()
      };
      for (Vec3 candidate : candidates) {
         BlockPos feet = findSafeFeet(level, BlockPos.containing(candidate));
         if (feet == null) {
            continue;
         }
         Vec3 dest = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
         if (level.noCollision(entity, entity.getBoundingBox().move(dest.subtract(entity.position())))) {
            return dest;
         }
      }
      return entity.position();
   }

   private static BlockPos findSafeFeet(ServerLevel level, BlockPos anchor) {
      for (int dy = -3; dy <= 4; dy++) {
         BlockPos feet = anchor.offset(0, dy, 0);
         BlockPos below = feet.below();
         if (level.getBlockState(below).isSolidRender(level, below)
            && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
            && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
            return feet;
         }
      }
      return null;
   }

   private static void spawnClayBodyBreakFx(ServerLevel level, Vec3 center, float width, float height) {
      level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 34, width * 0.8, height * 0.35, width * 0.8, 0.08);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y, center.z, 18, width * 0.7, height * 0.28, width * 0.7, 0.03);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y, center.z, 18, width * 0.75, height * 0.35, width * 0.75, 0.04);
      for (int i = 0; i < 8; i++) {
         double angle = Math.PI * 2.0 * i / 8.0;
         level.sendParticles(ParticleTypes.POOF, center.x + Math.cos(angle) * width, center.y, center.z + Math.sin(angle) * width, 4, 0.08, 0.16, 0.08, 0.02);
      }
   }

   private static void spawnClayBodyReformFx(ServerLevel level, Vec3 center) {
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y, center.z, 36, 0.45, 0.75, 0.45, 0.06);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 20, 0.32, 0.58, 0.32, 0.045);
      level.sendParticles(ParticleTypes.POOF, center.x, center.y - 0.25, center.z, 16, 0.35, 0.18, 0.35, 0.04);
   }

   private static boolean canGrowTemporaryPlant(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return !state.hasBlockEntity() && (state.isAir() || state.canBeReplaced());
   }

   private static BlockState temporaryBulwarkState(ServerLevel level, BlockPos pos, boolean core, int layer, int height, Direction.Axis axis) {
      double noise = blockNoise(level, pos);
      if (core || noise < 0.88) {
         return orientedLogStateForGround(level, pos, noise, axis);
      }
      if (isWetGround(level, pos.below())) {
         return Blocks.MANGROVE_ROOTS.defaultBlockState();
      }
      if (layer >= height - 1 && noise > 0.96) {
         return leafStateForGround(level, pos, noise);
      }
      if (noise > 0.94) {
         return leafStateForGround(level, pos, noise);
      }
      if (noise > 0.91) {
         return leafStateForGround(level, pos, noise);
      }
      return orientedLogStateForGround(level, pos, noise, axis);
   }

   private static BlockState smallPlantState(ServerLevel level, BlockPos pos) {
      double noise = blockNoise(level, pos);
      if (isWetGround(level, pos.below()) && noise > 0.58) {
         return Blocks.MANGROVE_ROOTS.defaultBlockState();
      }
      if (noise > 0.72) {
         return Blocks.MOSS_BLOCK.defaultBlockState();
      }
      if (noise > 0.58) {
         return Blocks.MOSS_CARPET.defaultBlockState();
      }
      if (noise > 0.42) {
         return Blocks.FERN.defaultBlockState();
      }
      return Blocks.SHORT_GRASS.defaultBlockState();
   }

   private static BlockState logStateForGround(ServerLevel level, BlockPos pos, double noise) {
      return orientedLogStateForGround(level, pos, noise, Direction.Axis.Y);
   }

   private static BlockState orientedLogStateForGround(ServerLevel level, BlockPos pos, double noise, Direction.Axis axis) {
      BlockState state;
      if (isWetGround(level, pos.below())) {
         state = Blocks.MANGROVE_LOG.defaultBlockState();
      } else if (isColdGround(level, pos.below())) {
         state = Blocks.SPRUCE_LOG.defaultBlockState();
      } else if (isDryGround(level, pos.below())) {
         state = Blocks.ACACIA_LOG.defaultBlockState();
      } else if (noise > 0.84) {
         state = Blocks.CHERRY_LOG.defaultBlockState();
      } else if (noise > 0.66) {
         state = Blocks.JUNGLE_LOG.defaultBlockState();
      } else if (noise > 0.42) {
         state = Blocks.OAK_LOG.defaultBlockState();
      } else {
         state = Blocks.BIRCH_LOG.defaultBlockState();
      }
      return state.hasProperty(RotatedPillarBlock.AXIS) ? state.setValue(RotatedPillarBlock.AXIS, axis) : state;
   }

   private static BlockState leafStateForGround(ServerLevel level, BlockPos pos, double noise) {
      if (isColdGround(level, pos.below())) {
         return Blocks.SPRUCE_LEAVES.defaultBlockState();
      }
      if (noise > 0.75) {
         return Blocks.CHERRY_LEAVES.defaultBlockState();
      }
      if (noise > 0.45) {
         return Blocks.JUNGLE_LEAVES.defaultBlockState();
      }
      return Blocks.OAK_LEAVES.defaultBlockState();
   }

   private static AxisChoice bulwarkAxis(Vec3 offset, int layer, double noise) {
      if (layer <= 1 || noise < 0.26) {
         Direction dir = Math.abs(offset.x) > Math.abs(offset.z)
            ? (offset.x >= 0.0 ? Direction.EAST : Direction.WEST)
            : (offset.z >= 0.0 ? Direction.SOUTH : Direction.NORTH);
         return new AxisChoice(dir.getAxis(), dir);
      }
      if (noise > 0.78) {
         return new AxisChoice(Direction.Axis.Y, Direction.UP);
      }
      Direction dir = Math.abs(offset.x) > Math.abs(offset.z)
         ? (offset.x >= 0.0 ? Direction.EAST : Direction.WEST)
         : (offset.z >= 0.0 ? Direction.SOUTH : Direction.NORTH);
      return new AxisChoice(dir.getAxis(), dir);
   }

   private record AxisChoice(Direction.Axis axis, Direction branchDirection) {
   }

   private static void clearTemporaryPlants(ServerLevel level, List<BlockPos> positions) {
      for (BlockPos pos : positions) {
         EnkiduTemporaryPlantHelper.unregister(level, pos);
         if (EnkiduTemporaryPlantHelper.isTemporaryPlantState(level.getBlockState(pos))) {
            level.removeBlock(pos, false);
         }
      }
   }

   private static boolean isWetGround(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.is(Blocks.MUD)
         || state.is(Blocks.CLAY)
         || state.is(Blocks.MANGROVE_ROOTS)
         || state.is(Blocks.WATER)
         || state.is(Blocks.SEAGRASS);
   }

   private static boolean isColdGround(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.is(Blocks.SNOW_BLOCK)
         || state.is(Blocks.POWDER_SNOW)
         || state.is(Blocks.ICE)
         || state.is(Blocks.PACKED_ICE)
         || state.is(Blocks.BLUE_ICE)
         || state.is(Blocks.SPRUCE_LOG)
         || state.is(Blocks.PODZOL);
   }

   private static boolean isDryGround(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.is(Blocks.SAND)
         || state.is(Blocks.RED_SAND)
         || state.is(Blocks.SANDSTONE)
         || state.is(Blocks.RED_SANDSTONE)
         || state.is(Blocks.TERRACOTTA)
         || state.is(Blocks.DEAD_BUSH);
   }

   private static double blockNoise(ServerLevel level, BlockPos pos) {
      long seed = pos.asLong() ^ (level.getGameTime() * 341873128712L);
      seed ^= seed >>> 33;
      seed *= 0xff51afd7ed558ccdL;
      seed ^= seed >>> 33;
      seed *= 0xc4ceb9fe1a85ec53L;
      seed ^= seed >>> 33;
      return (double)(seed & 0xFFFFFFL) / (double)0x1000000;
   }

   private static void spawnEarthWeapons(EnkiduEntity entity, ServerLevel level, LivingEntity target, int count, int batch, float speed, boolean volley) {
      Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 origin = entity.position().add(0.0, 0.8, 0.0);
      Vec3 toTarget = targetCenter.subtract(origin);
      Vec3 forward = new Vec3(toTarget.x, 0.0, toTarget.z);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(entity.getLookAngle().x, 0.0, entity.getLookAngle().z);
      }
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 volleyCenter = entity.position().subtract(forward.scale(5.5 + Math.min(18.0, batch * 1.1)));
      int volleyColumns = volley ? Math.max(8, (int)Math.ceil(Math.sqrt(count * 1.35))) : 1;
      int volleyRows = volley ? Math.max(1, (int)Math.ceil((double)count / volleyColumns)) : 1;
      double sideSpacing = volley ? (count >= 80 ? 3.8 : 3.15) : 1.15;
      double rowSpacing = volley ? (count >= 80 ? 3.35 : 2.75) : 0.7;
      for (int i = 0; i < count; i++) {
         int globalIndex = batch * count + i;
         double spreadIndex = i - (count - 1) * 0.5;
         Vec3 spawn;
         Vec3 aimPoint;
         if (volley) {
            int row = i / volleyColumns;
            int col = i % volleyColumns;
            double forwardOffset = -(row - (volleyRows - 1) * 0.42) * rowSpacing - entity.getRandom().nextDouble() * 1.45;
            double sideOffset = (col - (volleyColumns - 1) * 0.5) * sideSpacing + (entity.getRandom().nextDouble() - 0.5) * 1.45;
            Vec3 ringOffset = forward.scale(forwardOffset).add(side.scale(sideOffset));
            spawn = groundSpawn(level, volleyCenter.add(ringOffset));
            aimPoint = targetCenter
               .add(side.scale((entity.getRandom().nextDouble() - 0.5) * (count >= 80 ? 8.5 : 5.2)))
               .add(forward.scale((entity.getRandom().nextDouble() - 0.5) * (count >= 80 ? 6.5 : 3.8)))
               .add(0.0, (entity.getRandom().nextDouble() - 0.5) * 2.1, 0.0);
         } else {
            double sideOffset = spreadIndex * 1.15 + (entity.getRandom().nextDouble() - 0.5) * 0.75;
            double forwardOffset = 1.8 + (i % 3) * 0.7 + entity.getRandom().nextDouble() * 0.8;
            spawn = groundSpawn(level, entity.position().add(forward.scale(forwardOffset)).add(side.scale(sideOffset)));
            aimPoint = targetCenter
               .add(side.scale((entity.getRandom().nextDouble() - 0.5) * 1.45))
               .add(0.0, (entity.getRandom().nextDouble() - 0.5) * 0.55, 0.0);
         }
         ItemStack stack = randomWeapon(entity, volley);
         float damage = applyAgeOfBabylonDivinitySpecialAttack(entity, target, weaponDamage(stack, volley));
         if (!volley || globalIndex % VOLLEY_EFFECT_STRIDE == 0) {
            spawnAgeOfBabylonGate(level, spawn, volley);
         }
         final Vec3 finalSpawn = spawn;
         final Vec3 finalAimPoint = aimPoint;
         final ItemStack finalStack = stack;
         final float finalDamage = damage;
         final int delay = 6 + (volley ? (globalIndex % 4) : (i % 3));
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (!entity.isAlive() || !(entity.level() instanceof ServerLevel serverLevel) || !target.isAlive()) {
               return;
            }
            EnkiduEarthWeaponProjectileEntity projectile;
            if (finalStack.is(Items.BOW) || finalStack.is(Items.CROSSBOW)) {
               projectile = EnkiduEarthWeaponProjectileEntity.bow(serverLevel, entity, finalStack, finalDamage, target, finalStack.is(Items.CROSSBOW));
            } else {
               projectile = EnkiduEarthWeaponProjectileEntity.weapon(serverLevel, entity, finalStack, finalDamage, target, volley ? 0.22F : 0.16F, true);
            }
            projectile.setPos(finalSpawn.x, finalSpawn.y, finalSpawn.z);
            projectile.setHeavyInteractions(!volley || globalIndex % VOLLEY_EFFECT_STRIDE == 0);
            double shotSpeed = speed + entity.getRandom().nextDouble() * (volley ? 0.72 : 0.42);
            if (finalStack.is(Items.BOW) || finalStack.is(Items.CROSSBOW)) {
               projectile.setDeltaMovement(0.0, 0.135, 0.0);
            } else if (volley) {
               Vec3 aim = finalAimPoint.subtract(finalSpawn);
               projectile.setDeltaMovement(aim.normalize().scale(shotSpeed));
            } else {
               projectile.setDeltaMovement(0.0, 0.14, 0.0);
            }
            projectile.alignToMotion();
            serverLevel.addFreshEntity(projectile);
            if (!volley || globalIndex % VOLLEY_EFFECT_STRIDE == 0) {
               spawnEarthWeaponBirthFx(serverLevel, finalSpawn, volley);
            }
         });
      }
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.HOSTILE, volley ? 1.4F : 0.75F, 1.35F);
   }

   private static void spawnEarthWeaponsAroundTarget(EnkiduEntity entity, ServerLevel level, LivingEntity target, int count, int batch, float speed, boolean volley) {
      Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      double baseRadius = volley ? 7.5 : 4.0;
      for (int i = 0; i < count; i++) {
         int globalIndex = batch * count + i;
         double angle = (Math.PI * 2.0 * i) / Math.max(1, count) + entity.getRandom().nextDouble() * 0.28;
         double radius = baseRadius + entity.getRandom().nextDouble() * (volley ? 7.0 : 2.0);
         Vec3 spawn = groundSpawn(level, target.position().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius));
         ItemStack stack = randomWeapon(entity, volley);
         float damage = applyAgeOfBabylonDivinitySpecialAttack(entity, target, weaponDamage(stack, volley));
         if (!volley || globalIndex % VOLLEY_EFFECT_STRIDE == 0) {
            spawnAgeOfBabylonGate(level, spawn, volley);
         }
         final Vec3 finalSpawn = spawn;
         final ItemStack finalStack = stack;
         final float finalDamage = damage;
         final int delay = 6 + (globalIndex % 5);
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (!entity.isAlive() || !(entity.level() instanceof ServerLevel serverLevel) || !target.isAlive()) {
               return;
            }
            EnkiduEarthWeaponProjectileEntity projectile;
            if (finalStack.is(Items.BOW) || finalStack.is(Items.CROSSBOW)) {
               projectile = EnkiduEarthWeaponProjectileEntity.bow(serverLevel, entity, finalStack, finalDamage, target, finalStack.is(Items.CROSSBOW));
            } else {
               projectile = EnkiduEarthWeaponProjectileEntity.weapon(serverLevel, entity, finalStack, finalDamage, target, volley ? 0.28F : 0.18F, true);
            }
            projectile.setPos(finalSpawn.x, finalSpawn.y, finalSpawn.z);
            projectile.setHeavyInteractions(!volley || globalIndex % VOLLEY_EFFECT_STRIDE == 0);
            projectile.setDeltaMovement(0.0, 0.15, 0.0);
            projectile.alignToMotion();
            serverLevel.addFreshEntity(projectile);
            if (!volley || globalIndex % VOLLEY_EFFECT_STRIDE == 0) {
               spawnEarthWeaponBirthFx(serverLevel, finalSpawn, volley);
            }
         });
      }
      level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.HOSTILE, volley ? 1.45F : 0.8F, 1.22F);
   }

   private static Vec3 groundSpawn(ServerLevel level, Vec3 approximate) {
      BlockPos column = BlockPos.containing(approximate.x, approximate.y, approximate.z);
      BlockPos heightSurface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
      BlockPos surface = findSurface(level, heightSurface);
      return Vec3.atBottomCenterOf(surface).add(0.0, 0.08, 0.0);
   }

   private static void spawnAgeOfBabylonGate(ServerLevel level, Vec3 pos, boolean volley) {
      VFXServerEffects.spawn(level, "servant_enkidu_age_of_babylon_gate", pos, volley ? 160.0 : 96.0);
      level.sendParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y + 0.04, pos.z, volley ? 18 : 10, volley ? 0.9 : 0.45, 0.02, volley ? 0.9 : 0.45, 0.03);
      level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.08, pos.z, volley ? 5 : 2, volley ? 0.45 : 0.25, 0.03, volley ? 0.45 : 0.25, 0.02);
   }

   private static boolean isProjectionCounterDuelTarget(LivingEntity target) {
      return target instanceof EmiyaArcherEntity;
   }

   private static void triggerProjectionCounterVolley(LivingEntity target, EnkiduEntity enkidu, ServerLevel level, int batch, boolean counterDuel) {
      if (!counterDuel || !(target instanceof EmiyaArcherEntity emiya) || batch % 2 != 0) {
         return;
      }
      VFXServerEffects.spawn(level, "servant_emiya_projection", emiya, 128.0);
      Vec3 center = emiya.position().add(0.0, 0.8, 0.0);
      Vec3 toEnkidu = enkidu.position().add(0.0, enkidu.getBbHeight() * 0.5, 0.0).subtract(center);
      if (toEnkidu.lengthSqr() < 1.0E-4) {
         return;
      }
      Vec3 dir = toEnkidu.normalize();
      Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
      for (int i = 0; i < 4; i++) {
         Vec3 spawn = center.add(side.scale((i - 1.5) * 0.7)).add(0.0, 0.25 + i * 0.08, 0.0);
         EnkiduEarthWeaponProjectileEntity counter = EnkiduEarthWeaponProjectileEntity.weapon(level, emiya, new ItemStack(Items.IRON_SWORD), 12.0F, enkidu, 0.18F, false);
         counter.setPos(spawn.x, spawn.y, spawn.z);
         counter.setDeltaMovement(dir.scale(2.5));
         counter.alignToMotion();
         level.addFreshEntity(counter);
      }
   }

   private static LivingEntity resolveAgeOfBabylonTarget(ServerLevel level, EnkiduEntity entity, CompoundTag data) {
      if (data.getLong(TAG_BIG_VOLLEY_TOKEN) <= level.getGameTime()) {
         clearWeaponBurstState(entity);
         return null;
      }
      Entity targetEntity = data.hasUUID(TAG_BIG_VOLLEY_TARGET) ? level.getEntity(data.getUUID(TAG_BIG_VOLLEY_TARGET)) : null;
      if (targetEntity instanceof LivingEntity living && living.isAlive() && entity.distanceTo(living) <= 48.0) {
         return living;
      }
      clearWeaponBurstState(entity);
      return null;
   }

   private static void clearWeaponBurstState(EnkiduEntity entity) {
      CompoundTag data = entity.getPersistentData();
      data.remove(TAG_SMALL_WEAPON_TARGET);
      data.remove(TAG_SMALL_WEAPON_TOKEN);
      data.remove(TAG_BIG_VOLLEY_TARGET);
      data.remove(TAG_BIG_VOLLEY_TOKEN);
   }

   private static void spawnEarthWeaponBirthFx(ServerLevel level, Vec3 pos, boolean volley) {
      int green = volley ? 9 : 5;
      int shine = volley ? 7 : 4;
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, green, volley ? 0.42 : 0.24, volley ? 0.36 : 0.18, volley ? 0.42 : 0.24, 0.03);
      level.sendParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z, shine, volley ? 0.28 : 0.16, volley ? 0.28 : 0.14, volley ? 0.28 : 0.16, 0.045);
      level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.08, pos.z, volley ? 3 : 1, 0.08, 0.12, 0.08, 0.025);
   }

   private static void interceptHostileProjectiles(EnkiduEntity entity, ServerLevel level, LivingEntity combatTarget, long now) {
      if (combatTarget == null || !combatTarget.isAlive()) {
         return;
      }
      interceptOdaMatchlockBullets(entity, level, now);
      if (now % 7L != 0L) {
         return;
      }
      List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, entity.getBoundingBox().inflate(18.0),
         p -> shouldInterceptProjectile(entity, p));
      int spawned = 0;
      for (Projectile hostile : projectiles) {
         if (spawned >= 2) {
            break;
         }
         Vec3 towardProjectile = hostile.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
         if (towardProjectile.lengthSqr() < 1.0E-4) {
            towardProjectile = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
         }
         if (towardProjectile.lengthSqr() < 1.0E-4) {
            towardProjectile = new Vec3(0.0, 0.0, 1.0);
         }
         towardProjectile = towardProjectile.normalize();
         Vec3 side = new Vec3(-towardProjectile.z, 0.0, towardProjectile.x).scale((spawned == 0 ? -1.0 : 1.0) * 1.4);
         Vec3 spawn = groundSpawn(level, entity.position().add(towardProjectile.scale(3.2 + spawned * 1.4)).add(side));
         spawnAgeOfBabylonGate(level, spawn, false);
         EnkiduEarthWeaponProjectileEntity interceptor = new EnkiduEarthWeaponProjectileEntity(level, entity, new ItemStack(Items.IRON_SWORD), 8.0F);
         interceptor.setPos(spawn.x, spawn.y, spawn.z);
         interceptor.setDeltaMovement(0.0, 0.14, 0.0);
         level.addFreshEntity(interceptor);
         final EnkiduEarthWeaponProjectileEntity finalInterceptor = interceptor;
         final Projectile finalHostile = hostile;
         final int delay = 14 + spawned * 2;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (finalInterceptor.isAlive() && finalHostile.isAlive()) {
               Vec3 aim = finalHostile.position().subtract(finalInterceptor.position());
               if (aim.lengthSqr() > 1.0E-4) {
                  finalInterceptor.setDeltaMovement(aim.normalize().scale(2.8));
                  finalInterceptor.alignToMotion();
               }
            }
         });
         spawnEarthWeaponBirthFx(level, spawn, false);
         spawned++;
      }
   }

   private static void interceptOdaMatchlockBullets(EnkiduEntity entity, ServerLevel level, long now) {
      if (now % 2L != 0L) {
         return;
      }
      List<OdaMatchlockBulletEntity> bullets = level.getEntitiesOfClass(OdaMatchlockBulletEntity.class, entity.getBoundingBox().inflate(26.0),
         bullet -> shouldInterceptProjectile(entity, bullet) && bullet.getPersistentData().getLong(TAG_MATCHLOCK_INTERCEPT_RESERVED) <= now);
      int spawned = 0;
      for (OdaMatchlockBulletEntity bullet : bullets) {
         if (spawned >= 4) {
            break;
         }
         Vec3 bulletMotion = bullet.getDeltaMovement();
         Vec3 predicted = bullet.position().add(bulletMotion.scale(2.0));
         Vec3 horizontal = predicted.subtract(entity.position()).multiply(1.0, 0.0, 1.0);
         if (horizontal.lengthSqr() < 1.0E-4) {
            horizontal = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
         }
         if (horizontal.lengthSqr() < 1.0E-4) {
            horizontal = new Vec3(0.0, 0.0, 1.0);
         }
         horizontal = horizontal.normalize();
         Vec3 side = new Vec3(-horizontal.z, 0.0, horizontal.x).scale((spawned - 1.5) * 0.85);
         Vec3 spawn = groundSpawn(level, entity.position().add(horizontal.scale(2.4 + spawned * 0.6)).add(side));
         spawnAgeOfBabylonGate(level, spawn, false);
         EnkiduEarthWeaponProjectileEntity interceptor = new EnkiduEarthWeaponProjectileEntity(level, entity, new ItemStack(Items.IRON_SWORD), 0.0F);
         bullet.getPersistentData().putLong(TAG_MATCHLOCK_INTERCEPT_RESERVED, now + 18L);
         interceptor.setPos(spawn.x, spawn.y, spawn.z);
         Vec3 aim = predicted.subtract(spawn);
         interceptor.setDeltaMovement(aim.lengthSqr() > 1.0E-4 ? aim.normalize().scale(4.6) : new Vec3(0.0, 1.2, 0.0));
         interceptor.alignToMotion();
         level.addFreshEntity(interceptor);
         spawnEarthWeaponBirthFx(level, spawn, false);
         spawned++;
      }
   }

   private static boolean shouldInterceptProjectile(EnkiduEntity entity, Projectile projectile) {
      if (projectile == null || !projectile.isAlive() || projectile.getId() == entity.getId()) {
         return false;
      }
      Entity owner = projectile.getOwner();
      if (owner == entity || (owner != null && owner.isAlliedTo(entity))) {
         return false;
      }
      if (projectile instanceof EnkiduEarthWeaponProjectileEntity && owner == null) {
         return false;
      }
      return true;
   }

   private static boolean tryBeginEnumaElish(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase) {
      return tryBeginEnumaElish(entity, level, target, now, phase, false);
   }

   public static void startGilgameshFinale(EnkiduEntity entity, ServerLevel level, GilgameshEntity target, long now, int chargeTicks) {
      int synchronizedChargeTicks = Math.max(1, chargeTicks);
      CompoundTag data = entity.getPersistentData();
      data.putLong(TAG_LAST_ENUMA, now);
      data.putLong(TAG_ENUMA_RELEASE, now + synchronizedChargeTicks);
      data.putLong(TAG_ENUMA_FINISH, now + synchronizedChargeTicks + ENUMA_RELEASE_VISUAL);
      data.putBoolean(TAG_ENUMA_DAMAGE_DONE, false);
      data.putBoolean(TAG_ENUMA_PREV_INVISIBLE, entity.isInvisible());
      data.remove(TAG_ENUMA_INVISIBLE);
      data.putUUID(TAG_ENUMA_TARGET, target.getUUID());
      data.putDouble(TAG_ENUMA_START_X, entity.getX()); data.putDouble(TAG_ENUMA_START_Y, entity.getY()); data.putDouble(TAG_ENUMA_START_Z, entity.getZ());
      data.putDouble(TAG_ENUMA_IMPACT_X, target.getX()); data.putDouble(TAG_ENUMA_IMPACT_Y, target.getY()); data.putDouble(TAG_ENUMA_IMPACT_Z, target.getZ());
      data.putInt(TAG_ENUMA_STAGE, 0); data.putInt(TAG_ENUMA_BIND_STEP, ENUMA_BIND_COUNT); data.putLong(TAG_ENUMA_NEXT_BIND, now + 200L);
      data.putBoolean(TAG_ENUMA_DUEL_FINALE, true);
      data.putLong(TAG_FLIGHT_UNTIL, now + synchronizedChargeTicks + ENUMA_RELEASE_VISUAL + 20L); data.remove(TAG_LAND_UNTIL);
      entity.setNoGravity(true);
      entity.triggerNamedActionAnimation("enkidu_enuma_elish");
      VFXServerEffects.spawn(level, "servant_enkidu_enuma_elish", entity.position(), 192.0);
   }

   public static void cancelGilgameshDuelFinale(EnkiduEntity entity) {
      if (entity == null || !entity.getPersistentData().getBoolean(TAG_ENUMA_DUEL_FINALE)) return;
      clearEnumaState(entity);
      entity.setNoGravity(false);
      entity.setDeltaMovement(Vec3.ZERO);
   }

   private static boolean tryBeginEnumaElish(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase, boolean forceCounter) {
      CompoundTag data = entity.getPersistentData();
      if (data.getLong(TAG_ENUMA_RELEASE) > now
         || entity.distanceTo(target) > 32.0
         || entity.getCurrentMp() < 150.0
         || now - data.getLong(TAG_LAST_ENUMA) < ENUMA_COOLDOWN
         || (!forceCounter && phase != ServantCombatPhase.DECISIVE)) {
         return false;
      }
      data.putLong(TAG_LAST_ENUMA, now);
      data.putLong(TAG_ENUMA_RELEASE, now + ENUMA_WINDUP);
      data.putLong(TAG_ENUMA_FINISH, now + ENUMA_WINDUP + ENUMA_RELEASE_VISUAL);
      data.putBoolean(TAG_ENUMA_DAMAGE_DONE, false);
      data.putBoolean(TAG_ENUMA_PREV_INVISIBLE, entity.isInvisible());
      data.remove(TAG_ENUMA_INVISIBLE);
      data.putUUID(TAG_ENUMA_TARGET, target.getUUID());
      data.putDouble(TAG_ENUMA_START_X, entity.getX());
      data.putDouble(TAG_ENUMA_START_Y, entity.getY());
      data.putDouble(TAG_ENUMA_START_Z, entity.getZ());
      data.putDouble(TAG_ENUMA_IMPACT_X, target.getX());
      data.putDouble(TAG_ENUMA_IMPACT_Y, target.getY());
      data.putDouble(TAG_ENUMA_IMPACT_Z, target.getZ());
      data.putInt(TAG_ENUMA_STAGE, 0);
      data.putInt(TAG_ENUMA_BIND_STEP, 0);
      data.putLong(TAG_ENUMA_NEXT_BIND, now);
      data.putLong(TAG_ENUMA_LAST_FLIGHT_FX, 0L);
      data.putLong(TAG_FLIGHT_UNTIL, now + ENUMA_WINDUP + ENUMA_RELEASE_VISUAL + 20L);
      data.remove(TAG_LAND_UNTIL);
      entity.setNoGravity(true);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 150.0));
      entity.triggerNamedActionAnimation("enkidu_enuma_elish");
      ServantVoiceHelper.tryPlayEnkiduNp(entity);
      VFXServerEffects.spawn(level, "servant_enkidu_enuma_elish", entity.position(), 192.0);
      spawnEnumaWindupVanillaFx(level, entity.position());
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.6F, 0.85F);
      return true;
   }

   private static void spawnEnumaWindupVanillaFx(ServerLevel level, Vec3 origin) {
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, origin.x, origin.y + 0.15, origin.z, 280, 14.0, 0.16, 14.0, 0.1);
      level.sendParticles(ParticleTypes.END_ROD, origin.x, origin.y + 12.0, origin.z, 260, 2.2, 11.0, 2.2, 0.2);
      level.sendParticles(ParticleTypes.ENCHANTED_HIT, origin.x, origin.y + 3.0, origin.z, 210, 12.0, 3.5, 12.0, 0.16);
      level.sendParticles(ParticleTypes.FLASH, origin.x, origin.y + 1.0, origin.z, 3, 0.16, 0.16, 0.16, 0.0);
   }

   private static void tickEnumaWindup(EnkiduEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      long release = data.getLong(TAG_ENUMA_RELEASE);
      long finish = data.getLong(TAG_ENUMA_FINISH);
      if (release <= 0L && finish <= 0L) {
         return;
      }
      boolean duelFinale = data.getBoolean(TAG_ENUMA_DUEL_FINALE);
      if (!duelFinale && finish > 0L && now > finish + 60L) {
         clearEnumaState(entity);
         entity.setNoGravity(false);
         entity.setDeltaMovement(Vec3.ZERO);
         data.putLong(TAG_LAND_UNTIL, now + 160L);
         return;
      }
      Entity targetEntity = data.hasUUID(TAG_ENUMA_TARGET) ? level.getEntity(data.getUUID(TAG_ENUMA_TARGET)) : null;
      LivingEntity target = targetEntity instanceof LivingEntity livingTarget && livingTarget.isAlive() ? livingTarget : null;
      if (target == null) {
         if ((data.getBoolean(TAG_ENUMA_DAMAGE_DONE) || data.getInt(TAG_ENUMA_STAGE) == 1) && now < finish) {
            entity.getNavigation().stop();
            if (data.getInt(TAG_ENUMA_STAGE) == 1 && !data.getBoolean(TAG_ENUMA_DAMAGE_DONE)) {
               maybeSpawnEnumaFlightFx(entity, level, now);
               Vec3 groundImpact = new Vec3(data.getDouble(TAG_ENUMA_GROUND_X), data.getDouble(TAG_ENUMA_GROUND_Y), data.getDouble(TAG_ENUMA_GROUND_Z));
               Vec3 toGround = groundImpact.subtract(entity.position());
               if (toGround.length() > 1.8 && now < release + ENUMA_RELEASE_VISUAL - 4L) {
                  entity.setNoGravity(true);
                  entity.setDeltaMovement(toGround.normalize().scale(2.65));
                  emitEnumaDrillFx(level, entity, groundImpact, now, true);
                  return;
               }
               data.putBoolean(TAG_ENUMA_DAMAGE_DONE, true);
               data.putDouble(TAG_ENUMA_IMPACT_X, groundImpact.x);
               data.putDouble(TAG_ENUMA_IMPACT_Y, groundImpact.y);
               data.putDouble(TAG_ENUMA_IMPACT_Z, groundImpact.z);
               entity.setPos(groundImpact.x, groundImpact.y, groundImpact.z);
               entity.setDeltaMovement(Vec3.ZERO);
               restoreEnumaInvisibility(entity);
               if (!duelFinale) applyEnumaGroundExplosion(entity, level, groundImpact, null);
               return;
            }
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.1, 0.0, 0.1));
            return;
         }
         clearEnumaState(entity);
         entity.setNoGravity(false);
         data.putLong(TAG_LAND_UNTIL, now + 120L);
         return;
      }
      entity.getNavigation().stop();
      Vec3 targetPoint = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      entity.faceToward(targetPoint);
      if (now < release) {
         if (duelFinale) {
            // The synchronized finale is a face-to-face stationary charge;
            // the rush starts only on the shared release tick.
            entity.setNoGravity(true);
            entity.setDeltaMovement(Vec3.ZERO);
            if (now % 4L == 0L) {
               emitEnumaDrillFx(level, entity, targetPoint, now, false);
            }
            return;
         }
         tickEnumaSequentialBinds(entity, level, target, now);
         entity.setNoGravity(true);
         double progress = 1.0 - (double)(release - now) / Math.max(1.0, ENUMA_WINDUP);
         double startX = data.getDouble(TAG_ENUMA_START_X);
         double startZ = data.getDouble(TAG_ENUMA_START_Z);
         double desiredY = data.getDouble(TAG_ENUMA_START_Y) + 13.0 + progress * 7.0;
         Vec3 anchor = new Vec3(startX, desiredY, startZ);
         Vec3 hover = anchor.subtract(entity.position());
         entity.setDeltaMovement(entity.getDeltaMovement().scale(0.45).add(
            Mth.clamp(hover.x * 0.04, -0.16, 0.16),
            Mth.clamp(hover.y * 0.07, 0.03, 0.34),
            Mth.clamp(hover.z * 0.04, -0.16, 0.16)
         ));
         if (now % 4L == 0L) {
            emitEnumaDrillFx(level, entity, targetPoint, now, false);
         }
         return;
      }
      if (duelFinale) GilgameshDuelState.markEnkiduRushStarted(entity, level, now);
      if (data.getBoolean(TAG_ENUMA_DAMAGE_DONE)) {
         if (now % 5L == 0L) {
            emitEnumaDrillFx(level, entity, targetPoint, now, true);
         }
         if (now >= finish) {
            clearEnumaState(entity);
            entity.setNoGravity(false);
            data.putLong(TAG_LAND_UNTIL, now + 160L);
         }
         return;
      }
      int stage = data.getInt(TAG_ENUMA_STAGE);
      if (stage <= 0) {
         activateEnumaInvisibility(entity);
      }
      maybeSpawnEnumaFlightFx(entity, level, now);
      Vec3 impact = stage == 1
         ? new Vec3(data.getDouble(TAG_ENUMA_GROUND_X), data.getDouble(TAG_ENUMA_GROUND_Y), data.getDouble(TAG_ENUMA_GROUND_Z))
         : targetPoint;
      Vec3 toImpact = impact.subtract(entity.position());
      if (duelFinale) {
         if (toImpact.length() > 1.8) {
            entity.setNoGravity(true);
            entity.setDeltaMovement(toImpact.normalize().scale(2.65));
            emitEnumaDrillFx(level, entity, impact, now, true);
            return;
         }
         entity.setPos(targetPoint.x, targetPoint.y, targetPoint.z);
         entity.setDeltaMovement(Vec3.ZERO);
         if (target instanceof GilgameshEntity gilgamesh) {
            GilgameshDuelState.completeDuelRush(entity, level, gilgamesh, targetPoint);
         } else {
            cancelGilgameshDuelFinale(entity);
         }
         return;
      }
      if (toImpact.length() > 1.8 && now < release + ENUMA_RELEASE_VISUAL - 4L) {
         entity.setNoGravity(true);
         entity.setDeltaMovement(toImpact.normalize().scale(2.65));
         emitEnumaDrillFx(level, entity, impact, now, true);
         return;
      }
      if (stage <= 0) {
         Vec3 flightDir = impact.subtract(new Vec3(data.getDouble(TAG_ENUMA_START_X), data.getDouble(TAG_ENUMA_START_Y) + 18.0, data.getDouble(TAG_ENUMA_START_Z)));
         if (flightDir.lengthSqr() < 1.0E-4) {
            flightDir = impact.subtract(entity.position());
         }
         if (flightDir.lengthSqr() < 1.0E-4) {
            flightDir = entity.getLookAngle();
         }
         flightDir = flightDir.normalize();
         Vec3 groundImpact = findEnumaGroundImpact(level, impact, flightDir, 34.0);
         data.putInt(TAG_ENUMA_STAGE, 1);
         data.putDouble(TAG_ENUMA_IMPACT_X, impact.x);
         data.putDouble(TAG_ENUMA_IMPACT_Y, impact.y);
         data.putDouble(TAG_ENUMA_IMPACT_Z, impact.z);
         data.putDouble(TAG_ENUMA_GROUND_X, groundImpact.x);
         data.putDouble(TAG_ENUMA_GROUND_Y, groundImpact.y);
         data.putDouble(TAG_ENUMA_GROUND_Z, groundImpact.z);
         data.putDouble(TAG_ENUMA_DIR_X, flightDir.x);
         data.putDouble(TAG_ENUMA_DIR_Y, flightDir.y);
         data.putDouble(TAG_ENUMA_DIR_Z, flightDir.z);
         entity.setPos(impact.x, Math.max(target.getY(), impact.y - entity.getBbHeight() * 0.45), impact.z);
         entity.setDeltaMovement(Vec3.ZERO);
         if (!duelFinale) applyNoDefenseDamageOverTicks(entity, target, 4000.0F, 20);
         restoreEnumaInvisibility(entity);
         if (!duelFinale) applyEnumaSmallExplosion(entity, level, impact, target);
         if (impact.distanceTo(groundImpact) > 1.8 && impact.distanceTo(groundImpact) <= 28.0 && now < release + ENUMA_RELEASE_VISUAL - 10L) {
            return;
         }
         impact = groundImpact;
      }
      data.putBoolean(TAG_ENUMA_DAMAGE_DONE, true);
      data.putDouble(TAG_ENUMA_IMPACT_X, impact.x);
      data.putDouble(TAG_ENUMA_IMPACT_Y, impact.y);
      data.putDouble(TAG_ENUMA_IMPACT_Z, impact.z);
      entity.setPos(impact.x, Math.max(impact.y, impact.y - entity.getBbHeight() * 0.45), impact.z);
      entity.setDeltaMovement(Vec3.ZERO);
      restoreEnumaInvisibility(entity);
      if (!duelFinale) applyEnumaGroundExplosion(entity, level, impact, target);
   }

   private static void tickEnumaSequentialBinds(EnkiduEntity entity, ServerLevel level, LivingEntity target, long now) {
      CompoundTag data = entity.getPersistentData();
      if (isChainForbiddenTarget(entity, target)) {
         data.putInt(TAG_ENUMA_BIND_STEP, ENUMA_BIND_COUNT);
         return;
      }
      int step = data.getInt(TAG_ENUMA_BIND_STEP);
      if (step >= ENUMA_BIND_COUNT || now < data.getLong(TAG_ENUMA_NEXT_BIND)) {
         return;
      }
      boolean divine = hasTrait(target, ServantTraitTag.DIVINE);
      bindTarget(entity, level, target, ENUMA_BIND_DURATION, divine);
      entity.triggerNamedActionAnimation("chain_of_heaven");
      VFXServerEffects.spawnReplayable(level, "servant_enkidu_chain_of_heaven", target, Math.max(1.5F, ENUMA_BIND_DURATION / 20.0F));
      level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1.55F, divine ? 1.45F : 1.12F);
      data.putInt(TAG_ENUMA_BIND_STEP, step + 1);
      data.putLong(TAG_ENUMA_NEXT_BIND, now + ENUMA_BIND_DURATION + 4L);
   }

   private static boolean isChainForbiddenTarget(EnkiduEntity entity, LivingEntity target) {
      return target == entity || target instanceof EnkiduEntity || target instanceof GilgameshEntity;
   }

   private static void maybeSpawnEnumaFlightFx(EnkiduEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      if (now - data.getLong(TAG_ENUMA_LAST_FLIGHT_FX) < 18L) {
         return;
      }
      data.putLong(TAG_ENUMA_LAST_FLIGHT_FX, now);
      VFXServerEffects.spawn(level, "servant_enkidu_enuma_elish_flight", entity, 192.0);
      Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 28, 0.55, 0.55, 0.55, 0.12);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y, center.z, 18, 0.42, 0.42, 0.42, 0.08);
   }

   private static void clearEnumaState(EnkiduEntity entity) {
      CompoundTag data = entity.getPersistentData();
      restoreEnumaInvisibility(entity);
      data.remove(TAG_ENUMA_RELEASE);
      data.remove(TAG_ENUMA_FINISH);
      data.remove(TAG_ENUMA_TARGET);
      data.remove(TAG_ENUMA_DAMAGE_DONE);
      data.remove(TAG_ENUMA_INVISIBLE);
      data.remove(TAG_ENUMA_PREV_INVISIBLE);
      data.remove(TAG_ENUMA_START_X);
      data.remove(TAG_ENUMA_START_Y);
      data.remove(TAG_ENUMA_START_Z);
      data.remove(TAG_ENUMA_BIND_STEP);
      data.remove(TAG_ENUMA_NEXT_BIND);
      data.remove(TAG_ENUMA_LAST_FLIGHT_FX);
      data.remove(TAG_ENUMA_IMPACT_X);
      data.remove(TAG_ENUMA_IMPACT_Y);
      data.remove(TAG_ENUMA_IMPACT_Z);
      data.remove(TAG_ENUMA_STAGE);
      data.remove(TAG_ENUMA_GROUND_X);
      data.remove(TAG_ENUMA_GROUND_Y);
      data.remove(TAG_ENUMA_GROUND_Z);
      data.remove(TAG_ENUMA_DIR_X);
      data.remove(TAG_ENUMA_DIR_Y);
      data.remove(TAG_ENUMA_DIR_Z);
      data.remove(TAG_ENUMA_DUEL_FINALE);
   }

   private static void activateEnumaInvisibility(EnkiduEntity entity) {
      CompoundTag data = entity.getPersistentData();
      if (!data.getBoolean(TAG_ENUMA_INVISIBLE)) {
         if (!data.contains(TAG_ENUMA_PREV_INVISIBLE)) {
            data.putBoolean(TAG_ENUMA_PREV_INVISIBLE, entity.isInvisible());
         }
         data.putBoolean(TAG_ENUMA_INVISIBLE, true);
         entity.setInvisible(true);
      }
   }

   private static void restoreEnumaInvisibility(EnkiduEntity entity) {
      CompoundTag data = entity.getPersistentData();
      if (data.getBoolean(TAG_ENUMA_INVISIBLE)) {
         entity.setInvisible(data.getBoolean(TAG_ENUMA_PREV_INVISIBLE));
         data.remove(TAG_ENUMA_INVISIBLE);
      }
   }

   private static boolean isEnumaActive(EnkiduEntity entity, long now) {
      CompoundTag data = entity.getPersistentData();
      return data.getLong(TAG_ENUMA_RELEASE) > now || data.getLong(TAG_ENUMA_FINISH) > now;
   }

   private static void emitEnumaDrillFx(ServerLevel level, EnkiduEntity entity, Vec3 targetPoint, long now, boolean release) {
      Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
      Vec3 dir = targetPoint.subtract(center);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = entity.getLookAngle();
      }
      dir = dir.normalize();
      Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
      if (side.lengthSqr() < 1.0E-4) {
         side = new Vec3(1.0, 0.0, 0.0);
      }
      side = side.normalize();
      Vec3 up = side.cross(dir).normalize();
      int points = release ? 18 : 10;
      double radius = release ? 1.05 : 0.65;
      for (int i = 0; i < points; i++) {
         double angle = (now * 0.48 + i * Math.PI * 2.0 / points);
         double along = i * (release ? 0.32 : 0.22);
         Vec3 ring = side.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
         Vec3 pos = center.add(dir.scale(along)).add(ring);
         level.sendParticles(i % 3 == 0 ? ParticleTypes.END_ROD : ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.01);
      }
      if (now % 3L == 0L) {
         level.sendParticles(ParticleTypes.ENCHANTED_HIT, center.x, center.y, center.z, release ? 16 : 8, 0.65, 0.65, 0.65, 0.08);
      }
   }

   private static Vec3 findEnumaGroundImpact(ServerLevel level, Vec3 start, Vec3 direction, double maxDistance) {
      Vec3 dir = direction.lengthSqr() < 1.0E-4 ? new Vec3(0.0, -1.0, 0.0) : direction.normalize();
      if (dir.y > -0.12) {
         dir = new Vec3(dir.x, Math.min(dir.y, -0.35), dir.z).normalize();
      }
      Vec3 last = start;
      for (double d = 1.0; d <= maxDistance; d += 0.75) {
         Vec3 sample = start.add(dir.scale(d));
         BlockPos pos = BlockPos.containing(sample);
         if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
            return new Vec3(sample.x, Math.max(sample.y, pos.getY() + 0.15), sample.z);
         }
         last = sample;
      }
      BlockPos surface = findSurface(level, BlockPos.containing(last));
      return Vec3.atBottomCenterOf(surface).add(0.0, 0.18, 0.0);
   }

   private static void applyEnumaSmallExplosion(EnkiduEntity entity, ServerLevel level, Vec3 impact, LivingEntity directTarget) {
      VFXServerEffects.spawn(level, "servant_enkidu_enuma_elish_impact", impact, 192.0);
      level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 0.35, impact.z, 8, 1.2, 0.55, 1.2, 0.02);
      level.sendParticles(ParticleTypes.FLASH, impact.x, impact.y + 0.25, impact.z, 1, 0.08, 0.08, 0.08, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, impact.x, impact.y, impact.z, 80, 1.7, 1.0, 1.7, 0.16);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, impact.x, impact.y + 0.2, impact.z, 38, 1.5, 0.75, 1.5, 0.1);
      level.playSound(null, BlockPos.containing(impact), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 1.45F);
      applyEnumaAreaDamage(entity, level, impact, 7.0, 500.0F, directTarget);
      breakEnumaImpactTerrain(level, impact, 6.0, 260);
   }

   private static void applyEnumaGroundExplosion(EnkiduEntity entity, ServerLevel level, Vec3 impact, LivingEntity directTarget) {
      double radius = ENUMA_GROUND_EXPLOSION_RADIUS;
      VFXServerEffects.spawn(level, "servant_enkidu_enuma_elish_ground_impact", impact, 192.0);
      VFXServerEffects.spawn(level, "servant_enkidu_enuma_elish_aftermath", impact, 192.0);
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y, impact.z, 14, 1.2, 0.8, 1.2, 0.0);
      level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 1.2, impact.z, 76, radius * 0.34, radius * 0.22, radius * 0.34, 0.045);
      level.sendParticles(ParticleTypes.FLASH, impact.x, impact.y + 0.4, impact.z, 5, 0.4, 0.25, 0.4, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, impact.x, impact.y, impact.z, 720, radius * 0.55, radius * 0.5, radius * 0.55, 0.34);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, impact.x, impact.y + 0.35, impact.z, 420, radius * 0.5, radius * 0.34, radius * 0.5, 0.24);
      level.sendParticles(ParticleTypes.ENCHANTED_HIT, impact.x, impact.y + 0.45, impact.z, 380, radius * 0.48, radius * 0.42, radius * 0.48, 0.28);
      level.sendParticles(ParticleTypes.CLOUD, impact.x, impact.y + 0.05, impact.z, 340, radius * 0.58, radius * 0.26, radius * 0.58, 0.18);
      level.sendParticles(ParticleTypes.LARGE_SMOKE, impact.x, impact.y + 0.2, impact.z, 220, radius * 0.52, radius * 0.34, radius * 0.52, 0.12);
      level.sendParticles(ParticleTypes.GUST, impact.x, impact.y + 0.1, impact.z, 180, radius * 0.62, 0.18, radius * 0.62, 0.16);
      level.playSound(null, BlockPos.containing(impact), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 5.5F, 0.82F);
      applyEnumaAreaDamage(entity, level, impact, radius, 500.0F, directTarget);
      breakEnumaImpactTerrainInWaves(level, impact, radius);
   }

   private static void applyEnumaAreaDamage(EnkiduEntity entity, ServerLevel level, Vec3 impact, double radius, float damage, LivingEntity directTarget) {
      AABB box = new AABB(impact, impact).inflate(radius);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e))) {
         double distance = Math.max(1.0, living.position().distanceTo(impact));
         if (distance <= radius) {
            float scaled = (float)(damage * Math.max(0.2, 1.0 - distance / (radius + 4.0)));
            if (living == directTarget) {
               scaled = Math.max(scaled, damage * 0.85F);
            }
            applyNoDefenseDamage(entity, living, scaled);
         }
      }
   }

   private static void breakEnumaImpactTerrain(ServerLevel level, Vec3 impact, double radius, int maxBroken) {
      BlockPos center = BlockPos.containing(impact);
      double radiusSqr = radius * radius;
      double guaranteedCore = Math.min(radius * 0.34, 18.0);
      double guaranteedCoreSqr = guaranteedCore * guaranteedCore;
      DeferredTerrainDestruction.queueSphere(level, impact, radius, 24, (serverLevel, pos, ignoredDistanceSqr, currentRadius, origin) -> {
         double dx = pos.getX() - center.getX() + 0.5 - (impact.x - center.getX());
         double dy = pos.getY() - center.getY() + 0.5 - (impact.y - center.getY());
         double dz = pos.getZ() - center.getZ() + 0.5 - (impact.z - center.getZ());
         double distSqr = dx * dx + dy * dy + dz * dz;
         if (distSqr > radiusSqr) {
            return false;
         }
         double edge = Math.sqrt(distSqr) / Math.max(1.0, radius);
         boolean innerCore = distSqr <= guaranteedCoreSqr;
         double noiseThreshold = radius > 40.0 ? 0.24 + edge * 0.18 : 0.1;
         return (innerCore || blockNoise(serverLevel, pos) >= noiseThreshold) && canEnumaBreakBlock(serverLevel, pos);
      }, (serverLevel, pos, removed) -> {
         if ((removed & 63) == 0) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
         }
      });
   }

   private static void breakEnumaImpactTerrainInWaves(ServerLevel level, Vec3 impact, double radius) {
      int waveCount = Math.max(1, Mth.ceil(radius / 2.0));
      double waveStep = radius / waveCount;
      for (int wave = 1; wave <= waveCount; wave++) {
         final int waveIndex = wave;
         TYPE_MOON_WORLD.queueServerWork(waveIndex * 2, () -> {
            double previousRadius = Math.max(0.0, (waveIndex - 1) * waveStep);
            double currentRadius = waveIndex * waveStep;
            breakEnumaImpactTerrainShell(level, impact, currentRadius, previousRadius);
            if (waveIndex % 3 == 0) {
               level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2F, 0.72F);
            }
         });
      }
   }

   private static void breakEnumaImpactTerrainShell(ServerLevel level, Vec3 impact, double currentRadius, double previousRadius) {
      DeferredTerrainDestruction.queueShell(level, impact, currentRadius, previousRadius, 48,
         (serverLevel, pos, distanceSqr, radius, origin) -> canEnumaBreakBlock(serverLevel, pos),
         (serverLevel, pos, removed) -> {
            if ((removed & 127) == 0) {
               serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.24, 0.24, 0.24, 0.0);
            }
         });
   }

   private static boolean canEnumaBreakBlock(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      return !state.isAir()
         && !state.hasBlockEntity()
         && hardness >= 0.0F
         && hardness <= 80.0F
         && state.getExplosionResistance(level, pos, null) < 1200.0F;
   }

   private static void applyNoDefenseDamageOverTicks(EnkiduEntity entity, LivingEntity target, float totalAmount, int ticks) {
      int duration = Math.max(1, ticks);
      float perTick = totalAmount / duration;
      for (int delay = 0; delay < duration; delay++) {
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (entity.isAlive() && target.isAlive()) {
               applyNoDefenseDamage(entity, target, perTick);
            }
         });
      }
   }

   private static void applyNoDefenseDamage(EnkiduEntity entity, LivingEntity target, float amount) {
      target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
      target.removeEffect(MobEffects.ABSORPTION);
      target.setAbsorptionAmount(0.0F);
      target.invulnerableTime = 0;
      float before = target.getHealth();
      target.hurt(entity.damageSources().magic(), amount);
      target.invulnerableTime = 0;
      // Do not let the no-defense fallback overwrite a God Hand revival.
      if (target.getPersistentData().getBoolean("GodHandActive")) {
         return;
      }
      float expected = before - amount;
      if (target.isAlive() && target.getHealth() > expected) {
         target.setHealth(Math.max(0.0F, expected));
         if (target.getHealth() <= 0.0F) {
            target.die(entity.damageSources().magic());
         }
      }
   }

   private static void clearNegativeEffects(LivingEntity entity) {
      entity.removeEffect(MobEffects.POISON);
      entity.removeEffect(MobEffects.WITHER);
      entity.removeEffect(MobEffects.WEAKNESS);
      entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
      entity.removeEffect(MobEffects.DIG_SLOWDOWN);
      entity.removeEffect(MobEffects.BLINDNESS);
      entity.removeEffect(MobEffects.CONFUSION);
   }

   private static boolean hasTrait(LivingEntity entity, ServantTraitTag trait) {
      if (ServantIdentityHelper.hasTrait(entity, trait)) {
         return true;
      }
      return trait == ServantTraitTag.BEAST && entity instanceof Enemy && entity.getMaxHealth() >= 200.0F;
   }

   private static int divinityLevel(LivingEntity entity) {
      List<String> skills = ServantIdentityHelper.skillIdsOf(entity);
      if (!skills.isEmpty()) {
         if (hasAnySkill(skills, "divinity_a", "god_hand_a", "god_hand_passive")) {
            return 5;
         }
         if (hasAnySkill(skills, "divinity_b_plus")) {
            return 4;
         }
         if (hasAnySkill(skills, "divinity_b")) {
            return 3;
         }
         if (hasAnySkill(skills, "divinity_c")) {
            return 2;
         }
         if (hasAnySkill(skills, "divinity_d", "divinity_e", "divinity_e_minus")) {
            return 1;
         }
      }
      return hasTrait(entity, ServantTraitTag.DIVINE) ? 1 : 0;
   }

   private static float applyAgeOfBabylonDivinitySpecialAttack(EnkiduEntity entity, LivingEntity target, float damage) {
      if (!isBoundByEnkidu(entity, target, entity.level().getGameTime())) {
         return damage;
      }
      int divinity = Mth.clamp(divinityLevel(target), 0, 5);
      if (divinity <= 0) {
         return damage;
      }
      return damage * Math.min(3.0F, 1.0F + divinity * 0.4F);
   }

   private static boolean hasAnySkill(List<String> skills, String... ids) {
      if (skills == null || skills.isEmpty()) {
         return false;
      }
      for (String skill : skills) {
         String normalized = skill == null ? "" : skill.toLowerCase(Locale.ROOT);
         for (String id : ids) {
            if (normalized.equals(id)) {
               return true;
            }
         }
      }
      return false;
   }

   private static ItemStack randomWeapon(EnkiduEntity entity, boolean volley) {
      int bound = volley ? EARTH_WEAPONS.length : Math.max(4, EARTH_WEAPONS.length - 3);
      return EARTH_WEAPONS[entity.getRandom().nextInt(bound)].copy();
   }

   private static float weaponDamage(ItemStack stack, boolean volley) {
      float base = volley ? 20.0F : 13.0F;
      if (stack.is(Items.NETHERITE_SWORD) || stack.is(Items.NETHERITE_AXE) || stack.is(Items.NETHERITE_PICKAXE)) {
         return (base + 12.0F) * 2.0F;
      }
      if (stack.is(Items.DIAMOND_SWORD) || stack.is(Items.DIAMOND_AXE) || stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.TRIDENT) || stack.is(Items.CROSSBOW)) {
         return (base + 7.0F) * 2.0F;
      }
      if (stack.is(Items.BOW)) {
         return (base + 4.0F) * 2.0F;
      }
      return base * 2.0F;
   }

   private static int phasedCooldown(int baseCooldown, ServantCombatPhase phase) {
      if (phase == ServantCombatPhase.DECISIVE) {
         return Math.max(10, (int)(baseCooldown * 0.65));
      }
      if (phase == ServantCombatPhase.NORMAL) {
         return Math.max(10, (int)(baseCooldown * 0.82));
      }
      return baseCooldown;
   }

   private static int phaseChance(int baseChance, ServantCombatPhase phase) {
      if (phase == ServantCombatPhase.DECISIVE) {
         return Math.min(95, baseChance + 24);
      }
      if (phase == ServantCombatPhase.NORMAL) {
         return Math.min(90, baseChance + 12);
      }
      return baseChance;
   }

   private static void addModifier(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
      if (attribute == null) {
         return;
      }
      attribute.removeModifier(id);
      attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null) {
         attribute.removeModifier(id);
      }
   }
}
