package net.xxxjk.TYPE_MOON_WORLD.client.world;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.DimensionSpecialEffects.SkyType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HajunDimensionEffects extends DimensionSpecialEffects {
   public HajunDimensionEffects() {
      super(96.0F, true, SkyType.NORMAL, false, false);
   }

   @Override
   public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
      return new Vec3(
         fogColor.x * (brightness * 0.76F + 0.24F),
         fogColor.y * (brightness * 0.42F + 0.24F),
         fogColor.z * (brightness * 0.34F + 0.18F)
      );
   }

   @Override
   public boolean isFoggyAt(int x, int z) {
      return false;
   }
}
