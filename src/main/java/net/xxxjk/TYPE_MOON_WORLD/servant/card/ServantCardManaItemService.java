package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Converts explicit mana items into servant-card MP when the card is active. */
@EventBusSubscriber(modid = "typemoonworld")
public final class ServantCardManaItemService {
   private ServantCardManaItemService() {
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
      if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() == null) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (ServantCardManaService.restoreFromActiveItem(player, vars, event.getHand())) {
         event.setCanceled(true);
         event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
      }
   }
}
