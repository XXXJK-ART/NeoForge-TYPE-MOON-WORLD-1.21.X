package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;

public final class ServantCardCommonSkills {
   private ServantCardCommonSkills() {
   }

   public static void performPresenceConcealment(ServerPlayer player, String servantId) {
      int duration = "sasaki_kojiro".equals(servantId) ? 600 : 220;
      player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false, false));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1, false, false, false));
      player.getPersistentData().putInt("ServantCardConcealmentUntil", player.tickCount + duration);
   }

   public static void performBerkana(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1, false, true, true));
   }

   public static void performFallback(ServerPlayer player, String id) {
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      if (isDashAction(id)) {
         player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 1.35, 0.18, dir.z * 1.35));
         player.hurtMarked = true;
      }
      float damage = switch (id) {
         case "zabaniya", "wu_er_da" -> 90.0F;
         case "bellerophon", "bloodfort_np", "three_thousand", "hajun", "enuma_elish" -> 80.0F;
         case "earth_rend", "slam", "mega_age", "mixed_element", "thunder" -> 32.0F;
         default -> 18.0F;
      };
      double range = id.endsWith("_np") || "bellerophon".equals(id) || "three_thousand".equals(id) || "enuma_elish".equals(id) ? 12.0 : 5.0;
      ServantCardSkillUtils.hitForwardArc(player, dir, range, damage);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + dir.x * 1.5, player.getY() + 1.0, player.getZ() + dir.z * 1.5, 6, 0.5, 0.3, 0.5, 0.0);
         level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
      }
   }

   private static boolean isDashAction(String id) {
      return id.contains("step") || id.contains("rush") || id.contains("leap") || id.contains("charge") || id.contains("pursuit") || id.contains("vault");
   }
}
