package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.combat.OriginBulletHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class ToggleMagicCircuit {
   public static void execute(Entity entity) {
      if (entity != null) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (entity instanceof Player player && OriginBulletHelper.isSealed(player)) {
            vars.is_magic_circuit_open = false;
            vars.magic_circuit_open_timer = 0.0;
            vars.player_mana = 0.0;
            vars.syncMana(entity);
            if (!player.level().isClientSide()) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.origin_bullet.sealed"), true);
            }
            return;
         }
         boolean newState = !vars.is_magic_circuit_open;
         vars.is_magic_circuit_open = newState;
         if (newState) {
            vars.magic_circuit_open_timer = 0.0;
         } else {
            vars.magic_circuit_open_timer = 0.0;
         }

         vars.syncMana(entity);
      }
   }
}
