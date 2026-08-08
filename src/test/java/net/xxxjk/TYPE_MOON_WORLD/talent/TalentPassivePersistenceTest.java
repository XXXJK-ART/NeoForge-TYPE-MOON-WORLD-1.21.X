package net.xxxjk.TYPE_MOON_WORLD.talent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveRank;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;
import org.junit.jupiter.api.Test;

class TalentPassivePersistenceTest {
   @Test
   void roundTripPreservesTalentsPassivesAndThreshold() {
      Map<String, Double> talents = new HashMap<>();
      talents.put(TalentService.MONSTROUS_STRENGTH, 73.0);
      talents.put(TalentService.CLAIRVOYANCE, 25.0);
      Map<String, PassiveRank> passives = new HashMap<>();
      passives.put(PassiveService.CLAIRVOYANCE, PassiveRank.B);
      passives.put(PassiveService.DIVINITY, PassiveRank.D);
      CompoundTag tag = new CompoundTag();
      TalentPassiveDataCodec.save(tag, talents, passives, 230);

      Map<String, Double> restoredTalents = new HashMap<>();
      Map<String, PassiveRank> restoredPassives = new HashMap<>();
      int threshold = TalentPassiveDataCodec.load(tag, restoredTalents, restoredPassives, 0.0);

      assertEquals(73.0, restoredTalents.get(TalentService.MONSTROUS_STRENGTH));
      assertEquals(PassiveRank.B, restoredPassives.get(PassiveService.CLAIRVOYANCE));
      assertEquals(PassiveRank.D, restoredPassives.get(PassiveService.DIVINITY));
      assertEquals(230, threshold);
      assertEquals(80.0, TalentService.effectiveClairvoyanceProficiency(
         restoredTalents.get(TalentService.CLAIRVOYANCE), restoredPassives.get(PassiveService.CLAIRVOYANCE)));
   }

   @Test
   void clairvoyanceSourceTracksIndependentAndPassiveOwnership() {
      assertTrue(TalentService.hasClairvoyanceSource(false, PassiveRank.E));
      assertFalse(TalentService.hasClairvoyanceSource(false, null));
      assertTrue(TalentService.hasClairvoyanceSource(true, null));
   }

   @Test
   void oldSaveStartsAtAlreadyReachedMartialThreshold() {
      CompoundTag tag = new CompoundTag();
      Map<String, Double> talents = new HashMap<>();
      Map<String, PassiveRank> passives = new HashMap<>();
      int threshold = TalentPassiveDataCodec.load(tag, talents, passives, 179.0);
      assertEquals(170, threshold);
      assertTrue(passives.isEmpty());
   }
}
