package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.client.model.PrelatisSpellbookModel;
import com.example.typemoonaddon.item.PrelatisSpellbookItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class PrelatisSpellbookRenderer extends GeoItemRenderer<PrelatisSpellbookItem> {
    public PrelatisSpellbookRenderer() {
        super(new PrelatisSpellbookModel());
    }
}
