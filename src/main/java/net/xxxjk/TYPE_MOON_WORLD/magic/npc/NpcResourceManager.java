package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import net.minecraft.server.level.ServerLevel;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.world.leyline.LeylineService;
import net.xxxjk.typemoonworld.api.AddonMagicHooks;

/** Owns NPC mana recovery and local cooldown resources. */
public final class NpcResourceManager {
   private NpcResourceManager() { }

   public static void tick(MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (npc == null || vars == null || !(npc.level() instanceof ServerLevel level)) return;
      var magic = vars.magicState();
      double multiplier = LeylineService.getRegenMultiplier(level, npc.blockPosition());
      vars.current_mana_regen_multiplier = multiplier;
      double interval = Math.max(1.0D, magic.restoreMagicMoment());
      if (vars.is_magic_circuit_open) interval = Math.max(1.0D, interval / 2.0D);
      double regen = AddonMagicHooks.adjustNaturalManaRegen(
         npc, magic.manaRegen() * multiplier / interval);
      if (magic.mana() < magic.maxMana()) {
         magic.setMana(Math.min(magic.maxMana(), magic.mana() + regen));
      }
      if (magic.cooldown() > 0.0D) magic.setCooldown(Math.max(0.0D, magic.cooldown() - 1.0D));
   }
}
