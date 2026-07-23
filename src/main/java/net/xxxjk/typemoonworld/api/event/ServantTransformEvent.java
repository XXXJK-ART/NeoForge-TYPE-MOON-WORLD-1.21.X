package net.xxxjk.typemoonworld.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class ServantTransformEvent extends Event {
   private final ServerPlayer player;
   private final ResourceLocation servantId;

   protected ServantTransformEvent(ServerPlayer player, ResourceLocation servantId) {
      this.player = player;
      this.servantId = servantId;
   }

   public ServerPlayer player() { return this.player; }
   public ResourceLocation servantId() { return this.servantId; }

   public static final class Pre extends ServantTransformEvent implements ICancellableEvent {
      public Pre(ServerPlayer player, ResourceLocation servantId) { super(player, servantId); }
   }
   public static final class Post extends ServantTransformEvent {
      public Post(ServerPlayer player, ResourceLocation servantId) { super(player, servantId); }
   }
   public static final class End extends ServantTransformEvent {
      public End(ServerPlayer player, ResourceLocation servantId) { super(player, servantId); }
   }
}
