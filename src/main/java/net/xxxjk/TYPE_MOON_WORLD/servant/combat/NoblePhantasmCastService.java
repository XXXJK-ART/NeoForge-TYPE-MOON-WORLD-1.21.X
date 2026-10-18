package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.IntConsumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

/** One cast token owns its paid costs, voice window and delayed effects. */
@EventBusSubscriber(modid = "typemoonworld")
public final class NoblePhantasmCastService {
   private record Policy(int voiceTicks) { }
   private record Credit(UUID player, boolean card, double amount) { }
   private static final Map<String, Policy> POLICIES = loadPolicies();
   private static final Map<UUID, Cast> CASTS = new HashMap<>();
   private static final Map<UUID, List<Credit>> PAYMENTS = new HashMap<>();
   private static final ThreadLocal<Cast> EFFECT_CONTEXT = new ThreadLocal<>();

   private static final class Cast {
      final LivingEntity caster;
      final String action;
      final ResourceKey<Level> dimension;
      final String form;
      final long voiceStart;
      final long voiceEnd;
      final long releaseAt;
      final int cooldown;
      final IntConsumer setCooldown;
      final Runnable cleanup;
      final List<Credit> credits = new ArrayList<>();
      double npcPaid;
      boolean cancelled, released, queued;
      Cast(LivingEntity caster, String action, int cooldown, IntConsumer setCooldown, Runnable cleanup) {
         this.caster = caster; this.action = action; this.cooldown = cooldown;
         this.setCooldown = setCooldown; this.cleanup = cleanup;
         dimension = caster.level().dimension();
         form = caster instanceof ServerPlayer p ? p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().id() : "";
         voiceStart = caster.level().getGameTime();
         voiceEnd = voiceStart + POLICIES.get(action).voiceTicks();
         releaseAt = NoblePhantasmCastRules.instant(action) ? voiceStart : NoblePhantasmCastRules.releaseAt(voiceStart, voiceEnd);
      }
   }

   private NoblePhantasmCastService() { }

   private static Map<String, Policy> loadPolicies() {
      try (var input = NoblePhantasmCastService.class.getResourceAsStream("/assets/typemoonworld/np_casts.json")) {
         if (input == null) throw new IllegalStateException("Missing NP release policies");
         Map<String, Policy> result = new HashMap<>();
         var json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
         json.entrySet().forEach(e -> result.put(e.getKey(), new Policy(e.getValue().getAsJsonObject().get("voice_ticks").getAsInt())));
         return Map.copyOf(result);
      } catch (Exception exception) { throw new IllegalStateException("Invalid NP release policies", exception); }
   }

   public static String policyForAction(String effect) {
      String policy = switch (effect) {
         case "bellerophon" -> "medusa_bellerophon";
         case "zabaniya" -> "cursed_arm_hassan_zabaniya";
         case "wu_er_da" -> "li_shuwen_wu_er_da";
         case "lancelot_aroundight" -> "lancelot_arondight";
         case "paracelsus_sword_np" -> "paracelsus_elemental_sword";
         case "muramasa_tsumukari" -> "muramasa_tsumukari";
         case "muramasa_no_gen_kensai" -> "muramasa_summon";
         case "zhao_yun_changbanpo", "zhao_yun_np", "changbanpo_no_ikki_gake" -> "zhao_yun_changbanpo";
         default -> "";
      };
      // Instant card actions keep their normal target validation, result and release voice.
      return NoblePhantasmCastRules.instant(policy) ? "" : policy;
   }
   public static boolean cancelled(LivingEntity caster) {
      Cast cast = CASTS.get(caster.getUUID());
      return cast != null && cast.cancelled;
   }
   public static boolean awaitVoice(LivingEntity caster) {
      Cast cast = CASTS.get(caster.getUUID());
      if (cast == null) return false;
      if (!valid(cast)) return true;
      if (caster.level().getGameTime() < cast.releaseAt) return true;
      cast.released = true;
      return false;
   }
   public static boolean configured(String action) { return POLICIES.containsKey(action); }
   public static long voiceEnd(LivingEntity caster) {
      Cast cast = CASTS.get(caster.getUUID());
      return cast == null ? 0 : cast.voiceEnd;
   }
   public static long releaseAt(LivingEntity caster) {
      Cast cast = CASTS.get(caster.getUUID());
      return cast == null ? 0 : cast.releaseAt;
   }
   public static boolean chanting(LivingEntity caster) {
      Cast cast = CASTS.get(caster.getUUID());
      return cast != null && !cast.cancelled && !cast.released;
   }
   public static boolean inWindow(LivingEntity caster) {
      Cast cast = CASTS.get(caster.getUUID());
      long now = caster.level().getGameTime();
      if (cast == null || cast.cancelled || cast.released || !NoblePhantasmCastRules.interruptible(cast.action)) return false;
      return NoblePhantasmCastRules.interruptibleAt(now, cast.voiceStart, cast.voiceEnd);
   }

   public static long begin(LivingEntity caster, String action, int cooldown, IntConsumer setCooldown, Runnable cleanup) {
      if (!configured(action) || caster.level().isClientSide()) return caster.level().getGameTime();
      Cast previous = CASTS.get(caster.getUUID());
      if (previous != null && !previous.cancelled && !previous.released) return previous.voiceEnd;
      Cast cast = new Cast(caster, action, cooldown, setCooldown, cleanup);
      var payments = PAYMENTS.remove(caster.getUUID());
      if (payments != null) for (Credit credit : payments) {
         if (credit.player() == null) cast.npcPaid += credit.amount(); else cast.credits.add(credit);
      }
      CASTS.put(caster.getUUID(), cast);
      caster.getPersistentData().putInt("TypeMoonNpCastCount", caster.getPersistentData().getInt("TypeMoonNpCastCount") + 1);
      caster.getPersistentData().putString("TypeMoonNpCastAction", action);
      caster.getPersistentData().putLong("TypeMoonNpVoiceEnd", cast.voiceEnd);
      if (!NoblePhantasmCastRules.instant(action) && caster.level() instanceof ServerLevel level) {
         SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("typemoonworld", "np_chant_" + action));
         level.playSound(null, caster.blockPosition(), sound, SoundSource.VOICE, 1.2F, 1.0F);
      }
      return cast.voiceEnd;
   }

   public static void noteNpcPayment(ServantEntity caster, double amount) { notePayment(caster, new Credit(null, false, amount)); }
   public static void notePlayerPayment(ServerPlayer caster, ServerPlayer payer, boolean card, double amount) {
      if (payer != null && amount > 0) notePayment(caster, new Credit(payer.getUUID(), card, amount));
   }
   private static void notePayment(LivingEntity caster, Credit credit) {
      Cast cast = CASTS.get(caster.getUUID());
      if (cast != null && !cast.cancelled && !cast.released) {
         if (credit.player() == null) cast.npcPaid += credit.amount(); else cast.credits.add(credit);
      } else PAYMENTS.computeIfAbsent(caster.getUUID(), unused -> new ArrayList<>()).add(credit);
   }

   /** Returns true when this service has taken ownership of the effect. */
   public static boolean deferEffect(LivingEntity caster, Runnable effect) {
      Cast cast = CASTS.get(caster.getUUID());
      if (cast == null) return false;
      if (!valid(cast)) return true;
      if (cast.released) return false;
      long now = caster.level().getGameTime();
      if (now >= cast.releaseAt) {
         cast.released = true;
         if (NoblePhantasmCastRules.instant(cast.action)) {
            EFFECT_CONTEXT.set(cast);
            try { effect.run(); } finally { EFFECT_CONTEXT.remove(); }
            return true;
         }
         return false;
      }
      if (!cast.queued) {
         cast.queued = true;
         TYPE_MOON_WORLD.queueServerWork((int)(cast.releaseAt - now), () -> {
            if (CASTS.get(caster.getUUID()) != cast || !valid(cast)) return;
            cast.released = true;
            EFFECT_CONTEXT.set(cast);
            try { effect.run(); } finally { EFFECT_CONTEXT.remove(); }
         });
      }
      return true;
   }

   public static Runnable guardDelayedEffect(Runnable effect) {
      Cast cast = EFFECT_CONTEXT.get();
      return cast == null ? effect : () -> {
         if (!valid(cast) || CASTS.get(cast.caster.getUUID()) != cast) return;
         EFFECT_CONTEXT.set(cast);
         try { effect.run(); } finally { EFFECT_CONTEXT.remove(); }
      };
   }

   private static boolean valid(Cast cast) {
      if (cast.cancelled || !cast.caster.isAlive() || cast.caster.isRemoved()
         || !cast.dimension.equals(cast.caster.level().dimension())) return false;
      if (cast.caster instanceof ServerPlayer player) {
         if (player.hasDisconnected()) return false;
         var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!Objects.equals(cast.form, vars.servantCardState().id())) return false;
      }
      return true;
   }

   @SubscribeEvent public static void afterDamage(LivingDamageEvent.Post event) {
      if (event.getNewDamage() > 0 && inWindow(event.getEntity())) cancel(event.getEntity(), true);
   }
   @SubscribeEvent public static void tick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()) return;
      Cast cast = CASTS.get(entity.getUUID());
      if (cast != null && !valid(cast)) cancel(entity, false);
      if (cast != null && (cast.cancelled || cast.released) && entity.level().getGameTime() > cast.voiceEnd + 2400)
         CASTS.remove(entity.getUUID());
      // Payments are only a hand-off between synchronous payment and begin.
      PAYMENTS.remove(entity.getUUID());
   }
   public static void cancel(LivingEntity caster, boolean interrupted) {
      Cast cast = CASTS.get(caster.getUUID());
      if (cast == null || cast.cancelled || cast.released) return;
      cast.cancelled = true;
      cast.cleanup.run();
      if (interrupted) {
         if (caster instanceof ServantEntity servant) servant.setCurrentMp(Math.min(servant.getMaxMp(), servant.getCurrentMp() + cast.npcPaid * 0.5));
         for (Credit credit : cast.credits) {
            ServerPlayer payer = caster.getServer() == null ? null : caster.getServer().getPlayerList().getPlayer(credit.player());
            if (payer == null) continue;
            var vars = payer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (credit.card()) vars.servantCardState().setMana(Math.min(vars.servantCardState().maxMana(), vars.servantCardState().mana() + credit.amount() * 0.5));
            else vars.magicState().setMana(vars.magicState().mana() + credit.amount() * 0.5);
            vars.syncMana(payer);
         }
         cast.setCooldown.accept(NoblePhantasmCastRules.interruptedCooldown(cast.cooldown));
      }
      caster.getPersistentData().remove("TypeMoonNpCastAction");
      caster.getPersistentData().remove("TypeMoonNpVoiceEnd");
      if (caster.level() instanceof ServerLevel level) {
         var stop = new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(
            ResourceLocation.fromNamespaceAndPath("typemoonworld", "np_chant_" + cast.action), SoundSource.VOICE);
         for (ServerPlayer player : level.players()) player.connection.send(stop);
      }
   }

   @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
      cancel(event.getEntity(), false); CASTS.remove(event.getEntity().getUUID()); PAYMENTS.remove(event.getEntity().getUUID());
   }
   @SubscribeEvent public static void dimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
      cancel(event.getEntity(), false);
   }
   @SubscribeEvent public static void death(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
      cancel(event.getEntity(), false);
   }
   @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
      CASTS.clear(); PAYMENTS.clear(); EFFECT_CONTEXT.remove();
   }
   public static boolean suppressLegacyVoice(LivingEntity entity, SoundEvent sound) {
      Cast cast = CASTS.get(entity.getUUID());
      if (cast == null || cast.cancelled || sound == null || entity.level().getGameTime() >= cast.voiceEnd) return false;
      if (NoblePhantasmCastRules.instant(cast.action)) return false;
      String key = BuiltInRegistries.SOUND_EVENT.getKey(sound).getPath();
      if (key.endsWith("_short")) return false;
      return key.contains("voice_np") || key.contains("voice_excalibur") || key.contains("voice_gae_bolg")
         || key.contains("voice_zabaniya") || key.contains("voice_wu_er_da") || key.contains("voice_ea_") || key.contains("voice_bellerophon")
         || key.contains("voice_tsumukari") || key.contains("voice_gallatin") || key.contains("voice_hajun");
   }
}
