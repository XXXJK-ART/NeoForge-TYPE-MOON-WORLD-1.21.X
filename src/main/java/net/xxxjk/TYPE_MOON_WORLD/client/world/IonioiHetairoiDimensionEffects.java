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
      return new Vec3(
         fogColor.x * (brightness * 0.88F + 0.22F),
         fogColor.y * (brightness * 0.80F + 0.24F),
         fogColor.z * (brightness * 0.58F + 0.18F)
      );
   }

   @Override
   public boolean isFoggyAt(int x, int z) {
      return false;
   }
}
