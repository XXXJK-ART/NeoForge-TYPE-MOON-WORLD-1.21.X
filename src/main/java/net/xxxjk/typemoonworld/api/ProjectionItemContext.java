package net.xxxjk.typemoonworld.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record ProjectionItemContext(ServerPlayer player, ServerLevel level, BlockPos target, ItemStack source, double manaBudget) {
   public ProjectionItemContext {
      source = source == null ? ItemStack.EMPTY : source.copy();
      manaBudget = Math.max(0.0, manaBudget);
   }
}
