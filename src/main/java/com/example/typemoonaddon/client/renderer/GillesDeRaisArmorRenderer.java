package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.client.model.GillesDeRaisArmorModel;
import com.example.typemoonaddon.item.CursedArmorRenderItem;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class GillesDeRaisArmorRenderer extends GeoArmorRenderer<CursedArmorRenderItem> {
    public GillesDeRaisArmorRenderer() {
        super(new GillesDeRaisArmorModel());
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        setAllBonesVisible(true);
    }
}
