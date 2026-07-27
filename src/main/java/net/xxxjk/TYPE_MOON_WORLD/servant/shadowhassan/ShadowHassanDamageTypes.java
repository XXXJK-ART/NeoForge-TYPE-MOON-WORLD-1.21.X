package net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class ShadowHassanDamageTypes {
   public static final ResourceKey<DamageType> MEDITATIVE_SENSITIVITY = ResourceKey.create(
      Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "meditative_sensitivity"));
   public static final TagKey<EntityType<?>> BLADE_IMMUNE = TagKey.create(
      Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "shadow_hassan_blade_immune"));

   private ShadowHassanDamageTypes() {
   }
}
