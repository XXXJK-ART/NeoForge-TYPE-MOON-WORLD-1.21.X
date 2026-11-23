package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.network.CuChulainnVisualMessage;

/** A cast owns its own sound instance; stopping one Cu never silences another. */
public final class CuChulainnVoiceClient {
   private record Voice(long start,long end,EntityBoundSoundInstance sound) { }
   private static final Map<UUID,Voice> VOICES=new HashMap<>();
   private static final Map<UUID,CuChulainnVisualMessage> PENDING=new HashMap<>();
   private CuChulainnVoiceClient() { }
   public static void begin(CuChulainnVisualMessage message) {
      var mc=Minecraft.getInstance();if(mc.level==null || mc.level.getGameTime()>=message.start()+message.ticks())return;
      Voice previous=VOICES.get(message.owner());if(previous!=null && previous.start()==message.start())return;
      for(var entity:mc.level.entitiesForRendering())if(entity.getUUID().equals(message.owner())) {
         stop(message.owner());
         var sound=new EntityBoundSoundInstance(ModSounds.NP_CHANT_CU_GAE_BOLG.get(),SoundSource.VOICE,1.5F,1,entity,mc.level.random.nextLong());
         VOICES.put(message.owner(),new Voice(message.start(),message.start()+message.ticks(),sound));mc.getSoundManager().play(sound);return;
      }
      if(mc.level.getGameTime()-message.start()<10)PENDING.put(message.owner(),message);
   }
   public static boolean active(UUID owner) {
      Voice voice=VOICES.get(owner);return voice!=null && Minecraft.getInstance().getSoundManager().isActive(voice.sound());
   }
   public static void stop(UUID owner) {
      PENDING.remove(owner);
      Voice voice=VOICES.remove(owner);if(voice!=null)Minecraft.getInstance().getSoundManager().stop(voice.sound());
   }
   public static void tick() {
      var mc=Minecraft.getInstance();if(mc.level==null){clear();return;}
      for(var message:java.util.List.copyOf(PENDING.values())) {
         if(mc.level.getGameTime()-message.start()>=10)PENDING.remove(message.owner());else begin(message);
      }
      for(UUID owner:java.util.List.copyOf(VOICES.keySet()))if(mc.level.getGameTime()>=VOICES.get(owner).end())stop(owner);
   }
   public static void clear(){for(UUID owner:java.util.List.copyOf(VOICES.keySet()))stop(owner);PENDING.clear();}
}
