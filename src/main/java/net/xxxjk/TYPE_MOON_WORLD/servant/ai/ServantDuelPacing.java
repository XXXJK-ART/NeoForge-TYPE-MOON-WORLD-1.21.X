package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockGunEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

/** A shared decision gate in front of both tactical and legacy NPC combat. */
public final class ServantDuelPacing {
   private static final String PREFIX = "TypeMoonDuel";
   private ServantDuelPacing() { }
   public static boolean duel(ServantEntity servant) { return servant.getTarget() instanceof ServantEntity; }
   public static boolean skillDecisionAllowed(ServantEntity servant) {
      if (!duel(servant)) return true;
      var data = servant.getPersistentData();
      return !data.getBoolean(PREFIX + "Basic") && data.getLong(PREFIX + "NextDecision") == servant.level().getGameTime() + 20;
   }
   public static boolean probing(ServantEntity servant, long now) {
      return now - servant.getPersistentData().getLong(PREFIX + "Start") < 300;
   }
   public static boolean useBasicDecision(long decision, boolean probing) {
      return Math.floorMod(decision, 20) < (probing ? 16 : 13);
   }
   public static boolean tick(ServantEntity servant) {
      if (!(servant.level() instanceof ServerLevel level) || !(servant.getTarget() instanceof ServantEntity target)
         || !target.isAlive()) return false;
      CompoundTag data = servant.getPersistentData();
      long now = level.getGameTime();
      if (!data.hasUUID(PREFIX + "Target") || !data.getUUID(PREFIX + "Target").equals(target.getUUID())) {
         data.putUUID(PREFIX + "Target", target.getUUID()); data.putLong(PREFIX + "Start", now);
         data.putLong(PREFIX + "Decision", 0);
      }
      if (NoblePhantasmCastService.chanting(servant)) return true;
      if (nativeNpActive(data)) return false;
      boolean chooseBasic;
      if (now >= data.getLong(PREFIX + "NextDecision")) {
         long decision = data.getLong(PREFIX + "Decision");
         chooseBasic = useBasicDecision(decision, probing(servant, now));
         if (!chooseBasic && now < data.getLong(PREFIX + "NextSkill")) chooseBasic = true;
         data.putLong(PREFIX + "Decision", decision + 1);
         data.putLong(PREFIX + "NextDecision", now + 20);
         data.putBoolean(PREFIX + "Basic", chooseBasic);
         if (!chooseBasic) {
            data.putLong(PREFIX + "NextSkill", now + 90); // 3s base x 1.5
            data.putInt(PREFIX + "SkillDecisions", data.getInt(PREFIX + "SkillDecisions") + 1);
            return false;
         }
      } else if (!data.getBoolean(PREFIX + "Basic")) {
         // Only one tick may enter a character's offensive skill selector.
         chooseBasic = true;
      } else chooseBasic = true;
      long next = data.getLong(PREFIX + "NextBasic");
      if (next > 0 && next - now <= 9 && next > now) {
         net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService.setGuarding(target, true);
      }
      basic(servant, target, now);
      return true;
   }

   private static boolean nativeNpActive(CompoundTag data) {
      for (String key : new String[]{"ArtoriaExcaliburWindupUntil", "ArtoriaExcaliburReleaseUntil", "GawainGallatinWindupUntil",
         "CuGaeBolgWindupUntil", "CursedArmZabaniyaWindupUntil", "LiShuwenWuErDaRelease", "MedusaBellerophonLaunchTick",
         "OdaThreeThousandChantEnd", "OdaHajunChantEnd", "NightingaleNpCastEnd", "ZhaoYunNpChantUntil"}) {
         if (data.getLong(key) > 0) return true;
      }
      return false;
   }
   private static void basic(ServantEntity servant, LivingEntity target, long now) {
      servant.faceToward(target.position());
      servant.getLookControl().setLookAt(target, 60, 60);
      boolean ranged = ServantEngagementService.role(servant) == ServantEngagementService.CombatRole.RANGED;
      double distance = servant.distanceTo(target);
      if (distance > (ranged ? 15 : 3)) servant.getNavigation().moveTo(target, 1.1);
      else servant.getNavigation().stop();
      CompoundTag data = servant.getPersistentData();
      if (now < data.getLong(PREFIX + "NextBasic") || !servant.getSensing().hasLineOfSight(target)) return;
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
         attacked = ServantCombatTempoService.tryBasicAttack(servant, target, now);
      }
      if (attacked) {
         data.putLong(PREFIX + "NextBasic", now + 45);
         data.putInt(PREFIX + "BasicAttacks", data.getInt(PREFIX + "BasicAttacks") + 1);
      }
   }
}
