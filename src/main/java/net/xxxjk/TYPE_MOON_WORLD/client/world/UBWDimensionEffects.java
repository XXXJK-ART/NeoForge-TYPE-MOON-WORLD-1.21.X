package net.xxxjk.TYPE_MOON_WORLD.client.world;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.DimensionSpecialEffects.SkyType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UBWDimensionEffects extends DimensionSpecialEffects {
   public UBWDimensionEffects() {
      super(120.0F, true, SkyType.NORMAL, false, false);
   }

   public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
      return fogColor.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
   }

   public boolean isFoggyAt(int x, int z) {
      return false;
   }
}
