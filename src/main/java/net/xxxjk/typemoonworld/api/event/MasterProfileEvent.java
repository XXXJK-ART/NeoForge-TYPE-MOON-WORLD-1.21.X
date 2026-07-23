package net.xxxjk.typemoonworld.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class MasterProfileEvent extends Event {
   private final ServerPlayer player;
   private final ResourceLocation profileId;
   protected MasterProfileEvent(ServerPlayer player, ResourceLocation profileId) {
      this.player = player;
      this.profileId = profileId;
   }
   public ServerPlayer player() { return this.player; }
   public ResourceLocation profileId() { return this.profileId; }

   public static final class Pre extends MasterProfileEvent implements ICancellableEvent {
      public Pre(ServerPlayer player, ResourceLocation profileId) { super(player, profileId); }
   }
   public static final class Post extends MasterProfileEvent {
      public Post(ServerPlayer player, ResourceLocation profileId) { super(player, profileId); }
   }
}
