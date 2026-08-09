package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosEntity;

public final class NeroChaosRenderer extends DeadApostleRenderer<NeroChaosEntity> {
   public NeroChaosRenderer(EntityRendererProvider.Context context) {
      super(context, "nero_chaos");
   }
}
