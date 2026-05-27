package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

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
   public static final String EXHAUST_EXPIRES_TAG = "CuExhaustExpires";
   public static final int SINGLE_GAE_BOLG_COOLDOWN = 160;
   public static final int ARMY_GAE_BOLG_COOLDOWN = 2400;
   public static final int GAE_BOLG_WINDUP_TICKS = 50;
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
      ALGIZ("algiz");

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

   public static boolean isLaguzActive(ServantEntity entity) {
      return getActiveRune(entity) == RuneType.LAGUZ;
   }

   public static RuneType getActiveRune(ServantEntity entity) {
      return RuneType.fromId(entity.getPersistentData().getString(ACTIVE_RUNE_TAG));
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
      clearRune(entity);
      if (runeType == RuneType.NONE) {
         return;
      }

      entity.getPersistentData().putString(ACTIVE_RUNE_TAG, runeType.id());
      entity.getPersistentData().putLong(RUNE_EXPIRES_TAG, entity.level().getGameTime() + durationTicks);
      switch (runeType) {
         case TIWAZ -> applyTiwaz(entity);
         case ALGIZ -> entity.getPersistentData().putFloat(ALGIZ_SHIELD_TAG, 100.0F);
         default -> {
         }
      }
   }

   public static void clearRune(ServantEntity entity) {
      RuneType active = getActiveRune(entity);
      if (active == RuneType.TIWAZ) {
         removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), TIWAZ_ATTACK_ID);
         removeModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), TIWAZ_SPEED_ID);
         removeModifier(entity.getAttribute(Attributes.ARMOR), TIWAZ_ARMOR_ID);
      }

      entity.getPersistentData().remove(ACTIVE_RUNE_TAG);
      entity.getPersistentData().remove(RUNE_EXPIRES_TAG);
      entity.getPersistentData().remove(ALGIZ_SHIELD_TAG);
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
      if (lastCombat > 0 && outOfCombat) {
         restoreOutOfCombat(entity);
         entity.getPersistentData().remove(LAST_COMBAT_TICK_TAG);
         lastCombat = 0;
      }

      if (lastCombat > 0 && now - lastCombat > RECAST_RESET_TICKS) {
         entity.getPersistentData().putBoolean(RECAST_STANCE_USED_TAG, false);
         entity.getPersistentData().putBoolean(GAE_BOLG_ARMY_USED_TAG, false);
      }

      long runeExpires = entity.getPersistentData().getLong(RUNE_EXPIRES_TAG);
      if (runeExpires > 0 && now >= runeExpires) {
         clearRune(entity);
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
