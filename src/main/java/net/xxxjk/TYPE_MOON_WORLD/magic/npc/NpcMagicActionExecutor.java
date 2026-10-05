package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;

/** Single entry point for executing an NPC magic decision. */
public final class NpcMagicActionExecutor {
   private NpcMagicActionExecutor() {
   }

   public static void tick(MysticMagicianEntity npc) {
      if (npc != null) {
         NpcHazardController.update(npc);
         NpcMagicCastBridge.tickServer(npc);
      }
   }
}
