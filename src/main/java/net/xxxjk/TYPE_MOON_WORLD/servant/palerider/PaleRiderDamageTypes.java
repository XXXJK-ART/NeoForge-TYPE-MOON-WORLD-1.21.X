package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class PaleRiderDamageTypes {
   public static final ResourceKey<DamageType> INFECTION = key("pale_rider_infection");
   public static final ResourceKey<DamageType> RAT_BITE = key("pale_rider_rat_bite");
   public static final ResourceKey<DamageType> FAMINE = key("pale_rider_famine");
   public static final ResourceKey<DamageType> CONCEPT_SWORD = key("pale_rider_concept_sword");
   public static final ResourceKey<DamageType> CONCEPT_DEATH = key("pale_rider_concept_death");

   private PaleRiderDamageTypes() {
   }

   private static ResourceKey<DamageType> key(String path) {
      return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path));
   }
}
