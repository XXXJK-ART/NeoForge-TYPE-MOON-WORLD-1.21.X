package net.xxxjk.TYPE_MOON_WORLD.servant.jeanne;

import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;

/** Vanilla-template black/silver/crimson spawn egg; tinting is handled by DeferredSpawnEggItem. */
public final class JeanneAlterSpawnEggItem extends DeferredSpawnEggItem {
   public JeanneAlterSpawnEggItem(Properties properties) {
      super(ModEntities.JEANNE_ALTER, 0x16121C, 0x8E1739, properties);
   }
}
