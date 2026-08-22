package net.xxxjk.TYPE_MOON_WORLD.servant.nightingale;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.NightingaleEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class NightingaleSupportService {
   public static final String ANGEL_CRY_UNTIL = "NightingaleAngelCryUntil";
   public static final String NP_POWER_DOWN_UNTIL = "NightingaleNpPowerDownUntil";
   private static final net.minecraft.resources.ResourceLocation ATTACK_SPEED_ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "nightingale_angel_cry_attack_speed"
   );
   private static final List<SafetyCircle> SAFETY_CIRCLES = new ArrayList<>();

   private NightingaleSupportService() {}

   public static boolean isPaleRider(LivingEntity entity) {
      return entity instanceof PaleRiderEntity || PaleRiderInfectionService.isPaleRiderCardPlayer(entity);
   }

   public static boolean isNightingaleSource(LivingEntity source) {
      if (source instanceof NightingaleEntity) return true;
      if (source instanceof Player player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.servant_card_transformed && "nightingale".equals(vars.servant_card_id);
      }
      return false;
   }

   public static boolean isAlly(LivingEntity nightingale, LivingEntity entity) {
      if (entity == nightingale || nightingale.isAlliedTo(entity) || entity.isAlliedTo(nightingale)) return true;
      if (nightingale instanceof NightingaleEntity npc && npc.getEntityMaster() == entity) return true;
      if (entity instanceof ServantEntity servant && servant.getEntityMaster() == nightingale) return true;
      if (nightingale instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return MasterServantLinkService.getLinkedMaster(player, vars) == entity;
      }
      return false;
   }

   public static LivingEntity findSteelNursingTarget(NightingaleEntity nightingale) {
      AABB area = nightingale.getBoundingBox().inflate(NightingaleRules.SUPPORT_RANGE);
      return nightingale.level().getEntitiesOfClass(LivingEntity.class, area,
            entity -> entity.isAlive() && isAlly(nightingale, entity) && !isPaleRider(entity)
               && NightingaleRules.isNursingEligible(entity.getHealth(), entity.getMaxHealth()))
         .stream().min(Comparator.comparingDouble(entity -> NightingaleRules.nursingPriority(entity.getHealth(), entity.getMaxHealth()))).orElse(null);
   }

   public static LivingEntity findPaleRiderTarget(NightingaleEntity nightingale) {
      return nightingale.level().getEntitiesOfClass(LivingEntity.class, nightingale.getBoundingBox().inflate(NightingaleRules.SUPPORT_RANGE),
            entity -> entity.isAlive() && entity != nightingale && isPaleRider(entity))
         .stream().min(Comparator.comparingDouble(nightingale::distanceToSqr)).orElse(null);
   }

   public static LivingEntity findAngelCryTarget(NightingaleEntity nightingale, long now) {
      return nightingale.level().getEntitiesOfClass(LivingEntity.class, nightingale.getBoundingBox().inflate(NightingaleRules.SUPPORT_RANGE),
            entity -> isAlly(nightingale, entity) && canReceiveAngelCry(entity, now))
         .stream().max(Comparator.comparingDouble(entity -> entity.getAttributeValue(Attributes.ATTACK_DAMAGE))).orElse(nightingale);
   }

   public static boolean canReceiveAngelCry(LivingEntity entity, long now) {
      return entity != null && entity.isAlive()
         && entity.getPersistentData().getLong(ANGEL_CRY_UNTIL) <= now
         && entity.getAttribute(Attributes.ATTACK_DAMAGE) != null;
   }

   public static void applyHealing(LivingEntity nightingale, LivingEntity target, float amount) {
      if (isPaleRider(target)) {
         applyHealingReversal(nightingale, target, amount);
         return;
      }
      target.heal(amount);
      cleanse(target);
      spawnHealFx(nightingale, target);
   }

   public static void applyHealingReversal(LivingEntity nightingale, LivingEntity target, float amount) {
      if (EntityUtils.isImmunePlayerTarget(target)) return;
      PaleRiderInfectionService.cleanse(target, true);
      var source = NightingaleDamageTypes.healingReversal(nightingale);
      if (target.isInvulnerableTo(source)) return;
      float before = target.getHealth();
      target.invulnerableTime = 0;
      target.hurt(source, amount);
      target.invulnerableTime = 0;
      float exactHealth = Math.max(0.0F, before - amount);
      if (target.getHealth() != exactHealth) target.setHealth(exactHealth);
      if (exactHealth <= 0.0F && !target.isDeadOrDying()) target.die(source);
      if (nightingale.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 20, 0.4, 0.5, 0.4, 0.1);
      }
   }

   public static void cleanse(LivingEntity target) {
      for (MobEffectInstance effect : List.copyOf(target.getActiveEffects())) {
         if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) target.removeEffect(effect.getEffect());
      }
      PaleRiderInfectionService.cleanse(target, true);
   }

   public static void applyAngelCry(LivingEntity source, LivingEntity target, long now) {
      target.getPersistentData().putLong(ANGEL_CRY_UNTIL, now + NightingaleRules.ANGEL_CRY_DURATION);
      var attackSpeed = target.getAttribute(Attributes.ATTACK_SPEED);
      if (attackSpeed != null) {
         attackSpeed.removeModifier(ATTACK_SPEED_ID);
         attackSpeed.addTransientModifier(new AttributeModifier(ATTACK_SPEED_ID, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
      if (source.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.0, target.getZ(), 24, 0.45, 0.6, 0.45, 0.04);
         level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.0F, 1.35F);
      }
   }

   public static boolean hasAngelCry(LivingEntity entity, long now) {
      return entity.getPersistentData().getLong(ANGEL_CRY_UNTIL) > now;
   }

   public static int adjustActionTicks(LivingEntity entity, int baseTicks, long now) {
      return NightingaleRules.adjustActionTicks(baseTicks, hasAngelCry(entity, now));
   }

   public static void tickBuffs(LivingEntity entity, long now) {
      if (entity.getPersistentData().getLong(ANGEL_CRY_UNTIL) <= now) {
         var attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
         if (attackSpeed != null) attackSpeed.removeModifier(ATTACK_SPEED_ID);
      }
      if (entity.getPersistentData().getLong(NP_POWER_DOWN_UNTIL) <= now) entity.getPersistentData().remove(NP_POWER_DOWN_UNTIL);
   }

   public static void createSafetyCircle(LivingEntity nightingale, ServerLevel level, Vec3 center, long now) {
      AABB area = new AABB(center, center).inflate(NightingaleRules.SUPPORT_RANGE);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
         if (isPaleRider(target)) {
            applyHealingReversal(nightingale, target, NightingaleRules.NOBLE_PHANTASM_HEAL);
         } else if (isAlly(nightingale, target)) {
            target.heal(NightingaleRules.NOBLE_PHANTASM_HEAL);
            restoreMana(target, NightingaleRules.NOBLE_PHANTASM_MANA);
            cleanse(target);
         } else if (isEnemy(nightingale, target)) {
            target.getPersistentData().putLong(NP_POWER_DOWN_UNTIL, now + 600L);
         }
      }
      SAFETY_CIRCLES.add(new SafetyCircle(level.dimension().location(), nightingale.getUUID(), center, now + NightingaleRules.SAFETY_CIRCLE_DURATION));
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.8, center.z, 160, 7.0, 1.2, 7.0, 0.08);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y + 0.5, center.z, 100, 6.5, 0.8, 6.5, 0.05);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.5F, 1.15F);
   }

   public static boolean isProtected(LivingEntity target) {
      if (!(target.level() instanceof ServerLevel level)) return false;
      long now = level.getGameTime();
      prune(now);
      for (SafetyCircle circle : SAFETY_CIRCLES) {
         if (!circle.dimension.equals(level.dimension().location()) || circle.center.distanceToSqr(target.position()) > 225.0) continue;
         if (level.getEntity(circle.owner) instanceof LivingEntity nightingale
            && isNightingaleSource(nightingale) && isAlly(nightingale, target)) return true;
      }
      return false;
   }

   public static void tickSafetyCircleVisuals(ServerLevel level, long now) {
      prune(now);
      if (now % 5L != 0L) return;
      for (SafetyCircle circle : SAFETY_CIRCLES) {
         if (!circle.dimension.equals(level.dimension().location())) continue;
         for (int index = 0; index < 32; index++) {
            double angle = Math.PI * 2.0 * index / 32.0;
            level.sendParticles(ParticleTypes.END_ROD, circle.center.x + Math.cos(angle) * 15.0,
               circle.center.y + 0.15, circle.center.z + Math.sin(angle) * 15.0, 1, 0.0, 0.04, 0.0, 0.0);
         }
      }
   }

   private static void restoreMana(LivingEntity target, double amount) {
      if (target instanceof ServantEntity servant) {
         servant.setCurrentMp(Math.min(servant.getMaxMp(), servant.getCurrentMp() + amount));
      } else if (target instanceof Player player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.servant_card_transformed) {
            vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + amount);
            vars.syncMana(player);
         } else {
            vars.player_mana = Math.min(vars.player_max_mana, vars.player_mana + amount);
            vars.syncPlayerVariables(player);
         }
      }
   }

   private static void spawnHealFx(LivingEntity source, LivingEntity target) {
      if (source.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 0.8, target.getZ(), 28, 0.45, 0.6, 0.45, 0.05);
         level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.9F, 1.5F);
      }
   }

   private static void prune(long now) {
      Iterator<SafetyCircle> iterator = SAFETY_CIRCLES.iterator();
      while (iterator.hasNext()) if (iterator.next().until <= now) iterator.remove();
   }

   private static boolean isEnemy(LivingEntity source, LivingEntity target) {
      if (source instanceof NightingaleEntity nightingale) return nightingale.canAttackTarget(target);
      return target != source && !isAlly(source, target) && !EntityUtils.isImmunePlayerTarget(target);
   }

   private record SafetyCircle(net.minecraft.resources.ResourceLocation dimension, UUID owner, Vec3 center, long until) {}
}
