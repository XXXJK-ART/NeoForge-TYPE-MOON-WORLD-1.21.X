package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantCombatActionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantLifecycleContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantNoblePhantasmContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusBalanceRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusWorkshopHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusSpiritCannonEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.registry.ServantAddonRegistry;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import org.joml.Vector3f;

public final class ParacelsusServantSkills {
   public static final String ACTION_ELEMENTAL_SPIRIT = "elemental_spirit";
   public static final String ACTION_PHILOSOPHER_STONE = "philosopher_stone";
   public static final String NP_ELEMENTAL_SWORD = "elemental_sword";
   /** Shared server-side gate for all Paracelsus NPC combat actions. */
   public static final String TAG_NEXT_COMBAT_ACTION = "ParacelsusNextCombatActionTick";
   private static final String TAG_LAST_ELEMENTAL_SPIRIT = "ParacelsusLastElementalSpiritTick";
   private static final String TAG_LAST_PHILOSOPHER_STONE = "ParacelsusLastPhilosopherStoneTick";
   private static final String TAG_LAST_NP = "ParacelsusLastNpTick";
   private static final String TAG_LAST_NP_OVERDRAFT = "ParacelsusLastNpOverdraft";
   private static final String TAG_NP_CHANT_END = "ParacelsusNpChantEnd";
   private static final String TAG_NP_PENDING_TARGET = "ParacelsusNpPendingTarget";
   private static final String TAG_NP_PENDING_OVERCHARGE = "ParacelsusNpPendingOverCharge";
   private static final String TAG_NP_RESERVED_MP = "ParacelsusNpReservedMp";
   private static final String TAG_NP_FORWARD_X = "ParacelsusNpForwardX";
   private static final String TAG_NP_FORWARD_Y = "ParacelsusNpForwardY";
   private static final String TAG_NP_FORWARD_Z = "ParacelsusNpForwardZ";
   private static final String TAG_PHILOSOPHER_STONE_COUNT = "ParacelsusPhilosopherStoneCount";
   private static final String TAG_HIGH_SPEED_UNTIL = "ParacelsusHighSpeedChantingUntil";
   private static final String TAG_ELEMENTAL_SPIRIT_ACTIVE_UNTIL = "ParacelsusElementalSpiritActiveUntil";
   private static final String TAG_ELEMENTAL_SPIRIT_LAST_SUMMON = "ParacelsusLastElementalSpiritSummon";
   private static final String TAG_LAST_ELEMENTAL_STRIKE = "ParacelsusLastElementalStrikeTick";
   private static final String TAG_SWORD_BUFF_UNTIL = "ParacelsusSwordBuffUntil";
   private static final String TAG_FIRE_MAGIC_A_UNTIL = "ParacelsusFireMagicAUntil";
   private static final String TAG_FIRE_MAGIC_B_UNTIL = "ParacelsusFireMagicBUntil";
   private static final String TAG_WATER_MAGIC_A_UNTIL = "ParacelsusWaterMagicAUntil";
   private static final String TAG_WATER_MAGIC_B_UNTIL = "ParacelsusWaterMagicBUntil";
   private static final String TAG_EARTH_MAGIC_A_UNTIL = "ParacelsusEarthMagicAUntil";
   private static final String TAG_EARTH_MAGIC_B_UNTIL = "ParacelsusEarthMagicBUntil";
   private static final String TAG_WIND_MAGIC_A_UNTIL = "ParacelsusWindMagicAUntil";
   private static final String TAG_WIND_MAGIC_B_UNTIL = "ParacelsusWindMagicBUntil";
   private static final String TAG_LAST_FIRE_MAGIC = "ParacelsusLastFireMagicTick";
   private static final String TAG_LAST_WATER_MAGIC = "ParacelsusLastWaterMagicTick";
   private static final String TAG_LAST_EARTH_MAGIC = "ParacelsusLastEarthMagicTick";
   private static final String TAG_LAST_WIND_MAGIC = "ParacelsusLastWindMagicTick";
   private static final int ELEMENTAL_SPIRIT_COOLDOWN = 300;
   private static final int PHILOSOPHER_STONE_COOLDOWN = 900;
   private static final int NP_COOLDOWN = 900;
   private static final int NP_CHANT_TICKS = 140;
   private static final int ELEMENTAL_MAGIC_COOLDOWN = 100;
   private static final int TARGET_CANNON_COOLDOWN = 90;
   private static final int PHILOSOPHER_STONE_STARTING_CHARGES = 3;
   private static final int PHILOSOPHER_STONE_MAX_CHARGES = 3;
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
      registry.register("fire_magic_a", context -> markElementalMagic(context, TAG_FIRE_MAGIC_A_UNTIL), "typemoonworld_core");
      registry.register("fire_magic_b", context -> markElementalMagic(context, TAG_FIRE_MAGIC_B_UNTIL), "typemoonworld_core");
      registry.register("water_magic_a", context -> markElementalMagic(context, TAG_WATER_MAGIC_A_UNTIL), "typemoonworld_core");
      registry.register("water_magic_b", context -> markElementalMagic(context, TAG_WATER_MAGIC_B_UNTIL), "typemoonworld_core");
      registry.register("earth_magic_a", context -> markElementalMagic(context, TAG_EARTH_MAGIC_A_UNTIL), "typemoonworld_core");
      registry.register("earth_magic_b", context -> markElementalMagic(context, TAG_EARTH_MAGIC_B_UNTIL), "typemoonworld_core");
      registry.register("wind_magic_a", context -> markElementalMagic(context, TAG_WIND_MAGIC_A_UNTIL), "typemoonworld_core");
      registry.register("wind_magic_b", context -> markElementalMagic(context, TAG_WIND_MAGIC_B_UNTIL), "typemoonworld_core");
   }

   public static void registerCombatActions(IServantAddonRegistry registry) {
      registry.registerCombatAction(ACTION_ELEMENTAL_SPIRIT, ParacelsusServantSkills::castElementalSpirit, "typemoonworld_core");
      registry.registerCombatAction(ACTION_PHILOSOPHER_STONE, ParacelsusServantSkills::castPhilosopherStone, "typemoonworld_core");
      registry.registerCombatAction("fire_magic_a_cast", context -> castElementalBurst(context, "fire_a"), "typemoonworld_core");
      registry.registerCombatAction("fire_magic_b_cast", context -> castElementalBurst(context, "fire_b"), "typemoonworld_core");
      registry.registerCombatAction("water_magic_a_cast", context -> castElementalBurst(context, "water_a"), "typemoonworld_core");
      registry.registerCombatAction("water_magic_b_cast", context -> castElementalBurst(context, "water_b"), "typemoonworld_core");
      registry.registerCombatAction("earth_magic_a_cast", context -> castElementalBurst(context, "earth_a"), "typemoonworld_core");
      registry.registerCombatAction("earth_magic_b_cast", context -> castElementalBurst(context, "earth_b"), "typemoonworld_core");
      registry.registerCombatAction("wind_magic_a_cast", context -> castElementalBurst(context, "wind_a"), "typemoonworld_core");
      registry.registerCombatAction("wind_magic_b_cast", context -> castElementalBurst(context, "wind_b"), "typemoonworld_core");
      registry.registerCombatAction("fire_magic_burst", context -> castElementalBurst(context, "fire_a"), "typemoonworld_core");
      registry.registerCombatAction("water_magic_burst", context -> castElementalBurst(context, "water_a"), "typemoonworld_core");
      registry.registerCombatAction("earth_magic_burst", context -> castElementalBurst(context, "earth_a"), "typemoonworld_core");
      registry.registerCombatAction("wind_magic_burst", context -> castElementalBurst(context, "wind_a"), "typemoonworld_core");
      registry.registerNoblePhantasm(NP_ELEMENTAL_SWORD, ParacelsusServantSkills::castElementalSword, "typemoonworld_core");
      registry.registerLifecycleHandler("paracelsus_lifecycle", ParacelsusServantSkills::tickParacelsus, "typemoonworld_core");
   }

   public static boolean isNoblePhantasmChanting(ParacelsusEntity entity, long now) {
      return entity != null && entity.getPersistentData().getLong(TAG_NP_CHANT_END) > now;
   }

   public static boolean combatActionReady(ParacelsusEntity entity, long now) {
      return entity != null && entity.getPersistentData().getLong(TAG_NEXT_COMBAT_ACTION) <= now;
   }

   public static void markCombatAction(ParacelsusEntity entity, long now, long gapTicks) {
      if (entity != null) {
         entity.getPersistentData().putLong(TAG_NEXT_COMBAT_ACTION, now + Math.max(1L, gapTicks));
      }
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

   private static ServantExecutionResult markElementalMagic(ServantExecutionContext context, String tag) {
      if (!(context.caster() instanceof ParacelsusEntity entity)) {
         return ServantExecutionResult.FAILED;
      }
      long now = entity.level().getGameTime();
      entity.getPersistentData().putLong(tag, now + 20L * 60L);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult castElementalBurst(ServantCombatActionContext context, String element) {
      if (!(context.caster() instanceof ParacelsusEntity entity)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      LivingEntity target = context.target();
      if (target == null || !target.isAlive() || !isHostileElementTarget(entity, target) || context.distance() > 32.0) {
         return ServantExecutionResult.NOT_HANDLED;
      }

      long now = context.gameTick();
      if (isNoblePhantasmChanting(entity, now) || !combatActionReady(entity, now)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      if (entity.isPerformingAction() && !entity.isSoftCombatActionActive() && entity.getPersistentData().getLong(TAG_HIGH_SPEED_UNTIL) <= now) {
         return ServantExecutionResult.NOT_HANDLED;
      }

      String lastTickKey = switch (element) {
         case "fire_a", "fire_b" -> TAG_LAST_FIRE_MAGIC;
         case "water_a", "water_b" -> TAG_LAST_WATER_MAGIC;
         case "earth_a", "earth_b" -> TAG_LAST_EARTH_MAGIC;
         default -> TAG_LAST_WIND_MAGIC;
      };
      if (now - entity.getPersistentData().getLong(lastTickKey) < ELEMENTAL_MAGIC_COOLDOWN) {
         return ServantExecutionResult.NOT_HANDLED;
      }

      if (entity.getCurrentMp() < 7.0) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      entity.getPersistentData().putLong(lastTickKey, now);
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.45, 0.0));
      entity.triggerRuneCastAnimation(14);
      ServantVoiceHelper.tryPlayParacelsusSpell(entity);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 7.0));

      if (entity.level() instanceof ServerLevel level) {
         Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
         switch (element) {
            case "fire_a" -> castFireFurnace(level, entity, target, center, now);
            case "fire_b" -> castFireRing(level, entity, target, center, now);
            case "water_a" -> castDeepSeaPressure(level, entity, target, center, now);
            case "water_b" -> castWaterGeyser(level, entity, target, center, now);
            case "earth_a" -> castMountainRoar(level, entity, target, center, now);
            case "earth_b" -> castEarthPrison(level, entity, target, center, now);
            case "wind_a" -> castFirmamentCut(level, entity, target, center, now);
            default -> castWindCut(level, entity, target, center, now);
         }
      }
      markCombatAction(entity, now, 20L);
      return ServantExecutionResult.SUCCESS.withMpCost(7.0);
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
      int phase = entity.computeCombatPhase();
      entity.setCombatPhase(phase);
      tickElementalMagicAuras(entity, now);
      tickNoblePhantasmChant(entity, now);
      if (entity.getPersistentData().getLong(TAG_HIGH_SPEED_UNTIL) > now) {
         entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 10, 0, false, false, true));
      }

      boolean spiritActive = entity.getPersistentData().getLong(TAG_ELEMENTAL_SPIRIT_ACTIVE_UNTIL) > now;
      if (spiritActive && entity.level() instanceof ServerLevel level && now % 6L == 0L) {
         spawnElementalSpiritAura(level, entity, now);
      }

      LivingEntity target = context.target();
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         if (!spiritActive) {
            entity.setCombatPhase(entity.computeCombatPhase());
         }
         return ServantExecutionResult.NOT_HANDLED;
      }

      entity.getLookControl().setLookAt(target, 35.0F, 35.0F);
      if (spiritActive
         && entity.distanceTo(target) <= 20.0
         && combatActionReady(entity, now)
         && now - entity.getPersistentData().getLong(TAG_LAST_ELEMENTAL_STRIKE) >= 34L) {
         entity.getPersistentData().putLong(TAG_LAST_ELEMENTAL_STRIKE, now);
         releaseElementalStrike(entity, target, now);
         markCombatAction(entity, now, 20L);
      }

      double distance = entity.distanceTo(target);
      if (distance > 13.5 && now % 10L == 0L) {
         entity.getNavigation().moveTo(target, 0.95);
      } else if (distance < 6.5 && now % 10L == 0L) {
         Vec3 away = entity.position().subtract(target.position());
         if (away.lengthSqr() > 1.0E-4) {
            away = away.normalize().scale(4.5);
            entity.getNavigation().moveTo(entity.getX() + away.x, entity.getY(), entity.getZ() + away.z, 1.0);
         }
      }
      return ServantExecutionResult.NOT_HANDLED;
   }

   private static void tickElementalMagicAuras(ParacelsusEntity entity, long now) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      if (now % 10L != 0L) {
         return;
      }
      Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.72, 0.0);
      if (entity.getPersistentData().getLong(TAG_FIRE_MAGIC_A_UNTIL) > now || entity.getPersistentData().getLong(TAG_FIRE_MAGIC_B_UNTIL) > now) {
         level.sendParticles(FIRE, center.x + 0.7, center.y, center.z, 8, 0.08, 0.08, 0.08, 0.01);
      }
      if (entity.getPersistentData().getLong(TAG_WATER_MAGIC_A_UNTIL) > now || entity.getPersistentData().getLong(TAG_WATER_MAGIC_B_UNTIL) > now) {
         level.sendParticles(WATER, center.x - 0.7, center.y, center.z, 8, 0.08, 0.08, 0.08, 0.01);
      }
      if (entity.getPersistentData().getLong(TAG_EARTH_MAGIC_A_UNTIL) > now || entity.getPersistentData().getLong(TAG_EARTH_MAGIC_B_UNTIL) > now) {
         level.sendParticles(EARTH, center.x, center.y, center.z + 0.7, 8, 0.08, 0.08, 0.08, 0.01);
      }
      if (entity.getPersistentData().getLong(TAG_WIND_MAGIC_A_UNTIL) > now || entity.getPersistentData().getLong(TAG_WIND_MAGIC_B_UNTIL) > now) {
         level.sendParticles(WIND, center.x, center.y, center.z - 0.7, 8, 0.08, 0.08, 0.08, 0.01);
      }
   }

   private static boolean tickNoblePhantasmChant(ParacelsusEntity entity, long now) {
      long chantEnd = entity.getPersistentData().getLong(TAG_NP_CHANT_END);
      if (chantEnd <= 0L) {
         return false;
      }
      if (now < chantEnd) {
         if (entity.level() instanceof ServerLevel level) {
            LivingEntity target = resolveNoblePhantasmTarget(level, entity);
            if (target != null) {
               faceNoblePhantasmTarget(entity, target);
            }
         }
         return true;
      }
      if (entity.level() instanceof ServerLevel level) {
         LivingEntity target = resolveNoblePhantasmTarget(level, entity);
         if (target != null && target.isAlive() && isHostileNoblePhantasmTarget(entity, target)) {
            releaseElementalSword(entity, target, now);
         }
      }
      entity.getPersistentData().remove(TAG_NP_CHANT_END);
      entity.getPersistentData().remove(TAG_NP_PENDING_TARGET);
      entity.getPersistentData().remove(TAG_NP_PENDING_OVERCHARGE);
      entity.getPersistentData().remove(TAG_NP_RESERVED_MP);
      entity.getPersistentData().remove(TAG_NP_FORWARD_X);
      entity.getPersistentData().remove(TAG_NP_FORWARD_Y);
      entity.getPersistentData().remove(TAG_NP_FORWARD_Z);
      return false;
   }

   private static void releaseElementalSword(ParacelsusEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      int overCharge = entity.getPersistentData().getInt(TAG_NP_PENDING_OVERCHARGE);
      float damage = (overCharge > 1 ? 1000.0F : 500.0F)
         * (float)ServantNoblePhantasmResourceService.powerScale(entity);
      entity.getPersistentData().putLong(TAG_SWORD_BUFF_UNTIL, now + NP_BUFF_DURATION);
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, NP_BUFF_DURATION, 1, false, false, true));
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, NP_BUFF_DURATION, 0, false, false, true));
      entity.triggerHorizontalSwingAnimation();
      spawnNoblePhantasmFx(level, entity);
      applyElementalSwordDamage(entity, target, damage, level);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.0F, 1.1F);
      level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 24, 0.4, 0.4, 0.4, 0.05);
   }

   private static void spawnNoblePhantasmFx(ServerLevel level, ParacelsusEntity entity) {
      VFXServerEffects.spawnReplayable(level, "paracelsus_noble_phantasm", entity, 2.5F);
      Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.75, 0.0);
      level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.2, center.z, 34, 0.45, 0.15, 0.45, 0.05);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.4, center.z, 20, 0.35, 0.2, 0.35, 0.03);
      level.sendParticles(FIRE, center.x + 0.7, center.y, center.z, 10, 0.12, 0.12, 0.12, 0.01);
      level.sendParticles(WATER, center.x - 0.7, center.y, center.z, 10, 0.12, 0.12, 0.12, 0.01);
      level.sendParticles(EARTH, center.x, center.y, center.z + 0.7, 10, 0.12, 0.12, 0.12, 0.01);
      level.sendParticles(WIND, center.x, center.y, center.z - 0.7, 10, 0.12, 0.12, 0.12, 0.01);
      level.sendParticles(AETHER, center.x, center.y + 0.8, center.z, 20, 0.25, 0.2, 0.25, 0.02);
   }

   private static void applyElementalSwordDamage(ParacelsusEntity entity, LivingEntity target, float damage, ServerLevel level) {
      Vec3 forward = new Vec3(
         entity.getPersistentData().getDouble(TAG_NP_FORWARD_X),
         entity.getPersistentData().getDouble(TAG_NP_FORWARD_Y),
         entity.getPersistentData().getDouble(TAG_NP_FORWARD_Z)
      );
      if (forward.lengthSqr() < 1.0E-4) {
         forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      if (side.lengthSqr() < 1.0E-4) {
         side = new Vec3(1.0, 0.0, 0.0);
      } else {
         side = side.normalize();
      }

      AABB search = entity.getBoundingBox().inflate(100.0).expandTowards(forward.scale(100.0));
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, search, e -> isHostileNoblePhantasmTarget(entity, e))) {
         if (!isInStoredForwardFan(entity, living, forward, 100.0, 22.5)) {
            continue;
         }
         living.invulnerableTime = 0;
         living.hurt(entity.damageSources().magic(), paracelsusSkillDamage(damage));
         living.invulnerableTime = 0;
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 0, false, true, true));
         living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0, false, true, true));
         Vec3 impact = living.position().add(0.0, living.getBbHeight() * 0.45, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, impact.x, impact.y, impact.z, 12, 0.35, 0.25, 0.35, 0.04);
      }

      breakElementalSwordTerrain(level, entity, forward);
   }

   private static LivingEntity resolveNoblePhantasmTarget(ServerLevel level, ParacelsusEntity entity) {
      Entity resolved = level.getEntity(entity.getPersistentData().getInt(TAG_NP_PENDING_TARGET));
      LivingEntity target = resolved instanceof LivingEntity living && living.isAlive() ? living : entity.getTarget();
      return target != null && target.isAlive() && isHostileNoblePhantasmTarget(entity, target) ? target : null;
   }

   private static void faceNoblePhantasmTarget(ParacelsusEntity entity, LivingEntity target) {
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.45, 0.0));
      Vec3 forward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      entity.getPersistentData().putDouble(TAG_NP_FORWARD_X, forward.x);
      entity.getPersistentData().putDouble(TAG_NP_FORWARD_Y, forward.y);
      entity.getPersistentData().putDouble(TAG_NP_FORWARD_Z, forward.z);
   }

   private static boolean isHostileNoblePhantasmTarget(ParacelsusEntity entity, LivingEntity living) {
      if (living == null || living == entity) {
         return false;
      }
      if (!EntityUtils.isValidCombatTarget(entity, living)) {
         return false;
      }
      if (living instanceof net.minecraft.world.entity.NeutralMob neutral && !neutral.isAngry()) {
         return false;
      }
      if (living instanceof net.minecraft.world.entity.monster.Monster) {
         return true;
      }
      if (living instanceof net.minecraft.world.entity.player.Player player) {
         return !EntityUtils.isImmunePlayerTarget(player) && !entity.isAlliedTo(player) && !player.isAlliedTo(entity);
      }
      return living instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == entity;
   }

   private static boolean isInStoredForwardFan(ParacelsusEntity entity, LivingEntity living, Vec3 forward, double maxDistance, double halfAngleDeg) {
      Vec3 toLiving = living.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(toLiving.x, 0.0, toLiving.z);
      if (horizontal.lengthSqr() < 1.0E-4 || horizontal.lengthSqr() > maxDistance * maxDistance) {
         return false;
      }
      horizontal = horizontal.normalize();
      return horizontal.dot(forward) >= Math.cos(Math.toRadians(halfAngleDeg));
   }

   private static void breakElementalSwordTerrain(ServerLevel level, ParacelsusEntity entity, Vec3 forward) {
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      if (right.lengthSqr() < 1.0E-4) {
         right = new Vec3(1.0, 0.0, 0.0);
      } else {
         right = right.normalize();
      }
      Vec3 origin = entity.position().add(0.0, entity.getBbHeight() * 0.7, 0.0);
      BlockPos center = BlockPos.containing(origin.add(forward.scale(2.0)));
      breakFanTerrain(level, entity, center, forward, right, 1, 50, 90);
      breakFanTerrain(level, entity, center, forward, right, 51, 100, 130);
   }

   private static void breakFanTerrain(ServerLevel level, ParacelsusEntity entity, BlockPos center, Vec3 forward, Vec3 right, int startStep, int length, int maxBroken) {
      int broken = 0;
      int vertical = 4;
      for (int step = Math.max(1, startStep); step <= length && broken < maxBroken; step++) {
         Vec3 stepCenter = entity.position().add(forward.scale(step));
         BlockPos base = BlockPos.containing(stepCenter.x, center.getY(), stepCenter.z);
         int sideRange = Math.max(1, Mth.ceil(step * Math.tan(Math.toRadians(22.5))));
         for (int sideStep = -sideRange; sideStep <= sideRange && broken < maxBroken; sideStep++) {
            for (int y = -1; y <= vertical && broken < maxBroken; y++) {
               Vec3 posVec = new Vec3(base.getX() + 0.5, base.getY() + y, base.getZ() + 0.5)
                  .add(right.scale(sideStep));
               BlockPos pos = BlockPos.containing(posVec);
               if (!isInFanShape(center, posVec, forward, right, length)) {
                  continue;
               }
               if (destroyFanBlock(level, pos, step > 50, step > 50 ? 70.0F : 40.0F)) {
                  broken++;
               }
            }
         }
      }
   }

   private static boolean isInFanShape(BlockPos center, Vec3 pos, Vec3 forward, Vec3 right, int length) {
      Vec3 fromCenter = pos.subtract(Vec3.atCenterOf(center));
      double forwardDist = fromCenter.dot(forward);
      if (forwardDist < 0.0 || forwardDist > length) {
         return false;
      }
      double sideDist = Math.abs(fromCenter.dot(right));
      double maxSide = Math.max(0.75, forwardDist * Math.tan(Math.toRadians(22.5)));
      return sideDist <= maxSide;
   }

   private static boolean destroyFanBlock(ServerLevel level, BlockPos pos, boolean heavy, float maxHardness) {
      if (pos == null) {
         return false;
      }
      var state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      if (state.isAir() || state.is(net.minecraft.world.level.block.Blocks.BEDROCK) || hardness < 0.0F || hardness >= maxHardness) {
         return false;
      }
      if (level.removeBlock(pos, false)) {
         level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, heavy ? 3 : 1, 0.12, 0.12, 0.12, 0.02);
         return true;
      }
      return false;
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
      if (isNoblePhantasmChanting(entity, now)
         || !combatActionReady(entity, now)
         || now - entity.getPersistentData().getLong(TAG_ELEMENTAL_SPIRIT_LAST_SUMMON) < ELEMENTAL_SPIRIT_COOLDOWN
         || entity.getCurrentMp() < 18.0) {
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
         Vec3 guardianPos = target.position().add(0.0, target.getBbHeight() + 1.2, 0.0);
         ParacelsusSpiritCannonEntity guardian = ParacelsusSpiritCannonEntity.summonGuardian(level, entity, guardianPos, entity.getRandom().nextInt(4), 120 * 20);
         level.addFreshEntity(guardian);
      }
      markCombatAction(entity, now, 20L);
      return ServantExecutionResult.SUCCESS.withMpCost(18.0);
   }

   private static void castFireFurnace(ServerLevel level, ParacelsusEntity entity, LivingEntity target, Vec3 center, long now) {
      level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.35, center.z, 120, 3.6, 0.35, 3.6, 0.08);
      level.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.25, center.z, 34, 2.8, 0.18, 2.8, 0.02);
      level.sendParticles(FIRE, center.x, center.y + 0.5, center.z, 70, 2.6, 0.45, 2.6, 0.02);
      breakElementalTerrain(level, entity, center, 8.0, 4, 48, 0.72F, 0.52F);

      int pillars = 3 + (int)(Math.abs(now) % 3L);
      for (int i = 0; i < pillars; i++) {
         double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
         double distance = 1.0 + entity.getRandom().nextDouble() * 7.0;
         Vec3 pos = center.add(Math.cos(angle) * distance, 0.2, Math.sin(angle) * distance);
         level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 1.5, pos.z, 54, 0.38, 1.45, 0.38, 0.08);
         level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.35, pos.z, 18, 0.25, 0.16, 0.25, 0.0);
         for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(2.2, 3.4, 2.2), e -> isHostileElementTarget(entity, e))) {
            applyElementalMagicHit(entity, living, 80.0F, 100, false, false, true, level);
            scheduleMagicBurn(entity, living, level, 15.0F, 5);
         }
      }
      level.playSound(null, target.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.2F, 0.78F);
   }

   private static void castDeepSeaPressure(ServerLevel level, ParacelsusEntity entity, LivingEntity target, Vec3 center, long now) {
      level.sendParticles(ParticleTypes.SPLASH, center.x, center.y + 0.8, center.z, 160, 3.0, 2.1, 3.0, 0.10);
      level.sendParticles(ParticleTypes.BUBBLE, center.x, center.y + 1.0, center.z, 120, 2.4, 1.7, 2.4, 0.08);
      level.sendParticles(WATER, center.x, center.y + 0.7, center.z, 76, 2.1, 1.1, 2.1, 0.02);
      breakElementalTerrain(level, entity, center, 7.5, 4, 38, 0.58F, 0.42F);
      AABB prison = new AABB(center, center).inflate(3.2, 3.2, 3.2);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, prison, e -> isHostileElementTarget(entity, e))) {
         applyElementalMagicHit(entity, living, 30.0F, 0, true, false, false, level);
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2, false, true, true));
         living.addEffect(new MobEffectInstance(MobEffects.JUMP, 120, 128, false, true, true));
         Vec3 pull = center.subtract(living.position()).multiply(0.13, 0.04, 0.13);
         living.setDeltaMovement(living.getDeltaMovement().scale(0.35).add(pull.x, pull.y, pull.z));
         living.hurtMarked = true;
      }
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(7.0, 3.5, 7.0), e -> isHostileElementTarget(entity, e))) {
         Vec3 away = living.position().subtract(center).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() > 1.0E-4) {
            away = away.normalize();
            living.push(away.x * 1.0, 0.22, away.z * 1.0);
         }
         applyElementalMagicHit(entity, living, 50.0F, 0, false, false, false, level);
      }
      level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 1.25F, 0.72F);
   }

   private static void castMountainRoar(ServerLevel level, ParacelsusEntity entity, LivingEntity target, Vec3 center, long now) {
      level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.25, center.z, 22, 3.0, 0.35, 3.0, 0.02);
      level.sendParticles(EARTH, center.x, center.y + 0.65, center.z, 90, 3.2, 0.65, 3.2, 0.03);
      breakElementalTerrain(level, entity, center, 10.0, 5, 72, 0.84F, 0.62F);
      int pillars = 3 + (int)(Math.abs(now) % 4L);
      for (int i = 0; i < pillars; i++) {
         double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
         double distance = 1.5 + entity.getRandom().nextDouble() * 8.5;
         Vec3 pos = center.add(Math.cos(angle) * distance, 0.1, Math.sin(angle) * distance);
         level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()), pos.x, pos.y + 0.9, pos.z, 34, 0.6, 0.9, 0.6, 0.06);
         for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(1.8, 3.0, 1.8), e -> isHostileElementTarget(entity, e))) {
            applyElementalMagicHit(entity, living, 60.0F, 0, false, true, false, level);
            living.push(0.0, 0.72, 0.0);
         }
      }
      for (int i = 0; i < 8; i++) {
         Vec3 impact = center.add((entity.getRandom().nextDouble() - 0.5) * 14.0, 0.4, (entity.getRandom().nextDouble() - 0.5) * 14.0);
         level.sendParticles(ParticleTypes.POOF, impact.x, impact.y, impact.z, 20, 0.55, 0.22, 0.55, 0.08);
         for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(impact, impact).inflate(2.5, 2.8, 2.5), e -> isHostileElementTarget(entity, e))) {
            applyElementalMagicHit(entity, living, 40.0F, 0, false, false, false, level);
         }
      }
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(8.0, 3.0, 8.0), e -> isHostileElementTarget(entity, e))) {
         applyElementalMagicHit(entity, living, 80.0F, 0, true, false, false, level);
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 9, false, true, true));
      }
      level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.25F, 0.62F);
   }

   private static void castFirmamentCut(ServerLevel level, ParacelsusEntity entity, LivingEntity target, Vec3 center, long now) {
      Vec3 forward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x).normalize();
      int blades = 3 + (int)(Math.abs(now) % 3L);
      for (int blade = 0; blade < blades; blade++) {
         double offset = (blade - (blades - 1) * 0.5) * 0.85;
         java.util.Set<Integer> damaged = new java.util.HashSet<>();
         for (int step = 1; step <= 20; step++) {
            Vec3 p = entity.position().add(0.0, entity.getBbHeight() * 0.65, 0.0).add(forward.scale(step)).add(side.scale(offset));
            level.sendParticles(WIND, p.x, p.y, p.z, 7, 0.16, 0.1, 0.16, 0.01);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            AABB hit = new AABB(p, p).inflate(1.7, 1.5, 1.7);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, hit, e -> isHostileElementTarget(entity, e))) {
               if (damaged.add(living.getId())) {
                  applyElementalMagicHit(entity, living, 70.0F, 0, true, false, false, level);
                  Vec3 pull = p.subtract(living.position()).multiply(0.08, 0.0, 0.08);
                  living.setDeltaMovement(living.getDeltaMovement().scale(0.45).add(pull.x, 0.03, pull.z));
                  living.hurtMarked = true;
               }
            }
         }
      }
      breakElementalTerrain(level, entity, entity.position().add(forward.scale(10.0)), 8.0, 3, 36, 0.50F, 0.34F);
      level.playSound(null, target.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.15F, 1.2F);
   }

   private static void castFireBurst(ServerLevel level, ParacelsusEntity entity, LivingEntity target, Vec3 center, long now) {
      AABB area = new AABB(center, center).inflate(6.5, 4.5, 6.5);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, e -> isHostileElementTarget(entity, e))) {
         applyElementalMagicHit(entity, living, 10.0F, 1, true, false, false, level);
      }
      breakElementalTerrain(level, entity, center, 8.5, 5, 52, 0.82F, 0.70F);
      level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.25, center.z, 80, 2.2, 0.55, 2.2, 0.06);
      level.sendParticles(ParticleTypes.SMOKE, center.x, center.y + 0.35, center.z, 40, 2.0, 0.35, 2.0, 0.03);
      level.playSound(null, target.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 0.9F + (now % 4) * 0.05F);
      summonTargetCannonRing(level, entity, target, 1, 5.0, 0.65F, now);
   }

   private static void castFireRing(ServerLevel level, ParacelsusEntity entity, LivingEntity target, Vec3 center, long now) {
      AABB area = new AABB(center, center).inflate(8.0, 3.5, 8.0);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, e -> isHostileElementTarget(entity, e))) {
         double distance = living.position().multiply(1.0, 0.0, 1.0).distanceTo(center.multiply(1.0, 0.0, 1.0));
         if (distance >= 2.4 && distance <= 8.0) {
            applyElementalMagicHit(entity, living, 7.0F, 120, false, false, true, level);
         }
      }
      for (int i = 0; i < 12; i++) {
         double angle = Math.PI * 2.0 * i / 12.0;
         level.sendParticles(ParticleTypes.FLAME, center.x + Math.cos(angle) * 5.2, center.y + 0.3, center.z + Math.sin(angle) * 5.2, 12, 0.25, 0.12, 0.25, 0.04);
      }
      breakElementalTerrain(level, entity, center, 9.0, 3, 38, 0.75F, 0.55F);
      level.playSound(null, target.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.1F, 0.85F);
   }

   private static void castWaterBurst(ServerLevel level, ParacelsusEntity entity, LivingEntity target, Vec3 center, long now) {
      AABB area = new AABB(center, center).inflate(6.5, 4.5, 6.5);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, e -> isHostileElementTarget(entity, e))) {
         applyElementalMagicHit(entity, living, 8.0F, 1, false, true, false, level);
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, true, true));
      }
      breakElementalTerrain(level, entity, center, 7.0, 4, 42, 0.68F, 0.58F);
      level.sendParticles(ParticleTypes.SPLASH, center.x, center.y + 0.25, center.z, 72, 2.0, 0.55, 2.0, 0.08);
      level.sendParticles(AETHER, center.x, center.y + 0.45, center.z, 36, 1.5, 0.45, 1.5, 0.02);
      level.playSound(null, target.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.HOSTILE, 1.0F, 1.1F);
      summonTargetCannonRing(level, entity, target, 1, 4.2, 0.75F, now);
   }

   private static void castWaterGeyser(ServerLevel level, ParacelsusEntity entity, LivingEntity target, Vec3 center, long now) {
      AABB area = new AABB(center, center).inflate(5.5, 6.5, 5.5);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, e -> isHostileElementTarget(entity, e))) {
         applyElementalMagicHit(entity, living, 6.5F, 0, true, true, false, level);
         living.clearFire();
         living.push(0.0, 0.65, 0.0);
      }
      level.sendParticles(ParticleTypes.SPLASH, center.x, center.y + 1.0, center.z, 120, 1.0, 1.4, 1.0, 0.18);
      level.sendParticles(ParticleTypes.BUBBLE, center.x, center.y + 0.6, center.z, 70, 1.2, 0.9, 1.2, 0.08);
      breakElementalTerrain(level, entity, center, 6.0, 5, 34, 0.62F, 0.48F);
      level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 1.2F, 0.75F);
   }

   private static void castEarthBurst(ServerLevel level, ParacelsusEntity entity, LivingEntity target, Vec3 center, long now) {
      AABB area = new AABB(center, center).inflate(6.5, 4.5, 6.5);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, e -> isHostileElementTarget(entity, e))) {
         applyElementalMagicHit(entity, living, 9.0F, 1, false, false, true, level);
         living.push(0.0, 0.15, 0.0);
      }
      breakElementalTerrain(level, entity, center, 9.0, 6, 72, 0.78F, 0.62F);
      level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2, center.z, 14, 1.8, 0.45, 1.8, 0.01);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.7, center.z, 26, 1.0, 0.25, 1.0, 0.03);
      level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.1F, 0.85F);
      summonTargetCannonRing(level, entity, target, 1, 5.5, 0.65F, now);
   }

   private static void castEarthPrison(ServerLevel level, ParacelsusEntity entity, LivingEntity target, Vec3 center, long now) {
      AABB area = new AABB(center, center).inflate(7.0, 4.0, 7.0);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, e -> isHostileElementTarget(entity, e))) {
         applyElementalMagicHit(entity, living, 7.5F, 0, true, true, false, level);
         living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1, false, true, true));
      }
      for (int i = 0; i < 10; i++) {
         double angle = Math.PI * 2.0 * i / 10.0;
         level.sendParticles(ParticleTypes.EXPLOSION, center.x + Math.cos(angle) * 3.8, center.y + 0.15, center.z + Math.sin(angle) * 3.8, 2, 0.1, 0.15, 0.1, 0.0);
         level.sendParticles(EARTH, center.x + Math.cos(angle) * 3.8, center.y + 0.65, center.z + Math.sin(angle) * 3.8, 8, 0.25, 0.35, 0.25, 0.02);
      }
      breakElementalTerrain(level, entity, center, 8.5, 6, 70, 0.80F, 0.65F);
      level.playSound(null, target.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.3F, 0.65F);
   }

   private static void castWindBurst(ServerLevel level, ParacelsusEntity entity, LivingEntity target, Vec3 center, long now) {
      AABB area = new AABB(center, center).inflate(7.0, 4.5, 7.0);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, e -> isHostileElementTarget(entity, e))) {
         applyElementalMagicHit(entity, living, 7.0F, 1, false, false, false, level);
         Vec3 dir = living.position().subtract(center);
         Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
         if (horizontal.lengthSqr() > 1.0E-4) {
            horizontal = horizontal.normalize();
            living.push(horizontal.x * 0.8, 0.25, horizontal.z * 0.8);
         }
      }
      breakElementalTerrain(level, entity, center, 10.0, 4, 36, 0.60F, 0.52F);
      level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.25, center.z, 80, 2.6, 0.3, 2.6, 0.06);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.7, center.z, 30, 1.5, 0.35, 1.5, 0.03);
      level.playSound(null, target.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.HOSTILE, 1.0F, 0.8F);
      summonTargetCannonRing(level, entity, target, 1, 5.8, 0.55F, now);
   }

   private static void castWindCut(ServerLevel level, ParacelsusEntity entity, LivingEntity target, Vec3 center, long now) {
      Vec3 forward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x).normalize();
      AABB area = entity.getBoundingBox().inflate(18.0).expandTowards(forward.scale(18.0));
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, e -> isHostileElementTarget(entity, e))) {
         Vec3 rel = living.position().subtract(entity.position());
         double along = rel.dot(forward);
         double across = Math.abs(rel.dot(side));
         if (along >= 1.5 && along <= 18.0 && across <= 2.4) {
            applyElementalMagicHit(entity, living, 8.5F, 0, false, false, true, level);
         }
      }
      for (int i = 2; i <= 18; i += 2) {
         Vec3 p = entity.position().add(forward.scale(i)).add(0.0, entity.getBbHeight() * 0.55, 0.0);
         level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 12, 0.9, 0.12, 0.9, 0.05);
         level.sendParticles(WIND, p.x, p.y + 0.2, p.z, 8, 0.5, 0.1, 0.5, 0.02);
      }
      level.playSound(null, target.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.0F, 1.2F);
   }

   private static void summonTargetCannonRing(ServerLevel level, ParacelsusEntity entity, LivingEntity target, int count, double radius, float lifeScale, long now) {
      if (count <= 0) {
         return;
      }
      if (now - entity.getPersistentData().getLong("ParacelsusLastTargetCannonMagic") < TARGET_CANNON_COOLDOWN) {
         return;
      }
      entity.getPersistentData().putLong("ParacelsusLastTargetCannonMagic", now);
      for (int i = 0; i < count; i++) {
         double angle = (Math.PI * 2.0 * i) / count + entity.tickCount * 0.12;
         double x = target.getX() + Math.cos(angle) * radius;
         double z = target.getZ() + Math.sin(angle) * radius;
         double y = target.getY() + target.getBbHeight() * 0.55 + Math.sin(angle * 2.0) * 0.35;
         LivingEntity fakeAnchor = target;
         ParacelsusSpiritCannonEntity cannon = ParacelsusSpiritCannonEntity.summonAroundTarget(level, entity, fakeAnchor, Math.max(40, (int)(ELEMENTAL_SPIRIT_DURATION * lifeScale)));
         cannon.setPos(x, y, z);
         level.addFreshEntity(cannon);
      }
   }

   private static void breakElementalTerrain(ServerLevel level, ParacelsusEntity entity, Vec3 center, double radius, int yScale, int maxBroken, float softMax, float hardMax) {
      BlockPos origin = BlockPos.containing(center);
      int broken = 0;
      int r = (int)Math.ceil(radius);
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -yScale, -r), origin.offset(r, yScale, r))) {
         if (broken >= maxBroken) {
            break;
         }
         double dx = pos.getX() + 0.5 - center.x;
         double dy = (pos.getY() + 0.5 - center.y) / Math.max(1.0, yScale);
         double dz = pos.getZ() + 0.5 - center.z;
         if (dx * dx + dy * dy + dz * dz > radius * radius) {
            continue;
         }
         var state = level.getBlockState(pos);
         float hardness = state.getDestroySpeed(level, pos);
         float cap = hardness < 0.0F ? 0.0F : (hardness < 8.0F ? softMax : hardMax);
         if (!state.isAir() && hardness >= 0.0F && hardness < cap && level.removeBlock(pos, false)) {
            broken++;
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.15, 0.15, 0.15, 0.02);
         }
      }
   }

   private static void applyElementalMagicHit(ParacelsusEntity entity, LivingEntity living, float damage, int fireTicks, boolean slow, boolean knockUp, boolean knockBack, ServerLevel level) {
      if (living == null || !living.isAlive()) {
         return;
      }
      living.invulnerableTime = 0;
      living.hurt(entity.damageSources().magic(), paracelsusSkillDamage(damage));
      if (fireTicks > 0) {
         living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), fireTicks));
      }
      if (slow) {
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, true, true));
      }
      if (knockUp) {
         living.push(0.0, 0.3, 0.0);
      }
      if (knockBack) {
         Vec3 dir = living.position().subtract(entity.position());
         Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
         if (horizontal.lengthSqr() > 1.0E-4) {
            horizontal = horizontal.normalize();
            living.push(horizontal.x * 0.7, 0.2, horizontal.z * 0.7);
         }
      }
      Vec3 impact = living.position().add(0.0, living.getBbHeight() * 0.45, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, impact.x, impact.y, impact.z, 14, 0.25, 0.25, 0.25, 0.03);
   }

   private static void scheduleMagicBurn(ParacelsusEntity entity, LivingEntity target, ServerLevel level, float damage, int seconds) {
      for (int tick = 20; tick <= seconds * 20; tick += 20) {
         net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork(tick, () -> {
            if (target.isAlive() && target.level() == level && entity.isAlive() && target.getRemainingFireTicks() > 0) {
               target.invulnerableTime = 0;
               target.hurt(entity.damageSources().magic(), paracelsusSkillDamage(damage));
            }
         });
      }
   }

   private static boolean isHostileElementTarget(ParacelsusEntity entity, LivingEntity living) {
      if (living == null || living == entity || entity.isAlliedTo(living) || living.isAlliedTo(entity) || EntityUtils.isImmunePlayerTarget(living)) {
         return false;
      }
      if (living instanceof Player player) {
         return !EntityUtils.isImmunePlayerTarget(player);
      }
      if (living instanceof NeutralMob neutral) {
         return neutral.isAngry();
      }
      return living instanceof Monster || (living instanceof Mob mob && mob.getTarget() == entity);
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
      if (isNoblePhantasmChanting(entity, now)
         || !combatActionReady(entity, now)
         || now - entity.getPersistentData().getLong(TAG_LAST_PHILOSOPHER_STONE) < PHILOSOPHER_STONE_COOLDOWN
         || entity.getCurrentMp() < 20.0) {
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
      markCombatAction(entity, now, 20L);
      return ServantExecutionResult.SUCCESS.withMpCost(20.0);
   }

   private static ServantExecutionResult castElementalSword(ServantNoblePhantasmContext context) {
      if (!(context.caster() instanceof ParacelsusEntity entity)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      LivingEntity target = context.target();
      if (target == null || !target.isAlive() || !isHostileNoblePhantasmTarget(entity, target)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      long now = entity.level().getGameTime();
      int phase = entity.computeCombatPhase();
      if (phase < 3) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      long lastNp = entity.getPersistentData().getLong(TAG_LAST_NP);
      long chantEnd = entity.getPersistentData().getLong(TAG_NP_CHANT_END);
      int previousCooldown = entity.getPersistentData().getBoolean(TAG_LAST_NP_OVERDRAFT) ? NP_COOLDOWN * 2 : NP_COOLDOWN;
      if (chantEnd > now || now - lastNp < previousCooldown
         || !combatActionReady(entity, now) || context.powerScale() <= 0.0) {
         return ServantExecutionResult.FAILED;
      }
      entity.getPersistentData().putLong(TAG_LAST_NP, now);
      entity.getPersistentData().putBoolean(TAG_LAST_NP_OVERDRAFT, context.overdraft());
      entity.getPersistentData().putLong(TAG_NP_CHANT_END, now + NP_CHANT_TICKS);
      entity.getPersistentData().putInt(TAG_NP_PENDING_TARGET, target.getId());
      entity.getPersistentData().putInt(TAG_NP_PENDING_OVERCHARGE, context.overChargeLevel());
      entity.getPersistentData().putDouble(TAG_NP_RESERVED_MP,
         context.noblePhantasmDefinition().mpCost() * context.powerScale());
      Vec3 forward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      entity.getPersistentData().putDouble(TAG_NP_FORWARD_X, forward.x);
      entity.getPersistentData().putDouble(TAG_NP_FORWARD_Y, forward.y);
      entity.getPersistentData().putDouble(TAG_NP_FORWARD_Z, forward.z);
      entity.faceToward(target.position());
      ServantVoiceHelper.tryPlayParacelsusNp(entity);
      markCombatAction(entity, now, 20L);
      return ServantExecutionResult.SUCCESS.withMpCost(context.noblePhantasmDefinition().mpCost());
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
      if (!isHostileElementTarget(entity, target)) {
         return;
      }
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().magic(), paracelsusSkillDamage((float)(6.0 + entity.getCurrentMp() * 0.02)));
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

   private static float paracelsusSkillDamage(float baseDamage) {
      return ParacelsusBalanceRules.reduceDamage(baseDamage * 0.5F);
   }

}
