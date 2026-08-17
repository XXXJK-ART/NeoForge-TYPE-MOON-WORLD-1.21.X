package net.xxxjk.TYPE_MOON_WORLD.client.world;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.DimensionSpecialEffects.SkyType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IonioiHetairoiDimensionEffects extends DimensionSpecialEffects {
   public IonioiHetairoiDimensionEffects() {
      super(160.0F, true, SkyType.NORMAL, false, false);
   }

   @Override
   public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
      Vec3 sunHaze = new Vec3(1.0, 0.90, 0.56);
      Vec3 litFog = new Vec3(
         fogColor.x * (brightness * 0.96F + 0.34F),
         fogColor.y * (brightness * 0.92F + 0.34F),
         fogColor.z * (brightness * 0.62F + 0.24F)
      );
      return litFog.scale(0.78).add(sunHaze.scale(0.22));
   }

   @Override
   public boolean isFoggyAt(int x, int z) {
      return false;
   }
}
