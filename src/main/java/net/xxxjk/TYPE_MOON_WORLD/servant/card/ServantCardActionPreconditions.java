package net.xxxjk.TYPE_MOON_WORLD.servant.card;

public final class ServantCardActionPreconditions {
   private ServantCardActionPreconditions() {
   }

   public static boolean requiresLookTarget(String id) {
      return switch (id) {
         case "cybele", "charm", "chains", "gilgamesh_chains", "gilgamesh_ring_vault", "anti_mystery", "hasebe_repel", "oda_encircle_matchlocks",
            "ushiwakamaru_moonlit_step", "ushiwakamaru_usumidori", "ushiwakamaru_eight_boat",
            "shadow_hassan_ambush", "shadow_hassan_bind", "shadow_hassan_flurry", "shadow_hassan_meditative_sensitivity",
            "fanatic_heartbeat", "fanatic_computer" -> true;
         default -> false;
      };
   }

   public static double targetRangeFor(String id) {
      return switch (id) {
         case "enuma_elish" -> 44.0;
         case "shadow_hassan_meditative_sensitivity" -> 32.0;
         case "shadow_hassan_ambush" -> 18.0;
         case "shadow_hassan_bind" -> 16.0;
         case "shadow_hassan_flurry" -> 7.0;
         case "fanatic_heartbeat" -> 5.0;
         case "fanatic_computer" -> 1.8;
         case "oda_encircle_matchlocks" -> 30.0;
         case "anti_mystery", "chains", "gilgamesh_chains", "gilgamesh_ring_vault" -> 30.0;
         case "cybele", "charm", "bellerophon" -> 18.0;
         case "ushiwakamaru_moonlit_step" -> 12.0;
         case "ushiwakamaru_usumidori" -> 14.0;
         case "ushiwakamaru_eight_boat" -> 32.0;
         default -> 8.0;
      };
   }
}
