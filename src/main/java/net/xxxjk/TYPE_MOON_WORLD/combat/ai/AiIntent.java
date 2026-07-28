package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record AiIntent(
   ResourceLocation id,
   int priority,
   double utility,
   Set<AiControl> controls,
   int commitmentTicks,
   boolean interruptible,
   Runnable executor
) {
   public static final int PRIORITY_COMMAND = 900;
   public static final int PRIORITY_LETHAL_DEFENSE = 800;
   public static final int PRIORITY_PHASE = 700;
   public static final int PRIORITY_ATTACK = 500;
   public static final int PRIORITY_POSITION = 300;
   public static final int PRIORITY_IDLE = 0;

   public AiIntent {
      if (id == null) throw new IllegalArgumentException("intent id cannot be null");
      controls = controls == null || controls.isEmpty()
         ? Collections.emptySet()
         : Collections.unmodifiableSet(EnumSet.copyOf(controls));
      commitmentTicks = Math.max(0, commitmentTicks);
      if (executor == null) throw new IllegalArgumentException("intent executor cannot be null");
   }

   public static AiIntent of(ResourceLocation id, int priority, double utility, int commitmentTicks,
                             boolean interruptible, Runnable executor, AiControl first, AiControl... rest) {
      EnumSet<AiControl> controls = EnumSet.of(first, rest);
      return new AiIntent(id, priority, utility, controls, commitmentTicks, interruptible, executor);
   }
}
