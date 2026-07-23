package net.xxxjk.typemoonworld.api.event;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.xxxjk.typemoonworld.api.ExecutionResult;
import net.xxxjk.typemoonworld.api.MagicCastContext;

public abstract class MagicCastEvent extends Event {
   private final ResourceLocation magicId;
   private final MagicCastContext context;

   protected MagicCastEvent(ResourceLocation magicId, MagicCastContext context) {
      this.magicId = magicId;
      this.context = context;
   }

   public ResourceLocation magicId() { return this.magicId; }
   public MagicCastContext context() { return this.context; }

   public static final class Pre extends MagicCastEvent implements ICancellableEvent {
      public Pre(ResourceLocation magicId, MagicCastContext context) { super(magicId, context); }
   }
   public static final class Post extends MagicCastEvent {
      private final ExecutionResult result;
      public Post(ResourceLocation magicId, MagicCastContext context, ExecutionResult result) {
         super(magicId, context);
         this.result = result;
      }
      public ExecutionResult result() { return this.result; }
   }
}
