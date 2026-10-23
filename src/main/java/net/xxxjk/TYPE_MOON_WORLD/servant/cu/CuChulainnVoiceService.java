package net.xxxjk.TYPE_MOON_WORLD.servant.cu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService;

/** One voice gate for weapon skills, ordinary attacks and both incarnations. */
public final class CuChulainnVoiceService {
   private static final String END="CuVoiceUntil";
   private CuChulainnVoiceService() { }
   public static void attack(LivingEntity owner){play(owner,ModSounds.CU_CHULAINN_VOICE_ATTACK.get(),35,false);}
   public static void victory(LivingEntity owner){play(owner,ModSounds.CU_CHULAINN_VOICE_VICTORY.get(),180,false);}
   public static void death(LivingEntity owner){play(owner,ModSounds.CU_CHULAINN_VOICE_FAIL.get(),180,true);}
   public static void chant(LivingEntity owner){
      owner.getPersistentData().putLong(END,NoblePhantasmCastService.voiceEnd(owner));
      if(owner.level() instanceof ServerLevel level)for(var player:level.players())player.connection.send(
         new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(ModSounds.CU_CHULAINN_VOICE_ATTACK.get().getLocation(),SoundSource.VOICE));
      CuChulainnVisualService.voice(owner,CuChulainnRules.CHANT_VOICE,(int)(NoblePhantasmCastService.voiceEnd(owner)-owner.level().getGameTime()));
   }
   public static boolean locked(LivingEntity owner){return owner.level().getGameTime()<owner.getPersistentData().getLong(END);}
   public static void cancelChant(LivingEntity owner) {
      owner.getPersistentData().remove(END);CuChulainnVisualService.voice(owner,CuChulainnRules.STOP_CHANT_VOICE,0);
   }
   private static void play(LivingEntity owner,SoundEvent sound,int ticks,boolean force) {
      if(!(owner.level() instanceof ServerLevel level))return;
      long now=level.getGameTime();
      if(!force && (NoblePhantasmCastService.chanting(owner) || now<owner.getPersistentData().getLong(END)))return;
      owner.getPersistentData().putLong(END,now+ticks);
      level.playSound(null,owner.getX(),owner.getY(),owner.getZ(),sound,SoundSource.VOICE,1.15F,1);
   }
}
