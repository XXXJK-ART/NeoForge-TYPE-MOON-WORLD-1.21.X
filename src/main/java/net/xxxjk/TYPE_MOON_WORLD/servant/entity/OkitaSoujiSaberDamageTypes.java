package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class OkitaSoujiSaberDamageTypes {
   public static final ResourceKey<DamageType> MUMYOUDAN_ZUKI = ResourceKey.create(
      Registries.DAMAGE_TYPE,
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "okita_mumyoudan_zuki")
   );

   private OkitaSoujiSaberDamageTypes() {
   }
}
