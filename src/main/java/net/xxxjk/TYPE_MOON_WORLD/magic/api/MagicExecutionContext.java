package net.xxxjk.TYPE_MOON_WORLD.magic.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public record MagicExecutionContext(Entity entity, TypeMoonWorldModVariables.PlayerVariables vars, String magicId, boolean crestCast) {
   public Player asPlayer() {
      return this.entity instanceof Player player ? player : null;
   }

   public ServerPlayer asServerPlayer() {
      return this.entity instanceof ServerPlayer player ? player : null;
   }
}
