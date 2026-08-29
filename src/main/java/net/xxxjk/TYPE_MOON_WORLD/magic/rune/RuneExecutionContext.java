package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;

/** Mutable server-owned state for one ordered rune cast. */
public final class RuneExecutionContext {
   private final ServerPlayer caster;
   private final LivingEntity target;
   private final RuneProgram program;
   private final Vec3 originOverride;
   private final Vec3 directionOverride;
   private final Map<String, Integer> effects = new LinkedHashMap<>();
   private final Map<String, Integer> modifiers = new LinkedHashMap<>();
   private final Map<String, Integer> pendingModifiers = new LinkedHashMap<>();
   private final List<String> executionTrace = new ArrayList<>();
   private double damage = 4.0D;
   private double radius = 0.0D;
   private int repeats = 1;
   private boolean pierce;
   private boolean projectileImpact;
   private boolean delayedDispatch;
   private int particleBudget = 192;
   private int particlesEmitted;
   private boolean spectacleEmitted;
   private final Map<String, Double> effectDamage = new LinkedHashMap<>();
   private final Map<String, Double> effectRadius = new LinkedHashMap<>();
   private final Map<String, Integer> effectQuantity = new LinkedHashMap<>();
   private final Map<String, Integer> effectDuration = new LinkedHashMap<>();
   private final Map<String, Vec3> effectDirections = new LinkedHashMap<>();
   private final List<EffectParameters> effectParameterList = new ArrayList<>();
   private int projectilesSpawned;
   private boolean failed;
   private String failureReason = "";

   public RuneExecutionContext(ServerPlayer caster, LivingEntity target, RuneProgram program) {
      this(caster, target, program, null, null);
   }

   public RuneExecutionContext(ServerPlayer caster, LivingEntity target, RuneProgram program, Vec3 origin, Vec3 direction) {
      this.caster = caster;
      this.target = target;
      this.program = program;
      this.originOverride = origin;
      this.directionOverride = direction;
      int runeCount = program == null ? 0 : program.sequence().size();
      this.particleBudget = runeCount >= RuneProgram.SLOT_COUNT ? 1024 : Math.min(768, 192 + runeCount * 20);
   }
   public ServerPlayer caster() { return caster; }
   public ServerLevel level() { return caster.serverLevel(); }
   /** Explicit entity target, or the caster for self-directed effects. */
   public LivingEntity target() { return target == null ? caster : target; }
   public boolean hasExplicitTarget() { return target != null && target != caster; }
   public Vec3 origin() { return originOverride == null ? caster.position().add(0.0D, caster.getEyeHeight(), 0.0D) : originOverride; }
   public Vec3 direction() { return directionOverride == null ? caster.getLookAngle().normalize() : directionOverride.normalize(); }
   public Vec3 endpoint(double distance) { return origin().add(direction().scale(Math.max(0.0D, distance))); }
   /** Server-side cursor point: block hit when present, otherwise a directional endpoint. */
   public Vec3 cursorPoint(double maxDistance) {
      Vec3 start = origin();
      Vec3 end = endpoint(maxDistance);
      BlockHitResult hit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
      return hit.getType() == BlockHitResult.Type.MISS ? end : hit.getLocation();
   }
   public RuneProgram program() { return program; }
   public int runeCount() { return program == null ? 0 : program.sequence().size(); }
   /** A full twenty-rune sequence is treated as a grand magic cast. */
   public boolean grandMagic() { return runeCount() >= RuneProgram.SLOT_COUNT; }
   /** Damage, terrain and visual systems share this cast intensity. */
   public double powerScale() { return grandMagic() ? 4.0D : 3.0D; }
   public float visualScale() {
      int amplifiers = modifierCount("power") + modifierCount("empower") + modifierCount("amplify");
      return (float)Math.min(6.0D, powerScale() * (1.0D + Math.min(0.5D, amplifiers * 0.15D)));
   }
   public boolean spectacleEmitted() { return spectacleEmitted; }
   public void markSpectacleEmitted() { spectacleEmitted = true; }
   public Map<String, Integer> effects() { return Collections.unmodifiableMap(effects); }
   public Map<String, Integer> modifiers() { return Collections.unmodifiableMap(modifiers); }
   public int effectCount(String semantic) { return effects.getOrDefault(semantic, 0); }
   public int modifierCount(String semantic) { return modifiers.getOrDefault(semantic, 0); }
   public void addEffect(String semantic) {
      if (semantic != null && !semantic.isBlank()) {
         effects.merge(semantic, 1, Integer::sum);
         effectParameterList.add(new EffectParameters(semantic, damage, radius, 1, 100, direction()));
      }
   }
   public void addModifier(String semantic) { if (semantic != null && !semantic.isBlank()) modifiers.merge(semantic, 1, Integer::sum); }
   public void addPendingModifier(String semantic) { if (semantic != null && !semantic.isBlank()) pendingModifiers.merge(semantic, 1, Integer::sum); }
   public int pendingModifierCount(String semantic) { return pendingModifiers.getOrDefault(semantic, 0); }
   public Map<String, Integer> pendingModifiers() { return Collections.unmodifiableMap(pendingModifiers); }
   public void clearPendingModifiers() { pendingModifiers.clear(); }
   public double effectDamage(String semantic, double fallback) { return effectDamage.getOrDefault(semantic, fallback); }
   public void setEffectDamage(String semantic, double value) { if (semantic != null) effectDamage.put(semantic, Math.max(0.0D, value)); }
   public double effectRadius(String semantic, double fallback) { return effectRadius.getOrDefault(semantic, fallback); }
   public void setEffectRadius(String semantic, double value) { if (semantic != null) effectRadius.put(semantic, Math.max(0.0D, Math.min(64.0D, value))); }
   public int effectQuantity(String semantic, int fallback) { return effectQuantity.getOrDefault(semantic, fallback); }
   public void setEffectQuantity(String semantic, int value) { if (semantic != null) effectQuantity.put(semantic, Math.max(1, Math.min(32, value))); }
   public int effectDuration(String semantic, int fallback) { return effectDuration.getOrDefault(semantic, fallback); }
   public void setEffectDuration(String semantic, int value) { if (semantic != null) effectDuration.put(semantic, Math.max(1, Math.min(2400, value))); }
   public Vec3 effectDirection(String semantic) { return effectDirections.getOrDefault(semantic, direction()); }
   public void effectDirection(String semantic, Vec3 value) { if (semantic != null && value != null && value.lengthSqr() > 1.0E-6) effectDirections.put(semantic, value.normalize()); }
   public List<EffectParameters> effectParameters() { return Collections.unmodifiableList(effectParameterList); }
   public int projectilesSpawned() { return projectilesSpawned; }
   public void markProjectilesSpawned(int count) { projectilesSpawned += Math.max(0, count); }
   public void updateLastEffect(String semantic) {
      if (effectParameterList.isEmpty() || semantic == null) return;
      int index = effectParameterList.size() - 1;
      EffectParameters previous = effectParameterList.get(index);
      if (!semantic.equals(previous.semantic())) return;
      effectParameterList.set(index, new EffectParameters(semantic,
         effectDamage(semantic, previous.damage()), effectRadius(semantic, previous.radius()),
         effectQuantity(semantic, previous.quantity()), effectDuration(semantic, previous.duration()),
         effectDirection(semantic)));
   }
   public record EffectParameters(String semantic, double damage, double radius, int quantity, int duration, Vec3 direction) { }
   public int particlesRemaining() { return Math.max(0, particleBudget - particlesEmitted); }
   public int particlesEmitted() { return particlesEmitted; }
   public int particleBudget() { return particleBudget; }
   public void particleBudget(int budget) { particleBudget = Math.max(0, Math.min(1024, budget)); }
   public void resetParticleBudget() { particlesEmitted = 0; }
   public boolean emit(ParticleOptions particle, Vec3 point, int requested) {
      if (particle == null || point == null || requested <= 0 || particlesRemaining() <= 0) return false;
      int intensity = grandMagic() ? 4 : 3;
      int count = Math.min(Math.max(1, requested) * intensity, particlesRemaining());
      level().sendParticles(particle, point.x, point.y, point.z, count, .18D, .18D, .18D, .02D);
      particlesEmitted += count;
      return true;
   }
   public void emitRuneParticle(String runeId) {
      emitRuneParticle(runeId, 0, 1);
   }

   /** Emits one rune glyph on a deterministic ring so large programs stay legible. */
   public void emitRuneParticle(String runeId, int index, int total) {
      emitRuneParticleAt(runeId, index, total, origin());
   }

   public void emitRuneParticleAt(String runeId, int index, int total, Vec3 center) {
      ParticleOptions particle = runeParticle(runeId);
      int count = Math.max(1, total);
      double ring = Math.max(0.85D, count * 0.16D);
      double angle = (Math.PI * 2.0D * Math.max(0, index)) / count - Math.PI / 2.0D;
      emit(particle, (center == null ? caster.position() : center).add(Math.cos(angle) * ring, 0.0D, Math.sin(angle) * ring), grandMagic() ? 2 : 1);
   }

   private static ParticleOptions runeParticle(String runeId) {
      return switch (runeId == null ? "" : runeId) {
         case "fehu" -> ModParticles.FEHU_RUNE.get(); case "uruz" -> ModParticles.URUZ_RUNE.get();
         case "thurisaz" -> ModParticles.THURISAZ_RUNE.get(); case "ansuz" -> ModParticles.ANSUZ_RUNE.get();
         case "raidho" -> ModParticles.RAIDHO_RUNE.get(); case "kenaz" -> ModParticles.KENAZ_RUNE.get();
         case "gebo" -> ModParticles.GEBO_RUNE.get(); case "wunjo" -> ModParticles.WUNJO_RUNE.get();
         case "hagalaz" -> ModParticles.HAGALAZ_RUNE.get(); case "nauthiz" -> ModParticles.NAUTHIZ_RUNE.get();
         case "isa" -> ModParticles.ISA_RUNE.get(); case "jera" -> ModParticles.JERA_RUNE.get();
         case "eihwaz" -> ModParticles.EIHWAZ_RUNE.get(); case "perthro" -> ModParticles.PERTHRO_RUNE.get();
         case "algiz" -> ModParticles.ALGIZ_RUNE.get(); case "sowilo" -> ModParticles.SOWILO_RUNE.get();
         case "tiwaz" -> ModParticles.TIWAZ_RUNE.get(); case "berkano" -> ModParticles.BERKANO_RUNE.get();
         case "ehwaz" -> ModParticles.EHWAZ_RUNE.get(); case "mannaz" -> ModParticles.MANNAZ_RUNE.get();
         case "laguz" -> ModParticles.LAGUZ_RUNE.get(); case "ingwaz" -> ModParticles.INGWAZ_RUNE.get();
         case "dagaz" -> ModParticles.DAGAZ_RUNE.get(); case "othala" -> ModParticles.OTHALA_RUNE.get();
         default -> ParticleTypes.CRIT;
      };
   }
   public void trace(RunePosition position, String semantic) { executionTrace.add(position.name().toLowerCase(java.util.Locale.ROOT) + ":" + semantic); }
   public List<String> executionTrace() { return Collections.unmodifiableList(executionTrace); }
   public double damage() { return damage; }
   public void damage(double value) { damage = Math.max(0.0D, value); }
   /** Sets the pre-effect impact damage used when resolving a projectile hit. */
   public void baseDamage(double value) { damage = Math.max(0.0D, value); }
   public boolean projectileImpact() { return projectileImpact; }
   public void projectileImpact(boolean value) { projectileImpact = value; }
   public boolean delayedDispatch() { return delayedDispatch; }
   public void delayedDispatch(boolean value) { delayedDispatch = value; }
   /** Adds an effect bonus without truncating high-tier rune damage. */
   public void addDamage(double value) { damage(Math.min(4000.0D, damage + value)); }
   public double radius() { return radius; }
   public void radius(double value) { radius = Math.max(0.0D, Math.min(64.0D, value)); }
   public int repeats() { return repeats; }
   public void repeats(int value) { repeats = Math.max(1, Math.min(8, value)); }
   public boolean pierce() { return pierce; }
   public void pierce(boolean value) { pierce = value; }
   public boolean failed() { return failed; }
   public String failureReason() { return failureReason; }
   public void fail(String reason) { failed = true; failureReason = reason == null || reason.isBlank() ? "failed" : reason; }
}
