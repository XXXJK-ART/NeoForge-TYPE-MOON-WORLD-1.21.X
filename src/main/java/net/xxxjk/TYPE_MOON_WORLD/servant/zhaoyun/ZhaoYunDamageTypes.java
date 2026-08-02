package net.xxxjk.TYPE_MOON_WORLD.servant.zhaoyun;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

/** Damage types used by Zhao Yun's weapon-specific effects. */
public final class ZhaoYunDamageTypes {
   public static final ResourceKey<DamageType> QINGGANG_SECOND_HIT = ResourceKey.create(
      Registries.DAMAGE_TYPE,
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "qinggang_second_hit"));

   private ZhaoYunDamageTypes() {
   }
}
