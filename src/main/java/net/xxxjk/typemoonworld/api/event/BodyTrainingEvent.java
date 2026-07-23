package net.xxxjk.typemoonworld.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class BodyTrainingEvent extends Event {
   private final ServerPlayer player;
   protected BodyTrainingEvent(ServerPlayer player) { this.player = player; }
   public ServerPlayer player() { return this.player; }

   public static final class Award extends BodyTrainingEvent implements ICancellableEvent {
      private final int amount;
      public Award(ServerPlayer player, int amount) { super(player); this.amount = amount; }
      public int amount() { return this.amount; }
   }
   public static final class Allocate extends BodyTrainingEvent implements ICancellableEvent {
      private final String stat;
      public Allocate(ServerPlayer player, String stat) { super(player); this.stat = stat; }
      public String stat() { return this.stat; }
   }
   public static final class Changed extends BodyTrainingEvent {
      public Changed(ServerPlayer player) { super(player); }
   }
}
