package net.xxxjk.typemoonworld.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class ServantSummonEvent extends Event {
   private final ServerLevel level;
   private final ResourceLocation servantId;
   private final BlockPos position;

   protected ServantSummonEvent(ServerLevel level, ResourceLocation servantId, BlockPos position) {
      this.level = level;
      this.servantId = servantId;
      this.position = position.immutable();
   }

   public ServerLevel level() { return this.level; }
   public ResourceLocation servantId() { return this.servantId; }
   public BlockPos position() { return this.position; }

   public static final class Pre extends ServantSummonEvent implements ICancellableEvent {
      public Pre(ServerLevel level, ResourceLocation servantId, BlockPos position) { super(level, servantId, position); }
   }

   public static final class Post extends ServantSummonEvent {
      private final LivingEntity servant;
      public Post(ServerLevel level, ResourceLocation servantId, BlockPos position, LivingEntity servant) {
         super(level, servantId, position);
         this.servant = servant;
      }
      public LivingEntity servant() { return this.servant; }
   }
}
