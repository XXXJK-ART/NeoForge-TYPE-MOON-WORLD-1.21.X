package net.xxxjk.typemoonworld.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public record CommandSpellContext(ServerPlayer master, LivingEntity target, int spellIndex, long gameTick) {
   public CommandSpellContext { spellIndex = Math.max(0, spellIndex); }
}
