package io.github.typemoonaddon.client.renderer;

import io.github.typemoonaddon.client.model.VoidRingRegaliaModel;
import io.github.typemoonaddon.item.VoidRingRegaliaItem;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** Renders every bone from the single chest-slot regalia item exactly once. */
public final class VoidRingRegaliaRenderer extends GeoArmorRenderer<VoidRingRegaliaItem> {
    public VoidRingRegaliaRenderer() {
        super(new VoidRingRegaliaModel());
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        setAllBonesVisible(true);
    }
}
