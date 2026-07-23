package net.xxxjk.typemoonworld.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class ServantContractEvent extends Event {
   private final ServerPlayer master;
   private final LivingEntity servant;

   protected ServantContractEvent(ServerPlayer master, LivingEntity servant) {
      this.master = master;
      this.servant = servant;
   }

   public ServerPlayer master() { return this.master; }
   public LivingEntity servant() { return this.servant; }

   public static final class Pre extends ServantContractEvent implements ICancellableEvent {
      public Pre(ServerPlayer master, LivingEntity servant) { super(master, servant); }
   }
   public static final class Post extends ServantContractEvent {
      public Post(ServerPlayer master, LivingEntity servant) { super(master, servant); }
   }
}
