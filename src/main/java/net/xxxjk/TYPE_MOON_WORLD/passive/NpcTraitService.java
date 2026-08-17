package net.xxxjk.TYPE_MOON_WORLD.passive;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ProjectileThreatClassifier;
import net.xxxjk.TYPE_MOON_WORLD.entity.BajiquanApprenticeEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.BajiquanMasterEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.KendoApprenticeEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.KendoMasterEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysteriousSwordsmanEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TohsakaRinEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.church.ChurchExecutorEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.DeadApostleEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.LivingDeadEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosBeastLogic;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NightKinEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.MysticMagicianRank;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.lancelot.LancelotCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactBypass;
import net.xxxjk.TYPE_MOON_WORLD.talent.TalentService;

/** Rare one-time passive/talent seeding and runtime support for non-servant Type-Moon NPCs. */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class NpcTraitService {
   private static final String TAG_INIT = "TypeMoonNpcTraitsInitV1";
   private static final String TAG_KIND = "TypeMoonNpcTraitsKind";
   private static final String TAG_NEXT_MONSTROUS_STRENGTH = "TypeMoonNpcTalentMonstrousStrengthNext";
   private static final String TAG_CLAIRVOYANCE_NIGHT_VISION = "TypeMoonNpcPassiveClairvoyanceNightVision";

   private static final String[] MAGIC_PASSIVES = new String[]{
      PassiveService.CLAIRVOYANCE,
      PassiveService.HIGH_SPEED_INCANTATION,
      PassiveService.HIGH_SPEED_DIVINE_WORDS,
      PassiveService.PARTITIONED_THOUGHT,
      PassiveService.MIND_EYE_FALSE,
      PassiveService.INSTINCT
   };
   private static final String[] MARTIAL_PASSIVES = new String[]{
      PassiveService.MIND_EYE_TRUE,
      PassiveService.MIND_EYE_FALSE,
      PassiveService.INSTINCT,
      PassiveService.CLAIRVOYANCE
   };
   private static final String[] CHURCH_PASSIVES = new String[]{
      PassiveService.MIND_EYE_FALSE,
      PassiveService.INSTINCT,
      PassiveService.CLAIRVOYANCE,
      PassiveService.HIGH_SPEED_INCANTATION,
      PassiveService.HIGH_SPEED_DIVINE_WORDS
   };
   private static final String[] DEAD_APOSTLE_PASSIVES = new String[]{
      PassiveService.INSTINCT,
      PassiveService.MIND_EYE_FALSE,
      PassiveService.CLAIRVOYANCE,
      PassiveService.MIND_EYE_TRUE
   };
   private static final String[] TALENTS = new String[]{
      TalentService.MONSTROUS_STRENGTH,
      TalentService.CLAIRVOYANCE
   };

   private NpcTraitService() {
   }

   @SubscribeEvent
   public static void onEntityJoin(EntityJoinLevelEvent event) {
      if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
         ensureInitialized(living);
      }
   }

   @SubscribeEvent
   public static void onEntityTick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) return;
      if (!isEligible(living)) return;
      ensureInitialized(living);
      tickActiveTraits(living);
   }

   @SubscribeEvent
   public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
      if (event.isCanceled() || event.getEntity().level().isClientSide()) return;
      if (!(event.getEntity() instanceof LivingEntity living) || !isEligible(living)) return;
      ensureInitialized(living);
      if (LancelotCombatHelper.rollsEternalArmsDodgeBypass(event.getSource())) return;
      if (tryDodge(living, event.getSource())) {
         event.setCanceled(true);
         event.setAmount(0.0F);
      }
   }

   public static boolean isEligible(LivingEntity entity) {
      return profile(entity) != null;
   }

   public static void ensureInitialized(LivingEntity entity) {
      TraitProfile profile = profile(entity);
      if (entity == null || profile == null || entity.level().isClientSide()) return;
      CompoundTag data = entity.getPersistentData();
      if (data.getBoolean(TAG_INIT)) return;

      TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      RandomSource random = entity.getRandom();
      boolean changed = false;

      if (random.nextDouble() < profile.passiveChance()) {
         changed |= grantRandomPassive(vars, random, profile);
         if (random.nextDouble() < profile.secondPassiveChance()) {
            changed |= grantRandomPassive(vars, random, profile);
         }
      }
      if (random.nextDouble() < profile.talentChance()) {
         changed |= grantRandomTalent(vars, random, profile);
      }

      data.putBoolean(TAG_INIT, true);
      data.putString(TAG_KIND, profile.kind());
      if (changed) {
         TYPE_MOON_WORLD.LOGGER.debug("Seeded rare NPC traits for {} {}: passives={}, talents={}",
            profile.kind(), entity.getUUID(), vars.passive_ranks, vars.talent_proficiencies);
      }
   }

   public static boolean hasAnyTrait(LivingEntity entity) {
      if (entity == null || !isEligible(entity)) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars != null && (!vars.passive_ranks.isEmpty() || !vars.talent_proficiencies.isEmpty());
   }

   private static boolean grantRandomPassive(TypeMoonWorldModVariables.PlayerVariables vars, RandomSource random, TraitProfile profile) {
      if (vars == null || profile.passivePool().length == 0) return false;
      List<String> candidates = new ArrayList<>();
      for (String id : profile.passivePool()) {
         if (PassiveService.isPassive(id)
            && !PassiveService.DIVINITY.equals(id)
            && !PassiveService.GOLDEN_RULE.equals(id)
            && !vars.passive_ranks.containsKey(id)) {
            candidates.add(id);
         }
      }
      if (candidates.isEmpty()) return false;
      String id = candidates.get(random.nextInt(candidates.size()));
      vars.passive_ranks.put(id, rollRank(random, profile.rankBias()));
      return true;
   }

   private static boolean grantRandomTalent(TypeMoonWorldModVariables.PlayerVariables vars, RandomSource random, TraitProfile profile) {
      if (vars == null) return false;
      List<String> candidates = new ArrayList<>();
      for (String id : TALENTS) {
         if (TalentService.isTalent(id) && !vars.talent_proficiencies.containsKey(id)) {
            candidates.add(id);
         }
      }
      if (candidates.isEmpty()) return false;
      String id = candidates.get(random.nextInt(candidates.size()));
      vars.talent_proficiencies.put(id, rollTalentProficiency(random, profile.rankBias()));
      return true;
   }

   private static PassiveRank rollRank(RandomSource random, double bias) {
      double r = random.nextDouble();
      double e = Mth.clamp(0.60 - bias * 0.10, 0.42, 0.68);
      double d = Mth.clamp(e + 0.24 - bias * 0.04, e + 0.18, 0.86);
      double c = Mth.clamp(d + 0.12 - bias * 0.02, d + 0.08, 0.96);
      double b = Mth.clamp(c + 0.035 + bias * 0.01, c + 0.02, 0.992);
      if (r < e) return PassiveRank.E;
      if (r < d) return PassiveRank.D;
      if (r < c) return PassiveRank.C;
      if (r < b) return PassiveRank.B;
      return PassiveRank.A;
   }

   private static double rollTalentProficiency(RandomSource random, double bias) {
      double r = random.nextDouble();
      double value;
      if (r < 0.68) {
         value = 15.0 + random.nextDouble() * 25.0;
      } else if (r < 0.90) {
         value = 40.0 + random.nextDouble() * 20.0;
      } else if (r < 0.985) {
         value = 60.0 + random.nextDouble() * 20.0;
      } else {
         value = 80.0 + random.nextDouble() * 20.0;
      }
      return Mth.clamp(value + bias * random.nextDouble() * 8.0, 1.0, 100.0);
   }

   private static void tickActiveTraits(LivingEntity entity) {
      TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars == null || PassiveService.effectsSuppressed(vars)) return;

      if (entity.tickCount % 80 == Math.floorMod(entity.getId(), 80) && PassiveService.has(vars, PassiveService.CLAIRVOYANCE)) {
         MobEffectInstance current = entity.getEffect(MobEffects.NIGHT_VISION);
         if (current == null || current.getDuration() < 120) {
            entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false, false));
            entity.getPersistentData().putBoolean(TAG_CLAIRVOYANCE_NIGHT_VISION, true);
         }
      }

      if (!(entity instanceof Mob mob) || !TalentService.owns(vars, TalentService.MONSTROUS_STRENGTH)) return;
      if (entity.tickCount % 20 != Math.floorMod(entity.getId(), 20)) return;
      LivingEntity target = mob.getTarget();
      if (target == null || !target.isAlive() || entity.hasEffect(ModMobEffects.MONSTROUS_STRENGTH)) return;

      long now = entity.level().getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (now < data.getLong(TAG_NEXT_MONSTROUS_STRENGTH)) return;
      boolean pressure = entity.getHealth() <= entity.getMaxHealth() * 0.55F || entity.distanceToSqr(target) <= 49.0;
      if (!pressure && randomChance(entity, 0.12)) return;

      double proficiency = TalentService.proficiency(vars, TalentService.MONSTROUS_STRENGTH);
      int amplifier = TalentService.monstrousStrengthLevel(proficiency) - 1;
      int duration = Mth.clamp(TalentService.monstrousStrengthDurationTicks(proficiency), 120, 600);
      entity.addEffect(new MobEffectInstance(ModMobEffects.MONSTROUS_STRENGTH, duration, amplifier, false, true, true));
      data.putLong(TAG_NEXT_MONSTROUS_STRENGTH, now + duration + 600L + entity.getRandom().nextInt(600));
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.WITCH, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
            16, 0.35, 0.45, 0.35, 0.04);
         level.playSound(null, entity.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 0.7F, 0.75F);
      }
   }

   private static boolean randomChance(LivingEntity entity, double chance) {
      return entity.getRandom().nextDouble() >= chance;
   }

   private static boolean tryDodge(LivingEntity entity, DamageSource source) {
      if (entity == null || source == null || source.getEntity() == null
         || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         || source.is(FanaticDamageTypes.GUARANTEED_HITS)
         || ProjectileThreatClassifier.classify(source).contains(FactBypass.SURE_HIT)) {
         return false;
      }
      boolean projectile = source.getDirectEntity() instanceof Projectile;
      boolean melee = source.getDirectEntity() == source.getEntity() && source.getEntity() instanceof LivingEntity;
      if (!projectile && !melee) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double chance = PassiveService.dodgeChance(vars);
      if (chance <= 0.0 || entity.getRandom().nextDouble() >= chance) return false;
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(),
            10, 0.3, 0.45, 0.3, 0.03);
         level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.7F, 1.35F);
      }
      return true;
   }

   private static TraitProfile profile(LivingEntity entity) {
      if (entity == null || entity instanceof Player || entity instanceof TohsakaRinEntity || entity instanceof NeroChaosEntity
         || NeroChaosBeastLogic.isBeast(entity)) {
         return null;
      }
      if (entity instanceof MysticMagicianEntity mage) {
         return magicianProfile(mage.getMagicianRank());
      }
      if (entity instanceof NightKinEntity) {
         return new TraitProfile("dead_apostle_stage_4", 0.018, 0.055, 0.0040, 0.45, DEAD_APOSTLE_PASSIVES);
      }
      if (entity instanceof LivingDeadEntity) {
         return new TraitProfile("dead_apostle_stage_3", 0.010, 0.025, 0.0020, 0.20, DEAD_APOSTLE_PASSIVES);
      }
      if (entity instanceof DeadApostleEntity) {
         return null;
      }
      if (entity instanceof ChurchExecutorEntity) {
         return new TraitProfile("church_executor", 0.009, 0.018, 0.0018, 0.20, CHURCH_PASSIVES);
      }
      if (entity instanceof MysteriousSwordsmanEntity || entity instanceof BajiquanMasterEntity || entity instanceof KendoMasterEntity) {
         return new TraitProfile("martial_master", 0.018, 0.045, 0.0035, 0.38, MARTIAL_PASSIVES);
      }
      if (entity instanceof BajiquanApprenticeEntity || entity instanceof KendoApprenticeEntity) {
         return new TraitProfile("martial_npc", 0.007, 0.012, 0.0012, 0.10, MARTIAL_PASSIVES);
      }
      if (entity instanceof PathfinderMob && entity instanceof Monster) {
         return null;
      }
      return null;
   }

   private static TraitProfile magicianProfile(MysticMagicianRank rank) {
      return switch (rank == null ? MysticMagicianRank.ADEPT : rank) {
         case GRAND -> new TraitProfile("mystic_magician_grand", 0.045, 0.110, 0.0090, 0.65, MAGIC_PASSIVES);
         case BRAND -> new TraitProfile("mystic_magician_brand", 0.026, 0.070, 0.0050, 0.48, MAGIC_PASSIVES);
         case PRIDE, FES -> new TraitProfile("mystic_magician_pride_fes", 0.018, 0.045, 0.0035, 0.34, MAGIC_PASSIVES);
         case ADEPT -> new TraitProfile("mystic_magician_adept", 0.012, 0.025, 0.0022, 0.20, MAGIC_PASSIVES);
         case UMNOS -> new TraitProfile("mystic_magician_umnos", 0.009, 0.018, 0.0015, 0.12, MAGIC_PASSIVES);
         case FRAME -> new TraitProfile("mystic_magician_frame", 0.006, 0.010, 0.0010, 0.05, MAGIC_PASSIVES);
      };
   }

   private record TraitProfile(
      String kind,
      double passiveChance,
      double secondPassiveChance,
      double talentChance,
      double rankBias,
      String[] passivePool
   ) {
   }
}
