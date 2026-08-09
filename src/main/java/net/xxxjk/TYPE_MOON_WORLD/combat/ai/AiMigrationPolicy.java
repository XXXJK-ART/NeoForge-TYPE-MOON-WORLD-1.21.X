package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;

public final class AiMigrationPolicy {
   private AiMigrationPolicy() { }

   public static boolean isArbitrated(Entity entity) {
      if (entity == null || !Config.arbitratedCombatAiEnabled) return false;
      // All Mystic Magician ranks use the same complete legacy stack:
      // target selection, personality, melee fallback, and spell casting.
      if (entity instanceof MysticMagicianEntity) return false;
      return !Config.legacyAiEntityTypes.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
   }
}
