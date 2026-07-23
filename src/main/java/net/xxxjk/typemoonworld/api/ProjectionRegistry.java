package net.xxxjk.typemoonworld.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative handlers for projection content. */
public interface ProjectionRegistry {
   boolean registerItem(String id, ProjectionItemExecutor executor);
   boolean registerStructure(String id, ProjectionStructureExecutor executor);
   boolean executeItem(String id, ProjectionItemContext context);
   boolean executeStructure(String id, ProjectionStructureContext context);
}
