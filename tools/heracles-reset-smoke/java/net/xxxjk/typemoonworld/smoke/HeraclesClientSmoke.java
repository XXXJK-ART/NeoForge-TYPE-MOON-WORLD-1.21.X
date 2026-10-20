package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.heracles.*;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;

@Mod(value="typemoonworld_heracles_smoke",dist=Dist.CLIENT)
public final class HeraclesClientSmoke {
   private static final String WORLD="Heracles-QA";
   private boolean opened,done;private int ticks;private volatile String failure;private volatile boolean verified;
   private UUID target;
   public HeraclesClientSmoke() { NeoForge.EVENT_BUS.register(this); }
   @SubscribeEvent public void tick(ClientTickEvent.Post event) {
      var mc=Minecraft.getInstance();if(done||mc.getOverlay()!=null)return;
      try {
         if(failure!=null)throw new AssertionError(failure);
         if(mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) { mc.options.onboardingAccessibilityFinished();mc.setScreen(new TitleScreen()); }
         if(mc.screen instanceof TitleScreen&&!opened) {
            opened=true;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.simulationDistance().set(4);mc.getWindow().setWindowed(1280,720);
            if(new File(mc.gameDirectory,"saves/"+WORLD+"/level.dat").isFile())mc.createWorldOpenFlows().openWorld(WORLD,()->{throw new IllegalStateException("QA save open failed");});
            else mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(42,false,false),registry->registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
         }
         if(mc.player==null||mc.getSingleplayerServer()==null)return;
         ticks++;
         if(ticks==10) {
            for(int i=0;i<9;i++)require(PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("typemoonworld","heracles_action_"+i))!=null,"missing player animation "+i);
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            server(mc,()->setup(mc));
         }
         if(ticks==22)capture(mc,"heracles-flurry");
         if(ticks==70)server(mc,()->{
            var level=mc.getSingleplayerServer().overworld();var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            var victim=(net.minecraft.world.entity.LivingEntity)level.getEntity(target);
            require(victim!=null&&Math.abs(victim.getHealth()-934)<.1,"real tick flurry did not deal 66");
            player.hurt(player.damageSources().source(NeoForgeMod.POISON_DAMAGE),10000);
            require(player.isAlive()&&GodHandService.read(player).lives()==10,"real player revival failed");verified=true;
         });
         if(ticks==90)capture(mc,"heracles-hud-revival");
         if(ticks>=110) {require(verified,"server checks incomplete");done=true;TYPE_MOON_WORLD.LOGGER.info("HERACLES_CLIENT_SMOKE_PASS animations=9 flurry=66 revival=in_place hud=rendered");mc.stop();}
      } catch(Throwable error) {done=true;TYPE_MOON_WORLD.LOGGER.error("HERACLES_CLIENT_SMOKE_FAIL ticks="+ticks,error);mc.stop();}
   }
   private void setup(Minecraft mc) {
      var server=mc.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayer(mc.player.getUUID());
      player.setGameMode(GameType.SURVIVAL);var vars=player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if(vars.servant_card_transformed)ServantCardTransformManager.release(player,false);
      vars.servantCardState().setTransformCooldown(0);vars.servantCardState().setReleaseCooldown(0);
      level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);level.setDayTime(6000);
      var previous=new java.util.ArrayList<net.minecraft.world.entity.Entity>();
      for(var entity:level.getAllEntities())if(entity instanceof net.minecraft.world.entity.Mob)previous.add(entity);
      previous.forEach(net.minecraft.world.entity.Entity::discard);
      for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++) {level.setBlock(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState(),2);for(int y=101;y<110;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
      player.teleportTo(.5,101,.5);player.setYRot(0);player.setXRot(0);require(ServantCardTransformManager.transform(player,"heracles"),"transform failed");
      player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
      require(player.getMaxHealth()==680&&player.getAttributeValue(Attributes.ATTACK_DAMAGE)==72,"real player common attributes");
      var victim=EntityType.HUSK.create(level);victim.setPos(.5,101,2.5);victim.setNoAi(true);victim.setNoGravity(true);victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);victim.getAttribute(Attributes.ARMOR).setBaseValue(0);victim.setHealth(1000);level.addFreshEntity(victim);target=victim.getUUID();
      require(HeraclesAbilityService.start(player,7),"real player flurry start failed");
      var npc=net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.HERACLES.get().create(level);
      npc.readAdditionalSaveData(npc.saveWithoutId(new net.minecraft.nbt.CompoundTag()));npc.setNoAi(true);npc.setPos(3.5,101,3.5);npc.setYRot(180);level.addFreshEntity(npc);npc.triggerNamedActionAnimation("martial_7");
   }
   private void server(Minecraft mc,Runnable work) {mc.getSingleplayerServer().execute(()->{try{work.run();}catch(Throwable error){failure=error.toString();}});}
   private static void require(boolean value,String message) {if(!value)throw new AssertionError(message);}
   private static void capture(Minecraft mc,String name) {File directory=new File(mc.gameDirectory,"qa");directory.mkdirs();Screenshot.grab(directory,name+".png",mc.getMainRenderTarget(),ignored->{});}
}
