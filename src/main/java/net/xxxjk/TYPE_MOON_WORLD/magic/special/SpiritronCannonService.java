package net.xxxjk.TYPE_MOON_WORLD.magic.special;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.SpiritronCannonBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class SpiritronCannonService {
   public static final String MAGIC_ID = "spiritron_cannon";
   public static final double MANA_COST = 500.0;
   public static final int WINDUP_TICKS = SpiritronCannonBeamEntity.WINDUP_TICKS;

   private SpiritronCannonService() {
   }

   public static boolean cast(LivingEntity caster, TypeMoonWorldModVariables.PlayerVariables vars, LivingEntity target, boolean consumeMana) {
      if (caster == null || vars == null || !(caster.level() instanceof ServerLevel level) || !caster.isAlive()) {
         return false;
      }
      if (consumeMana) {
         if (vars.player_mana < MANA_COST) {
            return false;
         }
         vars.player_mana = Math.max(0.0, vars.player_mana - MANA_COST);
      }
      Vec3 direction = aimDirection(caster, target);
      faceCasterToDirection(caster, direction);
      Vec3 start = caster.position().add(0.0, caster.getBbHeight() * 0.66, 0.0).add(direction.scale(1.2));
      SpiritronCannonBeamEntity beam = new SpiritronCannonBeamEntity(level, caster, start, direction);
      level.addFreshEntity(beam);
      MagicProficiencyService.add(vars, MAGIC_ID, 0.25);
      return true;
   }

   private static Vec3 aimDirection(LivingEntity caster, LivingEntity target) {
      if (target != null && target.isAlive() && target.level() == caster.level()) {
         Vec3 start = caster.getEyePosition();
         Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
         Vec3 direction = end.subtract(start);
         if (direction.lengthSqr() > 1.0E-4) {
            return direction.normalize();
         }
      }
      Vec3 look = caster.getLookAngle();
      return look.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
   }

   private static void faceCasterToDirection(LivingEntity caster, Vec3 direction) {
      Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
      if (horizontal.lengthSqr() > 1.0E-4) {
         float yaw = (float)(Mth.atan2(horizontal.z, horizontal.x) * 180.0F / Math.PI) - 90.0F;
         caster.setYRot(yaw);
         caster.yBodyRot = yaw;
         caster.yHeadRot = yaw;
      }
      float pitch = (float)(-(Mth.atan2(direction.y, horizontal.length()) * 180.0F / Math.PI));
      caster.setXRot(Mth.clamp(pitch, -80.0F, 80.0F));
   }
}
