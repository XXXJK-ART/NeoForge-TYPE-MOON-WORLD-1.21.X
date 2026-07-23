package net.xxxjk.typemoonworld.api;

import net.minecraft.server.level.ServerPlayer;

public record CardActionContext(ServerPlayer player, String servantId, int slot, boolean crouching, long gameTick) {
}
