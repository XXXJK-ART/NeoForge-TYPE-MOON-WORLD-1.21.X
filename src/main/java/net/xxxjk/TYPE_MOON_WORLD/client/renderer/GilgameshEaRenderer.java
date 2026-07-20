package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.client.model.GilgameshEaModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GilgameshEaRenderer extends GeoEntityRenderer<GilgameshEaBeamEntity> {
   public GilgameshEaRenderer(Context context) { super(context, new GilgameshEaModel()); }
   @Override public void render(GilgameshEaBeamEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      // EA is represented by the held item animation and VFX stages; the server controller is invisible.
   }
}
