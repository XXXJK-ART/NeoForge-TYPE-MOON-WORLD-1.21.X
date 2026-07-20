package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class ServantCardSasakiKojiroSkills {
   private ServantCardSasakiKojiroSkills() {
   }

   public static void tick(ServerPlayer player) {
      if (!player.hasEffect(MobEffects.INVISIBILITY)) {
         return;
      }
      for (Mob mob : player.level().getEntitiesOfClass(
         Mob.class,
         player.getBoundingBox().inflate(18.0),
         mob -> mob.getTarget() == player && !(mob instanceof ServantEntity)
      )) {
         mob.setTarget(null);
         mob.getNavigation().stop();
      }
   }

   public static boolean performTsubameGaeshi(ServerPlayer player) {
      return PlayerNoblePhantasmHelper.useTsubameGaeshi(player);
   }

   public static void performAfterimage(ServerPlayer player) {
      Vec3 side = new Vec3(-player.getLookAngle().z, 0.0, player.getLookAngle().x).normalize().scale(player.getRandom().nextBoolean() ? 4.0 : -4.0);
      if (ServantCardSkillUtils.trySafeHorizontalTeleport(player, player.position().add(side).add(0.0, 0.1, 0.0))) {
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 2, false, true, true));
      }
   }

   public static void performMindEye(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, true, true));
   }

   public static void performSweep(ServerPlayer player) {
      ServantCardSkillUtils.hitForwardArc(player, PlayerNoblePhantasmHelper.horizontalLook(player), 6.5, 24.0F);
   }

   public static void performTransparency(ServerPlayer player) {
      ServantCardSkillUtils.clearHarmfulEffects(player);
      player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false, false));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 1, false, true, true));
   }
}
