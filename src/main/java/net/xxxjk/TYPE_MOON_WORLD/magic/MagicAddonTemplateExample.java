package net.xxxjk.TYPE_MOON_WORLD.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class MagicAddonTemplateExample {
   private MagicAddonTemplateExample() {
   }

   public static final class ExampleEntrypoint implements IMagicAddonEntrypoint {
      @Override
      public String providerId() {
         return "example_addon_mod";
      }

      @Override
      public void registerMagics(IMagicRegistry registry) {
         registry.register("example_magic_spell", MagicAddonTemplateExample.ExampleMagic::execute, this.providerId());
      }
   }

   public static final class ExampleMagic {
      private static final double COST = 20.0;

      private ExampleMagic() {
      }

      public static MagicExecutionResult execute(MagicExecutionContext context) {
         if (context.entity() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = context.vars();
            if (vars.player_mana < 20.0) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
               return MagicExecutionResult.FAILED;
            } else {
               vars.player_mana -= 20.0;
               vars.syncMana(player);
               return MagicExecutionResult.SUCCESS;
            }
         } else {
            return MagicExecutionResult.FAILED;
         }
      }
   }
}
