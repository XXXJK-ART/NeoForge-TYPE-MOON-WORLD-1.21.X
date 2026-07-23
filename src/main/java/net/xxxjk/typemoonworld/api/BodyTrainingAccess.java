package net.xxxjk.typemoonworld.api;

/** Stable façade for the player's body-training progression. */
public interface BodyTrainingAccess {
   int experience();
   int unspentPoints();
   int strength();
   int speed();
   int resistance();
   int technique();
   int totalEarned();
   int nextPointCost();
   boolean allocate(String stat);
   void award(int amount);
}
