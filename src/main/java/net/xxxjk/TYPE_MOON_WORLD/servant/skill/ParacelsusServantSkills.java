package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import java.util.ArrayList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantCombatActionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantLifecycleContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantNoblePhantasmContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusSpiritCannonEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.registry.ServantAddonRegistry;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import org.joml.Vector3f;

public final class ParacelsusServantSkills {
   public static final String ACTION_ELEMENTAL_SPIRIT = "elemental_spirit";
   public static final String ACTION_PHILOSOPHER_STONE = "philosopher_stone";
   public static final String NP_ELEMENTAL_SWORD = "elemental_sword";
   private static final String TAG_LAST_ELEMENTAL_SPIRIT = "ParacelsusLastElementalSpiritTick";
   private static final String TAG_LAST_PHILOSOPHER_STONE = "ParacelsusLastPhilosopherStoneTick";
   private static final String TAG_LAST_NP = "ParacelsusLastNpTick";
   private static final String TAG_PHILOSOPHER_STONE_COUNT = "ParacelsusPhilosopherStoneCount";
   private static final String TAG_HIGH_SPEED_UNTIL = "ParacelsusHighSpeedChantingUntil";
   private static final String TAG_ELEMENTAL_SPIRIT_ACTIVE_UNTIL = "ParacelsusElementalSpiritActiveUntil";
   private static final String TAG_ELEMENTAL_SPIRIT_LAST_SUMMON = "ParacelsusLastElementalSpiritSummon";
   private static final String TAG_LAST_ELEMENTAL_STRIKE = "ParacelsusLastElementalStrikeTick";
   private static final String TAG_SWORD_BUFF_UNTIL = "ParacelsusSwordBuffUntil";
   private static final int ELEMENTAL_SPIRIT_COOLDOWN = 300;
   private static final int PHILOSOPHER_STONE_COOLDOWN = 900;
   private static final int NP_COOLDOWN = 900;
   private static final int PHILOSOPHER_STONE_STARTING_CHARGES = 3;
   private static final int PHILOSOPHER_STONE_MAX_CHARGES = 5;
   private static final int PHILOSOPHER_STONE_INVULN_TICKS = 60;
   private static final int ELEMENTAL_SPIRIT_DURATION = 220;
   private static final int HIGH_SPEED_DURATION = 160;
   private static final int NP_BUFF_DURATION = 300;
   private static final DustParticleOptions FIRE = new DustParticleOptions(new Vector3f(1.0F, 0.28F, 0.28F), 1.2F);
   private static final DustParticleOptions WATER = new DustParticleOptions(new Vector3f(0.28F, 0.55F, 1.0F), 1.2F);
   private static final DustParticleOptions EARTH = new DustParticleOptions(new Vector3f(0.35F, 0.95F, 0.35F), 1.2F);
   private static final DustParticleOptions WIND = new DustParticleOptions(new Vector3f(0.95F, 0.95F, 1.0F), 1.2F);
   private static final DustParticleOptions AETHER = new DustParticleOptions(new Vector3f(1.0F, 0.84F, 0.42F), 1.2F);

   private ParacelsusServantSkills() {
   }

   public static void registerBuiltin(ServantSkillRegistry registry) {
      registry.register("high_speed_chanting_a", ParacelsusServantSkills::markHighSpeedChanting, "typemoonworld_core");
      registry.register("elemental_spirit_a_plus", ParacelsusServantSkills::markElementalSpirit, "typemoonworld_core");
      registry.register("philosopher_stone_a", ParacelsusServantSkills::markPhilosopherStone, "typemoonworld_core");
   }

   public static void registerCombatActions(IServantAddonRegistry registry) {
      registry.registerCombatAction(ACTION_ELEMENTAL_SPIRIT, ParacelsusServantSkills::castElementalSpirit, "typemoonworld_core");
      registry.registerCombatAction(ACTION_PHILOSOPHER_STONE, ParacelsusServantSkills::castPhilosopherStone, "typemoonworld_core");
      registry.registerNoblePhantasm(NP_ELEMENTAL_SWORD, ParacelsusServantSkills::castElementalSword, "typemoonworld_core");
      registry.registerLifecycleHandler("paracelsus_lifecycle", ParacelsusServantSkills::tickParacelsus, "typemoonworld_core");
   }

   private static ServantExecutionResult markHighSpeedChanting(ServantExecutionContext context) {
      if (!(context.caster() instanceof ParacelsusEntity entity)) {
         return ServantExecutionResult.FAILED;
      }
      entity.getPersistentData().putBoolean("ParacelsusHighSpeedChantingActive", true);
      entity.getPersistentData().putLong(TAG_HIGH_SPEED_UNTIL, entity.level().getGameTime() + HIGH_SPEED_DURATION);
      if (entity.level() instanceof ServerLevel level) {
         Vec3 pos = entity.position().add(0.0, entity.getBbHeight() * 0.7, 0.0);
         level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, 24, 0.25, 0.4, 0.25, 0.05);
         level.sendParticles(AETHER, pos.x, pos.y, pos.z, 20, 0.2, 0.35, 0.2, 0.02);
         level.playSound(null, entity.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 0.9F, 1.35F);
      }
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult markElementalSpirit(ServantExecutionContext context) {
      if (!(context.caster() instanceof ParacelsusEntity entity)) {
         return ServantExecutionResult.FAILED;
      }
      long now = entity.level().getGameTime();
      entity.getPersistentData().putLong(TAG_ELEMENTAL_SPIRIT_ACTIVE_UNTIL, now + ELEMENTAL_SPIRIT_DURATION);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult markPhilosopherStone(ServantExecutionContext context) {
      if (!(context.caster() instanceof ParacelsusEntity entity)) {
         return ServantExecutionResult.FAILED;
      }
      entity.getPersistentData().putBoolean("ParacelsusPhilosopherStoneAvailable", true);
      int count = entity.getPersistentData().getInt(TAG_PHILOSOPHER_STONE_COUNT);
      if (count <= 0) {
         entity.getPersistentData().putInt(TAG_PHILOSOPHER_STONE_COUNT, PHILOSOPHER_STONE_STARTING_CHARGES);
      } else if (count > PHILOSOPHER_STONE_MAX_CHARGES) {
         entity.getPersistentData().putInt(TAG_PHILOSOPHER_STONE_COUNT, PHILOSOPHER_STONE_MAX_CHARGES);
      }
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult tickParacelsus(ServantLifecycleContext context) {
      if (!(context.entity() instanceof ParacelsusEntity entity)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      if (entity.tickCount == 1) {
         entity.getPersistentData().putInt(TAG_PHILOSOPHER_STONE_COUNT, Math.max(
            PHILOSOPHER_STONE_STARTING_CHARGES,
            entity.getPersistentData().getInt(TAG_PHILOSOPHER_STONE_COUNT)
         ));
         if (entity.getMainHandItem().isEmpty()) {
            entity.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.PARACELSUS_SWORD.get()));
         }
      }

      long now = entity.level().getGameTime();
      if (entity.getPersistentData().getLong(TAG_HIGH_SPEED_UNTIL) > now) {
         entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 10, 0, false, false, true));
      }

      boolean spiritActive = entity.getPersistentData().getLong(TAG_ELEMENTAL_SPIRIT_ACTIVE_UNTIL) > now;
      if (spiritActive && entity.level() instanceof ServerLevel level && now % 6L == 0L) {
         spawnElementalSpiritAura(level, entity, now);
      }

      LivingEntity target = context.target();
      if (target == null || !target.isAlive()) {
         if (!spiritActive) {
            entity.setCombatPhase(entity.computeCombatPhase());
         }
         return ServantExecutionResult.NOT_HANDLED;
      }

      if (spiritActive
         && entity.distanceTo(target) <= 20.0
         && now - entity.getPersistentData().getLong(TAG_LAST_ELEMENTAL_STRIKE) >= 30L) {
         entity.getPersistentData().putLong(TAG_LAST_ELEMENTAL_STRIKE, now);
         releaseElementalStrike(entity, target, now);
      }

      double distance = entity.distanceTo(target);
      if (distance > 13.5 && now % 8L == 0L) {
         entity.getNavigation().moveTo(target, 0.95);
      } else if (distance < 6.5 && now % 6L == 0L) {
         Vec3 away = entity.position().subtract(target.position());
         if (away.lengthSqr() > 1.0E-4) {
            away = away.normalize().scale(4.5);
            entity.getNavigation().moveTo(entity.getX() + away.x, entity.getY(), entity.getZ() + away.z, 1.0);
         }
      }
      return ServantExecutionResult.NOT_HANDLED;
   }

   private static ServantExecutionResult castElementalSpirit(ServantCombatActionContext context) {
      if (!(context.caster() instanceof ParacelsusEntity entity)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      LivingEntity target = context.target();
      if (target == null || !target.isAlive() || context.distance() > 18.0) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      long now = context.gameTick();
      if (now - entity.getPersistentData().getLong(TAG_ELEMENTAL_SPIRIT_LAST_SUMMON) < ELEMENTAL_SPIRIT_COOLDOWN || entity.getCurrentMp() < 18.0) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      entity.getPersistentData().putLong(TAG_ELEMENTAL_SPIRIT_LAST_SUMMON, now);
      entity.getPersistentData().putLong(TAG_ELEMENTAL_SPIRIT_ACTIVE_UNTIL, now + ELEMENTAL_SPIRIT_DURATION);
      entity.faceToward(target.position());
      entity.triggerRuneCastAnimation();
      ServantVoiceHelper.tryPlayParacelsusSpell(entity);
      if (entity.level() instanceof ServerLevel level) {
         Vec3 pos = entity.position().add(0.0, entity.getBbHeight() * 0.75, 0.0);
         VFXServerEffects.spawnReplayable(level, "paracelsus_elemental_spirit", entity, 1.1F);
         spawnElementalSpiritAura(level, entity, now);
         level.sendParticles(FIRE, pos.x + 0.7, pos.y, pos.z, 18, 0.12, 0.18, 0.12, 0.01);
         level.sendParticles(WATER, pos.x - 0.7, pos.y, pos.z, 18, 0.12, 0.18, 0.12, 0.01);
         level.sendParticles(EARTH, pos.x, pos.y, pos.z + 0.7, 18, 0.12, 0.18, 0.12, 0.01);
         level.sendParticles(WIND, pos.x, pos.y, pos.z - 0.7, 18, 0.12, 0.18, 0.12, 0.01);
         level.sendParticles(AETHER, pos.x, pos.y + 0.4, pos.z, 20, 0.18, 0.22, 0.18, 0.01);
         level.playSound(null, entity.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 1.0F, 1.25F);
         ParacelsusSpiritCannonEntity cannon = ParacelsusSpiritCannonEntity.summon(level, entity, target, ELEMENTAL_SPIRIT_DURATION);
         level.addFreshEntity(cannon);
      }
      return ServantExecutionResult.SUCCESS.withMpCost(18.0);
   }

   private static void spawnElementalSpiritAura(ServerLevel level, ParacelsusEntity entity, long now) {
      double baseX = entity.getX();
      double baseY = entity.getY() + entity.getBbHeight() * 0.75;
      double baseZ = entity.getZ();
      double orbit = 0.85;
      double rise = 0.18 + Math.sin(now * 0.18) * 0.08;
      double[][] points = new double[][]{
         {baseX + orbit, baseY + rise, baseZ},
         {baseX - orbit, baseY + rise, baseZ},
         {baseX, baseY + rise, baseZ + orbit},
         {baseX, baseY + rise, baseZ - orbit}
      };
      net.minecraft.core.particles.ParticleOptions[] elements = new net.minecraft.core.particles.ParticleOptions[]{FIRE, WATER, EARTH, WIND};
      for (int i = 0; i < points.length; i++) {
         double[] p = points[i];
         level.sendParticles(elements[i], p[0], p[1], p[2], 8, 0.07, 0.07, 0.07, 0.01);
         level.sendParticles(AETHER, p[0], p[1] + 0.08, p[2], 4, 0.05, 0.05, 0.05, 0.005);
      }
      level.sendParticles(ParticleTypes.ENCHANT, baseX, baseY + 0.12, baseZ, 18, 0.28, 0.28, 0.28, 0.02);
      level.sendParticles(ParticleTypes.END_ROD, baseX, baseY + 0.25, baseZ, 10, 0.18, 0.18, 0.18, 0.01);
   }

   private static ServantExecutionResult castPhilosopherStone(ServantCombatActionContext context) {
      if (!(context.caster() instanceof ParacelsusEntity entity)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      LivingEntity target = context.target();
      if (target == null || !target.isAlive() || context.distance() > 18.0) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      long now = context.gameTick();
      if (now - entity.getPersistentData().getLong(TAG_LAST_PHILOSOPHER_STONE) < PHILOSOPHER_STONE_COOLDOWN || entity.getCurrentMp() < 20.0) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      int count = entity.getPersistentData().getInt(TAG_PHILOSOPHER_STONE_COUNT);
      if (count <= 0) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      entity.getPersistentData().putLong(TAG_LAST_PHILOSOPHER_STONE, now);
      entity.getPersistentData().putInt(TAG_PHILOSOPHER_STONE_COUNT, Math.max(0, count - 1));
      entity.faceToward(target.position());
      entity.triggerRuneCastAnimation();
      ServantVoiceHelper.tryPlayParacelsusSpell(entity);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 20.0));
      entity.heal((float)(entity.getMaxHealth() - entity.getHealth()));
      entity.removeEffect(MobEffects.POISON);
      entity.removeEffect(MobEffects.WITHER);
      entity.removeEffect(MobEffects.WEAKNESS);
      entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
      entity.removeEffect(MobEffects.DIG_SLOWDOWN);
      new ArrayList<>(entity.getActiveEffects()).forEach(effect -> {
         if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            entity.removeEffect(effect.getEffect());
         }
      });
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, PHILOSOPHER_STONE_INVULN_TICKS, 1, false, false, true));
      entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, PHILOSOPHER_STONE_INVULN_TICKS, 2, false, false, true));
      entity.invulnerableTime = Math.max(entity.invulnerableTime, PHILOSOPHER_STONE_INVULN_TICKS);
      if (entity.level() instanceof ServerLevel level) {
         VFXServerEffects.spawnReplayable(level, "paracelsus_philosopher_stone", entity, 0.9F);
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + entity.getBbHeight() * 0.8, entity.getZ(), 18, 0.2, 0.3, 0.2, 0.02);
         level.sendParticles(AETHER, entity.getX(), entity.getY() + entity.getBbHeight() * 0.8, entity.getZ(), 26, 0.25, 0.35, 0.25, 0.01);
         level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.9F, 1.3F);
      }
      return ServantExecutionResult.SUCCESS.withMpCost(20.0);
   }

   private static ServantExecutionResult castElementalSword(ServantNoblePhantasmContext context) {
      if (!(context.caster() instanceof ParacelsusEntity entity)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      LivingEntity target = context.target();
      if (target == null || !target.isAlive()) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      long now = entity.level().getGameTime();
      if (now - entity.getPersistentData().getLong(TAG_LAST_NP) < NP_COOLDOWN || context.currentMp() < context.noblePhantasmDefinition().mpCost()) {
         return ServantExecutionResult.FAILED;
      }
      entity.getPersistentData().putLong(TAG_LAST_NP, now);
      entity.faceToward(target.position());
      entity.triggerHorizontalSwingAnimation();
      ServantVoiceHelper.tryPlayParacelsusNp(entity);
      if (entity.level() instanceof ServerLevel level) {
         Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.1, 0.0);
         spawnNoblePhantasmFx(level, center, entity);
      }
      float damage = context.overChargeLevel() > 1 ? 1000.0F : 500.0F;
      entity.getPersistentData().putLong(TAG_SWORD_BUFF_UNTIL, now + NP_BUFF_DURATION);
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, NP_BUFF_DURATION, 1, false, false, true));
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, NP_BUFF_DURATION, 0, false, false, true));
      applyElementalSwordDamage(entity, target, damage);
      if (entity.level() instanceof ServerLevel level) {
         level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.0F, 1.1F);
         level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 24, 0.4, 0.4, 0.4, 0.05);
      }
      return ServantExecutionResult.SUCCESS.withMpCost(context.noblePhantasmDefinition().mpCost());
   }

   private static void spawnNoblePhantasmFx(ServerLevel level, Vec3 center, ServantEntity entity) {
      VFXServerEffects.spawnReplayable(level, "paracelsus_noble_phantasm", entity, 2.5F);
      level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.2, center.z, 60, 0.6, 0.15, 0.6, 0.08);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.4, center.z, 44, 0.7, 0.35, 0.7, 0.05);
      level.sendParticles(FIRE, center.x + 0.8, center.y, center.z, 14, 0.3, 0.2, 0.3, 0.02);
      level.sendParticles(WATER, center.x - 0.8, center.y, center.z, 14, 0.3, 0.2, 0.3, 0.02);
      level.sendParticles(EARTH, center.x, center.y, center.z + 0.8, 14, 0.3, 0.2, 0.3, 0.02);
      level.sendParticles(WIND, center.x, center.y, center.z - 0.8, 14, 0.3, 0.2, 0.3, 0.02);
      level.sendParticles(AETHER, center.x, center.y + 0.8, center.z, 20, 0.35, 0.25, 0.35, 0.02);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.0F, 1.2F);
   }

   private static void releaseElementalStrike(ParacelsusEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || target == null || !target.isAlive()) {
         return;
      }
      int variant = (int)(now % 4L);
      Vec3 origin = entity.position().add(0.0, entity.getBbHeight() * 0.78, 0.0);
      Vec3 hitPoint = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      net.minecraft.core.particles.ParticleOptions element = switch (variant) {
         case 0 -> FIRE;
         case 1 -> WATER;
         case 2 -> EARTH;
         default -> WIND;
      };
      level.sendParticles(element, hitPoint.x, hitPoint.y, hitPoint.z, 18, 0.15, 0.15, 0.15, 0.02);
      level.sendParticles(AETHER, origin.x, origin.y, origin.z, 8, 0.2, 0.2, 0.2, 0.01);
      level.sendParticles(ParticleTypes.END_ROD, origin.x, origin.y, origin.z, 8, 0.25, 0.25, 0.25, 0.02);
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().magic(), (float)(6.0 + entity.getCurrentMp() * 0.02));
      if (variant == 0) {
         target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 80));
      } else if (variant == 1) {
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true));
      } else if (variant == 2) {
         target.push(0.0, 0.18, 0.0);
      } else {
         Vec3 dir = target.position().subtract(entity.position());
         Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
         if (horizontal.lengthSqr() > 1.0E-4) {
            horizontal = horizontal.normalize();
            target.push(horizontal.x * 0.5, 0.22, horizontal.z * 0.5);
         }
      }
      level.playSound(null, target.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.8F, 1.05F + variant * 0.08F);
   }

   private static void applyElementalSwordDamage(ServantEntity entity, LivingEntity target, float damage) {
      if (!(entity.level() instanceof ServerLevel level)) {
         target.invulnerableTime = 0;
         target.hurt(entity.damageSources().magic(), damage);
         return;
      }
      Vec3 forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      AABB search = entity.getBoundingBox().inflate(30.0).expandTowards(forward.scale(30.0));
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         search,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity)
      )) {
         Vec3 toTarget = living.position().subtract(entity.position());
         Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
         if (horizontal.lengthSqr() < 1.0E-4) {
            continue;
         }
         horizontal = horizontal.normalize();
         double dot = forward.x * horizontal.x + forward.z * horizontal.z;
         if (dot < 0.2 || entity.distanceTo(living) > 30.0) {
            continue;
         }
         living.invulnerableTime = 0;
         living.hurt(entity.damageSources().magic(), damage);
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 0, false, true, true));
         living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0, false, true, true));
         Vec3 impact = living.position().add(0.0, living.getBbHeight() * 0.45, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, impact.x, impact.y, impact.z, 12, 0.35, 0.25, 0.35, 0.04);
      }
      level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 34, 0.4, 0.35, 0.4, 0.05);
   }
}
