package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class CombatThreatService {
   private static final Map<ResourceKey<Level>, List<CombatThreat>> THREATS = new HashMap<>();
   private static final int MAX_THREATS_PER_LEVEL = 256;

   private CombatThreatService() { }

   public static void publish(ServerLevel level, CombatThreat threat) {
      if (level == null || threat == null) return;
      List<CombatThreat> threats = THREATS.computeIfAbsent(level.dimension(), ignored -> new ArrayList<>());
      if (threats.size() >= MAX_THREATS_PER_LEVEL) threats.remove(0);
      threats.add(threat);
   }

   public static CombatThreat publishWindup(LivingEntity caster, LivingEntity target, ResourceLocation actionId,
                                             int windupTicks, boolean ranged, int danger) {
      if (!(caster.level() instanceof ServerLevel level)) return null;
      long now = level.getGameTime();
      Vec3 aim = target == null ? caster.getLookAngle() : target.getEyePosition().subtract(caster.getEyePosition());
      CombatThreat threat = new CombatThreat(actionId, caster.getUUID(), target == null ? null : target.getUUID(),
         caster.getEyePosition(), aim, ranged ? CombatThreat.Shape.LINE : CombatThreat.Shape.SPHERE,
         ranged ? 1.5 : 4.0, ranged ? 48.0 : 4.0, Math.max(1, danger), now, now + windupTicks,
         now + windupTicks + 8L, !ranged, true, true);
      publish(level, threat);
      return threat;
   }

   public static List<CombatThreat> nearby(ServerLevel level, Vec3 point, double range, long now) {
      List<CombatThreat> source = THREATS.get(level.dimension());
      if (source == null || source.isEmpty()) return List.of();
      double rangeSqr = range * range;
      List<CombatThreat> result = new ArrayList<>();
      for (CombatThreat threat : source) {
         if (threat.endTick() >= now && threat.origin().distanceToSqr(point) <= rangeSqr) result.add(threat);
      }
      return result;
   }

   public static CombatThreat incoming(ServerLevel level, LivingEntity target, long now, long maximumTicks,
                                       Predicate<CombatThreat> filter) {
      if (level == null || target == null) return null;
      CombatThreat best = null;
      for (CombatThreat threat : nearby(level, target.position(), 64.0, now)) {
         if (threat.sourceUuid().equals(target.getUUID()) || threat.ticksToImpact(now) > maximumTicks
            || filter != null && !filter.test(threat)) continue;
         boolean targeted = target.getUUID().equals(threat.targetUuid());
         if (!targeted && !threat.threatens(target.getEyePosition(), target.getBbWidth() * 0.65)) continue;
         if (best == null || threat.ticksToImpact(now) < best.ticksToImpact(now)
            || threat.ticksToImpact(now) == best.ticksToImpact(now) && threat.danger() > best.danger()) best = threat;
      }
      return best;
   }

   @SubscribeEvent
   public static void tick(LevelTickEvent.Post event) {
      if (!(event.getLevel() instanceof ServerLevel level)) return;
      if (level.getGameTime() % 20L != 0L) return;
      List<CombatThreat> threats = THREATS.get(level.dimension());
      if (threats == null) return;
      long now = level.getGameTime();
      threats.removeIf(threat -> threat.endTick() < now);
      if (threats.isEmpty()) THREATS.remove(level.dimension());
   }

   @SubscribeEvent
   public static void unload(LevelEvent.Unload event) {
      if (event.getLevel() instanceof Level level && !level.isClientSide()) THREATS.remove(level.dimension());
   }
}
