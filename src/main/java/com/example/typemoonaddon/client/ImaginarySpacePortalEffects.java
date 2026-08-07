package com.example.typemoonaddon.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/** Client-side sky settings for the imaginary-space portal skybox. */
public final class ImaginarySpacePortalEffects extends DimensionSpecialEffects {
    public ImaginarySpacePortalEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        // Keep fog nearly black so it cannot reintroduce a broad blue veil over the star field.
        double dim = 0.008D + brightness * 0.006D;
        return new Vec3(
                dim,
                dim * 0.96D,
                dim * 1.04D);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }
}
