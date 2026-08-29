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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.MagicBulletProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

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
      if (!ctx.delayedDispatch()) primeModifiers(ctx);
      int delay = ctx.effectDuration("delay", 0);
      if (!ctx.delayedDispatch() && delay > 0 && !hasProjectileTrigger(ctx)) {
         ctx.delayedDispatch(true);
         TYPE_MOON_WORLD.queueServerWork(delay, () -> execute(ctx));
         return ctx;
      }
      if (!ctx.program().sequence().isEmpty()) {
         var ids = ctx.program().sequence();
         var roles = ctx.program().sequencePositions();
         for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            RunePosition role = i < roles.size() ? roles.get(i) : RunePosition.EFFECT;
            RuneDefinition definition = RuneRegistry.get(id);
            if (definition == null) { ctx.fail("unknown_rune"); return ctx; }
            ctx.emitRuneParticle(id, i, ids.size());
            String semantic = definition.semantic(role);
            String action = RuneRegistry.actionKey(id, role);
            ctx.trace(role, semantic);
            BiConsumer<RuneExecutionContext, String> custom = CUSTOM_ACTIONS.get(role.name() + ":" + semantic);
            if (role == RunePosition.MODIFIER) {
               // Apply modifiers as they are encountered. Programs built from the
               // four editor bands place modifiers after effects, so leaving them
               // pending made them inert for the normal ordered execution path.
               // Modifier declarations were primed before the cast; addon overrides still run here.
               if (custom != null) custom.accept(ctx, semantic);
            } else if (role == RunePosition.EFFECT) {
               if (custom != null) custom.accept(ctx, semantic); else effect(ctx, action);
               ctx.updateLastEffect(semantic);
            } else if (custom != null) custom.accept(ctx, semantic);
             else switch (role) {
               case TRIGGER -> trigger(ctx, action);
               // Projectile terminals are resolved by the projectile impact event.
               // Running them here would make fields and explosions happen before a hit.
               case TERMINAL -> { if (ctx.projectilesSpawned() == 0) terminal(ctx, action); }
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

   /** Applies only effect and terminal bands when a rune-created projectile hits. */
   public static RuneExecutionContext applyProjectileImpact(ServerPlayer caster, LivingEntity target, RuneProgram program) {
      return applyProjectileImpact(caster, target, program, 4.0D);
   }

   /** Resolves all effect/terminal bands against the actual projectile base damage. */
   public static RuneExecutionContext applyProjectileImpact(ServerPlayer caster, LivingEntity target, RuneProgram program, double baseDamage) {
      if (caster == null || target == null || program == null) return null;
      RuneExecutionContext context = new RuneExecutionContext(caster, target, program);
      context.baseDamage(baseDamage);
      context.projectileImpact(true);
      for (int i = 0; i < program.sequence().size(); i++) {
         RunePosition role = i < program.sequencePositions().size() ? program.sequencePositions().get(i) : RunePosition.EFFECT;
         if (role != RunePosition.EFFECT && role != RunePosition.MODIFIER && role != RunePosition.TERMINAL) continue;
         RuneDefinition definition = RuneRegistry.get(program.sequence().get(i));
         if (definition == null) { context.fail("unknown_rune"); return context; }
         String semantic = definition.semantic(role);
         String action = RuneRegistry.actionKey(definition.idPath(), role);
         if (role == RunePosition.MODIFIER) modifier(context, action);
         else if (role == RunePosition.EFFECT) effect(context, action);
         else terminal(context, action);
      }
      fusion(context);
      return context;
   }

   /** Structural coverage hook used by the 24 x 4 regression matrix. */
   public static boolean isBuiltInAction(RunePosition position, String action) {
      if (position == null || action == null || action.isBlank()) return false;
      return switch (position) {
         case TRIGGER -> TRIGGER_ACTIONS.contains(action);
         case EFFECT -> EFFECT_ACTIONS.contains(action);
         case MODIFIER -> MODIFIER_ACTIONS.contains(action);
         case TERMINAL -> TERMINAL_ACTIONS.contains(action);
      };
   }

   public static int projectileCountForSplit(int splitCount) { return 1 << Math.min(3, Math.max(0, splitCount)); }
   public static double projectileDamageForSplit(double damage, int splitCount) {
      int count = Math.min(3, Math.max(0, splitCount));
      return Math.max(0.0D, damage) * Math.pow(.80D, count);
   }

   private static final java.util.Set<String> TRIGGER_ACTIONS = java.util.Set.of(
      "materialize","charge","lightning","mind_blast","teleport","teleport_15","fireball","life_link","inspire","ice_spike","shadow_chain","freeze_aura","vine_bind","defense_barrier","fate_dice","guardian_shield","beam","sure_strike","healing_wave","mirror_clone","water_impact","nature_enchant","dawn","ancestor_summon");
   private static final java.util.Set<String> EFFECT_ACTIONS = java.util.Set.of(
      "create","life_drain","strength_amplify","lightning_addon","mind_addon","haste","fire_addon","connection","light_addon","frost_addon","shadow_addon","stasis","nature_addon","protection_addon","random_addon","sanctuary_addon","sharpness","healing_addon","transfer","self_addon","water_addon","earth_addon","cleanse_addon","soul_addon","fire","ice","heal","nature","barrier","shield","drain","light","shadow","pierce","water","earth","speed","mind","energy","impact");
   private static final java.util.Set<String> MODIFIER_ACTIONS = java.util.Set.of(
      "amplify","power","shatter","range","swift","heat","link","duration","area","cost","slow","repeat","resist","luck","guard","accuracy","critical","restore","mobility","focus","control","store","fuse","persist","split","pierce","volley","guidance","ricochet","chain","empower","delay","gravity","freeze_behavior","cycle","rebound","random_modifier","sanctuary","acceleration","precision","renewal","synchronize","mirror","liquid","storage","cleanse","territory");
   private static final java.util.Set<String> TERMINAL_ACTIONS = java.util.Set.of(
      "release","harvest","wild","thunderstorm","revelation","portal","embers","share","aura","ice_blast","curse_residue","ice_prison","overgrowth","rewind","unknown","bulwark","cleanse","judgment","flourishing_field","harmony","fusion","tide","germination","dawn_field","territory_arrival","strike","detonate","seal","return","burn","transfer","bless","disperse","banish","sacrifice","endure","protect","complete","reveal","purify","execute","renew","escape","awaken","dissolve","discharge","anchor");

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

   /** Equipment enchantment effects from the complete rune table. */
   public static void applyWeaponEnchantments(net.minecraft.server.level.ServerPlayer player, LivingEntity target, RuneProgram program) {
      if (player == null || target == null || program == null) return;
      for (String id : program.sequence()) {
         RuneDefinition d = RuneRegistry.get(id);
         if (d == null) continue;
         switch (id) {
            case "fehu" -> player.heal(2.0F);
            case "uruz" -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0));
            case "thurisaz" -> { target.hurt(player.damageSources().magic(), 20.0F); target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 10)); }
            case "ansuz" -> { target.hurt(player.damageSources().magic(), 15.0F); target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20, 0)); }
            case "raidho" -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
            case "kenaz" -> { target.hurt(player.damageSources().magic(), 25.0F); target.igniteForSeconds(5.0F); }
            case "gebo" -> target.heal(10.0F);
            case "wunjo" -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0));
            case "hagalaz" -> { target.hurt(player.damageSources().magic(), 20.0F); target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)); }
            case "nauthiz" -> { target.hurt(player.damageSources().magic(), 20.0F); target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0)); }
            case "isa" -> { if (player.getRandom().nextFloat() < .30F) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 10)); }
            case "jera" -> { target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0)); target.hurt(player.damageSources().magic(), 15.0F); }
            case "eihwaz" -> { }
            case "perthro" -> target.hurt(player.damageSources().magic(), 30.0F + player.getRandom().nextInt(31));
            case "algiz" -> player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            case "sowilo" -> target.removeAllEffects();
            case "tiwaz" -> { }
            case "berkano" -> target.heal(15.0F);
            case "ehwaz" -> { Vec3 p = target.position().subtract(target.getLookAngle().scale(1.5D)); player.teleportTo(p.x, p.y, p.z); }
            case "mannaz" -> target.hurt(player.damageSources().magic(), 0.5F * Math.max(1.0F, player.getAttackStrengthScale(0.0F) * 10.0F));
            case "laguz" -> { Vec3 push = target.position().subtract(player.position()).normalize(); target.setDeltaMovement(target.getDeltaMovement().add(push.scale(.7D))); }
            case "ingwaz" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 10));
            case "dagaz" -> target.removeAllEffects();
            case "othala" -> target.hurt(player.damageSources().magic(), 20.0F);
            default -> { }
         }
      }
   }

   /** Tool-specific inscription effects from the rune table. */
   public static void applyToolEnchantments(net.minecraft.server.level.ServerPlayer player, RuneProgram program) {
      if (player == null || program == null) return;
      for (String id : program.sequence()) {
         switch (id) {
            case "fehu" -> player.getPersistentData().putDouble("tmwRuneDropMultiplier", 3.0D);
            case "uruz" -> player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 1));
            case "thurisaz" -> player.getPersistentData().putDouble("tmwRuneToolBlastRadius", 3.0D);
            case "ansuz" -> player.getPersistentData().putInt("tmwRuneOreSenseTicks", 200);
            case "raidho" -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
            case "kenaz" -> player.getPersistentData().putBoolean("tmwRuneAutoSmelt", true);
            case "gebo" -> player.getPersistentData().putBoolean("tmwRuneSharedExperience", true);
            case "wunjo" -> player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0));
            case "hagalaz" -> player.getPersistentData().putInt("tmwRuneToolFreezeRadius", 2);
            case "nauthiz" -> player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0));
            case "isa" -> player.getPersistentData().putBoolean("tmwRuneFreezeWater", true);
            case "jera" -> player.getPersistentData().putDouble("tmwRuneHarvestMultiplier", 2.0D);
            case "eihwaz" -> player.getPersistentData().putBoolean("tmwRuneUnbreakableTool", true);
            case "perthro" -> player.getPersistentData().putBoolean("tmwRuneRandomDrop", true);
            case "algiz" -> player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
            case "sowilo" -> player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 3));
            case "tiwaz" -> player.getPersistentData().putBoolean("tmwRunePreciseMining", true);
            case "berkano" -> player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            case "ehwaz" -> teleport(new RuneExecutionContext(player, player, program), 5.0D);
            case "mannaz" -> player.getPersistentData().putBoolean("tmwRuneMirrorDrop", true);
            case "laguz" -> player.getPersistentData().putBoolean("tmwRuneWaterPush", true);
            case "ingwaz" -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
            case "dagaz" -> player.removeAllEffects();
            case "othala" -> player.getPersistentData().putDouble("tmwRuneExperienceMultiplier", 2.0D);
            default -> { }
         }
      }
      emitProgramGlyphs(player, program);
   }

   /** Armor runes react to an incoming hit and provide their listed protection. */
   public static float applyArmorEnchantments(net.minecraft.server.level.ServerPlayer player, float amount, RuneProgram program) {
      if (player == null || program == null) return amount;
      float result = amount;
      for (String id : program.sequence()) {
         switch (id) {
            case "fehu" -> player.heal(2.0F);
            case "uruz" -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0));
            case "ansuz" -> { }
            case "raidho" -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0));
            case "kenaz" -> { player.setRemainingFireTicks(0); }
            case "gebo" -> result *= .70F;
            case "wunjo" -> result *= .50F;
            case "hagalaz" -> { }
            case "nauthiz" -> player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0));
            case "isa" -> result *= .80F;
            case "jera" -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            case "eihwaz" -> player.heal(20.0F);
            case "perthro" -> { if (player.getRandom().nextFloat() < .30F) result = 0.0F; }
            case "algiz" -> { result *= .80F; player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 4)); }
            case "sowilo" -> result *= .50F;
            case "tiwaz" -> { }
            case "berkano" -> player.heal(10.0F);
            case "ehwaz" -> { if (player.getHealth() < player.getMaxHealth() * .20F) teleport(new RuneExecutionContext(player, player, program), 10.0D); }
            case "mannaz" -> { }
            case "laguz" -> { }
            case "ingwaz" -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
            case "dagaz" -> player.removeAllEffects();
            case "othala" -> result *= .50F;
            default -> { }
         }
      }
      return Math.max(0.0F, result);
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
         String action = RuneRegistry.actionKey(id, position);
         ctx.trace(position, semantic);
         BiConsumer<RuneExecutionContext, String> custom = CUSTOM_ACTIONS.get(position.name() + ":" + semantic);
         if (custom != null) {
            custom.accept(ctx, semantic);
            if (ctx.failed()) return;
            continue;
         }
         switch (position) {
            case TRIGGER -> trigger(ctx, action);
            case EFFECT -> effect(ctx, action);
            case MODIFIER -> { /* primed before trigger/effect phases */ }
            case TERMINAL -> terminal(ctx, action);
            default -> { }
         }
         if (ctx.failed()) return;
      }
   }

   /** Modifier bands are declarations; prime their values before trigger execution. */
   private static void primeModifiers(RuneExecutionContext c) {
      for (int i = 0; i < c.program().sequence().size(); i++) {
         RunePosition role = i < c.program().sequencePositions().size() ? c.program().sequencePositions().get(i) : RunePosition.EFFECT;
         if (role != RunePosition.MODIFIER) continue;
         RuneDefinition d = RuneRegistry.get(c.program().sequence().get(i));
         if (d != null) {
            String semantic = d.semantic(role);
            BiConsumer<RuneExecutionContext, String> custom = CUSTOM_ACTIONS.get(role.name() + ":" + semantic);
            if (custom != null) custom.accept(c, semantic); else modifier(c, RuneRegistry.actionKey(d.idPath(), role));
         }
      }
   }

   private static void trigger(RuneExecutionContext c, String s) {
      switch (s) {
         case "materialize" -> directionalBullet(c, MagicBulletProjectileEntity.ELEMENT_NONE, 50.0D, 1.0F, 0.0D, 20.0D);
         case "charge" -> charge(c, 20.0D, 5.0D);
          case "lightning" -> {
             Vec3 p = c.endpoint(30);
             int bolts = 1 + 2 * c.modifierCount("volley");
             for (int i = 0; i < bolts; i++) { lightning(c, p); area(c, p, 5, effectiveDamage(c, 50.0D), ModParticles.ELEMENTAL_LIGHTNING.get()); }
          }
         case "mind_blast" -> { area(c, c.endpoint(20), 20, effectiveDamage(c, 30.0D), ParticleTypes.WITCH); if (c.hasExplicitTarget()) c.target().addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0)); }
         case "teleport" -> teleport(c, 20);
         case "teleport_15" -> teleport(c, 15);
         case "fireball" -> directionalBullet(c, MagicBulletProjectileEntity.ELEMENT_FIRE, 50, 1.5F, 5, 40);
         case "life_link" -> link(c, 8);
         case "inspire" -> { c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0)); c.emit(ParticleTypes.END_ROD, c.caster().position().add(0, 1, 0), 20); }
         case "ice_spike" -> area(c, c.endpoint(20), 5, effectiveDamage(c, 35.0D), ParticleTypes.SNOWFLAKE);
         case "shadow_chain" -> { link(c, 8); c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3)); }
         case "freeze_aura" -> freezeArea(c, c.caster().position(), 10, 60);
         case "vine_bind" -> { link(c, 5); c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2)); }
         case "defense_barrier" -> { c.caster().addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 9)); c.emit(ModParticles.RUNE_BARRIER.get(), c.caster().position().add(0, 1, 0), 20); }
         case "fate_dice" -> directionalBullet(c, MagicBulletProjectileEntity.ELEMENT_NONE, 50, 1.0F, 0, 20 + c.caster().getRandom().nextInt(41));
         case "guardian_shield" -> { c.caster().addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 7)); c.emit(ModParticles.RUNE_BARRIER.get(), c.caster().position().add(0, 1, 0), 20); }
         case "beam" -> { VFXServerEffects.spawnOriented(c.level(), "beam", c.origin(), c.direction(), 50); lineDamage(c, 50, effectiveDamage(c, 60.0D)); }
         case "sure_strike" -> lineDamage(c, 15, effectiveDamage(c, 50.0D));
         case "healing_wave" -> healArea(c, c.endpoint(8), 15, 50);
         case "mirror_clone" -> spawnAncestor(c);
         case "water_impact" -> { directionalBullet(c, MagicBulletProjectileEntity.ELEMENT_WATER, 50, 1.2F, 0, 30); knockback(c, 10); }
         case "nature_enchant" -> { c.caster().getPersistentData().putInt("tmwRuneNatureDamage", 20); c.emit(ModParticles.JERA_RUNE.get(), c.caster().position().add(0, 1, 0), 12); }
         case "dawn" -> { area(c, c.endpoint(20), 20, 0, ParticleTypes.END_ROD); cleanse(c, 20, false); }
         case "ancestor_summon" -> { c.caster().getPersistentData().putBoolean("tmwRuneSoulDamage", true); spawnAncestor(c); }
         case "wealth" -> directionalBullet(c, MagicBulletProjectileEntity.ELEMENT_NONE, 50.0D, 1.0F, 0.0D, 20.0D);
         case "break" -> { Vec3 p = c.endpoint(30.0D); lightning(c, p); area(c, p, 5.0D, effectiveDamage(c, 50.0D), ModParticles.ELEMENTAL_LIGHTNING.get()); }
         case "strength" -> charge(c, 20.0D, 5.0D);
         case "self" -> {
            c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0));
            c.emit(ParticleTypes.ENCHANT, c.caster().position().add(0, 1, 0), 16);
         }
         case "storm" -> { Vec3 p = c.endpoint(20.0D); area(c, p, 5.0D, effectiveDamage(c, 35.0D), ParticleTypes.SNOWFLAKE); }
         case "command" -> { area(c, c.endpoint(20.0D), 20.0D, effectiveDamage(c, 30.0D), ParticleTypes.WITCH); if (c.hasExplicitTarget()) c.target().addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0)); }
         case "move" -> teleport(c, 20.0D);
         case "travel" -> teleport(c, 15.0D);
         case "ignite" -> directionalBullet(c, MagicBulletProjectileEntity.ELEMENT_FIRE, 50.0D, 1.5F, 5.0D, 40.0D);
         case "bind" -> { link(c, 8.0D); c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3)); }
         case "joy" -> { c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0)); c.emit(ParticleTypes.END_ROD, c.caster().position().add(0, 1, 0), 20); }
         case "need" -> { link(c, 8.0D); area(c, c.endpoint(8.0D), 8.0D, 0.0D, ParticleTypes.SQUID_INK); }
         case "freeze" -> freezeArea(c, c.caster().position(), 10.0D, 60);
         case "ward", "protect" -> { c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0)); c.emit(ModParticles.RUNE_BARRIER.get(), c.caster().position().add(0, 1, 0), 20); }
         case "shine" -> { VFXServerEffects.spawnOriented(c.level(), "beam", c.origin(), c.direction(), 50.0D); lineDamage(c, 50.0D, effectiveDamage(c, 60.0D)); }
         case "judge" -> { lineDamage(c, 15.0D, effectiveDamage(c, 50.0D)); c.emit(ParticleTypes.SWEEP_ATTACK, c.endpoint(8), 12); }
         case "grow" -> healArea(c, c.endpoint(8.0D), 15.0D, 50.0F);
         case "chance" -> directionalBullet(c, MagicBulletProjectileEntity.ELEMENT_NONE, 50.0D, 1.0F, 0.0D, 20.0D + c.caster().getRandom().nextInt(41));
         case "cycle" -> { link(c, 5.0D); c.repeats(2); }
         case "flow" -> { directionalBullet(c, MagicBulletProjectileEntity.ELEMENT_WATER, 50.0D, 1.2F, 0.0D, 30.0D); knockback(c, 10.0D); }
         case "transform" -> { area(c, c.endpoint(20.0D), 20.0D, 0.0D, ParticleTypes.END_ROD); cleanse(c, 20.0D, false); }
         case "inherit" -> {
            c.caster().getPersistentData().putBoolean("tmwRuneSoulDamage", true);
            c.level().sendParticles(ParticleTypes.SOUL, c.caster().getX(), c.caster().getY(1.0), c.caster().getZ(), 24, .4, .6, .4, .02);
         }
         default -> { }
      }
   }

   /** Calculates effect/modifier bonuses before non-projectile triggers fire. */
   private static double effectiveDamage(RuneExecutionContext c, double base) {
      double value = Math.max(0.0D, base);
      for (int i = 0; i < c.program().sequence().size(); i++) {
         RunePosition role = i < c.program().sequencePositions().size() ? c.program().sequencePositions().get(i) : RunePosition.EFFECT;
         String action = RuneRegistry.actionKey(c.program().sequence().get(i), role);
         if (role == RunePosition.EFFECT) {
            switch (action) {
               case "strength_amplify", "stasis" -> value *= 1.50D;
               case "lightning_addon" -> value += 30.0D;
               case "mind_addon" -> value += 15.0D;
               case "fire_addon" -> value += 10.0D;
               case "shadow_addon", "nature_addon" -> value += 20.0D;
               case "water_addon" -> value *= 1.20D;
               case "energy", "impact" -> value += 30.0D;
               default -> { }
            }
         } else if (role == RunePosition.MODIFIER) {
            switch (action) {
               case "power", "empower" -> value *= 1.50D;
               case "precision", "accuracy", "focus" -> value *= 1.50D;
               case "split" -> { if (!c.projectileImpact()) value *= .80D; }
               default -> { }
            }
         }
      }
      return Math.min(1000.0D, value);
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

   private static int actionCount(RuneExecutionContext c, String action) {
      int count = 0;
      for (int i = 0; i < c.program().sequence().size(); i++) {
         RunePosition role = i < c.program().sequencePositions().size() ? c.program().sequencePositions().get(i) : RunePosition.EFFECT;
         if (action.equals(RuneRegistry.actionKey(c.program().sequence().get(i), role))) count++;
      }
      return count;
   }

   private static boolean hasProjectileTrigger(RuneExecutionContext c) {
      for (int i = 0; i < c.program().sequence().size(); i++) {
         RunePosition role = i < c.program().sequencePositions().size() ? c.program().sequencePositions().get(i) : RunePosition.EFFECT;
         if (role != RunePosition.TRIGGER) continue;
         String action = RuneRegistry.actionKey(c.program().sequence().get(i), role);
         if (action.equals("materialize") || action.equals("fireball") || action.equals("fate_dice") || action.equals("water_impact")) return true;
      }
      return false;
   }

   private static void teleport(RuneExecutionContext c, double distance) {
      Vec3 p = c.caster().position().add(c.direction().scale(distance));
      c.caster().teleportTo(p.x, p.y, p.z);
      c.emit(ParticleTypes.PORTAL, p, 20);
   }

   private static void charge(RuneExecutionContext c, double distance, double knockback) {
      Vec3 start = c.caster().position();
      c.caster().setDeltaMovement(c.direction().scale(1.25D));
      for (int i = 1; i <= (int)distance; i++) {
         Vec3 point = start.add(c.direction().scale(i));
         for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(1.0D), x -> x.isAlive() && x != c.caster())) {
            e.hurt(c.caster().damageSources().magic(), (float)Math.max(1.0D, effectiveDamage(c, c.damage())));
            e.setDeltaMovement(e.getDeltaMovement().add(c.direction().scale(.35D).add(0, .2D, 0)));
         }
         if ((i & 3) == 0) c.emit(ParticleTypes.SWEEP_ATTACK, point, 1);
      }
   }

   private static void knockback(RuneExecutionContext c, double distance) {
      Vec3 center = c.hasExplicitTarget() ? c.target().position() : c.endpoint(Math.min(20, distance));
      for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(Math.max(2, c.radius())), x -> x.isAlive() && x != c.caster())) {
         Vec3 away = e.position().subtract(c.caster().position());
         if (away.lengthSqr() < 1.0E-4) away = c.direction();
         e.setDeltaMovement(e.getDeltaMovement().add(away.normalize().scale(.8D).add(0, .25D, 0)));
         e.hurtMarked = true;
      }
   }

   private static void link(RuneExecutionContext c, double radius) {
      c.radius(Math.min(16.0D, radius));
      c.emit(ModParticles.GEBO_RUNE.get(), c.target().position().add(0, 1, 0), 12);
      if (c.hasExplicitTarget()) {
         // The target carries the link; the damage event handler performs the
         // authoritative 50/50 split while keeping the caster server-side.
         c.target().getPersistentData().putUUID("tmwRuneLinkOwner", c.caster().getUUID());
         c.target().getPersistentData().putLong("tmwRuneLinkUntil", c.level().getGameTime() + 100L);
      }
   }

   private static void freezeArea(RuneExecutionContext c, Vec3 center, double radius, int ticks) {
      c.emit(ParticleTypes.SNOWFLAKE, center, 24);
      for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius), x -> x.isAlive() && x != c.caster())) {
         e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 10));
         e.setDeltaMovement(Vec3.ZERO);
      }
   }

   private static void healArea(RuneExecutionContext c, Vec3 center, double radius, float amount) {
      c.emit(ParticleTypes.HAPPY_VILLAGER, center, 24);
      for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius), LivingEntity::isAlive)) e.heal(amount);
   }

   private static void cleanse(RuneExecutionContext c, double radius, boolean includeCaster) {
      Vec3 center = c.hasExplicitTarget() ? c.target().position() : c.endpoint(radius);
      for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius), x -> x.isAlive() && (includeCaster || x != c.caster()))) e.removeAllEffects();
      c.emit(ParticleTypes.END_ROD, center, 24);
   }

   private static void shareEffects(RuneExecutionContext c) {
      for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, new AABB(c.caster().position(), c.caster().position()).inflate(10), x -> x.isAlive() && x != c.caster())) {
         if (c.caster().hasEffect(MobEffects.DAMAGE_BOOST)) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0));
         if (c.caster().hasEffect(MobEffects.DAMAGE_RESISTANCE)) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
      }
      c.emit(ModParticles.EHWAZ_RUNE.get(), c.caster().position().add(0, 1, 0), 16);
   }

   private static void spawnAncestor(RuneExecutionContext c) {
      c.emit(ParticleTypes.SOUL, c.caster().position().add(0, 1, 0), 32);
      c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1));
   }

   private static void directionalBullet(RuneExecutionContext c, int element, double range, float scale, double explosionRadius) {
      directionalBullet(c, element, range, scale, explosionRadius, c.damage());
   }

   private static void directionalBullet(RuneExecutionContext c, int element, double range, float scale, double explosionRadius, double damage) {
      int split = Math.min(3, c.modifierCount("split"));
      int count = projectileCountForSplit(split);
      float splitDamage = (float)projectileDamageForSplit(damage, split);
      double speedMultiplier = 1.0D + 2.0D * c.modifierCount("acceleration")
         + c.modifierCount("haste") + actionCount(c, "haste");
      int pierce = Math.min(3, c.modifierCount("pierce"));
      int ricochet = Math.min(5, c.modifierCount("ricochet"));
      boolean tracking = c.modifierCount("guidance") > 0;
      boolean gravity = c.modifierCount("gravity") > 0;
      Vec3 direction = c.direction();
      Vec3 right = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
      if (right.lengthSqr() < 1.0E-6) right = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
      right = right.normalize();
      for (int i = 0; i < count; i++) {
         MagicBulletProjectileEntity projectile = new MagicBulletProjectileEntity(c.level(), c.caster());
         projectile.setPos(c.origin());
         projectile.configure(splitDamage, 0.0F, range, element, scale);
         projectile.setRuneExplosionRadius(explosionRadius);
         Vec3 shotDirection = count == 1 ? direction : direction.add(right.scale((i - (count - 1) / 2.0D) * .08D)).normalize();
         projectile.shoot(shotDirection.x, shotDirection.y, shotDirection.z, (float)(2.5D * speedMultiplier), 0.0F);
         var tag = projectile.getPersistentData();
         tag.putBoolean("tmwRuneProjectile", true);
         tag.put("tmwRuneProgram", c.program().serializeNBT());
         tag.putBoolean("tmwRuneTracking", tracking);
         tag.putDouble("tmwRuneTrackingRange", tracking ? 50.0D : 0.0D);
         tag.putBoolean("tmwRuneSureHit", tracking && c.modifierCount("precision") > 0);
         tag.putInt("tmwRuneTrackingStrength", tracking ? Math.min(8, c.modifierCount("guidance") + c.modifierCount("precision")) : 0);
         tag.putInt("tmwRunePierceRemaining", pierce);
         tag.putInt("tmwRuneRicochetRemaining", ricochet);
         tag.putBoolean("tmwRuneLiquid", c.modifierCount("liquid") > 0);
         tag.putInt("tmwRuneCycleRemaining", c.modifierCount("cycle"));
         tag.putInt("tmwRuneReboundRemaining", Math.min(3, c.modifierCount("rebound")));
         tag.putBoolean("tmwRuneGravity", gravity);
         int delay = Math.min(40, c.effectDuration("delay", 0));
         if (delay > 0) {
            tag.putInt("tmwRuneDelayRemaining", delay);
            Vec3 velocity = projectile.getDeltaMovement();
            tag.putDouble("tmwRuneVelocityX", velocity.x); tag.putDouble("tmwRuneVelocityY", velocity.y); tag.putDouble("tmwRuneVelocityZ", velocity.z);
         }
         c.level().addFreshEntity(projectile);
      }
      c.markProjectilesSpawned(count);
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
         case "life_drain" -> { if (c.hasExplicitTarget()) c.caster().heal((float)Math.max(1.0D, c.damage() * .30D)); }
         case "strength_amplify" -> c.damage(c.damage() * 1.50D);
         case "lightning_addon" -> { c.addDamage(30); if (c.hasExplicitTarget()) c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 10)); }
         case "mind_addon" -> { c.addDamage(15); if (c.hasExplicitTarget()) c.target().addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0)); }
         case "haste" -> { c.addDamage(0); c.caster().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1)); }
         case "fire_addon" -> { c.addDamage(10); if (c.hasExplicitTarget()) c.target().igniteForSeconds(5); }
         case "connection" -> link(c, 5);
         case "light_addon" -> c.emit(ParticleTypes.END_ROD, c.target().position().add(0, 1, 0), 16);
         case "frost_addon" -> { if (c.hasExplicitTarget()) c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)); }
         case "shadow_addon" -> { c.addDamage(20); if (c.hasExplicitTarget()) c.target().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0)); }
         case "stasis" -> { c.damage(c.damage() * 1.50D); if (c.hasExplicitTarget()) c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)); }
         case "nature_addon" -> { if (c.hasExplicitTarget()) c.target().addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0)); c.addDamage(15); }
         case "protection_addon" -> c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
         case "random_addon" -> { if (c.caster().getRandom().nextBoolean()) c.addDamage(20); else c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)); }
         case "sanctuary_addon" -> c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
         case "sharpness" -> c.pierce(true);
         case "healing_addon" -> { if (c.hasExplicitTarget()) c.target().heal(20); else healArea(c, c.endpoint(8), 15, 20); }
         case "transfer" -> shareEffects(c);
         case "self_addon" -> c.addDamage(0);
         case "water_addon" -> c.addDamage(c.damage() * .20D);
         case "earth_addon" -> { if (c.hasExplicitTarget()) c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 10)); c.addDamage(0); }
         case "cleanse_addon" -> cleanse(c, 8, false);
         case "soul_addon" -> c.pierce(true);
         case "fire" -> { c.target().setRemainingFireTicks(Math.max(100, c.target().getRemainingFireTicks())); c.addDamage(10.0D); }
         case "ice" -> { if (c.hasExplicitTarget()) c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)); }
         case "heal", "nature" -> c.target().heal(20.0F);
         case "barrier", "shield" -> { c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0)); c.emit(ModParticles.RUNE_BARRIER.get(), c.caster().position().add(0, 1, 0), 12); }
         case "drain" -> { if (c.hasExplicitTarget()) c.caster().heal((float)Math.max(1.0D, c.damage() * .30D)); }
         case "light" -> c.emit(ParticleTypes.END_ROD, c.target().position().add(0, 1, 0), 20);
         case "shadow" -> { if (c.hasExplicitTarget()) c.target().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0)); }
         case "pierce" -> c.pierce(true);
         case "water" -> c.addDamage(Math.max(1.0D, c.damage() * .20D));
         case "earth" -> { if (c.hasExplicitTarget()) { c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4)); c.target().setDeltaMovement(c.target().getDeltaMovement().multiply(.2, 1, .2)); } }
         case "speed" -> c.emit(ModParticles.ELEMENTAL_FOAM.get(), c.origin(), 10);
         case "mind" -> { if (c.hasExplicitTarget()) c.target().addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0)); }
         case "energy", "impact" -> c.addDamage(30.0D);
         default -> { }
      }
      c.updateLastEffect(s);
   }

   private static void modifier(RuneExecutionContext c, String s) {
      c.addModifier(s);
      switch (s) {
         case "split" -> { c.repeats(c.repeats() * 2); if (!c.projectileImpact()) c.damage(c.damage() * .80D); }
         case "pierce" -> c.pierce(true);
         case "volley" -> c.repeats(c.repeats() + 2);
         case "guidance" -> c.radius(Math.max(c.radius(), 50));
         case "ricochet", "chain" -> c.repeats(c.repeats() + 5);
         case "empower" -> c.damage(c.damage() * 1.50D);
         case "delay" -> c.setEffectDuration("delay", 20);
         case "gravity" -> c.setEffectDuration("gravity", 1);
         case "freeze_behavior" -> c.radius(Math.max(c.radius(), 5));
         case "cycle" -> c.repeats(c.repeats() + 1);
         case "rebound" -> c.repeats(c.repeats() + 3);
         case "random_modifier" -> c.repeats(c.repeats() + c.caster().getRandom().nextInt(2));
         case "sanctuary" -> c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
         case "acceleration" -> c.addDamage(c.damage() * .20D);
         case "precision" -> c.addDamage(c.damage() * .50D);
         case "renewal" -> c.caster().heal(20);
         case "synchronize" -> shareEffects(c);
         case "mirror" -> c.repeats(c.repeats() + 1);
          case "liquid" -> c.pierce(true);
         case "storage" -> c.emit(ParticleTypes.ENCHANT, c.caster().position().add(0, 1, 0), 8);
         case "cleanse" -> cleanse(c, 5, false);
         case "territory" -> c.radius(Math.max(c.radius(), 10));
         case "amplify" -> { c.repeats(c.repeats() + 1); c.damage(c.damage() * .80D); }
         case "power" -> c.damage(c.damage() * 1.50D);
         case "shatter", "repeat" -> c.repeats(c.repeats() + 2);
         case "range", "area" -> c.radius(Math.max(3.0D, c.radius() + 4.0D));
          case "swift", "mobility" -> c.repeats(c.repeats() + 1);
         case "heat" -> c.setEffectDuration("delay", 20);
         case "link", "control" -> c.repeats(c.repeats() + 1);
         case "duration", "persist" -> c.setEffectDuration(s, 100 + c.modifierCount(s) * 40);
         case "accuracy", "focus", "critical" -> c.addDamage(c.damage() * .50D);
         case "slow" -> c.setEffectDuration(s, 40);
         case "resist", "guard" -> c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
         case "restore" -> c.caster().heal(20.0F);
          case "cost" -> c.damage(c.damage() * 1.10D);
          case "luck" -> { if (c.caster().getRandom().nextBoolean()) c.repeats(c.repeats() + 1); else c.addDamage(2.0D); }
          case "store" -> c.emit(ParticleTypes.ENCHANT, c.caster().position().add(0, 1, 0), 8);
          case "fuse" -> c.addDamage(1.0D);
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
         case "harvest" -> {
            Vec3 center = c.hasExplicitTarget() ? c.target().position() : c.endpoint(12);
            area(c, center, 3, 0, ParticleTypes.HAPPY_VILLAGER);
            if (c.hasExplicitTarget() && c.target().isAlive()) c.target().spawnAtLocation(new ItemStack(Items.GOLD_NUGGET, 3), 0.1F);
         }
         case "wild" -> { area(c, c.target().position(), 8, 0, ParticleTypes.SWEEP_ATTACK); knockback(c, 8); }
         case "thunderstorm" -> { Vec3 p = c.hasExplicitTarget() ? c.target().position() : c.endpoint(30); scheduleAreaPulse(c, p, 8, 100, 20, 50, 0, ModParticles.ELEMENTAL_LIGHTNING.get()); }
         case "revelation" -> { if (c.hasExplicitTarget()) c.target().addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0)); }
         case "portal" -> {
            Vec3 a = c.caster().position();
            Vec3 b = c.endpoint(20.0D);
            var tag = c.caster().getPersistentData();
            tag.putDouble("tmwRunePortalAX", a.x); tag.putDouble("tmwRunePortalAY", a.y); tag.putDouble("tmwRunePortalAZ", a.z);
            tag.putDouble("tmwRunePortalBX", b.x); tag.putDouble("tmwRunePortalBY", b.y); tag.putDouble("tmwRunePortalBZ", b.z);
            tag.putLong("tmwRunePortalUntil", c.level().getGameTime() + 600L);
            tag.putInt("tmwRunePortalLastSide", 1);
            c.emit(ParticleTypes.PORTAL, a.add(0, 1, 0), 32); c.emit(ParticleTypes.PORTAL, b.add(0, 1, 0), 32);
         }
         case "embers" -> scheduleAreaPulse(c, c.hasExplicitTarget() ? c.target().position() : c.endpoint(12), 5, 200, 20, 10, 0, ParticleTypes.FLAME);
         case "share" -> shareEffects(c);
         case "aura" -> scheduleAreaPulse(c, c.caster().position(), 8, 300, 20, 0, 5, ParticleTypes.END_ROD);
         case "ice_blast" -> freezeArea(c, c.hasExplicitTarget() ? c.target().position() : c.endpoint(20), 8, 40);
         case "curse_residue" -> scheduleAreaPulse(c, c.hasExplicitTarget() ? c.target().position() : c.endpoint(8), 5, 200, 20, 10, 0, ParticleTypes.SQUID_INK);
         case "ice_prison" -> freezeArea(c, c.target().position(), 2, 100);
         case "overgrowth" -> { Vec3 p = c.hasExplicitTarget() ? c.target().position() : c.endpoint(8); area(c, p, 8, 0, ParticleTypes.HAPPY_VILLAGER); slowArea(c, p, 8, 200); }
         case "rewind" -> c.caster().heal(50);
         case "unknown" -> { int roll = c.caster().getRandom().nextInt(3); if (roll == 0) cleanse(c, 8, true); else if (roll == 1) healArea(c, c.caster().position(), 8, 10); else area(c, c.target().position(), 8, 20, ParticleTypes.END_ROD); }
         case "bulwark" -> { c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1)); area(c, c.caster().position(), 8, 0, ParticleTypes.END_ROD); }
         case "cleanse" -> cleanse(c, 10, true);
         case "judgment" -> { if (c.target().getHealth() / c.target().getMaxHealth() < .30F) c.target().hurt(c.caster().damageSources().magic(), Float.MAX_VALUE); }
         case "flourishing_field" -> scheduleAreaPulse(c, c.caster().position(), 8, 300, 20, 0, 10, ParticleTypes.HAPPY_VILLAGER);
         case "harmony" -> harmony(c, 10);
         case "fusion" -> { c.damage(c.damage() * 1.5D); }
         case "tide" -> knockback(c, 10);
         case "germination" -> scheduleAreaPulse(c, c.hasExplicitTarget() ? c.target().position() : c.endpoint(8), 5, 200, 20, 15, 0, ParticleTypes.HAPPY_VILLAGER);
         case "dawn_field" -> { Vec3 p = c.caster().position(); scheduleAreaPulse(c, p, 10, 300, 20, 0, 0, ParticleTypes.END_ROD); cleanse(c, 10, true); }
         case "territory_arrival" -> { c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1)); area(c, c.caster().position(), 15, 0, ParticleTypes.END_ROD); }
         case "release" -> { }
         case "strike" -> { hit(c, 50.0D); knockback(c, 5.0D); }
         case "detonate" -> { hit(c, 50.0D); area(c, c.hasExplicitTarget() ? c.target().position() : c.endpoint(30), 8.0D, 50.0D, ModParticles.ELEMENTAL_LIGHTNING.get()); }
         case "seal" -> c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
         case "return" -> teleport(c, 20.0D);
         case "burn" -> { c.target().setRemainingFireTicks(Math.max(200, c.target().getRemainingFireTicks())); area(c, c.target().position(), 5, 0, ParticleTypes.FLAME); }
         case "transfer" -> shareEffects(c);
         case "bless" -> healArea(c, c.caster().position(), 8, 5);
         case "disperse", "banish" -> cleanse(c, 8.0D, true);
         case "sacrifice" -> { c.caster().hurt(c.caster().damageSources().magic(), 2.0F); hit(c, 10.0D); }
         case "endure", "protect" -> { c.caster().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1)); c.emit(ModParticles.RUNE_BARRIER.get(), c.caster().position().add(0, 1, 0), 20); }
         case "complete" -> area(c, c.endpoint(8), 8, 0, ParticleTypes.HAPPY_VILLAGER);
         case "reveal" -> { if (c.hasExplicitTarget()) c.target().addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0)); }
         case "purify" -> cleanse(c, 10.0D, true);
         case "execute" -> { if (c.target().getHealth() / c.target().getMaxHealth() < .30F) c.target().hurt(c.caster().damageSources().magic(), Float.MAX_VALUE); else hit(c, 50.0D); }
         case "renew" -> healArea(c, c.caster().position(), 8, 10);
         case "escape" -> teleport(c, 10.0D);
         case "awaken" -> { spawnAncestor(c); }
         case "dissolve" -> { knockback(c, 10.0D); }
         case "discharge" -> area(c, c.target().position(), 5, 15, ModParticles.ELEMENTAL_LIGHTNING.get());
         case "anchor" -> area(c, c.caster().position(), 15, 0, ParticleTypes.END_ROD);
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

   private static void scheduleAreaPulse(RuneExecutionContext c, Vec3 center, double radius, int duration, int interval,
      double damage, double healing, net.minecraft.core.particles.ParticleOptions particle) {
      if (!(c.level() instanceof ServerLevel level) || center == null) return;
      int pulses = Math.max(1, duration / Math.max(1, interval));
      java.util.UUID casterId = c.caster().getUUID();
      for (int pulse = 0; pulse < pulses; pulse++) {
         final int delay = pulse * Math.max(1, interval);
         final Vec3 point = center;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (!level.isLoaded(net.minecraft.core.BlockPos.containing(point))) return;
            LivingEntity owner = level.getEntity(casterId) instanceof LivingEntity living ? living : null;
            if (owner == null || !owner.isAlive()) return;
            if (particle != null) level.sendParticles(particle, point.x, point.y, point.z, 8, .3, .15, .3, .01);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius),
               entity -> entity.isAlive() && isFriendly(owner, entity))) {
               if (damage > 0) entity.hurt(owner.damageSources().magic(), (float)damage);
               if (damage > 0 && entity == owner) continue;
               if (healing > 0) entity.heal((float)healing);
            }
         });
      }
   }

   private static void slowArea(RuneExecutionContext c, Vec3 center, double radius, int ticks) {
      for (LivingEntity entity : c.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
         entity -> entity.isAlive() && entity != c.caster())) entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
   }

   private static boolean isFriendly(LivingEntity owner, LivingEntity entity) {
      return entity == owner || owner.isAlliedTo(entity);
   }

   private static void harmony(RuneExecutionContext c, double radius) {
      List<LivingEntity> allies = c.level().getEntitiesOfClass(LivingEntity.class,
         new AABB(c.caster().position(), c.caster().position()).inflate(radius), LivingEntity::isAlive);
      if (allies.isEmpty()) return;
      float average = 0.0F;
      for (LivingEntity ally : allies) average += ally.getHealth();
      average /= allies.size();
      for (LivingEntity ally : allies) ally.setHealth(Math.min(ally.getMaxHealth(), average));
      c.emit(ParticleTypes.END_ROD, c.caster().position().add(0, 1, 0), 16);
   }

   private static void hit(RuneExecutionContext c, double amount) {
       double resolvedDamage = Math.max(amount, c.damage());
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
          case "fehu" -> new RuneVisualProfile(0xFFFFC857, 50.0D, 0.0D, "materialize_bolt");
          case "ansuz" -> new RuneVisualProfile(0xFFC9A7FF, 20.0D, 20.0D, "mind_wave");
          case "gebo" -> new RuneVisualProfile(0xFF55E38E, 8.0D, 8.0D, "life_link");
          case "wunjo" -> new RuneVisualProfile(0xFFFFD84D, 0.0D, 8.0D, "golden_aura");
          case "jera" -> new RuneVisualProfile(0xFF58D68D, 5.0D, 8.0D, "vine_field");
          case "eihwaz" -> new RuneVisualProfile(0xFFBFE8FF, 0.0D, 8.0D, "defense_barrier");
          case "perthro" -> new RuneVisualProfile(0xFFEC78D0, 50.0D, 0.0D, "fate_dice");
          case "tiwaz" -> new RuneVisualProfile(0xFFFFD34E, 15.0D, 15.0D, "sure_slash");
          case "ehwaz" -> new RuneVisualProfile(0xFFEAF4FF, 15.0D, 10.0D, "teleport_harmony");
          case "mannaz" -> new RuneVisualProfile(0xFFC9A7FF, 0.0D, 0.0D, "mirror_clone");
          case "laguz" -> new RuneVisualProfile(0xFF4DB8FF, 50.0D, 10.0D, "water_impact");
          case "ingwaz" -> new RuneVisualProfile(0xFF68D391, 0.0D, 5.0D, "germination_orb");
          case "dagaz" -> new RuneVisualProfile(0xFFFFE07A, 20.0D, 10.0D, "dawn_purify");
          case "othala" -> new RuneVisualProfile(0xFFFFD84D, 0.0D, 15.0D, "ancestor_territory");
          default -> new RuneVisualProfile(0xFFFFFFFF, 0.0D, 0.0D, "rune");
      };
   }

   public record RuneVisualProfile(int color, double length, double radius, String effectId) { }
}
