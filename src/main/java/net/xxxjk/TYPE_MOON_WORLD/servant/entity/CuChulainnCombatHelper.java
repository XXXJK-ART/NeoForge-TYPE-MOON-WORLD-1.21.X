package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaBeamEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;

public final class CuChulainnCombatHelper {
   public static final String PROTECTION_FROM_ARROWS_TAG = "CuProtectionFromArrows";
   public static final String RECAST_STANCE_READY_TAG = "CuRecastStanceReady";
   public static final String RECAST_STANCE_USED_TAG = "CuRecastStanceUsed";
   public static final String ACTIVE_RUNE_TAG = "CuActiveRune";
   public static final String RUNE_EXPIRES_TAG = "CuRuneExpires";
   public static final String RUNE_LAST_CAST_TAG = "CuRuneLastCast";
   public static final String LAST_COMBAT_TICK_TAG = "CuLastCombatTick";
   public static final String LAST_GAE_BOLG_TICK_TAG = "CuLastGaeBolgTick";
   public static final String LAST_GAE_BOLG_ARMY_TICK_TAG = "CuLastGaeBolgArmyTick";
   public static final String GAE_BOLG_ARMY_USED_TAG = "CuGaeBolgArmyUsed";
   public static final String GAE_BOLG_WINDUP_UNTIL_TAG = "CuGaeBolgWindupUntil";
   public static final String ALGIZ_SHIELD_TAG = "CuAlgizShield";
   public static final String BERKANA_NEXT_HEAL_TICK_TAG = "CuBerkanaNextHealTick";
   public static final String EXHAUST_EXPIRES_TAG = "CuExhaustExpires";
   public static final int SINGLE_GAE_BOLG_COOLDOWN = 160;
   public static final int ARMY_GAE_BOLG_COOLDOWN = 2400;
   public static final int GAE_BOLG_WINDUP_TICKS = 50;
   public static final int OUT_OF_COMBAT_RECOVERY_TICKS = 100;
   public static final int RUNE_CAST_COOLDOWN = 80;
   public static final int RECAST_RESET_TICKS = 200;
   private static final ResourceLocation TIWAZ_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "cu_tiwaz_attack");
   private static final ResourceLocation TIWAZ_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "cu_tiwaz_speed");
   private static final ResourceLocation TIWAZ_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "cu_tiwaz_armor");
   private static final ResourceLocation EXHAUST_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "cu_exhaust_attack");
   private static final ResourceLocation EXHAUST_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "cu_exhaust_speed");

   private CuChulainnCombatHelper() {
   }

   public enum RuneType {
      NONE(""),
      ANSUZ("ansuz"),
      LAGUZ("laguz"),
      TIWAZ("tiwaz"),
      ALGIZ("algiz"),
      BERKANA("berkana");

      private final String id;

      RuneType(String id) {
         this.id = id;
      }

      public String id() {
         return this.id;
      }

      public static RuneType fromId(String id) {
         if (id == null || id.isBlank()) {
            return NONE;
         }
         for (RuneType type : values()) {
            if (type.id.equals(id)) {
               return type;
            }
         }
         return NONE;
      }
   }

   public static boolean isCuChulainn(ServantEntity entity) {
      return entity != null && CuChulainnEntity.SERVANT_KEY.equals(entity.getServantId());
   }

   public static void markCombat(ServantEntity entity) {
      if (isCuChulainn(entity)) {
         entity.getPersistentData().putLong(LAST_COMBAT_TICK_TAG, entity.level().getGameTime());
      }
   }

   public static boolean isMovementRestricted(LivingEntity entity) {
      return entity.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || entity.getTicksFrozen() > 0;
   }

   public static boolean tryNegateMedeaSmallMagic(CuChulainnEntity entity, DamageSource source, float amount) {
      if (entity == null || source == null || amount <= 0.0F || !isMedeaSmallMagic(source, amount)) {
         return false;
      }
      if (entity.getPersistentData().getBoolean(PROTECTION_FROM_ARROWS_TAG)
         && !isMovementRestricted(entity)
         && entity.getRandom().nextFloat() < 0.35F) {
         spawnDefenseFx(entity, true);
         return true;
      }
      return false;
   }

   public static float applyMagicResistance(CuChulainnEntity entity, DamageSource source, float amount) {
      if (entity == null || source == null || amount <= 0.0F) {
         return amount;
      }
      return isMedeaSmallMagic(source, amount) ? amount * 0.5F : amount;
   }

   public static boolean tryBlock(CuChulainnEntity entity, DamageSource source) {
      if (entity == null || source == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }
      if (source.getEntity() == null && source.getDirectEntity() == null) {
         return false;
      }
      if (source.is(DamageTypes.FELL_OUT_OF_WORLD)
         || source.is(DamageTypes.GENERIC_KILL)
         || source.is(DamageTypes.FALL)
         || source.is(DamageTypes.DROWN)
         || source.is(DamageTypes.FREEZE)
         || source.is(DamageTypes.IN_FIRE)
         || source.is(DamageTypes.ON_FIRE)
         || source.is(DamageTypes.LAVA)
         || source.is(DamageTypes.IN_WALL)) {
         return false;
      }
      if (entity.getRandom().nextFloat() < 0.5F) {
         spawnDefenseFx(entity, false);
         return true;
      }
      return false;
   }

   public static boolean isLaguzActive(ServantEntity entity) {
      return hasRune(entity, RuneType.LAGUZ);
   }

   public static RuneType getActiveRune(ServantEntity entity) {
      RuneType lastCast = RuneType.fromId(entity.getPersistentData().getString(ACTIVE_RUNE_TAG));
      if (hasRune(entity, lastCast)) {
         return lastCast;
      }

      for (RuneType type : RuneType.values()) {
         if (hasRune(entity, type)) {
            return type;
         }
      }
      return RuneType.NONE;
   }

   public static boolean hasRune(ServantEntity entity, RuneType runeType) {
      return entity != null
         && runeType != null
         && runeType != RuneType.NONE
         && entity.getPersistentData().getLong(runeExpireTag(runeType)) > entity.level().getGameTime();
   }

   public static boolean canUseRecastStance(ServantEntity entity) {
      return isCuChulainn(entity) && entity.getPersistentData().getBoolean(RECAST_STANCE_READY_TAG)
         && !entity.getPersistentData().getBoolean(RECAST_STANCE_USED_TAG);
   }

   public static boolean canUseSingleGaeBolg(ServantEntity entity) {
      long now = entity.level().getGameTime();
      long lastUse = entity.getPersistentData().getLong(LAST_GAE_BOLG_TICK_TAG);
      return lastUse <= 0 || now - lastUse >= SINGLE_GAE_BOLG_COOLDOWN;
   }

   public static boolean canUseArmyGaeBolg(ServantEntity entity) {
      long now = entity.level().getGameTime();
      long lastUse = entity.getPersistentData().getLong(LAST_GAE_BOLG_ARMY_TICK_TAG);
      return !entity.getPersistentData().getBoolean(GAE_BOLG_ARMY_USED_TAG)
         && (lastUse <= 0 || now - lastUse >= ARMY_GAE_BOLG_COOLDOWN);
   }

   public static boolean canCastRune(ServantEntity entity) {
      long now = entity.level().getGameTime();
      long lastCast = entity.getPersistentData().getLong(RUNE_LAST_CAST_TAG);
      return lastCast <= 0 || now - lastCast >= RUNE_CAST_COOLDOWN;
   }

   public static void startGaeBolgWindup(ServantEntity entity, int durationTicks) {
      if (!isCuChulainn(entity)) {
         return;
      }

      entity.getPersistentData().putLong(GAE_BOLG_WINDUP_UNTIL_TAG, entity.level().getGameTime() + durationTicks);
      markCombat(entity);
   }

   public static void clearGaeBolgWindup(ServantEntity entity) {
      if (entity != null) {
         entity.getPersistentData().remove(GAE_BOLG_WINDUP_UNTIL_TAG);
      }
   }

   public static boolean isGaeBolgWindingUp(ServantEntity entity) {
      return isCuChulainn(entity) && entity.getPersistentData().getLong(GAE_BOLG_WINDUP_UNTIL_TAG) > entity.level().getGameTime();
   }

   public static void markSingleGaeBolg(ServantEntity entity) {
      entity.getPersistentData().putLong(LAST_GAE_BOLG_TICK_TAG, entity.level().getGameTime());
      markCombat(entity);
   }

   public static void markArmyGaeBolg(ServantEntity entity) {
      entity.getPersistentData().putBoolean(GAE_BOLG_ARMY_USED_TAG, true);
      entity.getPersistentData().putLong(LAST_GAE_BOLG_ARMY_TICK_TAG, entity.level().getGameTime());
      markCombat(entity);
   }

   public static void markRecastUsed(ServantEntity entity) {
      entity.getPersistentData().putBoolean(RECAST_STANCE_USED_TAG, true);
      markCombat(entity);
   }

   public static void markRuneCast(ServantEntity entity) {
      entity.getPersistentData().putLong(RUNE_LAST_CAST_TAG, entity.level().getGameTime());
      markCombat(entity);
   }

   public static void setRune(ServantEntity entity, RuneType runeType, int durationTicks) {
      if (runeType == RuneType.NONE) {
         return;
      }

      long now = entity.level().getGameTime();
      entity.getPersistentData().putString(ACTIVE_RUNE_TAG, runeType.id());
      entity.getPersistentData().putLong(runeExpireTag(runeType), now + durationTicks);
      switch (runeType) {
         case TIWAZ -> applyTiwaz(entity);
         case ALGIZ -> entity.getPersistentData().putFloat(ALGIZ_SHIELD_TAG, 100.0F);
         case BERKANA -> {
            entity.heal(Math.max(8.0F, entity.getMaxHealth() * 0.18F));
            entity.getPersistentData().putLong(BERKANA_NEXT_HEAL_TICK_TAG, now + 20L);
         }
         default -> {
         }
      }
   }

   public static void clearRune(ServantEntity entity) {
      for (RuneType runeType : RuneType.values()) {
         clearRune(entity, runeType);
      }
      entity.getPersistentData().remove(ACTIVE_RUNE_TAG);
      entity.getPersistentData().remove(RUNE_EXPIRES_TAG);
   }

   public static void applyExhaustion(ServantEntity entity, int durationTicks) {
      AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attack != null && attack.getModifier(EXHAUST_ATTACK_ID) == null) {
         attack.addTransientModifier(new AttributeModifier(EXHAUST_ATTACK_ID, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
      if (speed != null && speed.getModifier(EXHAUST_SPEED_ID) == null) {
         speed.addTransientModifier(new AttributeModifier(EXHAUST_SPEED_ID, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
      entity.getPersistentData().putLong(EXHAUST_EXPIRES_TAG, entity.level().getGameTime() + durationTicks);
   }

   public static void tickStatus(ServantEntity entity) {
      if (!isCuChulainn(entity)) {
         return;
      }

      long now = entity.level().getGameTime();
      long lastCombat = entity.getPersistentData().getLong(LAST_COMBAT_TICK_TAG);
      LivingEntity target = entity.getTarget();
      boolean outOfCombat = target == null || !target.isAlive() || entity.distanceToSqr(target) > 400.0;
      if (lastCombat > 0 && outOfCombat && now - lastCombat >= OUT_OF_COMBAT_RECOVERY_TICKS) {
         restoreOutOfCombat(entity);
         entity.getPersistentData().remove(LAST_COMBAT_TICK_TAG);
         lastCombat = 0;
      }

      if (lastCombat > 0 && now - lastCombat > RECAST_RESET_TICKS) {
         entity.getPersistentData().putBoolean(RECAST_STANCE_USED_TAG, false);
         entity.getPersistentData().putBoolean(GAE_BOLG_ARMY_USED_TAG, false);
      }

      for (RuneType runeType : RuneType.values()) {
         if (runeType == RuneType.NONE) {
            continue;
         }

         long runeExpires = entity.getPersistentData().getLong(runeExpireTag(runeType));
         if (runeExpires <= 0) {
            continue;
         }

         if (runeType == RuneType.BERKANA && now < runeExpires) {
            long nextHealTick = entity.getPersistentData().getLong(BERKANA_NEXT_HEAL_TICK_TAG);
            if (nextHealTick <= 0 || now >= nextHealTick) {
               entity.heal(Math.max(4.0F, entity.getMaxHealth() * 0.04F));
               entity.getPersistentData().putLong(BERKANA_NEXT_HEAL_TICK_TAG, now + 20L);
            }
         }

         if (now >= runeExpires) {
            clearRune(entity, runeType);
         }
      }

      long exhaustExpires = entity.getPersistentData().getLong(EXHAUST_EXPIRES_TAG);
      if (exhaustExpires > 0 && now >= exhaustExpires) {
         removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), EXHAUST_ATTACK_ID);
         removeModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), EXHAUST_SPEED_ID);
         entity.getPersistentData().remove(EXHAUST_EXPIRES_TAG);
      }

      long windupUntil = entity.getPersistentData().getLong(GAE_BOLG_WINDUP_UNTIL_TAG);
      if (windupUntil > 0 && now >= windupUntil) {
         entity.getPersistentData().remove(GAE_BOLG_WINDUP_UNTIL_TAG);
      }
   }

   private static boolean isMedeaSmallMagic(DamageSource source, float amount) {
      Entity attacker = source.getEntity();
      Entity direct = source.getDirectEntity();
      if (direct instanceof MedeaMagicBoltEntity bolt) {
         return switch (bolt.getMode()) {
            case BOLT, FIRE_BOLT, FROST_BOLT -> true;
            default -> false;
         };
      }
      if (direct instanceof MedeaBeamEffectEntity beam) {
         return beam.getDamage() <= 18.0F;
      }
      if (attacker instanceof MedeaEntity && (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC))) {
         return amount <= 18.0F;
      }
      return false;
   }

   private static void spawnDefenseFx(CuChulainnEntity entity, boolean dodge) {
      entity.playSound(
         dodge ? net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP : net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,
         1.0F,
         dodge ? 1.55F : 0.9F
      );
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(
            dodge ? net.minecraft.core.particles.ParticleTypes.CRIT : net.minecraft.core.particles.ParticleTypes.ENCHANT,
            entity.getX(),
            entity.getY() + entity.getBbHeight() * 0.6,
            entity.getZ(),
            dodge ? 12 : 16,
            0.35,
            0.45,
            0.35,
            0.04
         );
         level.sendParticles(
            net.minecraft.core.particles.ParticleTypes.END_ROD,
            entity.getX(),
            entity.getY() + entity.getBbHeight() * 0.7,
            entity.getZ(),
            8,
            0.22,
            0.32,
            0.22,
            0.02
         );
      }
   }

   public static int getLuckValue(LivingEntity target) {
      if (target instanceof ServantEntity servant) {
         var def = servant.getDefinition();
         if (def != null) {
            var params = def.parameters();
            return params.luckPlus() ? params.luck().plusCoefficient() : params.luck().coefficient();
         }
      }
      return 0;
   }

   public static float getDeathThornChance(LivingEntity target) {
      int luck = getLuckValue(target);
      return Math.max(0.0F, (float)(0.4 * (1.0 - luck / 100.0)));
   }

   private static void applyTiwaz(ServantEntity entity) {
      AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
      AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
      if (attack != null && attack.getModifier(TIWAZ_ATTACK_ID) == null) {
         attack.addTransientModifier(new AttributeModifier(TIWAZ_ATTACK_ID, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
      if (speed != null && speed.getModifier(TIWAZ_SPEED_ID) == null) {
         speed.addTransientModifier(new AttributeModifier(TIWAZ_SPEED_ID, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
      if (armor != null && armor.getModifier(TIWAZ_ARMOR_ID) == null) {
         armor.addTransientModifier(new AttributeModifier(TIWAZ_ARMOR_ID, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null) {
         attribute.removeModifier(id);
      }
   }

   private static void clearRune(ServantEntity entity, RuneType runeType) {
      if (entity == null || runeType == null || runeType == RuneType.NONE) {
         return;
      }

      if (runeType == RuneType.TIWAZ) {
         removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), TIWAZ_ATTACK_ID);
         removeModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), TIWAZ_SPEED_ID);
         removeModifier(entity.getAttribute(Attributes.ARMOR), TIWAZ_ARMOR_ID);
      } else if (runeType == RuneType.ALGIZ) {
         entity.getPersistentData().remove(ALGIZ_SHIELD_TAG);
      } else if (runeType == RuneType.BERKANA) {
         entity.getPersistentData().remove(BERKANA_NEXT_HEAL_TICK_TAG);
      }

      entity.getPersistentData().remove(runeExpireTag(runeType));
      if (runeType.id().equals(entity.getPersistentData().getString(ACTIVE_RUNE_TAG))) {
         entity.getPersistentData().remove(ACTIVE_RUNE_TAG);
      }
   }

   private static String runeExpireTag(RuneType runeType) {
      return "CuRuneExpire." + runeType.id();
   }

   private static void restoreOutOfCombat(ServantEntity entity) {
      entity.setHealth(entity.getMaxHealth());
      entity.setCurrentMp(entity.getMaxMp());
      entity.clearFire();
      entity.getPersistentData().putBoolean(RECAST_STANCE_USED_TAG, false);
      entity.getPersistentData().putBoolean(GAE_BOLG_ARMY_USED_TAG, false);
      clearGaeBolgWindup(entity);
      clearRune(entity);
      removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), EXHAUST_ATTACK_ID);
      removeModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), EXHAUST_SPEED_ID);
      entity.getPersistentData().remove(EXHAUST_EXPIRES_TAG);

      java.util.List<MobEffectInstance> activeEffects = java.util.List.copyOf(entity.getActiveEffects());
      for (MobEffectInstance effect : activeEffects) {
         if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            entity.removeEffect(effect.getEffect());
         }
      }
   }
}
