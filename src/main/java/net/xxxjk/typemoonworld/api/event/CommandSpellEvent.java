package net.xxxjk.typemoonworld.api.event;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.xxxjk.typemoonworld.api.CommandSpellContext;
import net.xxxjk.typemoonworld.api.ExecutionResult;

public abstract class CommandSpellEvent extends Event {
   private final ResourceLocation id;
   private final CommandSpellContext context;
   protected CommandSpellEvent(ResourceLocation id, CommandSpellContext context) { this.id = id; this.context = context; }
   public ResourceLocation id() { return id; }
   public CommandSpellContext context() { return context; }
   public static final class Pre extends CommandSpellEvent implements ICancellableEvent { public Pre(ResourceLocation id, CommandSpellContext context) { super(id, context); } }
   public static final class Post extends CommandSpellEvent {
      private final ExecutionResult result;
      public Post(ResourceLocation id, CommandSpellContext context, ExecutionResult result) { super(id, context); this.result = result; }
      public ExecutionResult result() { return result; }
   }
}
