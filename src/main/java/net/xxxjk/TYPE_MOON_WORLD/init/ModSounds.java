package net.xxxjk.TYPE_MOON_WORLD.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, "typemoonworld");
   public static final DeferredHolder<SoundEvent, SoundEvent> CYM_GEM_BIUBIUBIU = register("cym_gem_biubiubiu");
   public static final DeferredHolder<SoundEvent, SoundEvent> CYQ_GEM_SHOOT_STAR = register("cyq_gem_shoot_star");
   public static final DeferredHolder<SoundEvent, SoundEvent> SASAKI_KOJIRO_VOICE_ATTACK = register("sasaki_kojiro_voice_attack");
   public static final DeferredHolder<SoundEvent, SoundEvent> SASAKI_KOJIRO_VOICE_FAIL = register("sasaki_kojiro_voice_fail");
   public static final DeferredHolder<SoundEvent, SoundEvent> SASAKI_KOJIRO_VOICE_VICTORY = register("sasaki_kojiro_voice_victory");
   public static final DeferredHolder<SoundEvent, SoundEvent> SASAKI_KOJIRO_VOICE_TSURIGAMESHI = register("sasaki_kojiro_voice_tsurigameshi");
   public static final DeferredHolder<SoundEvent, SoundEvent> HERACLES_VOICE_ROAR = register("heracles_voice_roar");
   public static final DeferredHolder<SoundEvent, SoundEvent> HERACLES_VOICE_FAIL = register("heracles_voice_fail");
   public static final DeferredHolder<SoundEvent, SoundEvent> HERACLES_VOICE_VICTORY = register("heracles_voice_victory");
   public static final DeferredHolder<SoundEvent, SoundEvent> CU_CHULAINN_VOICE_ATTACK = register("cu_chulainn_voice_attack");
   public static final DeferredHolder<SoundEvent, SoundEvent> CU_CHULAINN_VOICE_FAIL = register("cu_chulainn_voice_fail");
   public static final DeferredHolder<SoundEvent, SoundEvent> CU_CHULAINN_VOICE_VICTORY = register("cu_chulainn_voice_victory");
   public static final DeferredHolder<SoundEvent, SoundEvent> CU_CHULAINN_VOICE_GAE_BOLG = register("cu_chulainn_voice_gae_bolg");
   public static final DeferredHolder<SoundEvent, SoundEvent> MEDEA_VOICE_ATTACK = register("medea_voice_attack");
   public static final DeferredHolder<SoundEvent, SoundEvent> MEDEA_VOICE_SPELL = register("medea_voice_spell");
   public static final DeferredHolder<SoundEvent, SoundEvent> MEDEA_VOICE_RULE_BREAKER = register("medea_voice_rule_breaker");
   public static final DeferredHolder<SoundEvent, SoundEvent> MEDEA_VOICE_FAIL = register("medea_voice_fail");
   public static final DeferredHolder<SoundEvent, SoundEvent> MEDEA_VOICE_VICTORY = register("medea_voice_victory");
   public static final DeferredHolder<SoundEvent, SoundEvent> MEDUSA_VOICE_ATTACK = register("medusa_voice_attack");
   public static final DeferredHolder<SoundEvent, SoundEvent> MEDUSA_VOICE_BELLEROPHON = register("medusa_voice_bellerophon");
   public static final DeferredHolder<SoundEvent, SoundEvent> MEDUSA_VOICE_FAIL = register("medusa_voice_fail");
   public static final DeferredHolder<SoundEvent, SoundEvent> MEDUSA_VOICE_VICTORY = register("medusa_voice_victory");

   private ModSounds() {
   }

   private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath("typemoonworld", name);
      return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
   }

   public static void register(IEventBus eventBus) {
      SOUND_EVENTS.register(eventBus);
   }
}
