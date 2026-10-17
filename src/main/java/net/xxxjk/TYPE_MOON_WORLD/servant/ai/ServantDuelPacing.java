package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockGunEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatExchangeService;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiBrain;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiIntent;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiControl;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiMigrationPolicy;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

/** A shared decision gate in front of both tactical and legacy NPC combat. */
public final class ServantDuelPacing {
   private static final String PREFIX = "TypeMoonDuel";
   /** Keep decisions chunky enough to read as intent while attacks stay rapid. */
   public static final int DECISION_INTERVAL_TICKS = 12;
   /** A basic strike is a beat in a combo, not a one-second cooldown. */
   public static final int BASIC_RECOVERY_TICKS = 16;
   private static final int COMBO_RECOVERY_TICKS = 6;
   private static final int RANGED_BASIC_RECOVERY_TICKS = 16;
   private static final int SKILL_RECOVERY_TICKS = 60;
   private static final String[] NP_TIMERS = {"ArtoriaExcaliburWindupUntil", "ArtoriaExcaliburReleaseUntil", "GawainGallatinWindupUntil",
      "CuGaeBolgWindupUntil", "CursedArmZabaniyaWindupUntil", "LiShuwenWuErDaRelease", "MedusaBellerophonLaunchTick",
      "OdaThreeThousandChantEnd", "OdaHajunChantEnd", "NightingaleNpCastEnd", "ZhaoYunNpChantUntil"};
   private ServantDuelPacing() { }
   public static boolean duel(ServantEntity servant) { return servant.getTarget() instanceof ServantEntity; }
   public static boolean skillDecisionAllowed(ServantEntity servant) {
      if (!duel(servant)) return true;
      if (ServantCombatExchangeService.scripted(servant, servant.getTarget())) return true;
      var data = servant.getPersistentData();
      return !data.getBoolean(PREFIX + "Basic")
         && data.getLong(PREFIX + "NextDecision") == servant.level().getGameTime() + DECISION_INTERVAL_TICKS;
   }
   public static boolean probing(ServantEntity servant, long now) {
      return now - servant.getPersistentData().getLong(PREFIX + "Start") < 300;
   }
   public static boolean useBasicDecision(long decision, boolean probing) {
      // Skills remain occasional set pieces, while the neutral loop is still
      // dominated by readable close-range exchanges.
      return Math.floorMod(decision, 16) < (probing ? 10 : 9);
   }
   public static boolean tick(ServantEntity servant) {
      if (!(servant.level() instanceof ServerLevel level) || !(servant.getTarget() instanceof ServantEntity target)
         || !target.isAlive()) return false;
      CompoundTag data = servant.getPersistentData();
      long now = level.getGameTime();
      if (!data.hasUUID(PREFIX + "Target") || !data.getUUID(PREFIX + "Target").equals(target.getUUID())) {
         data.putUUID(PREFIX + "Target", target.getUUID()); data.putLong(PREFIX + "Start", now);
         data.putLong(PREFIX + "Decision", 0);
         data.remove(PREFIX + "NextDecision"); data.remove(PREFIX + "NextBasic"); data.remove(PREFIX + "NextSkill");
      }
      if (ServantCombatExchangeService.scripted(servant, target)) return false;
      if (NoblePhantasmCastService.chanting(servant)) return true;
      if (nativeNpActive(servant) || ServantCombatExchangeService.protectedAction(servant)) return false;
      if (now >= data.getLong(PREFIX + "NextDecision")) {
         long decision = data.getLong(PREFIX + "Decision");
         boolean chooseBasic = useBasicDecision(decision, probing(servant, now));
         if (!chooseBasic && now < data.getLong(PREFIX + "NextSkill")) chooseBasic = true;
         data.putLong(PREFIX + "Decision", decision + 1);
         data.putLong(PREFIX + "NextDecision", now + DECISION_INTERVAL_TICKS);
         data.putBoolean(PREFIX + "Basic", chooseBasic);
         if (!chooseBasic) {
            data.putLong(PREFIX + "NextSkill", now + SKILL_RECOVERY_TICKS);
            data.putInt(PREFIX + "SkillDecisions", data.getInt(PREFIX + "SkillDecisions") + 1);
            return false;
         }
      }
      return !AiMigrationPolicy.isArbitrated(servant) && basic(servant, target, now);
   }

   public static void submitBasic(ServantEntity servant, AiBrain brain, long now) {
      if (!ServantCombatExchangeService.ordinary(servant, servant.getTarget())
         || ServantCombatExchangeService.protectedAction(servant) || !basicReady(servant, now)
         || skillDecisionAllowed(servant)) return;
      brain.submit(AiIntent.attempt(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("typemoonworld", "ai/duel_basic"),
         AiIntent.PRIORITY_ATTACK + 4, 120, 1, true, () -> basic(servant, servant.getTarget(), now),
         AiControl.ATTACK, AiControl.LOOK));
   }

   public static boolean nativeNpActive(ServantEntity servant) {
      CompoundTag data = servant.getPersistentData();
      long now = servant.level().getGameTime();
      for (String key : NP_TIMERS) {
         if (data.getLong(key) > now) return true;
      }
      return false;
   }
   public static boolean basicReady(ServantEntity servant, long now) {
      return now >= servant.getPersistentData().getLong(PREFIX + "NextBasic");
   }

   public static void noteBasic(ServantEntity servant, long now) {
      CompoundTag data = servant.getPersistentData();
      int strikes = data.getInt(PREFIX + "BasicAttacks") + 1;
      data.putLong(PREFIX + "NextBasic", now + (strikes % 3 == 0 ? BASIC_RECOVERY_TICKS : COMBO_RECOVERY_TICKS));
      data.putInt(PREFIX + "BasicAttacks", strikes);
      ServantCombatExchangeService.activity(servant, now);
   }

   public static void noteBasicMiss(ServantEntity servant, long now) {
      servant.getPersistentData().putLong(PREFIX + "NextBasic", now + 4);
   }

   private static boolean basic(ServantEntity servant, LivingEntity target, long now) {
      servant.faceToward(target.position());
      servant.getLookControl().setLookAt(target, 60, 60);
      boolean ranged = ServantEngagementService.role(servant) == ServantEngagementService.CombatRole.RANGED;
      double distance = servant.distanceTo(target);
      CompoundTag data = servant.getPersistentData();
      if (!basicReady(servant, now) || !servant.getSensing().hasLineOfSight(target)) return false;
      boolean attacked = false;
      if (ranged && distance <= 40) {
         Vec3 origin = servant.getEyePosition();
         Vec3 direction = target.getBoundingBox().getCenter().subtract(origin).normalize();
         String id = servant.getServantId();
         if (id.equals("gilgamesh") || id.equals("gilgamesh_caster")) {
            servant.level().addFreshEntity(new GilgameshGateWeaponProjectileEntity(servant.level(), servant, origin, direction, "durandal", 12));
         } else if (id.equals("oda_nobunaga")) {
            servant.level().addFreshEntity(OdaMatchlockGunEntity.oneShotTracking(servant.level(), servant, target, origin, 4));
         } else if (id.equals("arash") || id.equals("emiya_archer")) {
            var arrow = new net.xxxjk.TYPE_MOON_WORLD.entity.ArashParticleArrowEntity(servant.level(), servant,
               net.xxxjk.TYPE_MOON_WORLD.entity.ArashParticleArrowEntity.NORMAL, 12);
            arrow.setPos(origin); arrow.setDeltaMovement(direction.scale(3)); servant.level().addFreshEntity(arrow);
         } else if (id.equals("nightingale")) {
            var bullet = new net.xxxjk.TYPE_MOON_WORLD.entity.NightingaleBulletEntity(servant.level(), servant);
            bullet.setPos(origin); bullet.shoot(direction.x, direction.y, direction.z, 3, 0); servant.level().addFreshEntity(bullet);
         } else {
            MedeaMagicBoltEntity bolt = new MedeaMagicBoltEntity(servant.level(), servant);
            bolt.setMagicDamage(12); bolt.setPos(origin); bolt.shoot(direction.x, direction.y, direction.z, 1.8F, 0);
            servant.level().addFreshEntity(bolt);
         }
         servant.triggerBasicAttackAnimation(); attacked = true;
      } else if (!ranged && distance <= ServantCombatTempoService.basicAttackReach(servant, target)) {
         return ServantCombatExchangeService.beginBasic(servant, target, now);
      }
      if (attacked) {
         data.putLong(PREFIX + "NextBasic", now + RANGED_BASIC_RECOVERY_TICKS);
         data.putInt(PREFIX + "BasicAttacks", data.getInt(PREFIX + "BasicAttacks") + 1);
         ServantCombatExchangeService.activity(servant, now);
      }
      return attacked;
   }
}
