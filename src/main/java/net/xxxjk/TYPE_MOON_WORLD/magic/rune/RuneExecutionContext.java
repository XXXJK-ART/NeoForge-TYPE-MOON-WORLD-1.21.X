package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Mutable server-owned state shared by the four rune execution phases. */
public final class RuneExecutionContext {
   private final ServerPlayer caster;
   private final LivingEntity target;
   private final RuneProgram program;
   private final Set<String> effects = new HashSet<>();
   private final Set<String> modifiers = new HashSet<>();
   private final List<String> executionTrace = new ArrayList<>();
   private double damage = 4.0D;
   private double radius = 0.0D;
   private int repeats = 1;
   private boolean pierce;
   private boolean failed;
   private String failureReason = "";

   public RuneExecutionContext(ServerPlayer caster, LivingEntity target, RuneProgram program) {
      this.caster = caster;
      this.target = target;
      this.program = program;
   }
   public ServerPlayer caster() { return caster; }
   public ServerLevel level() { return caster.serverLevel(); }
   public LivingEntity target() { return target; }
   public RuneProgram program() { return program; }
   public Set<String> effects() { return effects; }
   public Set<String> modifiers() { return modifiers; }
   public void trace(RunePosition position, String semantic) { executionTrace.add(position.name().toLowerCase(java.util.Locale.ROOT) + ":" + semantic); }
   public List<String> executionTrace() { return Collections.unmodifiableList(executionTrace); }
   public double damage() { return damage; }
   public void damage(double value) { damage = Math.max(0.0D, value); }
   public void addDamage(double value) { damage(Math.min(40.0D, damage + value)); }
   public double radius() { return radius; }
   public void radius(double value) { radius = Math.max(0.0D, Math.min(16.0D, value)); }
   public int repeats() { return repeats; }
   public void repeats(int value) { repeats = Math.max(1, Math.min(8, value)); }
   public boolean pierce() { return pierce; }
   public void pierce(boolean value) { pierce = value; }
   public boolean failed() { return failed; }
   public String failureReason() { return failureReason; }
   public void fail(String reason) { failed = true; failureReason = reason == null || reason.isBlank() ? "failed" : reason; }
}
