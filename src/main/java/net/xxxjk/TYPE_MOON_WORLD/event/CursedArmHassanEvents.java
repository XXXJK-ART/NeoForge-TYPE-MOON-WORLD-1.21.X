package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHassanSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HassanPresenceService;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class CursedArmHassanEvents {
   private CursedArmHassanEvents() { }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onDeath(LivingDeathEvent event) {
      if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player && HassanPresenceService.isHassan(player))
         player.level().playSound(null, player.blockPosition(), net.xxxjk.TYPE_MOON_WORLD.init.ModSounds.CURSED_ARM_HASSAN_VOICE_FAIL.get(),
            net.minecraft.sounds.SoundSource.VOICE, 1.0F, 1.0F);
      if (event.isCanceled() || event.getEntity().level().isClientSide()
         || !ServantIdentityHelper.isServantLike(event.getEntity())) return;
      LivingEntity killer = event.getSource().getEntity() instanceof LivingEntity living ? living
         : event.getSource().getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner ? owner : null;
      // Death protections finish first; only a committed death grants an evolution.
      TYPE_MOON_WORLD.queueServerWork(1, () -> {
         if (event.isCanceled() || event.getEntity().isAlive() || killer == null || !killer.isAlive()
            || !HassanPresenceService.isHassan(killer)) return;
         if (killer instanceof CursedArmHassanEntity npc) npc.evolveAfterServantKill();
         else if (killer instanceof ServerPlayer player) ServantCardHassanSkills.evolveAfterServantKill(player);
         if (killer instanceof ServerPlayer player) player.level().playSound(null, player.blockPosition(),
            net.xxxjk.TYPE_MOON_WORLD.init.ModSounds.CURSED_ARM_HASSAN_VOICE_VICTORY.get(), net.minecraft.sounds.SoundSource.VOICE, 1.0F, 1.0F);
      });
   }
}
