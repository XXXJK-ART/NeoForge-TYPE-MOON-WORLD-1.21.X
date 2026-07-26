package net.xxxjk.TYPE_MOON_WORLD.martial;

/** A small server-synchronised action state used by humanoid NPC models. */
public interface NpcActionPose {
   int getNpcActionPose();
   int getNpcActionPoseTicks();
   void triggerNpcActionPose(int pose, int ticks);
}
