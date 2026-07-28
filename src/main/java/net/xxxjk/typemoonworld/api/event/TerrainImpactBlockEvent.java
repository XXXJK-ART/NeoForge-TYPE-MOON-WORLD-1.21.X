package net.xxxjk.typemoonworld.api.event;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;

/** Fired immediately before a terrain impact removes one block. */
public final class TerrainImpactBlockEvent extends Event implements ICancellableEvent {
   private final ServerLevel level;
   private final LivingEntity source;
   private final BlockPos pos;
   private final BlockState state;
   private final TerrainImpactProfile profile;
   private final String shape;

   public TerrainImpactBlockEvent(ServerLevel level, @Nullable LivingEntity source, BlockPos pos,
                                  BlockState state, TerrainImpactProfile profile, String shape) {
      this.level = level;
      this.source = source;
      this.pos = pos.immutable();
      this.state = state;
      this.profile = profile;
      this.shape = shape;
   }

   public ServerLevel level() { return level; }
   @Nullable public LivingEntity source() { return source; }
   public BlockPos pos() { return pos; }
   public BlockState state() { return state; }
   public TerrainImpactProfile profile() { return profile; }
   public String shape() { return shape; }
}
