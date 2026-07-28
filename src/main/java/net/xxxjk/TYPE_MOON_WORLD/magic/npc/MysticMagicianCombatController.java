package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ThompsonContenderItem;
import net.xxxjk.TYPE_MOON_WORLD.network.FirearmPoseMessage;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantEngagementService;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

/** Combat goal for randomly generated martial and Thompson loadouts on magicians. */
public final class MysticMagicianCombatController {
   private static final String TAG_NEXT_MELEE = "TypeMoonMagicianNpcNextMelee";
   private static final String TAG_NEXT_SHOT = "TypeMoonMagicianNpcNextShot";

   private MysticMagicianCombatController() {}

   public static Goal combatGoal(MysticMagicianEntity npc) {
      return new Goal() {
         {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
         }

         @Override public boolean canUse() {
            return npc.hasPhysicalLoadout() && validTarget(npc, npc.getTarget());
         }

         @Override public boolean canContinueToUse() {
            return npc.hasPhysicalLoadout() && validTarget(npc, npc.getTarget());
         }

         @Override public void tick() {
            MysticMagicianCombatController.tick(npc);
         }

         @Override public void stop() {
            npc.getNavigation().stop();
         }
      };
   }

   private static void tick(MysticMagicianEntity npc) {
      LivingEntity target = npc.getTarget();
      if (!validTarget(npc, target)) return;

      double distance = Math.sqrt(npc.distanceToSqr(target));
      boolean hasCloseCombat = npc.hasSwordSchool() || npc.hasBajiquan();
      boolean useGun = npc.hasThompson() && (!hasCloseCombat || distance > 6.0 || !npc.hasLineOfSight(target));
      npc.setRangedWeaponMode(useGun);
      npc.ensurePhysicalEquipment();
      npc.getLookControl().setLookAt(target, 70.0F, 55.0F);

      if (useGun) {
         tickGun(npc, target, distance);
      } else {
         tickMelee(npc, target, distance);
      }
   }

   private static void tickGun(MysticMagicianEntity npc, LivingEntity target, double distance) {
      long now = npc.level().getGameTime();
      ServantEngagementService.RangeBand band = ServantEngagementService.rangedBand(target, 7.0, 11.0, 14.0);
      boolean rangedDuel = ServantEngagementService.role(target) == ServantEngagementService.CombatRole.RANGED;
      if (distance < band.minimum() || distance > band.maximum()) {
         Vec3 destination = ServantEngagementService.rangedDestination(npc, target, now, band);
         npc.getNavigation().moveTo(destination.x, destination.y, destination.z,
            distance < band.minimum() ? 1.18 : 1.08);
      } else {
         npc.getNavigation().stop();
         float forward = rangedDuel ? 0.08F : distance < band.preferred() ? -0.28F : 0.12F;
         float direction = ((npc.getId() + (int)(now / 60L)) & 1) == 0 ? 1.0F : -1.0F;
         npc.getMoveControl().strafe(forward, direction * (rangedDuel ? 0.82F : 0.62F));
      }

      if (distance <= 18.0 && npc.hasLineOfSight(target) && now >= npc.getPersistentData().getLong(TAG_NEXT_SHOT)) {
         fireNormalRound(npc, target);
         npc.getPersistentData().putLong(TAG_NEXT_SHOT, now + 14L);
      }
   }

   private static void tickMelee(MysticMagicianEntity npc, LivingEntity target, double distance) {
      double reach = 3.1 + (npc.getBbWidth() + target.getBbWidth()) * 0.4;
      if (distance > reach) {
         double speed = npc.getMartialProficiency() >= 80.0 ? 1.35 : 1.15;
         if (ServantEngagementService.role(target) == ServantEngagementService.CombatRole.RANGED && distance > 7.0) {
            Vec3 intercept = ServantEngagementService.meleeApproachPoint(npc, target, npc.level().getGameTime());
            npc.getNavigation().moveTo(intercept.x, intercept.y, intercept.z, speed * 1.1);
         } else {
            npc.getNavigation().moveTo(target, speed);
         }
         if (distance > 4.5 && npc.tickCount % 12 == 0) {
            Vec3 direction = target.position().subtract(npc.position()).multiply(1.0, 0.0, 1.0);
            if (direction.lengthSqr() > 0.01) npc.setDeltaMovement(npc.getDeltaMovement().add(direction.normalize().scale(0.08)));
         }
         return;
      }

      long now = npc.level().getGameTime();
      if (now < npc.getPersistentData().getLong(TAG_NEXT_MELEE)) return;
      npc.getNavigation().stop();
      if (npc.tickCount % 3 == 0) {
         Vec3 side = new Vec3(-(target.getZ() - npc.getZ()), 0.0, target.getX() - npc.getX());
         if (side.lengthSqr() > 0.01) npc.setDeltaMovement(npc.getDeltaMovement().add(side.normalize().scale(0.1)));
      }

      boolean useSword = npc.hasSwordSchool() && (!npc.hasBajiquan() || distance > 2.2 || npc.getRandom().nextBoolean());
      if (useSword) performSwordStrike(npc, target);
      else performBajiquanStrike(npc, target);
      npc.getPersistentData().putLong(TAG_NEXT_MELEE, now + (npc.getMartialProficiency() >= 80.0 ? 9L : 14L));
   }

   private static void performSwordStrike(MysticMagicianEntity npc, LivingEntity target) {
      npc.swing(InteractionHand.MAIN_HAND, true);
      target.invulnerableTime = 0;
      boolean hit = target.hurt(npc.damageSources().mobAttack(npc), (float)Math.max(3.0, npc.getAttributeValue(Attributes.ATTACK_DAMAGE) + npc.getMartialProficiency() * 0.04));
      target.invulnerableTime = 0;
      if (hit) target.addEffect(new MobEffectInstance(ModMobEffects.STAGGER, 8, 0, false, true, true));
   }

   private static void performBajiquanStrike(MysticMagicianEntity npc, LivingEntity target) {
      npc.swing(InteractionHand.MAIN_HAND, true);
      target.invulnerableTime = 0;
      float damage = (float)Math.max(3.0, npc.getAttributeValue(Attributes.ATTACK_DAMAGE) + npc.getMartialProficiency() * 0.035);
      if (npc.getRandom().nextInt(100) < 22 && npc.getMartialProficiency() >= 80.0) {
         damage *= 1.35F;
         target.addEffect(new MobEffectInstance(ModMobEffects.OFF_BALANCE, 20, 0, false, true, true));
      } else {
         target.addEffect(new MobEffectInstance(ModMobEffects.STAGGER, 6, 0, false, true, true));
      }
      boolean hit = target.hurt(npc.damageSources().mobAttack(npc), damage);
      target.invulnerableTime = 0;
      if (hit) {
         Vec3 direction = target.position().subtract(npc.position()).multiply(1.0, 0.0, 1.0);
         if (direction.lengthSqr() > 0.01) target.push(direction.normalize().x * 0.35, 0.1, direction.normalize().z * 0.35);
      }
   }

   private static void fireNormalRound(MysticMagicianEntity npc, LivingEntity target) {
      if (!(npc.level() instanceof ServerLevel level)) return;
      Vec3 look = target.getEyePosition().subtract(npc.getEyePosition()).normalize();
      Vec3 spawn = npc.getEyePosition().add(look.scale(0.65));
      OdaMatchlockBulletEntity bullet = new OdaMatchlockBulletEntity(level, npc, null, 20.0F).setBulletKind(3);
      bullet.getPersistentData().putBoolean(ThompsonContenderItem.CONTENDER_ARROW_TAG, true);
      bullet.getPersistentData().putBoolean(ThompsonContenderItem.ORIGIN_ARROW_TAG, false);
      bullet.setPos(spawn.x, spawn.y - 0.05, spawn.z);
      bullet.shoot(look.x, look.y, look.z, 4.8F, 0.0F);
      level.addFreshEntity(bullet);
      level.sendParticles(ParticleTypes.FLASH, spawn.x, spawn.y, spawn.z, 1, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.CRIT, spawn.x, spawn.y, spawn.z, 8, 0.08, 0.08, 0.08, 0.03);
      level.playSound(null, npc.getX(), npc.getY(), npc.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 0.95F, 1.65F);
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(npc, new FirearmPoseMessage(npc.getUUID(), 12), new net.minecraft.network.protocol.common.custom.CustomPacketPayload[0]);
   }

   private static boolean validTarget(MysticMagicianEntity npc, LivingEntity target) {
      if (target == null || !target.isAlive() || target == npc || npc.isAlliedTo(target) || EntityUtils.isImmunePlayerTarget(target)) return false;
      return !(target instanceof Player player) || !player.isCreative() && !player.isSpectator();
   }
}
