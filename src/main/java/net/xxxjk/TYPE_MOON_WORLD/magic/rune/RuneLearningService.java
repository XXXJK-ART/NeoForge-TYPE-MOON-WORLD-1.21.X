package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class RuneLearningService {
   public static final String ORIGIN_MAGIC_ID = "rune_origin";
   private RuneLearningService() { }

   public static boolean hasRune(TypeMoonWorldModVariables.PlayerVariables vars, String runeId) {
      RuneDefinition definition = RuneRegistry.get(runeId);
      if (vars == null || definition == null || vars.learned_runes == null) return false;
      // Accept legacy namespaced entries as well as the canonical path form.
      // This keeps old player data from making the editor's learned runes
      // appear valid on the client but fail the server save check.
      return vars.learned_runes.stream().map(RuneRegistry::get)
         .filter(java.util.Objects::nonNull)
         .anyMatch(learned -> definition.idPath().equals(learned.idPath()));
   }

   public static boolean learn(Player player, String runeId) {
      if (player == null || player.level().isClientSide()) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      RuneDefinition definition = RuneRegistry.get(runeId);
      if (definition == null || hasRune(vars, definition.idPath())) return false;
      vars.learned_runes.add(definition.idPath());
      if (!vars.rune_origin_unlocked) {
         vars.rune_origin_unlocked = true;
         if (!vars.learned_magics.contains(ORIGIN_MAGIC_ID)) vars.learned_magics.add(ORIGIN_MAGIC_ID);
      }
      vars.forceSyncPlayerVariables(player);
      player.displayClientMessage(Component.translatable("message.typemoonworld.rune.learned", definition.displayName()), false);
      return true;
   }

   public static boolean hasOrigin(TypeMoonWorldModVariables.PlayerVariables vars) {
      return vars != null && vars.rune_origin_unlocked;
   }
}
