package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;

/** Spawn/load phase for NPC magic state. */
public final class NpcStateInitializer {
   private NpcStateInitializer() {
   }

   public static void initialize(MysticMagicianEntity npc) {
      if (npc != null) {
         NpcMagicCastBridge.onSpawnInitialized(npc);
      }
   }
}
