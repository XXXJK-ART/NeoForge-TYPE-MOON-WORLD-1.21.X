package net.xxxjk.TYPE_MOON_WORLD.servant.perception;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantPerceptionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;

@EventBusSubscriber(modid = "typemoonworld")
public final class ServantPerceptionService {
   private ServantPerceptionService() { }

   @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
      if (event.getServer().getTickCount() % ServantPerceptionRules.SCAN_INTERVAL != 0) return;
      for (ServerPlayer observer : event.getServer().getPlayerList().getPlayers()) {
         var vars = observer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!vars.servant_card_transformed) {
            ModNetwork.sendToPlayer(observer, new ServantPerceptionMessage(List.of()));
            continue;
         }
         var skills = ServantIdentityHelper.skillIdsOf(observer);
         boolean hasConcealment = ServantPerceptionRules.highestRank(skills, "presence_concealment_") > 0;
         boolean concealmentActive = hasConcealment && net.xxxjk.TYPE_MOON_WORLD.servant.concealment.ServantConcealment.isFullyConcealed(observer);
         double radius = ServantPerceptionRules.radius(skills, concealmentActive);
         List<ServantPerceptionMessage.Target> targets = new ArrayList<>();
         if (radius > 0.0) {
            double radiusSqr = radius * radius;
            for (LivingEntity target : observer.serverLevel().getEntitiesOfClass(LivingEntity.class,
               new AABB(observer.blockPosition()).inflate(radius), entity -> entity != observer
                  && ServantPerceptionRules.isPerceivableTarget(entity)
                  && entity.distanceToSqr(observer) <= radiusSqr)) {
               int color = ServantPerceptionRules.color(observer, target).ordinal();
               targets.add(new ServantPerceptionMessage.Target(target.getId(), color));
               if (targets.size() >= 128) break;
            }
         }
         ModNetwork.sendToPlayer(observer, new ServantPerceptionMessage(targets));
      }
   }
}
