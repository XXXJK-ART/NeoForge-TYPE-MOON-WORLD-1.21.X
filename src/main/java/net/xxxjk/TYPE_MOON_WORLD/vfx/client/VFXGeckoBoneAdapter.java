package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

public final class VFXGeckoBoneAdapter {
   private static final boolean GECKOLIB_PRESENT = isPresent("software.bernie.geckolib.cache.object.GeoBone");

   private VFXGeckoBoneAdapter() {
   }

   public static Optional<Vector3f> resolveBonePosition(LivingEntity entity, String boneName) {
      if (!GECKOLIB_PRESENT || entity == null || boneName == null || boneName.isBlank()) {
         return Optional.empty();
      }
      return Optional.empty();
   }

   private static boolean isPresent(String className) {
      try {
         Class.forName(className, false, VFXGeckoBoneAdapter.class.getClassLoader());
         return true;
      } catch (ClassNotFoundException ex) {
         return false;
      }
   }
}
