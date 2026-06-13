package net.xxxjk.TYPE_MOON_WORLD.mixin;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TranslatableContents.class)
public class TranslatableContentsMixin {
   @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
   private static Object[] typemoonworld$sanitizeTranslationArgs(Object[] args) {
      if (args == null || args.length == 0) {
         return args;
      }

      Object[] sanitized = null;
      for (int i = 0; i < args.length; i++) {
         Object argument = args[i];
         if (argument instanceof Optional<?>) {
            if (sanitized == null) {
               sanitized = args.clone();
            }
            sanitized[i] = sanitizeOptional((Optional<?>) argument);
         }
      }
      return sanitized == null ? args : sanitized;
   }

   private static Object sanitizeOptional(Optional<?> optional) {
      if (optional.isEmpty()) {
         return "";
      }

      Object value = optional.get();
      if (value instanceof Component || value instanceof Number || value instanceof Boolean || value instanceof String) {
         return value;
      }
      return String.valueOf(value);
   }
}
