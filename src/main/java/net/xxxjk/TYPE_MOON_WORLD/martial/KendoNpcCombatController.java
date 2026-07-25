package net.xxxjk.TYPE_MOON_WORLD.martial;

import java.util.EnumSet;
import java.util.function.DoubleSupplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class KendoNpcCombatController {
   private KendoNpcCombatController() {}

   public static Goal combatGoal(PathfinderMob npc, KendoSchool school, DoubleSupplier proficiency) {
      return new Goal() {
         { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
         @Override public boolean canUse() { return valid(npc, npc.getTarget()); }
         @Override public boolean canContinueToUse() { return valid(npc, npc.getTarget()); }
         @Override public void tick() {
            LivingEntity target = npc.getTarget();
            if (target == null) return;
            npc.getLookControl().setLookAt(target, 70.0F, 55.0F);
            double distance = Math.sqrt(npc.distanceToSqr(target));
            long now = npc.level().getGameTime();
            if (distance > 2.8) npc.getNavigation().moveTo(target, proficiency.getAsDouble() >= 80.0 ? 1.35 : 1.15);
            if (distance <= 3.5 && now >= npc.getPersistentData().getLong("TypeMoonKendoNpcNextAttack")) {
               npc.getNavigation().stop();
               npc.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
               float damage = (float)Math.max(2.0, npc.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
               if (proficiency.getAsDouble() >= 80.0) damage *= 1.5F;
               target.hurt(npc.damageSources().mobAttack(npc), damage);
               target.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModMobEffects.STAGGER, 8, 0, false, true, true));
               npc.getPersistentData().putLong("TypeMoonKendoNpcNextAttack", now + (proficiency.getAsDouble() >= 80.0 ? 10L : 16L));
            }
         }
         @Override public void stop() { npc.getNavigation().stop(); }
      };
   }

   private static boolean valid(PathfinderMob npc, LivingEntity target) {
      if (target == null || !target.isAlive() || target == npc || npc.isAlliedTo(target) || EntityUtils.isImmunePlayerTarget(target)) return false;
      return !(target instanceof Player player) || !player.isCreative() && !player.isSpectator();
   }
}
