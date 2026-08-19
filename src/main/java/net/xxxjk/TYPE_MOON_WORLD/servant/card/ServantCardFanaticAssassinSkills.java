package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.network.EnkiduDetectionHighlightMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.FanaticAssassinJinnEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticAssassinCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticAssassinRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class ServantCardFanaticAssassinSkills {
   private static final String CONCEAL_UNTIL = "ServantCardFanaticConcealUntil";
   private static final String TEMPERATURE_UNTIL = "ServantCardFanaticTemperatureUntil";
   private static final String NERVES_UNTIL = "ServantCardFanaticNervesUntil";
   private static final String TOXIN_UNTIL = "ServantCardFanaticToxinUntil";
   private static final String LAST_MENTAL_CLEANSE = "ServantCardFanaticLastMentalCleanse";
   private static final ResourceLocation TEMPERATURE_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "servant_card_fanatic_temperature_armor");
   private static final double TEMPERATURE_ARMOR_BONUS = 12.0;

   private ServantCardFanaticAssassinSkills() {
   }

   public static boolean isFanatic(ServerPlayer player) {
      if (player == null) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "fanatic_assassin".equals(vars.servant_card_id);
   }

   public static void initialize(ServerPlayer player) {
      clearRuntimeTags(player);
      removeTemperatureArmor(player);
      discardOwnedJinn(player);
      player.getPersistentData().putLong(LAST_MENTAL_CLEANSE, player.level().getGameTime());
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_transformed || !"fanatic_assassin".equals(vars.servant_card_id)
         || !(player.level() instanceof ServerLevel level)) {
         clear(player);
         return;
      }
      long now = level.getGameTime();
      boolean concealed = player.getPersistentData().getLong(CONCEAL_UNTIL) > now;
      boolean temperature = player.getPersistentData().getLong(TEMPERATURE_UNTIL) > now;
      boolean nerves = player.getPersistentData().getLong(NERVES_UNTIL) > now;
      boolean toxin = player.getPersistentData().getLong(TOXIN_UNTIL) > now;

      if (concealed) {
         ServantCardConcealmentHelper.maintain(player, remaining(now, player.getPersistentData().getLong(CONCEAL_UNTIL)));
      } else {
         expireConcealment(player);
      }
      if (temperature) applyTemperatureArmor(player); else removeTemperatureArmor(player);
      if (!nerves) player.getPersistentData().remove(NERVES_UNTIL);
      if (!toxin) player.getPersistentData().remove(TOXIN_UNTIL);
      FanaticAssassinCombatHelper.spawnCardSustainedTechniqueEffects(player, level, temperature, nerves, toxin);

      long lastCleanse = player.getPersistentData().getLong(LAST_MENTAL_CLEANSE);
      if (now - lastCleanse >= FanaticAssassinRules.MENTAL_CLEANSE_INTERVAL) {
         cleanseMentalEffects(player);
         player.getPersistentData().putLong(LAST_MENTAL_CLEANSE, now);
      }
   }

   public static void clear(ServerPlayer player) {
      revealForAttack(player);
      removeTemperatureArmor(player);
      discardOwnedJinn(player);
      clearRuntimeTags(player);
   }

   public static void performConcealment(ServerPlayer player) {
      long until = player.level().getGameTime() + 600L;
      player.getPersistentData().putLong(CONCEAL_UNTIL, until);
      ServantCardConcealmentHelper.apply(player, 600);
   }

   public static boolean performHeartbeat(ServerPlayer player) {
      LivingEntity target = lookTarget(player, FanaticAssassinRules.HEARTBEAT_RANGE);
      if (target == null || !player.hasLineOfSight(target)) return false;
      revealForAttack(player);
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().source(FanaticDamageTypes.HEARTBEAT, player),
         FanaticAssassinCombatHelper.heartbeatDamageFor(target));
      target.addEffect(new MobEffectInstance(ModMobEffects.FANATIC_WOUNDED,
         FanaticAssassinRules.WOUNDED_DURATION, 0, false, true, true), player);
      if (player.level() instanceof ServerLevel level) {
         FanaticAssassinCombatHelper.spawnHeartbeatFx(level, player, target);
      }
      return true;
   }

   public static boolean performMarrow(ServerPlayer player) {
      List<LivingEntity> targets = enemiesAround(player, FanaticAssassinRules.MARROW_RADIUS);
      if (targets.isEmpty()) return false;
      revealForAttack(player);
      for (LivingEntity target : targets) {
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().source(FanaticDamageTypes.MARROW, player), FanaticAssassinRules.MARROW_DAMAGE);
         target.addEffect(new MobEffectInstance(ModMobEffects.REVERSE_MOVEMENT,
            FanaticAssassinRules.CONFUSION_DURATION, 0, false, true, true), player);
         target.addEffect(new MobEffectInstance(ModMobEffects.FANATIC_CIRCUIT_DISRUPTION,
            FanaticAssassinRules.CIRCUIT_DISRUPTION_DURATION, 0, false, true, true), player);
      }
      if (player.level() instanceof ServerLevel level) {
         FanaticAssassinCombatHelper.spawnMarrowFx(level, player, targets);
      }
      return true;
   }

   public static boolean performHair(ServerPlayer player) {
      Vec3 forward = horizontal(player.getLookAngle());
      List<LivingEntity> targets = new ArrayList<>();
      for (LivingEntity target : enemiesAround(player, FanaticAssassinRules.HAIR_RANGE)) {
         Vec3 to = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         if (to.lengthSqr() > 1.0E-5 && forward.dot(to.normalize()) >= Math.cos(Math.PI / 4.0)) targets.add(target);
      }
      if (targets.isEmpty()) return false;
      revealForAttack(player);
      boolean toxin = isToxinActive(player);
      for (LivingEntity target : targets) {
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().playerAttack(player), FanaticAssassinRules.HAIR_DAMAGE);
         if (toxin) FanaticAssassinCombatHelper.applyToxin(player, target);
      }
      if (player.level() instanceof ServerLevel level) {
         FanaticAssassinCombatHelper.spawnHairFx(level, player, forward);
      }
      return true;
   }

   public static void performTemperature(ServerPlayer player) {
      player.getPersistentData().putLong(TEMPERATURE_UNTIL,
         player.level().getGameTime() + FanaticAssassinRules.TEMPERATURE_DURATION);
      applyTemperatureArmor(player);
      if (player.level() instanceof ServerLevel level) {
         FanaticAssassinCombatHelper.spawnTemperatureActivation(level, player);
      }
   }

   public static void performNerves(ServerPlayer player) {
      player.getPersistentData().putLong(NERVES_UNTIL,
         player.level().getGameTime() + FanaticAssassinRules.NERVES_DURATION);
      if (!(player.level() instanceof ServerLevel level)) return;
      List<LivingEntity> targets = enemiesAround(player, 40.0);
      for (LivingEntity target : targets) {
         target.removeEffect(MobEffects.INVISIBILITY);
      }
      List<Integer> ids = targets.stream().map(LivingEntity::getId).toList();
      ModNetwork.sendToPlayer(player,
         new EnkiduDetectionHighlightMessage(ids, FanaticAssassinRules.NERVES_DURATION));
      FanaticAssassinCombatHelper.spawnNervesFx(level, player);
   }

   public static boolean performComputer(ServerPlayer player) {
      LivingEntity target = lookTarget(player, FanaticAssassinRules.COMPUTER_RANGE);
      if (target == null || player.distanceTo(target) > FanaticAssassinRules.COMPUTER_RANGE) return false;
      revealForAttack(player);
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().source(FanaticDamageTypes.COMPUTER, player), FanaticAssassinRules.COMPUTER_DAMAGE);
      target.addEffect(new MobEffectInstance(ModMobEffects.STAGGER,
         FanaticAssassinRules.STUN_DURATION, 0, false, true, true), player);
      AABB blast = new AABB(center, center).inflate(FanaticAssassinRules.COMPUTER_SPLASH_RADIUS);
      for (LivingEntity bystander : player.level().getEntitiesOfClass(LivingEntity.class, blast,
         living -> living != player && living != target && living.isAlive())) {
         float splash = FanaticAssassinRules.computerSplashDamage(
            bystander.position().add(0.0, bystander.getBbHeight() * 0.5, 0.0).distanceTo(center));
         if (splash <= 0.0F) continue;
         bystander.invulnerableTime = 0;
         bystander.hurt(player.damageSources().source(FanaticDamageTypes.COMPUTER_SPLASH, player), splash);
      }
      player.invulnerableTime = 0;
      player.hurt(player.damageSources().magic(), FanaticAssassinRules.COMPUTER_BACKLASH);
      if (player.level() instanceof ServerLevel level) {
         FanaticAssassinCombatHelper.spawnComputerFx(level, target, center);
      }
      return true;
   }

   public static void performToxin(ServerPlayer player) {
      player.getPersistentData().putLong(TOXIN_UNTIL,
         player.level().getGameTime() + FanaticAssassinRules.TOXIN_STANCE_DURATION);
      if (player.level() instanceof ServerLevel level) {
         FanaticAssassinCombatHelper.spawnToxinActivation(level, player);
      }
   }

   public static boolean performJinn(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      discardOwnedJinn(player);
      FanaticAssassinJinnEntity jinn = ModEntities.FANATIC_ASSASSIN_JINN.get().create(level);
      if (jinn == null) return false;
      LivingEntity target = lookTarget(player, 24.0);
      Vec3 forward = horizontal(player.getLookAngle());
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      jinn.moveTo(player.getX() + side.x * 1.5, player.getY(), player.getZ() + side.z * 1.5,
         player.getYRot(), 0.0F);
      jinn.initialize(player, target, level.getGameTime() + FanaticAssassinRules.JINN_LIFETIME);
      level.addFreshEntity(jinn);
      FanaticAssassinCombatHelper.spawnJinnSummonFx(level, player, jinn);
      return true;
   }

   public static void revealForAttack(ServerPlayer player) {
      if (player == null || !player.getPersistentData().contains(CONCEAL_UNTIL)) return;
      player.getPersistentData().remove(CONCEAL_UNTIL);
      player.removeEffect(MobEffects.INVISIBILITY);
   }

   public static boolean isConcealed(ServerPlayer player) {
      return isFanatic(player) && player.getPersistentData().getLong(CONCEAL_UNTIL) > player.level().getGameTime();
   }

   public static boolean isToxinActive(ServerPlayer player) {
      return isFanatic(player) && player.getPersistentData().getLong(TOXIN_UNTIL) > player.level().getGameTime();
   }

   public static void applyToxinOnMelee(ServerPlayer player, LivingEntity target) {
      if (!isToxinActive(player) || !EntityUtils.isValidCombatTarget(player, target)) return;
      FanaticAssassinCombatHelper.applyToxin(player, target);
   }

   public static void cleanseMentalEffects(ServerPlayer player) {
      for (MobEffectInstance effect : List.copyOf(player.getActiveEffects())) {
         if (effect.getEffect().is(FanaticAssassinCombatHelper.MENTAL_EFFECTS)) {
            player.removeEffect(effect.getEffect());
         }
      }
   }

   public static void discardOwnedJinn(ServerPlayer owner) {
      if (!(owner.level() instanceof ServerLevel level)) return;
      for (FanaticAssassinJinnEntity jinn : level.getEntitiesOfClass(FanaticAssassinJinnEntity.class,
         owner.getBoundingBox().inflate(128.0), entity -> owner.equals(entity.getOwner()))) {
         jinn.discard();
      }
   }

   private static LivingEntity lookTarget(ServerPlayer player, double range) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, range, 1.8);
      return EntityUtils.isValidCombatTarget(player, target) ? target : null;
   }

   private static List<LivingEntity> enemiesAround(ServerPlayer player, double radius) {
      double radiusSqr = radius * radius;
      return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
         target -> EntityUtils.isValidCombatTarget(player, target) && player.distanceToSqr(target) <= radiusSqr)
         .stream().sorted(Comparator.comparingDouble(player::distanceToSqr)).toList();
   }

   private static Vec3 horizontal(Vec3 value) {
      Vec3 horizontal = value.multiply(1.0, 0.0, 1.0);
      return horizontal.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }

   private static void applyTemperatureArmor(ServerPlayer player) {
      AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
      if (armor == null || armor.getModifier(TEMPERATURE_ARMOR_ID) != null) return;
      armor.addPermanentModifier(new AttributeModifier(TEMPERATURE_ARMOR_ID,
         TEMPERATURE_ARMOR_BONUS, AttributeModifier.Operation.ADD_VALUE));
   }

   private static void removeTemperatureArmor(ServerPlayer player) {
      AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
      if (armor != null) armor.removeModifier(TEMPERATURE_ARMOR_ID);
      if (player.getPersistentData().contains(TEMPERATURE_UNTIL)
         && player.level() instanceof ServerLevel level) {
         FanaticAssassinCombatHelper.spawnTemperatureRelease(level, player);
      }
      player.getPersistentData().remove(TEMPERATURE_UNTIL);
   }

   private static void expireConcealment(ServerPlayer player) {
      if (!player.getPersistentData().contains(CONCEAL_UNTIL)) return;
      player.getPersistentData().remove(CONCEAL_UNTIL);
      player.removeEffect(MobEffects.INVISIBILITY);
   }

   private static int remaining(long now, long until) {
      return (int)Math.max(1L, Math.min(Integer.MAX_VALUE, until - now));
   }

   private static void clearRuntimeTags(ServerPlayer player) {
      player.getPersistentData().remove(CONCEAL_UNTIL);
      player.getPersistentData().remove(TEMPERATURE_UNTIL);
      player.getPersistentData().remove(NERVES_UNTIL);
      player.getPersistentData().remove(TOXIN_UNTIL);
      player.getPersistentData().remove(LAST_MENTAL_CLEANSE);
   }
}
