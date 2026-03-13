package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

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
