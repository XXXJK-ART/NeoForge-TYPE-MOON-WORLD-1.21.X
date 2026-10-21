package net.xxxjk.TYPE_MOON_WORLD.client;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.FirstPersonModifier;

public final class ServantActionAnimationLayer {
   private ServantActionAnimationLayer() { }

   public static ModifierLayer<KeyframeAnimationPlayer> create() {
      ModifierLayer<KeyframeAnimationPlayer> layer = new ModifierLayer<>();
      layer.addModifierLast(new FirstPersonModifier() {
         @Override
         public FirstPersonMode getFirstPersonMode(float tickDelta) {
            var animation = getAnim();
            return animation != null && animation.isActive()
               ? FirstPersonMode.THIRD_PERSON_MODEL
               : FirstPersonMode.DISABLED;
         }
      });
      return layer;
   }
}
