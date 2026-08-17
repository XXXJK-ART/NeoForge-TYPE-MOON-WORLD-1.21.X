package net.xxxjk.TYPE_MOON_WORLD.servant.baobhan;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class BaobhanSithDamageTypes {
   public static final ResourceKey<DamageType> CURSE = key("baobhan_sith_curse");

   private BaobhanSithDamageTypes() {
   }

   public static boolean isCurse(DamageSource source) {
      return source != null && source.is(CURSE);
   }

   private static ResourceKey<DamageType> key(String path) {
      return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path));
   }
}
