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
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.MagicBulletProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
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
      if (ctx.caster() == null) { ctx.fail("missing_caster"); return ctx; }
      if (ctx.program() == null) { ctx.fail("missing_program"); return ctx; }
      if (!ctx.program().sequence().isEmpty()) {
         var ids = ctx.program().sequence();
         var roles = ctx.program().sequencePositions();
         for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            RunePosition role = i < roles.size() ? roles.get(i) : RunePosition.EFFECT;
            RuneDefinition definition = RuneRegistry.get(id);
            if (definition == null) { ctx.fail("unknown_rune"); return ctx; }
            ctx.emitRuneParticle(id, i, ids.size());
            if (role == RunePosition.EFFECT) {
               double oldDamage = ctx.damage();
               double oldRadius = ctx.radius();
               int oldRepeats = ctx.repeats();
               applyPendingModifiers(ctx);
               boolean handled = fixedRune(ctx, id);
               ctx.damage(oldDamage); ctx.radius(oldRadius); ctx.repeats(oldRepeats); ctx.clearPendingModifiers();
               if (handled) continue;
            } else if (role == RunePosition.TRIGGER && fixedRune(ctx, id)) continue;
            String semantic = definition.semantic(role);
            ctx.trace(role, semantic);
            BiConsumer<RuneExecutionContext, String> custom = CUSTOM_ACTIONS.get(role.name() + ":" + semantic);
            if (role == RunePosition.MODIFIER) {
               ctx.addModifier(semantic);
               ctx.addPendingModifier(semantic);
            } else if (role == RunePosition.EFFECT) {
               double oldDamage = ctx.damage();
               double oldRadius = ctx.radius();
               int oldRepeats = ctx.repeats();
               applyPendingModifiers(ctx);
               if (custom != null) custom.accept(ctx, semantic); else effect(ctx, semantic);
               ctx.updateLastEffect(semantic);
               ctx.damage(oldDamage); ctx.radius(oldRadius); ctx.repeats(oldRepeats); ctx.clearPendingModifiers();
            } else if (custom != null) custom.accept(ctx, semantic);
            else switch (role) {
               case TRIGGER -> trigger(ctx, semantic);
               case TERMINAL -> terminal(ctx, semantic);
               default -> { }
            }
            if (ctx.failed()) return ctx;
         }
      } else {
         phase(ctx, RunePosition.TRIGGER);
         phase(ctx, RunePosition.EFFECT);
         phase(ctx, RunePosition.MODIFIER);
      }
      fusion(ctx);
      if (ctx.program().sequence().isEmpty()) phase(ctx, RunePosition.TERMINAL);
      if (ctx.program().kind() == RuneProgramKind.REINFORCEMENT) applyReinforcement(ctx.caster(), ctx.program());
      return ctx;
   }

   /** Applies armor/body reinforcement without mana cost or one-shot inscription charges. */
   public static void applyReinforcement(net.minecraft.server.level.ServerPlayer caster, RuneProgram program) {
      if (caster == null || program == null || program.kind() != RuneProgramKind.REINFORCEMENT) return;
      int power = 0;
      int protection = 0;
      int recovery = 0;
      for (int i = 0; i < program.sequence().size(); i++) {
         RunePosition role = i < program.sequencePositions().size() ? program.sequencePositions().get(i) : RunePosition.MODIFIER;
         RuneDefinition definition = RuneRegistry.get(program.sequence().get(i));
         if (definition == null) continue;
         String semantic = definition.semantic(role);
         if (role == RunePosition.MODIFIER) {
            if ("power".equals(semantic) || "amplify".equals(semantic)) power++;
            if ("resist".equals(semantic) || "guard".equals(semantic)) protection++;
            if ("restore".equals(semantic) || "persist".equals(semantic)) recovery++;
         } else if (role == RunePosition.TERMINAL) {
            if ("protect".equals(semantic) || "endure".equals(semantic) || "anchor".equals(semantic)) protection++;
            if ("renew".equals(semantic) || "bless".equals(semantic)) recovery++;
         }
      }
      if (power > 0) caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, Math.min(4, power - 1)));
      if (protection > 0) caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, Math.min(3, protection - 1)));
      if (recovery > 0) caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, Math.min(2, recovery - 1)));
   }

   /** Emits the same readable glyph ring for passive release media. */
   public static void emitProgramGlyphs(net.minecraft.server.level.ServerPlayer caster, RuneProgram program) {
      emitProgramGlyphs(caster, program, null);
   }

   public static void emitProgramGlyphs(net.minecraft.server.level.ServerPlayer caster, RuneProgram program, Vec3 center) {
      if (caster == null || program == null || program.sequence().isEmpty()) return;
      RuneExecutionContext context = new RuneExecutionContext(caster, caster, program);
      int visible = Math.min(8, program.sequence().size());
      for (int i = 0; i < visible; i++) {
         int sourceIndex = i * program.sequence().size() / visible;
         context.emitRuneParticleAt(program.sequence().get(sourceIndex), i, visible, center);
      }
   }

   /** Low-frequency ambient cue for a maintained armor inscription. */
   public static void emitPassiveGlyph(net.minecraft.server.level.ServerPlayer caster, RuneProgram program) {
      if (caster == null || program == null || program.sequence().isEmpty()) return;
      int index = Math.floorMod((int)(caster.level().getGameTime() + caster.getId()), program.sequence().size());
      RuneExecutionContext context = new RuneExecutionContext(caster, caster, program);
      context.emitRuneParticleAt(program.sequence().get(index), 0, 1,
         caster.position().add(0.0D, caster.getBbHeight() + 0.2D, 0.0D));
   }

   private static void phase(RuneExecutionContext ctx, RunePosition position) {
      List<String> slots = ctx.program().slots(position);
      int index = 0;
      int total = Math.max(1, ctx.program().sequence().size());
      for (String id : slots) {
         if (id == null || id.isEmpty()) continue;
         RuneDefinition definition = RuneRegistry.get(id);
         if (definition == null) { ctx.fail("unknown_rune"); return; }
         ctx.emitRuneParticle(id, index++, total);
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

   private static boolean fixedRune(RuneExecutionContext c, String id) {
      c.addEffect(id);
      c.effectDirection(id, c.direction());
      switch (id) {
         case "kenaz" -> { c.setEffectDamage(id, c.damage()); c.setEffectRadius(id, 5.0D); c.setEffectQuantity(id, 1); directionalBullet(c, MagicBulletProjectileEntity.ELEMENT_FIRE, 200.0D, 2.0F, 5.0D); c.updateLastEffect(id); return true; }
         case "hagalaz" -> { c.setEffectRadius(id, 10.0D); c.setEffectQuantity(id, 10); Vec3 p = c.endpoint(24.0D); for (int i = 12; i >= 0; i--) c.emit(ParticleTypes.SNOWFLAKE, p.add(0.0D, i, 0.0D), 1); area(c, p, 10.0D, 6.0D, ParticleTypes.SNOWFLAKE); c.updateLastEffect(id); return true; }
         case "thurisaz" -> { c.radius(8.0D); Vec3 p = c.endpoint(24.0D); lightning(c, p); area(c, p, 8.0D, 8.0D, ModParticles.ELEMENTAL_LIGHTNING.get()); return true; }
         case "sowilo" -> { c.setEffectRadius(id, 0.75D); c.setEffectQuantity(id, 50); VFXServerEffects.spawnOriented(c.level(), "beam", c.origin(), c.direction(), 64.0D); lineDamage(c, 50.0D, 6.0D); c.updateLastEffect(id); return true; }
         case "nauthiz" -> { c.radius(8.0D); area(c, c.endpoint(8.0D), 8.0D, 5.0D, ParticleTypes.SQUID_INK); return true; }
         case "isa" -> { c.radius(12.0D); area(c, c.caster().position(), 12.0D, 0.0D, ParticleTypes.SNOWFLAKE); return true; }
         case "algiz" -> { c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1)); c.emit(ParticleTypes.ENCHANT, c.caster().position(), 32); return true; }
         case "berkano" -> { area(c, c.endpoint(6.0D), 5.0D, -4.0D, ParticleTypes.HAPPY_VILLAGER); return true; }
         case "raidho" -> { Vec3 p = c.cursorPoint(50.0D); c.caster().teleportTo(p.x, p.y, p.z); c.emit(ParticleTypes.PORTAL, p, 16); return true; }
         case "uruz" -> { int amplifier = Math.min(4, countRune(c, "uruz") - 1 + countModifier(c, "power") + countModifier(c, "amplify")); c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, amplifier)); return true; }
         default -> { return false; }
      }
   }

   private static int countRune(RuneExecutionContext c, String runeId) {
      return (int)c.program().sequence().stream().filter(runeId::equals).count();
   }

   private static int countModifier(RuneExecutionContext c, String semantic) {
      int count = 0;
      for (int i = 0; i < c.program().sequence().size(); i++) {
         if (i >= c.program().sequencePositions().size() || c.program().sequencePositions().get(i) != RunePosition.MODIFIER) continue;
         RuneDefinition definition = RuneRegistry.get(c.program().sequence().get(i));
         if (definition != null && semantic.equals(definition.semantic(RunePosition.MODIFIER))) count++;
      }
      return count;
   }

   private static void directionalBullet(RuneExecutionContext c, int element, double range, float scale, double explosionRadius) {
      MagicBulletProjectileEntity projectile = new MagicBulletProjectileEntity(c.level(), c.caster());
      projectile.setPos(c.origin());
      projectile.configure((float)c.effectDamage("projectile", c.damage()), 0.0F, range, element, scale);
      projectile.setRuneExplosionRadius(explosionRadius);
      Vec3 direction = c.direction();
      projectile.shoot(direction.x, direction.y, direction.z, 2.5F, 0.0F);
      c.level().addFreshEntity(projectile);
   }

   private static void lineDamage(RuneExecutionContext c, double length, double damage) {
      Vec3 origin = c.origin();
      for (int i = 1; i <= (int)length; i++) {
         Vec3 point = origin.add(c.direction().scale(i));
         for (LivingEntity entity : c.level().getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(0.75D), e -> e.isAlive() && e != c.caster())) entity.hurt(c.caster().damageSources().magic(), (float)damage);
         if (c.particlesRemaining() > 0) c.emit(ParticleTypes.END_ROD, point, 1);
      }
   }

   private static void area(RuneExecutionContext c, Vec3 center, double radius, double damage, net.minecraft.core.particles.ParticleOptions particle) {
      c.setEffectRadius("area", radius);
      if (particle != null) c.emit(particle, center, Math.min(32, (int)Math.ceil(radius)));
      for (LivingEntity entity : c.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius), e -> e.isAlive() && e != c.caster())) {
         if (damage > 0.0D) entity.hurt(c.caster().damageSources().magic(), (float)damage);
         else if (damage < 0.0D) entity.heal((float)-damage);
         if (c.program().sequence().contains("isa")) entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
      }
   }

   private static void effect(RuneExecutionContext c, String s) {
      c.addEffect(s);
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
      c.updateLastEffect(s);
   }

   private static void modifier(RuneExecutionContext c, String s) {
      c.addModifier(s);
      switch (s) {
         case "amplify", "power", "critical" -> c.addDamage(4.0D * c.modifierCount(s));
         case "range", "area" -> c.radius(Math.max(3.0D, c.radius() + (c.program().releaseConfig().contains("radius") ? c.program().releaseConfig().getDouble("radius") : 4.0D)));
         case "repeat" -> c.repeats(c.repeats() + 1);
         case "fuse" -> { }
         case "accuracy", "focus" -> c.addDamage(2.0D);
         case "duration", "persist" -> c.setEffectDuration(s, 100 + c.modifierCount(s) * 40);
         default -> { }
      }
   }

   private static void applyPendingModifiers(RuneExecutionContext c) {
      int power = c.pendingModifierCount("power") + c.pendingModifierCount("amplify") + c.pendingModifierCount("critical");
      int range = c.pendingModifierCount("range") + c.pendingModifierCount("area");
      int repeat = c.pendingModifierCount("repeat");
      int focus = c.pendingModifierCount("accuracy") + c.pendingModifierCount("focus");
      if (power > 0) c.addDamage(4.0D * power);
      if (focus > 0) c.addDamage(2.0D * focus);
      if (range > 0) c.radius(c.radius() + 3.0D * range);
      if (repeat > 0) c.repeats(c.repeats() + repeat);
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
      if (c.effectCount("fire") > 0 && c.effectCount("ice") > 0) {
         c.addDamage(8.0D); c.radius(Math.max(2.0D, c.radius()));
         c.emit(ParticleTypes.CLOUD, c.endpoint(3.0D), 8);
      }
      if (c.effectCount("light") > 0 && c.effectCount("shadow") > 0) {
         c.addDamage(12.0D);
         c.emit(ParticleTypes.END_ROD, c.target().position(), 8);
      }
   }

   private static void hit(RuneExecutionContext c, double amount) {
      double resolvedDamage = Math.min(40.0D, Math.max(amount, c.damage()));
      for (int i = 0; i < c.repeats(); i++) {
         if (c.radius() > 0.0D) {
            var center = c.hasExplicitTarget() ? c.target().position() : c.endpoint(Math.min(50.0D, c.program().releaseConfig().contains("range") ? c.program().releaseConfig().getDouble("range") : 12.0D));
            for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(c.radius()), x -> x.isAlive() && x != c.caster())) e.hurt(c.caster().damageSources().magic(), (float)resolvedDamage);
         } else if (c.hasExplicitTarget()) {
            c.target().hurt(c.caster().damageSources().magic(), (float)resolvedDamage);
         } else {
            // A standalone trigger still has a directional impact point when no entity is under the crosshair.
            Vec3 point = c.endpoint(12.0D);
            for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class,
               new AABB(point, point).inflate(1.0D), x -> x.isAlive() && x != c.caster())) {
               e.hurt(c.caster().damageSources().magic(), (float)resolvedDamage);
            }
            c.emit(ParticleTypes.CRIT, point, 6);
         }
      }
   }

   private static void lightning(RuneExecutionContext c) {
      lightning(c, c.hasExplicitTarget() ? c.target().position() : c.endpoint(32.0D));
   }

   private static void lightning(RuneExecutionContext c, Vec3 point) {
      LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(c.level());
      if (bolt == null) { c.fail("lightning_unavailable"); return; }
      bolt.moveTo(point); bolt.setCause(c.caster()); c.level().addFreshEntity(bolt);
      // The vanilla bolt remains authoritative; this adds the custom impact sprite.
      c.emit(ModParticles.ELEMENTAL_LIGHTNING.get(), point.add(0.0D, 1.0D, 0.0D), 8);
   }

   /** Stable behavior constants for tests and addon visual extensions. */
   public static RuneVisualProfile fixedProfile(String runeId) {
      return switch (runeId == null ? "" : runeId) {
         case "kenaz" -> new RuneVisualProfile(0xFFFF6B20, 200.0D, 5.0D, "flame_burst");
         case "hagalaz" -> new RuneVisualProfile(0xFFEAF7FF, 24.0D, 10.0D, "ice_fall");
         case "thurisaz" -> new RuneVisualProfile(0xFFECCBFF, 24.0D, 8.0D, "lightning");
         case "sowilo" -> new RuneVisualProfile(0xFFFFF1A8, 50.0D, 0.75D, "piercing_beam");
         case "nauthiz" -> new RuneVisualProfile(0xFF32123F, 8.0D, 8.0D, "shadow_chain");
         case "isa" -> new RuneVisualProfile(0xFFF5FCFF, 12.0D, 12.0D, "freeze_ring");
         case "algiz" -> new RuneVisualProfile(0xFF9BDFFF, 8.0D, 8.0D, "shield_ring");
         case "berkano" -> new RuneVisualProfile(0xFF58D68D, 6.0D, 5.0D, "healing_sprinkle");
         case "raidho" -> new RuneVisualProfile(0xFFE8F3FF, 12.0D, 0.0D, "displacement");
         case "uruz" -> new RuneVisualProfile(0xFFE53935, 0.0D, 0.0D, "strength");
         default -> new RuneVisualProfile(0xFFFFFFFF, 0.0D, 0.0D, "rune");
      };
   }

   public record RuneVisualProfile(int color, double length, double radius, String effectId) { }
}
