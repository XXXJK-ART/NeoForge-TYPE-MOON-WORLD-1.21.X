package net.xxxjk.TYPE_MOON_WORLD.servant.nightingale;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class NightingaleDamageTypes {
   public static final ResourceKey<DamageType> HEALING_REVERSAL = ResourceKey.create(
      Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "nightingale_healing_reversal")
   );

   private NightingaleDamageTypes() {}

   public static DamageSource healingReversal(LivingEntity source) {
      var holder = source.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(HEALING_REVERSAL);
      return new DamageSource(holder, source);
   }
}
