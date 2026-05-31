package net.xxxjk.TYPE_MOON_WORLD.servant.personality;

public final class BehaviorProfileMatrix {
   private static final ServantBehaviorProfile[][] MATRIX = new ServantBehaviorProfile[][] {
      {
         new ServantBehaviorProfile(0.2, 0.25, 24.0, 4.0, 0.7),
         new ServantBehaviorProfile(0.2, 0.22, 28.0, 3.8, 0.75),
         new ServantBehaviorProfile(0.2, 0.18, 32.0, 3.5, 0.85)
      },
      {
         new ServantBehaviorProfile(0.1, 0.28, 22.0, 4.3, 0.6),
         new ServantBehaviorProfile(0.1, 0.25, 26.0, 4.0, 0.65),
         new ServantBehaviorProfile(0.1, 0.20, 30.0, 3.7, 0.75)
      },
      {
         new ServantBehaviorProfile(-0.1, 0.32, 20.0, 4.8, 0.5),
         new ServantBehaviorProfile(-0.1, 0.28, 24.0, 4.5, 0.55),
         new ServantBehaviorProfile(-0.1, 0.22, 28.0, 4.0, 0.7)
      }
   };

   private BehaviorProfileMatrix() {
   }

   public static ServantBehaviorProfile lookup(ObedienceAxis obedience, PrincipleAxis principle) {
      int row = Math.min(Math.max(obedience.id(), 0), 2);
      int col = Math.min(Math.max(principle.id(), 0), 2);
      return MATRIX[row][col];
   }
}
