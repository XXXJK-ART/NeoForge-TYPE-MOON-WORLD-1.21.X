package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.renderer.BoundaryMarkRenderer;
import com.example.typemoonaddon.client.renderer.GillesDeRaisRenderer;
import com.example.typemoonaddon.client.renderer.HugeSeaMonsterRenderer;
import com.example.typemoonaddon.client.renderer.SeaMonsterRenderer;
import com.example.typemoonaddon.client.renderer.SummonedSpiritParticleRenderer;
import com.example.typemoonaddon.registry.AddonEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Keeps legacy entity renderers in the packaged jar; ClientModEvents is intentionally excluded. */
@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LegacyEntityRendererEvents {
    private LegacyEntityRendererEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AddonEntities.GILLES_DE_RAIS_CASTER.get(), GillesDeRaisRenderer::new);
        event.registerEntityRenderer(AddonEntities.GILLES_SEA_MONSTER.get(), SeaMonsterRenderer::new);
        event.registerEntityRenderer(AddonEntities.GILLES_SEA_MONSTER_SPIT.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(AddonEntities.GILLES_HUGE_SEA_MONSTER.get(), HugeSeaMonsterRenderer::new);
        event.registerEntityRenderer(AddonEntities.WRAITH.get(), SummonedSpiritParticleRenderer::new);
        event.registerEntityRenderer(AddonEntities.EVIL_SPIRIT.get(), SummonedSpiritParticleRenderer::new);
        event.registerEntityRenderer(AddonEntities.EVIL_SPIRIT_SMALL.get(), SummonedSpiritParticleRenderer::new);
        event.registerEntityRenderer(AddonEntities.BOUNDARY_MARK.get(), BoundaryMarkRenderer::new);
    }
}
