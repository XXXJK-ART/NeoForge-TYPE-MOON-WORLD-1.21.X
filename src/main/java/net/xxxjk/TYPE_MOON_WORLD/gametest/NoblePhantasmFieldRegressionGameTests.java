package net.xxxjk.TYPE_MOON_WORLD.gametest;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.gametest.*;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.cu.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.*;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;

@GameTestHolder("typemoonworld_np_fixes")
@PrefixGameTestTemplate(false)
public final class NoblePhantasmFieldRegressionGameTests {
   private static final java.util.List<Consumer<EntityTravelToDimensionEvent>> LISTENERS=new java.util.ArrayList<>();
   private static Vec3 arena(GameTestHelper h) {
      LISTENERS.forEach(NeoForge.EVENT_BUS::unregister);LISTENERS.clear();
      var previous=new java.util.ArrayList<Entity>();
      for(var level:h.getLevel().getServer().getAllLevels())for(var e:level.getEntities().getAll())
         if(e.getTags().contains("np_fix_fixture"))previous.add(e);
      previous.forEach(Entity::discard);
      Vec3 center = h.absolutePos(new BlockPos(6, 100, 6)).getCenter();
      var chunk = new net.minecraft.world.level.ChunkPos(BlockPos.containing(center));
      for (int x=-3;x<=3;x++) for (int z=-3;z<=3;z++) h.getLevel().setChunkForced(chunk.x+x,chunk.z+z,true);
      return center;
   }
   private static LivingEntity target(GameTestHelper h, Vec3 pos) {
      var t=EntityType.HUSK.create(h.getLevel());t.setPos(pos);t.setNoAi(true);t.setNoGravity(true);t.setPersistenceRequired();
      t.addTag("np_fix_fixture");t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000);t.setHealth(2000);h.getLevel().addFreshEntity(t);return t;
   }
   private static ServerPlayer player(GameTestHelper h) {
      var p=h.makeMockServerPlayerInLevel();p.setPos(arena(h));p.setNoGravity(true);
      p.addTag("np_fix_fixture");
      var v=p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);v.servant_card_transformed=true;v.servant_card_id="oda_nobunaga";
      v.servantCardState().setMaxMana(1000);v.servantCardState().setMana(500);return p;
   }
   private static int playerRelease(GameTestHelper h, ServerPlayer p) {
      var v=p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      h.assertTrue(ServantCardOdaNobunagaSkills.performOdaHajunAction(p,v,ServantCardSkillLayout.actionFor("oda_nobunaga",9,false)),"Hajun startup rejected");
      int release=(int)(NoblePhantasmCastService.releaseAt(p)-h.getLevel().getGameTime())+1;
      h.runAfterDelay(release,()->{
         p.getPersistentData().putInt("ServantCardOdaHajunCardProgress",3);
         ServantCardOdaNobunagaSkills.performOdaHajunAction(p,v,ServantCardSkillLayout.actionFor("oda_nobunaga",9,false));
      });return release;
   }
   private static void assertPlayerRefund(GameTestHelper h, ServerPlayer p) {
      var v=p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      h.assertTrue(p.level()==h.getLevel(),"Caster entered alone");
      h.assertTrue(Math.abs(v.servantCardState().mana()-500)<.01,"Both Hajun installments were not refunded: "+v.servantCardState().mana());
      h.assertTrue(v.servantCardState().npCooldown()==0,"Failed entry retained cooldown");
      h.assertTrue(!p.getPersistentData().getBoolean("OdaHajunEntryPending") && !p.getPersistentData().contains("ServantCardOdaHajunFieldActiveUntil"),"Failed field state remained armed");
      h.assertTrue(NoblePhantasmCastService.cancelled(p),"Failed cast token remained live");
      NoblePhantasmCastService.failHajunEntry(p);
      h.assertTrue(Math.abs(v.servantCardState().mana()-500)<.01,"Failure refunded twice");
   }
   private static Consumer<EntityTravelToDimensionEvent> deny(UUID id) {
      Consumer<EntityTravelToDimensionEvent> listener=e->{if(e.getEntity().getUUID().equals(id) && e.getDimension().equals(ModDimensions.HAJUN_KEY))e.setCanceled(true);};
      NeoForge.EVENT_BUS.addListener(listener);LISTENERS.add(listener);return listener;
   }
   @GameTest(template="ancient_temple",timeoutTicks=120,batch="np_fix_flight")
   public static void flightDamagesServantWithoutConsumingOrdinaryEvasion(GameTestHelper h) {
      var owner=ModEntities.CU_CHULAINN.get().create(h.getLevel());owner.setPos(arena(h));owner.setNoAi(true);owner.setNoGravity(true);
      owner.addTag("np_fix_fixture");
      owner.setYRot(0);owner.setXRot(0);h.getLevel().addFreshEntity(owner);owner.ensureDefaultNpcLoadout(false);owner.setCurrentMp(600);owner.setOnGround(true);
      var t=ModEntities.DIARMUID_UA_DUIBHNE.get().create(h.getLevel());t.setPos(owner.position().add(0,0,12));t.setNoAi(true);t.setNoGravity(true);
      t.addTag("np_fix_fixture");
      t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000);t.setHealth(2000);h.getLevel().addFreshEntity(t);
      ServantCombatDefenseService.initialize(t);int charges=t.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES);
      h.assertTrue(charges>0,"Fixture lacks evasion charges");
      t.getPersistentData().putLong(ServantCombatDefenseService.TAG_DODGE_COOLDOWN,h.getLevel().getGameTime()+200);
      h.assertTrue(CuChulainnWeaponService.noble(owner,true,40),"Flight rejected");
      int release=(int)(NoblePhantasmCastService.releaseAt(owner)-h.getLevel().getGameTime());
      h.runAfterDelay(release+50,()->{
         h.assertTrue(t.getHealth()<1900,"Committed flight dealt no meaningful servant damage: "+t.getHealth());
         h.assertTrue(t.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES)==charges,"NP splits were mistaken for ordinary projectiles");
         CuChulainnAbilityService.clear(owner);t.discard();owner.discard();h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=130,batch="np_fix_empty")
   public static void emptyPlayerFieldRefundsBothPayments(GameTestHelper h) {
      var p=player(h);int release=playerRelease(h,p);
      h.runAfterDelay(release+45,()->{assertPlayerRefund(h,p);h.succeed();});
   }
   @GameTest(template="ancient_temple",timeoutTicks=130,batch="np_fix_escape")
   public static void targetLeavingDuringTransitionPreventsSoloEntry(GameTestHelper h) {
      var p=player(h);var t=target(h,p.position().add(0,0,8));int release=playerRelease(h,p);
      h.runAfterDelay(release+2,()->t.teleportTo(p.getX(),p.getY(),p.getZ()+90));
      h.runAfterDelay(release+45,()->{assertPlayerRefund(h,p);t.discard();h.succeed();});
   }
   @GameTest(template="ancient_temple",timeoutTicks=130,batch="np_fix_target_rejected")
   public static void rejectedTargetTransitionPreventsSoloEntry(GameTestHelper h) {
      var p=player(h);var t=target(h,p.position().add(0,0,8));var listener=deny(t.getUUID());int release=playerRelease(h,p);
      h.runAfterDelay(release+45,()->{
         NeoForge.EVENT_BUS.unregister(listener);assertPlayerRefund(h,p);
         h.assertTrue(t.level()==h.getLevel() && !t.getPersistentData().hasUUID("ServantCardOdaHajunFieldOwner"),"Rejected target retained return state");
         t.discard();h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=130,batch="np_fix_caster_rejected")
   public static void rejectedCasterTransitionReturnsAlreadyMovedTargets(GameTestHelper h) {
      var p=player(h);var t=target(h,p.position().add(0,0,8));UUID id=t.getUUID();var listener=deny(p.getUUID());int release=playerRelease(h,p);
      h.runAfterDelay(release+45,()->{
         NeoForge.EVENT_BUS.unregister(listener);assertPlayerRefund(h,p);var returned=h.getLevel().getEntity(id);
         h.assertTrue(returned instanceof LivingEntity && !returned.getPersistentData().hasUUID("ServantCardOdaHajunFieldOwner"),"Moved target was stranded after caster rejection");
         returned.discard();h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=130,batch="np_fix_player_sphere")
   public static void playerRecapturesLateTargetsBelowAndBeyondOldRadius(GameTestHelper h) {
      var p=player(h);var starter=target(h,p.position().add(0,0,8));var outside=target(h,p.position().add(49,0,0));
      int release=playerRelease(h,p);UUID[] late={null};
      h.runAfterDelay(release+2,()->starter.teleportTo(p.getX(),p.getY(),p.getZ()+90));
      h.runAfterDelay(release+10,()->late[0]=target(h,p.position().add(0,-12,40)).getUUID());
      h.runAfterDelay(release+45,()->{
         h.assertTrue(p.level().dimension().equals(ModDimensions.HAJUN_KEY),"Player did not enter with the late target");
         h.assertTrue(p.serverLevel().getEntity(late[0]) instanceof LivingEntity,"Full sphere failed to pull lower/farther target");
         h.assertTrue(outside.level()==h.getLevel() && !outside.isRemoved(),"Sphere pulled a target beyond 48 blocks");
         h.assertTrue(ServantCardOdaNobunagaSkills.returnFromOdaHajunField(p),"Player could not leave field");
         var returned=h.getLevel().getEntity(late[0]);h.assertTrue(returned!=null,"Pulled target did not return");
         returned.discard();starter.discard();outside.discard();h.succeed();
      });
   }
   private static OdaNobunagaEntity npc(GameTestHelper h) {
      var n=ModEntities.ODA_NOBUNAGA.get().create(h.getLevel());n.setPos(arena(h));n.setNoAi(true);n.setNoGravity(true);
      n.addTag("np_fix_fixture");
      n.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);n.setHealth(500);n.setCurrentMp(500);h.getLevel().addFreshEntity(n);
      n.getPersistentData().putLong(OdaNobunagaCombatHelper.TAG_LAND_FOR_NP_UNTIL,h.getLevel().getGameTime()+500);return n;
   }
   private static int npcChant(GameTestHelper h,OdaNobunagaEntity n,LivingEntity t) {
      t.getPersistentData().putString("ServantCardTraitKeys","divine");n.setTarget(t);OdaNobunagaCombatHelper.tick(n);
      n.setNoGravity(true);n.setDeltaMovement(Vec3.ZERO);
      h.assertTrue(n.getPersistentData().getLong(OdaNobunagaCombatHelper.TAG_HAJUN_CHANT_END)>0,"NPC did not start Hajun");
      return (int)(n.getPersistentData().getLong(OdaNobunagaCombatHelper.TAG_HAJUN_CHANT_END)-h.getLevel().getGameTime());
   }
   @GameTest(template="ancient_temple",timeoutTicks=200,batch="np_fix_npc_rejected")
   public static void npcOffscreenDuelIgnoresDimensionTravelAndPaysFullCost(GameTestHelper h) {
      var n=npc(h);var t=target(h,n.position().add(0,0,10));var listener=deny(t.getUUID());int end=npcChant(h,n,t);
      h.assertTrue(Math.abs(n.getCurrentMp()-465)<.01,"NPC startup did not cost 35 MP");
      for(int tick=1;tick<=end+1;tick++)h.runAfterDelay(tick,()->{if(!n.isRemoved())OdaNobunagaCombatHelper.tickPersistentState(n);});
      h.runAfterDelay(end-1,()->{
         double before=n.getCurrentMp();
         n.getPersistentData().putLong(OdaNobunagaCombatHelper.TAG_HAJUN_CHANT_END,h.getLevel().getGameTime());
         n.getPersistentData().remove("OdaLastPersistentStateTick");
         OdaNobunagaCombatHelper.tickPersistentState(n);
         h.assertTrue(Math.abs(n.getCurrentMp()-(before-95))<.01,"NPC release did not cost 95 MP");
      });
      h.runAfterDelay(end+3,()->{
         NeoForge.EVENT_BUS.unregister(listener);h.assertTrue(!n.isRemoved() && n.level()==h.getLevel(),"NPC entered alone");
         h.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaHajunDuelService.active(n) && net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaHajunDuelService.active(t),"NPC battle attempted dimension travel instead of rolling");
         h.assertTrue(n.getPersistentData().getLong("OdaSharedNpUntil")>h.getLevel().getGameTime(),"Offscreen duel lost the normal NP cooldown");
         t.discard();n.discard();h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=300,batch="np_fix_npc_sphere")
   public static void npcPullsTargetsBelowAndBeyondOldHemisphere(GameTestHelper h) {
      var n=npc(h);var t=target(h,n.position().add(0,-12,40));var outside=target(h,n.position().add(49,0,0));int end=npcChant(h,n,t);
      for(int tick=1;tick<=end+1;tick++)h.runAfterDelay(tick,()->{if(!n.isRemoved())OdaNobunagaCombatHelper.tickPersistentState(n);});
      h.runAfterDelay(end+3,()->{
         h.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaHajunDuelService.active(n) && net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaHajunDuelService.active(t),"Full sphere missed the lower/farther NPC target");
         h.assertTrue(!net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaHajunDuelService.active(outside),"Sphere pulled an NPC beyond 48 blocks");
      });
      h.runAfterDelay(end+103,()->{
         h.assertTrue(!net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaHajunDuelService.active(n) && !net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaHajunDuelService.active(t),"Unobserved battle did not return in five seconds");
         h.assertTrue(!n.getPersistentData().getString(net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaHajunDuelService.LAST_OUTCOME).isBlank(),"Unobserved NPC battle never rolled");
         n.discard();t.discard();outside.discard();h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=250,batch="np_fix_npc_with_player")
   public static void npcWithCapturedSurvivalPlayerStillUsesRealHajun(GameTestHelper h) {
      var n=npc(h);UUID casterId=n.getUUID();
      var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(
         new com.mojang.authlib.GameProfile(UUID.randomUUID(),"oda-survival-test"),false);
      var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
      var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
      new io.netty.channel.embedded.EmbeddedChannel(connection);
      h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
      p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setPos(n.position().add(0,0,8));
      p.setNoGravity(true);p.setInvulnerable(true);p.addTag("np_fix_fixture");
      int end=npcChant(h,n,p);
      for(int tick=1;tick<=end+1;tick++)h.runAfterDelay(tick,()->{if(!n.isRemoved())OdaNobunagaCombatHelper.tickPersistentState(n);});
      h.runAfterDelay(end+15,()->{
         h.assertTrue(p.level().dimension().equals(ModDimensions.HAJUN_KEY),"Captured player did not enter the real field");
         h.assertTrue(!net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaHajunDuelService.active(p),"Captured player received an offscreen battle");
         var moved=p.serverLevel().getEntity(casterId);
         h.assertTrue(moved instanceof OdaNobunagaEntity,"NPC did not enter with the captured player");
         OdaNobunagaCombatHelper.cleanupHajun((OdaNobunagaEntity)moved);
         h.assertTrue(p.level()==h.getLevel(),"Captured player was not returned");
         var returned=h.getLevel().getEntity(casterId);if(returned!=null)returned.discard();
         h.getLevel().getServer().getPlayerList().remove(p);p.discard();h.succeed();
      });
   }
}
