package net.xxxjk.TYPE_MOON_WORLD.magic.crest;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber
public class CrestOriginInvalidationHandler {
   @SubscribeEvent
   public static void onLivingDeath(LivingDeathEvent event) {
      LivingEntity dead = event.getEntity();
      if (!dead.level().isClientSide()) {
         if (!(dead instanceof Player)) {
            if (dead.level() instanceof ServerLevel serverLevel) {
               for (ServerPlayer holder : serverLevel.getServer().getPlayerList().getPlayers()) {
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)holder.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  int removed = vars.invalidateNpcOriginCrestEntries(dead.getUUID());
                  if (removed > 0) {
                     holder.displayClientMessage(
                        Component.translatable("message.typemoonworld.crest.origin_invalidated", new Object[]{dead.getDisplayName(), removed}), true
                     );
                     vars.syncPlayerVariables(holder);
                  }
               }
            }
         }
      }
   }
}
