package net.xxxjk.TYPE_MOON_WORLD.talent;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveRank;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;

public final class TalentPassiveDataCodec {
   public static final String TALENTS_KEY = "talent_proficiencies";
   public static final String PASSIVES_KEY = "passive_ranks";
   public static final String THRESHOLD_KEY = "martial_passive_last_threshold";
   public static final String MAGIC_THRESHOLD_KEY = "magic_passive_last_threshold";

   private TalentPassiveDataCodec() {
   }

   public static void save(CompoundTag root, Map<String, Double> talents, Map<String, PassiveRank> passives, int threshold) {
      save(root, talents, passives, threshold, 290);
   }

   public static void save(CompoundTag root, Map<String, Double> talents, Map<String, PassiveRank> passives, int threshold, int magicThreshold) {
      CompoundTag talentTag = new CompoundTag();
      talents.forEach((id, value) -> {
         if (TalentService.isTalent(id)) talentTag.putDouble(id, clamp(value));
      });
      root.put(TALENTS_KEY, talentTag);

      CompoundTag passiveTag = new CompoundTag();
      passives.forEach((id, rank) -> {
         if (PassiveService.isPassive(id) && rank != null) passiveTag.putString(id, rank.name());
      });
      root.put(PASSIVES_KEY, passiveTag);
      root.putInt(THRESHOLD_KEY, Math.max(140, threshold));
      root.putInt(MAGIC_THRESHOLD_KEY, Math.max(290, magicThreshold));
   }

   public static int load(CompoundTag root, Map<String, Double> talents, Map<String, PassiveRank> passives, double totalMartial) {
      talents.clear();
      if (root.contains(TALENTS_KEY, 10)) {
         CompoundTag talentTag = root.getCompound(TALENTS_KEY);
         for (String id : talentTag.getAllKeys()) {
            if (TalentService.isTalent(id)) talents.put(id, clamp(talentTag.getDouble(id)));
         }
      }

      passives.clear();
      if (root.contains(PASSIVES_KEY, 10)) {
         CompoundTag passiveTag = root.getCompound(PASSIVES_KEY);
         for (String id : passiveTag.getAllKeys()) {
            PassiveRank rank = PassiveRank.parse(passiveTag.getString(id));
            if (PassiveService.isPassive(id) && rank != null) passives.put(id, rank);
         }
      }

      return root.contains(THRESHOLD_KEY)
         ? Math.max(140, root.getInt(THRESHOLD_KEY))
         : totalMartial >= 150.0 ? (int)Math.floor(totalMartial / 10.0) * 10 : 140;
   }

   public static int loadMagicThreshold(CompoundTag root, double totalMagic) {
      return root.contains(MAGIC_THRESHOLD_KEY)
         ? Math.max(290, root.getInt(MAGIC_THRESHOLD_KEY))
         : totalMagic >= 300.0 ? (int)Math.floor(totalMagic / 10.0) * 10 : 290;
   }

   private static double clamp(double value) {
      return Math.max(0.0, Math.min(100.0, value));
   }
}
