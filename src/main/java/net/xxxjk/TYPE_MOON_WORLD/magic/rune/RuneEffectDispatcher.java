package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

/** Ordered, data-driven rune actions. All methods are server-only. */
public final class RuneEffectDispatcher {
   private static final Map<String, BiConsumer<RuneExecutionContext, String>> CUSTOM_ACTIONS = new ConcurrentHashMap<>();
   private RuneEffectDispatcher() { }

   /** Registers an addon action without replacing the built-in fallback semantics. */
   public static void register(RunePosition position, String semantic, BiConsumer<RuneExecutionContext, String> action) {
      if (position == null || semantic == null || semantic.isBlank() || action == null) return;
      CUSTOM_ACTIONS.put(position.name() + ":" + semantic, action);
   }

   public static RuneExecutionContext execute(RuneExecutionContext ctx) {
      if (ctx == null) return null;
      if (ctx.caster() == null || ctx.target() == null) { ctx.fail("missing_target"); return ctx; }
      if (ctx.program() == null) { ctx.fail("missing_program"); return ctx; }
      VFXServerEffects.spawn(ctx.level(), "magic_circle", ctx.caster().position(), 48.0D);
      phase(ctx, RunePosition.TRIGGER);
      phase(ctx, RunePosition.EFFECT);
      phase(ctx, RunePosition.MODIFIER);
      fusion(ctx);
      phase(ctx, RunePosition.TERMINAL);
      if (!ctx.failed()) marker(ctx);
      return ctx;
   }

   private static void phase(RuneExecutionContext ctx, RunePosition position) {
      List<String> slots = ctx.program().slots(position);
      for (String id : slots) {
         if (id == null || id.isEmpty()) continue;
         RuneDefinition definition = RuneRegistry.get(id);
         if (definition == null) { ctx.fail("unknown_rune"); return; }
         String semantic = definition.semantic(position);
         ctx.trace(position, semantic);
         BiConsumer<RuneExecutionContext, String> custom = CUSTOM_ACTIONS.get(position.name() + ":" + semantic);
         if (custom != null) {
            custom.accept(ctx, semantic);
            if (ctx.failed()) return;
            continue;
         }
         switch (position) {
            case TRIGGER -> trigger(ctx, semantic);
            case EFFECT -> effect(ctx, semantic);
            case MODIFIER -> modifier(ctx, semantic);
            case TERMINAL -> terminal(ctx, semantic);
            default -> { }
         }
         if (ctx.failed()) return;
      }
   }

   private static void trigger(RuneExecutionContext c, String s) {
      switch (s) {
         case "wealth" -> hit(c, 6.0D);
         case "break" -> lightning(c);
         case "strength" -> c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 1));
         case "self" -> {
            c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 1));
            c.caster().addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
         }
         case "storm" -> { c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1)); hit(c, 7.0D); }
         case "command" -> c.target().addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
         case "move", "travel" -> c.caster().teleportTo(c.caster().getX() + c.caster().getLookAngle().x * 3.0D, c.caster().getY(), c.caster().getZ() + c.caster().getLookAngle().z * 3.0D);
         case "ignite" -> { c.target().setRemainingFireTicks(Math.max(80, c.target().getRemainingFireTicks())); hit(c, 5.0D); }
         case "bind" -> { c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3)); c.caster().heal(2.0F); }
         case "joy" -> {
            c.caster().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
            c.caster().addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
         }
         case "need" -> c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
         case "freeze" -> c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 5));
         case "ward", "protect" -> c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1));
         case "shine" -> c.level().sendParticles(ParticleTypes.END_ROD, c.target().getX(), c.target().getY(0.5), c.target().getZ(), 12, .4, .4, .4, .03);
         case "judge" -> hit(c, 8.0D);
         case "grow" -> c.target().heal(4.0F);
         case "chance" -> { if (c.caster().getRandom().nextBoolean()) hit(c, 10.0D); else c.caster().heal(2.0F); }
         case "cycle" -> c.repeats(2);
         case "flow" -> c.target().setDeltaMovement(c.target().getDeltaMovement().add(0, .45, 0));
         case "charge" -> c.addDamage(4.0D);
         case "transform" -> c.target().addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
         case "inherit" -> {
            c.caster().heal(2.0F);
            c.level().sendParticles(ParticleTypes.SOUL, c.caster().getX(), c.caster().getY(1.0), c.caster().getZ(), 14, .4, .6, .4, .02);
         }
         default -> { }
      }
   }

   private static void effect(RuneExecutionContext c, String s) {
      c.effects().add(s);
      switch (s) {
         case "fire" -> c.target().setRemainingFireTicks(Math.max(80, c.target().getRemainingFireTicks()));
         case "ice" -> c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
         case "heal", "nature" -> c.target().heal(4.0F);
         case "barrier", "shield" -> c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
         case "drain" -> { hit(c, 4.0D); c.caster().heal(2.0F); }
         case "light" -> c.level().sendParticles(ParticleTypes.END_ROD, c.target().getX(), c.target().getY(), c.target().getZ(), 16, .3, .5, .3, .04);
         case "shadow" -> c.target().addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
         case "pierce" -> c.pierce(true);
         case "water" -> c.target().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
         case "earth" -> c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
         case "speed" -> c.caster().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
         case "mind" -> c.target().addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
         case "energy", "impact" -> c.addDamage(3.0D);
         default -> { }
      }
   }

   private static void modifier(RuneExecutionContext c, String s) {
      c.modifiers().add(s);
      switch (s) {
         case "amplify", "power", "critical" -> c.addDamage(4.0D);
         case "range", "area" -> c.radius(Math.max(3.0D, c.program().releaseConfig().getDouble("radius")));
         case "repeat" -> c.repeats(2);
         case "fuse" -> { }
         case "accuracy", "focus" -> c.addDamage(2.0D);
         case "duration", "persist" -> { }
         default -> { }
      }
   }

   private static void terminal(RuneExecutionContext c, String s) {
      switch (s) {
         case "burn" -> c.target().setRemainingFireTicks(Math.max(120, c.target().getRemainingFireTicks()));
         case "purify", "disperse", "banish" -> c.target().removeAllEffects();
         case "renew", "bless" -> c.caster().heal(3.0F);
         case "discharge", "strike", "detonate", "execute" -> hit(c, s.equals("execute") ? 12.0D : 6.0D);
         case "protect" -> c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
         case "escape", "return" -> c.caster().teleportTo(c.caster().getX(), c.caster().getY() + 1.0D, c.caster().getZ());
         case "awaken" -> c.caster().removeAllEffects();
         default -> { }
      }
   }

   private static void fusion(RuneExecutionContext c) {
      if (c.effects().contains("fire") && c.effects().contains("ice")) { c.addDamage(8.0D); c.radius(Math.max(2.0D, c.radius())); }
      if (c.effects().contains("light") && c.effects().contains("shadow")) c.addDamage(12.0D);
   }

   private static void hit(RuneExecutionContext c, double amount) {
      double resolvedDamage = Math.min(40.0D, Math.max(amount, c.damage()));
      for (int i = 0; i < c.repeats(); i++) {
         if (c.radius() > 0.0D) {
            for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, c.target().getBoundingBox().inflate(c.radius()), x -> x.isAlive() && x != c.caster())) e.hurt(c.caster().damageSources().magic(), (float)resolvedDamage);
         } else if (c.target() != c.caster()) c.target().hurt(c.caster().damageSources().magic(), (float)resolvedDamage);
      }
   }

   private static void lightning(RuneExecutionContext c) {
      LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(c.level());
      if (bolt == null) { c.fail("lightning_unavailable"); return; }
      bolt.moveTo(c.target().position()); bolt.setCause(c.caster()); c.level().addFreshEntity(bolt);
   }

   private static void marker(RuneExecutionContext c) {
      c.level().sendParticles(ParticleTypes.ENCHANT, c.target().getX(), c.target().getY(.8), c.target().getZ(), 18, .45, .55, .45, .08);
      VFXServerEffects.spawn(c.level(), "beam", c.target().position(), 48.0D);
   }
}
