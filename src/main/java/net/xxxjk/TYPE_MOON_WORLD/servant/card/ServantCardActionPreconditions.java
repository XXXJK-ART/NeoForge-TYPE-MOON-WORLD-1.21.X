package net.xxxjk.TYPE_MOON_WORLD.servant.card;

public final class ServantCardActionPreconditions {
   private ServantCardActionPreconditions() {
   }

   public static boolean requiresLookTarget(String id) {
      return switch (id) {
         case "cybele", "charm", "chains", "gilgamesh_chains", "gilgamesh_ring_vault", "anti_mystery", "hasebe_repel", "oda_encircle_matchlocks",
            "ushiwakamaru_moonlit_step", "ushiwakamaru_usumidori", "ushiwakamaru_eight_boat" -> true;
         default -> false;
      };
   }

   public static double targetRangeFor(String id) {
      return switch (id) {
         case "enuma_elish" -> 44.0;
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
