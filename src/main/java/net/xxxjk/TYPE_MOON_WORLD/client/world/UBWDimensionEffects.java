package net.xxxjk.TYPE_MOON_WORLD.client.world;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UBWDimensionEffects extends DimensionSpecialEffects {
    public UBWDimensionEffects() {
        // float cloudLevel, boolean hasGround, SkyType skyType, boolean forceBrightLight, boolean constantAmbientLight
        // cloudLevel = 120.0F (lower than vanilla 192.0F to make clouds appear denser/closer)
        super(120.0F, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        // Maintain the atmospheric color
        return fogColor.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }
}
