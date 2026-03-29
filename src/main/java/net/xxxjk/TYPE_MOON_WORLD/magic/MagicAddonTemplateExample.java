package net.xxxjk.TYPE_MOON_WORLD.magic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.INpcMagicAdapter;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

/**
 * Reference template for addon magics under the current modular system.
 *
 * <p>Player-side addon loading:
 * <ol>
 * <li>Implement {@link IMagicAddonEntrypoint} and register your magic id.</li>
 * <li>Create {@code META-INF/services/net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicAddonEntrypoint}
 *     and write the fully qualified entrypoint class name into it.</li>
 * <li>Implement the executor with {@link MagicExecutionContext} and return:
 *     {@link MagicExecutionResult#SUCCESS} when the cast finished,
 *     {@link MagicExecutionResult#FAILED} when the cast was handled but failed,
 *     {@link MagicExecutionResult#NOT_HANDLED} when the current entity/path is not yours.</li>
 * </ol>
 *
 * <p>Preset data is now stored per wheel slot. If the same magic appears in multiple slots,
 * read the current slot payload instead of using a shared global variable, otherwise the slots
 * will overwrite each other.
 *
 * <p>NPC-side integration is currently still manual. {@link INpcMagicAdapter} is a reusable shape,
 * but the core NPC caster does not auto-discover adapters yet. To make an addon magic available to
 * NPCs, wire the same magic id into:
 * <ol>
 * <li>{@code NpcMagicExecutionService.castMagic(...)} for the actual cast branch</li>
 * <li>{@code NpcMagicExecutionService.getGlobalCooldownAfterCast(...)} for global cooldown</li>
 * <li>{@code NpcMagicExecutionService.getPerMagicCooldown(...)} for per-magic cooldown</li>
 * </ol>
 * Then put the magic into the NPC wheel slot with its own {@code presetPayload}.
 *
 * <p>Minimal NPC hook example:
 * <pre>
 * private static final ExampleNpcMagicAdapter EXAMPLE_NPC = new ExampleNpcMagicAdapter();
 *
 * case MagicAddonTemplateExample.MAGIC_ID -&gt; EXAMPLE_NPC.cast(caster, target, vars, slot, effectiveProficiency, gameTime);
 * </pre>
 *
 * <p>If your executor modifies fields beyond mana/proficiency, call
 * {@code vars.syncPlayerVariables(player)} yourself inside the executor.
 */
public final class MagicAddonTemplateExample {
   public static final String MAGIC_ID = "example_magic_spell";
   private static final double DEFAULT_COST = 20.0;
   private static final float DEFAULT_DAMAGE = 6.0F;
   private static final double PLAYER_RANGE = 12.0;

   private MagicAddonTemplateExample() {
   }

   public static final class ExampleEntrypoint implements IMagicAddonEntrypoint {
      @Override
      public String providerId() {
         return "example_addon_mod";
      }

      @Override
      public void registerMagics(IMagicRegistry registry) {
         registry.register(MAGIC_ID, ExampleMagic::execute, this.providerId());
      }
   }

   public static final class ExampleMagic {
      private ExampleMagic() {
      }

      public static MagicExecutionResult execute(MagicExecutionContext context) {
         ServerPlayer player = context.asServerPlayer();
         if (player == null) {
            return MagicExecutionResult.NOT_HANDLED;
         } else {
            CompoundTag payload = getPlayerPresetPayload(context);
            LivingEntity target = resolvePlayerTarget(player);
            if (target == null) {
               player.displayClientMessage(Component.literal("Example magic needs a living target."), true);
               return MagicExecutionResult.FAILED;
            } else {
               double cost = readManaCost(payload, context.crestCast());
               if (!consumeMana(context.vars(), cost)) {
                  player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
                  return MagicExecutionResult.FAILED;
               } else {
                  castShared(player, target, readDamage(payload, context.crestCast()));
                  return MagicExecutionResult.SUCCESS;
               }
            }
         }
      }
   }

   /**
    * Example NPC adapter for the current combat flow.
    *
    * <p>The core does not auto-register this yet. Use the adapter as the target implementation
    * for the {@code NpcMagicExecutionService} switch branches.
    */
   public static final class ExampleNpcMagicAdapter implements INpcMagicAdapter {
      @Override
      public String magicId() {
         return MAGIC_ID;
      }

      @Override
      public boolean canCast(
         MysticMagicianEntity caster,
         LivingEntity target,
         TypeMoonWorldModVariables.PlayerVariables vars,
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry slot,
         double proficiency,
         long gameTime
      ) {
         if (caster == null || target == null || !target.isAlive() || vars == null || slot == null || slot.isEmpty() || !MAGIC_ID.equals(slot.magicId)) {
            return false;
         } else {
            CompoundTag payload = getSlotPayload(slot);
            return caster.hasLineOfSight(target) && ("crest".equals(slot.sourceType) || vars.player_mana >= readManaCost(payload, false));
         }
      }

      @Override
      public boolean cast(
         MysticMagicianEntity caster,
         LivingEntity target,
         TypeMoonWorldModVariables.PlayerVariables vars,
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry slot,
         double proficiency,
         long gameTime
      ) {
         if (!this.canCast(caster, target, vars, slot, proficiency, gameTime)) {
            return false;
         } else {
            CompoundTag payload = getSlotPayload(slot);
            double cost = readManaCost(payload, "crest".equals(slot.sourceType));
            if (!consumeMana(vars, cost)) {
               return false;
            } else {
               castShared(caster, target, readDamage(payload, "crest".equals(slot.sourceType)));
               return true;
            }
         }
      }
   }

   private static CompoundTag getPlayerPresetPayload(MagicExecutionContext context) {
      TypeMoonWorldModVariables.PlayerVariables vars = context.vars();
      if (vars == null) {
         return new CompoundTag();
      } else {
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry current = vars.getCurrentRuntimeWheelEntry();
         return current != null && context.magicId().equals(current.magicId) ? getSlotPayload(current) : new CompoundTag();
      }
   }

   private static CompoundTag getSlotPayload(TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry slot) {
      return slot != null && slot.presetPayload != null ? slot.presetPayload.copy() : new CompoundTag();
   }

   private static LivingEntity resolvePlayerTarget(ServerPlayer player) {
      HitResult hitResult = EntityUtils.getRayTraceTarget(player, PLAYER_RANGE);
      return hitResult instanceof EntityHitResult entityHitResult
            && entityHitResult.getEntity() instanceof LivingEntity living
            && EntityUtils.isValidCombatTarget(player, living)
         ? living
         : null;
   }

   private static void castShared(LivingEntity caster, LivingEntity target, float damage) {
      target.hurt(caster.damageSources().magic(), damage);
   }

   private static double readManaCost(CompoundTag payload, boolean crestCast) {
      double configured = payload != null && payload.contains("example_cost") ? payload.getDouble("example_cost") : DEFAULT_COST;
      double clamped = Mth.clamp(configured, 0.0, 200.0);
      return crestCast ? Math.min(clamped, DEFAULT_COST * 0.5) : clamped;
   }

   private static float readDamage(CompoundTag payload, boolean crestCast) {
      float configured = payload != null && payload.contains("example_damage") ? payload.getFloat("example_damage") : DEFAULT_DAMAGE;
      float clamped = Mth.clamp(configured, 1.0F, 40.0F);
      return crestCast ? clamped + 2.0F : clamped;
   }

   private static boolean consumeMana(TypeMoonWorldModVariables.PlayerVariables vars, double amount) {
      if (amount <= 0.0) {
         return true;
      } else if (vars == null || vars.player_mana < amount) {
         return false;
      } else {
         vars.player_mana = Math.max(0.0, vars.player_mana - amount);
         return true;
      }
   }
}
