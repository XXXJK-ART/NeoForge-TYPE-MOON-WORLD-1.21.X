package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.FanaticAssassinModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.FanaticAssassinEntity;
import software.bernie.geckolib.util.Color;

public final class FanaticAssassinRenderer extends BaseServantRenderer<FanaticAssassinEntity> {
   public FanaticAssassinRenderer(Context context) {
      super(context, new FanaticAssassinModel(), 0.88F);
      this.shadowRadius = 0.42F;
   }

   @Override
   public Color getRenderColor(FanaticAssassinEntity entity, float partialTick, int packedLight) {
      if (entity.isSpiritualDissolving()) return super.getRenderColor(entity, partialTick, packedLight);
      return switch (entity.getActiveTechnique()) {
         case FanaticAssassinEntity.TECHNIQUE_HEARTBEAT -> Color.ofARGB(255, 151, 91, 164);
         case FanaticAssassinEntity.TECHNIQUE_COMPUTER -> Color.ofARGB(255, 178, 58, 48);
         case FanaticAssassinEntity.TECHNIQUE_MARROW -> Color.ofARGB(255, 220, 126, 222);
         case FanaticAssassinEntity.TECHNIQUE_HAIR -> Color.ofARGB(255, 194, 198, 207);
         case FanaticAssassinEntity.TECHNIQUE_TEMPERATURE -> Color.ofARGB(255, 205, 225, 236);
         case FanaticAssassinEntity.TECHNIQUE_NERVES -> Color.ofARGB(255, 177, 170, 236);
         case FanaticAssassinEntity.TECHNIQUE_TOXIN -> Color.ofARGB(255, 190, 104, 204);
         case FanaticAssassinEntity.TECHNIQUE_JINN -> Color.ofARGB(255, 170, 164, 181);
         default -> super.getRenderColor(entity, partialTick, packedLight);
      };
   }
}
