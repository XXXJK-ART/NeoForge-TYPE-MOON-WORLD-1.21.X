package net.xxxjk.typemoonworld.api.event;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.xxxjk.typemoonworld.api.ExecutionResult;
import net.xxxjk.typemoonworld.api.ServantContext;

public abstract class ServantActionEvent extends Event {
   public enum Kind { SKILL, NOBLE_PHANTASM }

   private final Kind kind;
   private final ResourceLocation actionId;
   private final ServantContext context;

   protected ServantActionEvent(Kind kind, ResourceLocation actionId, ServantContext context) {
      this.kind = kind;
      this.actionId = actionId;
      this.context = context;
   }

   public Kind kind() { return this.kind; }
   public ResourceLocation actionId() { return this.actionId; }
   public ServantContext context() { return this.context; }

   public static final class Pre extends ServantActionEvent implements ICancellableEvent {
      public Pre(Kind kind, ResourceLocation actionId, ServantContext context) { super(kind, actionId, context); }
   }
   public static final class Post extends ServantActionEvent {
      private final ExecutionResult result;
      public Post(Kind kind, ResourceLocation actionId, ServantContext context, ExecutionResult result) {
         super(kind, actionId, context);
         this.result = result;
      }
      public ExecutionResult result() { return this.result; }
   }
}
