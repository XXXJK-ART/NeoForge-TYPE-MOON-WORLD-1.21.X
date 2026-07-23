package net.xxxjk.typemoonworld.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public record ProjectionStructureContext(ServerPlayer player, ServerLevel level, BlockPos anchor, String templateId, int rotation, double manaBudget) {
   public ProjectionStructureContext {
      templateId = templateId == null ? "" : templateId.length() > 128 ? templateId.substring(0, 128) : templateId;
      rotation = Math.floorMod(rotation, 4);
      manaBudget = Math.max(0.0, manaBudget);
   }
}
