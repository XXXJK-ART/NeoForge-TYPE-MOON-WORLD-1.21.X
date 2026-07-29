package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber
public final class ServantCardHealthService {
   private static final int OUT_OF_COMBAT_TICKS = 200;
   private static final double MANA_PER_HEAL = 1.0;
   private static final float MAX_HEALTH_FRACTION = 0.005F;

   private ServantCardHealthService() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_transformed || player.tickCount % 20 != 0
         || player.getHealth() >= player.getMaxHealth()) return;
      long now = player.level().getGameTime();
      if (vars.servant_card_last_combat_tick == Long.MIN_VALUE) {
         vars.servant_card_last_combat_tick = now;
         return;
      }
      if (now - vars.servant_card_last_combat_tick < OUT_OF_COMBAT_TICKS) return;
      if (ServantCardManaService.consumeOwnMana(player, vars, MANA_PER_HEAL)) {
         player.heal(player.getMaxHealth() * MAX_HEALTH_FRACTION);
      }
   }

   @SubscribeEvent
   public static void onDamageApplied(LivingDamageEvent.Post event) {
      if (event.getNewDamage() <= 0.0F || event.getEntity().level().isClientSide) return;
      if (event.getEntity().getPersistentData().getBoolean("MasterLossDecayDamage")) return;
      long now = event.getEntity().level().getGameTime();
      mark(event.getEntity(), now);
      Entity attacker = event.getSource().getEntity();
      if (attacker != event.getEntity()) mark(attacker, now);
   }

   private static void mark(Entity entity, long now) {
      if (!(entity instanceof ServerPlayer player)) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed) vars.servant_card_last_combat_tick = now;
   }
}
