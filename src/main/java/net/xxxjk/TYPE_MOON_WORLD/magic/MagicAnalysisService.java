package net.xxxjk.TYPE_MOON_WORLD.magic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.typemoonworld.api.event.MagicCastEvent;
import net.neoforged.bus.api.SubscribeEvent;

/** Server-side observer for the magic_analysis wheel entry. */
public final class MagicAnalysisService {
   private record CastKey(UUID analyst, UUID caster, String magicId) {}
   private static final Map<CastKey, Long> LAST_CAST = new ConcurrentHashMap<>();
   private MagicAnalysisService() {}

   @SubscribeEvent
   public static void onMagicCast(MagicCastEvent.Post event) {
      if (event.context() == null || event.context().level() == null || event.context().level().isClientSide()) return;
      LivingEntity caster = event.context().caster();
      if (caster == null || !event.result().handled() || !event.result().success()) return;
      String id = event.magicId() == null ? event.context().magicId() : event.magicId().getPath();
      if (id == null || id.isBlank() || !MagicLearningStrategy.canAnalyze(id)) return;
      long tick = caster.level().getGameTime();
      for (ServerPlayer analyst : caster.level().getEntitiesOfClass(ServerPlayer.class, caster.getBoundingBox().inflate(25.0))) {
         var vars = analyst.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!vars.learned_magics.contains("magic_analysis") || !isActive(vars)) continue;
         CastKey castKey = new CastKey(analyst.getUUID(), caster.getUUID(), id);
         long previous = LAST_CAST.getOrDefault(castKey, Long.MIN_VALUE);
         if (previous == tick) continue;
         LAST_CAST.put(castKey, tick);
         double proficiency = MagicProficiencyService.get(vars, "magic_analysis");
         if (MagicLearningStrategy.isDivine(id) && proficiency < 100.0) continue;
         int complexity = MagicLearningStrategy.complexity(id);
         if (MagicLearningStrategy.verses(id) > MagicLearningStrategy.analysisVerseLimit(proficiency)) continue;
         double chance = MagicLearningStrategy.learningChance(id, proficiency);
         if (analyst.getRandom().nextDouble() <= chance) {
            if (!vars.learned_magics.contains(id)) {
               vars.learned_magics.add(id);
               MagicLearningService.awardAnalysisKnowledge(vars, id);
            }
            MagicProficiencyService.add(vars, "magic_analysis", Math.max(0.05, 1.0 - complexity / 100.0));
            analyst.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.typemoonworld.magic.analysis_success", id), true);
         }
         vars.syncPlayerVariables(analyst);
      }
      LAST_CAST.entrySet().removeIf(entry -> entry.getValue() < tick - 200L);
   }

   private static boolean isAnalysisSelected(TypeMoonWorldModVariables.PlayerVariables vars) {
      var current = vars.getCurrentRuntimeWheelEntry();
      if (current != null) return "magic_analysis".equals(current.magicId);
      return vars.current_magic_index >= 0
         && vars.current_magic_index < vars.selected_magics.size()
         && "magic_analysis".equals(vars.selected_magics.get(vars.current_magic_index));
   }

   public static boolean isActive(TypeMoonWorldModVariables.PlayerVariables vars) {
      return vars != null && vars.magic_analysis_active && vars.is_magic_circuit_open && isAnalysisSelected(vars);
   }
}
