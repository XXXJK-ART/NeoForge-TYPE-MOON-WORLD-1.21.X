package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

/** Damage sources for Paracelsus attacks that need distinct defense rules. */
public final class ParacelsusDamageTypes {
   public static final ResourceKey<DamageType> ELEMENTAL_SWORD = ResourceKey.create(
      Registries.DAMAGE_TYPE,
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "paracelsus_elemental_sword")
   );

   private ParacelsusDamageTypes() {}

   public static DamageSource elementalSword(LivingEntity source) {
      var holder = source.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ELEMENTAL_SWORD);
      return new DamageSource(holder, source);
   }
}
