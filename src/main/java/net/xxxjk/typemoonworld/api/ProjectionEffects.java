package net.xxxjk.typemoonworld.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Server-side visual effect façade for projection and structural-analysis addons. */
public interface ProjectionEffects {
   void structureStart(ServerLevel level, BlockPos anchor);
   void blockPlace(ServerLevel level, BlockPos position);
   void blockBreak(ServerLevel level, BlockPos position);
}
