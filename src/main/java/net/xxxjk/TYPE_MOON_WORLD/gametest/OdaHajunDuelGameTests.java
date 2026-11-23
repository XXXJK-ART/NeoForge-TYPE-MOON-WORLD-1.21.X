package net.xxxjk.TYPE_MOON_WORLD.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaHajunDuelService;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaHajunDuelRules.Outcome;

@GameTestHolder("typemoonworld_np_fixes")
@PrefixGameTestTemplate(false)
public final class OdaHajunDuelGameTests {
   private static OdaNobunagaEntity owner(GameTestHelper h) {
      var n = ModEntities.ODA_NOBUNAGA.get().create(h.getLevel());
      n.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(6, 20, 6))));
      n.setNoAi(true); n.setNoGravity(true);
      n.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); n.setHealth(500);
      h.getLevel().addFreshEntity(n); return n;
   }
   private static LivingEntity target(GameTestHelper h, OdaNobunagaEntity owner, int offset) {
      var t = EntityType.HUSK.create(h.getLevel());
      t.setPos(owner.position().add(0,0,offset)); t.setNoAi(true); t.setNoGravity(true);
      t.setPersistenceRequired(); t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); t.setHealth(500);
      h.getLevel().addFreshEntity(t); return t;
   }
   private static void result(GameTestHelper h, Outcome outcome) {
      var n=owner(h); var a=target(h,n,3); var b=target(h,n,5);
      OdaHajunDuelService.resolve(n,List.of(a,b),outcome);
      h.assertTrue(n.isAlive() == (outcome==Outcome.ODA_WINS || outcome==Outcome.NEITHER_WINS), "Caster fate disagrees with dice");
      h.assertTrue(a.isAlive() == (outcome==Outcome.OPPONENTS_WIN || outcome==Outcome.NEITHER_WINS)
         && b.isAlive()==a.isAlive(), "Every opponent must receive the party result");
      if(outcome==Outcome.NEITHER_WINS)h.assertTrue(n.getHealth()==350 && a.getHealth()==350 && b.getHealth()==350,"Draw must leave both sides wounded and alive");
      n.discard(); a.discard(); b.discard(); h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="oda_duel_win")
   public static void odaWinKillsEveryOpponent(GameTestHelper h) { result(h,Outcome.ODA_WINS); }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="oda_duel_loss")
   public static void opponentsWinKillsCaster(GameTestHelper h) { result(h,Outcome.OPPONENTS_WIN); }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="oda_duel_draw")
   public static void drawLeavesBothSidesAlive(GameTestHelper h) { result(h,Outcome.NEITHER_WINS); }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="oda_duel_mutual")
   public static void mutualDeathKillsBothSides(GameTestHelper h) { result(h,Outcome.BOTH_DIE); }
   @GameTest(template="ancient_temple",timeoutTicks=110,batch="oda_duel_timer")
   public static void serverTimerResolvesWithoutParticipantTicks(GameTestHelper h) {
      var n=owner(h); var t=target(h,n,4);
      t.setRemainingFireTicks(100);
      h.assertTrue(OdaHajunDuelService.start(n,List.of(t)),"NPC duel rejected");
      h.runAfterDelay(58,()->{
         h.assertTrue(OdaHajunDuelService.active(n) && OdaHajunDuelService.active(t),"Duel ended before three seconds");
         h.assertTrue(t.getRemainingFireTicks()==100,"Participants kept simulating an unseen battle");
      });
      h.runAfterDelay(102,()->{
         h.assertTrue(!OdaHajunDuelService.active(n) && !OdaHajunDuelService.active(t),"Duel exceeded five seconds");
         h.assertTrue(!n.getPersistentData().getString(OdaHajunDuelService.LAST_OUTCOME).isBlank(),"No outcome was rolled");
         h.assertTrue(!n.isInvisible() && !t.isInvisible() && !n.isInvulnerable() && !t.isInvulnerable(),"Offscreen protections leaked");
         h.assertTrue(n.level()==h.getLevel() && t.level()==h.getLevel(),"An NPC was moved into an unobserved dimension");
         n.discard();t.discard();h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="oda_duel_cancel")
   public static void cancellationRestoresEveryParticipantAndPreviousFlags(GameTestHelper h) {
      var n=owner(h);var a=target(h,n,3);var b=target(h,n,5);
      a.setInvisible(true);a.setInvulnerable(true);a.setNoGravity(false);
      h.assertTrue(OdaHajunDuelService.start(n,List.of(a,b)),"Duel rejected");
      b.discard();
      h.assertTrue(!OdaHajunDuelService.active(n) && !OdaHajunDuelService.active(a),"Removing one participant stranded the rest");
      h.assertTrue(!n.isInvisible() && !n.isInvulnerable() && n.isNoAi() && n.isNoGravity(),"Caster flags changed");
      h.assertTrue(a.isInvisible() && a.isInvulnerable() && !a.isNoGravity(),"Previous target flags were overwritten");
      h.assertTrue(n.isAlive() && a.isAlive(),"Canceled duel applied a dice result");
      n.discard();a.discard();h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="oda_duel_player")
   public static void playersCanNeverEnterOrReceiveAnOffscreenRoll(GameTestHelper h) {
      var n=owner(h);var p=h.makeMockServerPlayerInLevel();p.setPos(n.position().add(0,0,4));
      float health=p.getHealth();
      h.assertTrue(!OdaHajunDuelService.start(n,List.of(p)),"Player entered offscreen simulation");
      OdaHajunDuelService.resolve(n,List.of(p),Outcome.BOTH_DIE);
      h.assertTrue(p.getHealth()==health && n.isAlive() && !OdaHajunDuelService.active(p),"Player received an NPC dice result");
      n.discard();h.getLevel().getServer().getPlayerList().remove(p);p.discard();h.succeed();
   }
}
