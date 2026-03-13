package net.xxxjk.TYPE_MOON_WORLD.procedures;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber
public class Initialization_of_entering_the_world {
   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      execute(event, event.getEntity());
   }

   public static void execute(Entity entity) {
      execute(null, entity);
   }

   private static void execute(@Nullable Event event, Entity entity) {
      if (entity != null) {
         if (((TypeMoonWorldModVariables.PlayerVariables)entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES)).player_max_mana == 0.0) {
            TypeMoonWorldModVariables.PlayerVariables _vars = (TypeMoonWorldModVariables.PlayerVariables)entity.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            _vars.player_max_mana = 0.0;
            _vars.player_mana = 0.0;
            _vars.syncPlayerVariables(entity);
         }
      }
   }
}
