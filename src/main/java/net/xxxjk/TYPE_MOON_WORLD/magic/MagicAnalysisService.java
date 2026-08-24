package net.xxxjk.TYPE_MOON_WORLD.magic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcMagicCastBridge;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.AdvancedPassiveService;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;
import net.xxxjk.typemoonworld.api.event.MagicCastEvent;
import net.neoforged.bus.api.SubscribeEvent;

/** Server-side observer for the magic_analysis wheel entry. */
public final class MagicAnalysisService {
   private static final int MIN_PLAYER_ANALYSIS_TICKS = 20;
   private record CastKey(UUID analyst, UUID caster, String magicId) {}
   private record AnalysisTask(String magicId, int complexity, double workDone, double totalWork) {
      AnalysisTask advance(double work) {
         return new AnalysisTask(this.magicId, this.complexity, this.workDone + Math.max(1.0, work), this.totalWork);
      }
   }
   private static final Map<CastKey, Long> LAST_CAST = new ConcurrentHashMap<>();
   private static final Map<UUID, AnalysisTask> TASKS = new ConcurrentHashMap<>();
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
         if (analyst == caster) continue;
         var vars = analyst.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!vars.learned_magics.contains("magic_analysis") || !isActive(vars)) continue;
         CastKey castKey = new CastKey(analyst.getUUID(), caster.getUUID(), id);
         long previous = LAST_CAST.getOrDefault(castKey, Long.MIN_VALUE);
         if (previous == tick) continue;
         LAST_CAST.put(castKey, tick);
         if (TASKS.containsKey(analyst.getUUID())) continue;
         double proficiency = AdvancedPassiveService.effectiveMagicAnalysisProficiency(vars);
         if (!MagicLearningStrategy.learningRequirementsMet(vars, id)) continue;
         if (MagicLearningStrategy.isDivine(id) && proficiency < 100.0) continue;
         int complexity = MagicLearningStrategy.complexity(id);
         if (MagicLearningStrategy.verses(id) > MagicLearningStrategy.analysisVerseLimit(proficiency)) continue;
         TASKS.put(analyst.getUUID(), new AnalysisTask(id, complexity, 0.0, playerWorkTarget(vars, complexity)));
         analyst.displayClientMessage(Component.translatable("message.typemoonworld.magic.analysis_started", id), true);
      }
      for (MysticMagicianEntity analyst : caster.level().getEntitiesOfClass(
         MysticMagicianEntity.class,
         caster.getBoundingBox().inflate(25.0),
         npc -> npc != caster && npc.isAlive() && !npc.isRemoved())) {
         var vars = analyst.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!vars.learned_magics.contains("magic_analysis") || !isActive(analyst, vars)) continue;
         CastKey castKey = new CastKey(analyst.getUUID(), caster.getUUID(), id);
         long previous = LAST_CAST.getOrDefault(castKey, Long.MIN_VALUE);
         if (previous == tick) continue;
         LAST_CAST.put(castKey, tick);
         double proficiency = MagicProficiencyService.get(vars, "magic_analysis");
         if (!MagicLearningStrategy.learningRequirementsMet(vars, id)) continue;
         if (MagicLearningStrategy.isDivine(id) && proficiency < 100.0) continue;
         int complexity = MagicLearningStrategy.complexity(id);
         if (MagicLearningStrategy.verses(id) > MagicLearningStrategy.analysisVerseLimit(proficiency)) continue;
         double chance = MagicLearningStrategy.learningChance(id, proficiency) * 0.85;
         if (analyst.getRandom().nextDouble() <= chance) {
            if (NpcMagicCastBridge.grantTemporaryAnalyzedMagic(analyst, id, proficiency, tick)) {
               MagicProficiencyService.add(vars, "magic_analysis", Math.max(0.04, 0.85 - complexity / 120.0));
            }
         }
      }
      LAST_CAST.entrySet().removeIf(entry -> entry.getValue() < tick - 200L);
   }

   public static void tick(ServerPlayer player) {
      if (player == null) return;
      AnalysisTask task = TASKS.get(player.getUUID());
      if (task == null) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!player.isAlive() || !isActive(vars) || PassiveService.effectsSuppressed(vars)) {
         cancel(player);
         return;
      }
      int split = AdvancedPassiveService.partitionN(vars);
      if (split > 0) {
         player.causeFoodExhaustion(Math.max(0.01F, 0.015F * split));
      }
      AnalysisTask advanced = task.advance(AdvancedPassiveService.analysisWorkPerTick(vars));
      double analysisProficiency = AdvancedPassiveService.effectiveMagicAnalysisProficiency(vars);
      MagicLearningProgressService.addWithLearningCheck(player, advanced.magicId,
         analysisProficiency * analysisProficiency * 0.1D);
      if (advanced.workDone < advanced.totalWork) {
         TASKS.put(player.getUUID(), advanced);
         if (player.tickCount % 5 == 0) {
            int percent = (int)Math.min(99.0, Math.floor(advanced.workDone * 100.0 / advanced.totalWork));
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.analysis_progress", advanced.magicId, percent), true);
         }
         return;
      }
      TASKS.remove(player.getUUID());
      MagicProficiencyService.add(vars, "magic_analysis", Math.max(0.05, 1.0 - advanced.complexity / 100.0));
      player.displayClientMessage(Component.translatable("message.typemoonworld.magic.analysis_completed", advanced.magicId), true);
      vars.syncPlayerVariables(player);
   }

   public static void cancel(ServerPlayer player) {
      if (player != null) TASKS.remove(player.getUUID());
   }

   public static int baseWork(int complexity) {
      int clamped = Math.max(10, Math.min(100, complexity));
      return (int)Math.ceil(20.0 * Math.pow(600.0, (clamped - 10.0) / 90.0));
   }

   private static double playerWorkTarget(TypeMoonWorldModVariables.PlayerVariables vars, int complexity) {
      int workPerTick = Math.max(1, AdvancedPassiveService.analysisWorkPerTick(vars));
      return Math.max(baseWork(complexity), (double)workPerTick * MIN_PLAYER_ANALYSIS_TICKS);
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

   public static boolean isActive(LivingEntity analyst, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (analyst instanceof MysticMagicianEntity) {
         return vars != null
            && vars.magic_analysis_active
            && vars.is_magic_circuit_open
            && vars.learned_magics.contains("magic_analysis");
      }
      return isActive(vars);
   }
}
