package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber(modid = "typemoonworld")
final class ServantCardConcealmentHelper {
   private static final int AGGRO_CLEAR_INTERVAL = 5;
   private static final double AGGRO_CLEAR_RADIUS = 48.0;

   private ServantCardConcealmentHelper() {}

   static void apply(ServerPlayer player, int duration) {
      player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false, false));
      clearEnemyAggro(player);
   }

   static void tick(ServerPlayer player) {
      if (!player.hasEffect(MobEffects.INVISIBILITY) || player.tickCount % AGGRO_CLEAR_INTERVAL != 0) return;
      clearEnemyAggro(player);
   }

   @SubscribeEvent
   public static void hideEffectParticles(EffectParticleModificationEvent event) {
      if (!(event.getEntity() instanceof Player player) || !player.hasEffect(MobEffects.INVISIBILITY)) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed) {
         event.setVisible(false);
      }
   }

   private static void clearEnemyAggro(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return;
      for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(AGGRO_CLEAR_RADIUS),
         mob -> mob.isAlive() && mob.getTarget() == player)) {
         mob.setTarget(null);
         mob.getNavigation().stop();
      }
   }
}
