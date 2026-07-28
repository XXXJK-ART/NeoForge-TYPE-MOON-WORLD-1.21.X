package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public interface BeamClashParticipant {
   Entity clashEntity();

   BeamType beamType();

   LivingEntity beamOwner(ServerLevel level);

   Vec3 beamStart();

   Vec3 beamEnd();

   double beamHalfWidth();

   /** Relative output used by the clash resolver before current mana is considered. */
   float clashPower();

   boolean isBeamDamageActive();

   boolean isClashing();

   void setClashing(boolean clashing);

   void setClashDamageScale(float scale);

   void cancelClashBeam();

   void continueAfterClash();
}
