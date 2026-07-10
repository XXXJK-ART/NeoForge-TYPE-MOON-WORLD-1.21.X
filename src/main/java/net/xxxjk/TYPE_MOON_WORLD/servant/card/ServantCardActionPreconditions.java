package net.xxxjk.TYPE_MOON_WORLD.servant.card;

public final class ServantCardActionPreconditions {
   private ServantCardActionPreconditions() {
   }

   public static boolean requiresLookTarget(String id) {
      return switch (id) {
         case "cybele", "charm", "chains", "anti_mystery", "hasebe_repel", "oda_encircle_matchlocks" -> true;
         default -> false;
      };
   }

   public static double targetRangeFor(String id) {
      return switch (id) {
         case "enuma_elish" -> 44.0;
         case "oda_encircle_matchlocks" -> 30.0;
         case "anti_mystery", "chains" -> 26.0;
         case "cybele", "charm", "bellerophon" -> 18.0;
         default -> 8.0;
      };
   }
}
