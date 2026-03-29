package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/**
 * Reusable shape for NPC magic execution.
 *
 * <p>The current core NPC flow is still switch-based inside {@code NpcMagicExecutionService},
 * so implementations of this interface are not auto-discovered yet. Use it as a local adapter
 * object and forward the switch branches to it.
 *
 * <p>{@code slot.presetPayload} is the slot-local preset for this cast. Read parameters from the
 * slot instead of shared global state so multiple copies of the same magic do not overwrite each other.
 */
public interface INpcMagicAdapter {
   String magicId();

   boolean canCast(
      MysticMagicianEntity var1,
      LivingEntity var2,
      TypeMoonWorldModVariables.PlayerVariables var3,
      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry var4,
      double var5,
      long var7
   );

   boolean cast(
      MysticMagicianEntity var1,
      LivingEntity var2,
      TypeMoonWorldModVariables.PlayerVariables var3,
      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry var4,
      double var5,
      long var7
   );
}
