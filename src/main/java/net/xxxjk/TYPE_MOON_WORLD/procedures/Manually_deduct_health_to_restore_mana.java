package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class Manually_deduct_health_to_restore_mana {
   public static void execute(Entity entity) {
      if (entity != null) {
         if (((TypeMoonWorldModVariables.PlayerVariables)entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES)).player_mana
            != ((TypeMoonWorldModVariables.PlayerVariables)entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES)).player_max_mana) {
            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1.0F) > 1.0F) {
               TypeMoonWorldModVariables.PlayerVariables _vars = (TypeMoonWorldModVariables.PlayerVariables)entity.getData(
                  TypeMoonWorldModVariables.PLAYER_VARIABLES
               );
               _vars.player_mana = ((TypeMoonWorldModVariables.PlayerVariables)entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES)).player_mana + 10.0;
               _vars.syncMana(entity);
               LivingEntity _entity = (LivingEntity)entity;
               LivingEntity _livEntx = (LivingEntity)entity;
               _entity.setHealth(_livEntx.getHealth() - 1.0F);
            } else if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(Component.translatable("message.typemoonworld.health.depleted"), true);
            }
         } else if (entity instanceof Player _player && !_player.level().isClientSide()) {
            _player.displayClientMessage(Component.translatable("message.typemoonworld.mana.already_full"), true);
         }
      }
   }
}
