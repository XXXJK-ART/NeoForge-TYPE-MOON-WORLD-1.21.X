package net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;

public class MagicUnlimitedBladeWorks {
   public static void execute(Entity entity) {
      if (entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.has_unlimited_blade_works) {
            if (vars.is_chanting_ubw) {
               vars.is_chanting_ubw = false;
               vars.ubw_chant_progress = 0;
               vars.ubw_chant_timer = 0;
               vars.syncPlayerVariables(player);
               player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.cancelled"), true);
            } else {
               boolean isInUBWDimension = player.level().dimension().location().equals(ModDimensions.UBW_KEY.location());
               if (!vars.is_in_ubw && !isInUBWDimension) {
                  double initialCost = 50.0;
                  if (ManaHelper.consumeManaOrHealth(player, initialCost)) {
                     vars.is_chanting_ubw = true;
                     vars.ubw_chant_progress = 1;
                     vars.ubw_chant_timer = 0;
                     vars.syncPlayerVariables(player);
                     player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.chant"), true);
                  }
               } else {
                  ChantHandler.returnFromUBW(player, vars);
                  player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.exit"), true);
               }

               vars.syncPlayerVariables(player);
            }
         }
      }
   }

   public static void checkUnlock(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.player_magic_attributes_sword && !vars.has_unlimited_blade_works) {
         vars.has_unlimited_blade_works = true;
         vars.syncPlayerVariables(player);
         player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.unlocked"), false);
      }

      if (vars.has_unlimited_blade_works && !vars.learned_magics.contains("sword_barrel_full_open")) {
         vars.learned_magics.add("sword_barrel_full_open");
         vars.syncPlayerVariables(player);
         player.displayClientMessage(
            Component.translatable(
               "message.typemoonworld.magic.learned", new Object[]{Component.translatable("key.typemoonworld.magic.sword_barrel_full_open")}
            ),
            false
         );
      }
   }
}
