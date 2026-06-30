package net.xxxjk.TYPE_MOON_WORLD.servant.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;

public record ServantSpecialization(
   float bodyWidth,
   float bodyHeight,
   float eyeHeight,
   double handItemOffsetX,
   double handItemOffsetY,
   double handItemOffsetZ,
   String defaultWeaponItemId,
   boolean immuneToStoneAxeDebuff,
   Set<String> combatActions
) {
   public ServantSpecialization {
      bodyWidth = Math.max(0.0F, bodyWidth);
      bodyHeight = Math.max(0.0F, bodyHeight);
      eyeHeight = Math.max(0.0F, eyeHeight);
      defaultWeaponItemId = normalize(defaultWeaponItemId);
      combatActions = combatActions == null || combatActions.isEmpty()
         ? Collections.emptySet()
         : Collections.unmodifiableSet(new LinkedHashSet<>(combatActions));
   }

   public static ServantSpecialization empty() {
      return new ServantSpecialization(0.0F, 0.0F, 0.0F, 0.0, 0.0, 0.0, "", false, Collections.emptySet());
   }

   public static ServantSpecialization of(
      float bodyWidth,
      float bodyHeight,
      float eyeHeight,
      double handItemOffsetX,
      double handItemOffsetY,
      double handItemOffsetZ,
      String defaultWeaponItemId,
      boolean immuneToStoneAxeDebuff,
      Set<String> combatActions
   ) {
      return new ServantSpecialization(
         bodyWidth, bodyHeight, eyeHeight, handItemOffsetX, handItemOffsetY, handItemOffsetZ,
         defaultWeaponItemId, immuneToStoneAxeDebuff, combatActions
      );
   }

   public Optional<String> defaultWeaponItemIdOptional() {
      return Optional.ofNullable(this.defaultWeaponItemId).filter(s -> !s.isBlank());
   }

   public boolean hasCombatAction(String key) {
      return key != null && !key.isBlank() && this.combatActions != null && this.combatActions.contains(key);
   }

   public boolean hasBodyDimensions() {
      return this.bodyWidth > 0.0F && this.bodyHeight > 0.0F;
   }

   public EntityDimensions bodyDimensions() {
      EntityDimensions dimensions = EntityDimensions.scalable(this.bodyWidth, this.bodyHeight);
      return this.eyeHeight > 0.0F ? dimensions.withEyeHeight(this.eyeHeight) : dimensions;
   }

   public Vec3 handItemOffset() {
      return new Vec3(this.handItemOffsetX, this.handItemOffsetY, this.handItemOffsetZ);
   }

   private static String normalize(String value) {
      return value == null ? "" : value.trim();
   }
}
