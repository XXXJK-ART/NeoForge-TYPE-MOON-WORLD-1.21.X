package net.xxxjk.TYPE_MOON_WORLD.magic;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class MuramasaDamageTypes {
   public static final ResourceKey<DamageType> TSUMUKARI_MURAMASA = ResourceKey.create(
      Registries.DAMAGE_TYPE,
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "tsumukari_muramasa")
   );

   private MuramasaDamageTypes() {
   }
}
