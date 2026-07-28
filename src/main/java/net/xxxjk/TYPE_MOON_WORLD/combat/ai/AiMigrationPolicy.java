package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.Config;

public final class AiMigrationPolicy {
   private AiMigrationPolicy() { }

   public static boolean isArbitrated(Entity entity) {
      if (entity == null || !Config.arbitratedCombatAiEnabled) return false;
      return !Config.legacyAiEntityTypes.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
   }
}
