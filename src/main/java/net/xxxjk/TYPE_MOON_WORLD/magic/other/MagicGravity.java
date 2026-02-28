package net.xxxjk.TYPE_MOON_WORLD.magic.other;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public class MagicGravity {
    public static final int MODE_ULTRA_LIGHT = -2;
    public static final int MODE_LIGHT = -1;
    public static final int MODE_NORMAL = 0;
    public static final int MODE_HEAVY = 1;
    public static final int MODE_ULTRA_HEAVY = 2;

    private static final int MIN_DURATION_TICKS = 20 * 30;   // 30s
    private static final int MAX_DURATION_TICKS = 20 * 180;  // 180s (3min)
    private static final double CAST_MANA_COST = 20.0D;
    private static final double TARGET_RANGE = 10.0D;

    public static void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

        LivingEntity target = resolveTarget(player, vars.gravity_magic_target);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.gravity.no_target"), true);
            return;
        }

        if (!ManaHelper.consumeOneTimeMagicCost(player, CAST_MANA_COST)) {
            player.displayClientMessage(Component.translatable(MagicConstants.MSG_NOT_ENOUGH_MANA), true);
            return;
        }

        int currentMode = MagicGravityEffectHandler.getCurrentMode(target);
        // Reversed tier direction:
        // C (up) -> lighter tier, Shift + C (down) -> heavier tier.
        int step = player.isShiftKeyDown() ? 1 : -1;
        int nextMode = Math.max(MODE_ULTRA_LIGHT, Math.min(MODE_ULTRA_HEAVY, currentMode + step));
        Component targetComp = target == player
                ? Component.translatable("gui.typemoonworld.mode.self")
                : target.getDisplayName();

        if (nextMode == MODE_NORMAL) {
            MagicGravityEffectHandler.clearGravityState(target);
            player.displayClientMessage(
                    Component.translatable("message.typemoonworld.magic.gravity.normalized", targetComp),
                    true
            );
        } else {
            int duration = getDurationTicks(vars.proficiency_gravity_magic);
            MagicGravityEffectHandler.applyGravityState(target, nextMode, player.level().getGameTime() + duration);

            String modeKey = switch (nextMode) {
                case MODE_ULTRA_LIGHT -> "gui.typemoonworld.mode.gravity.ultra_light";
                case MODE_LIGHT -> "gui.typemoonworld.mode.gravity.light";
                case MODE_HEAVY -> "gui.typemoonworld.mode.gravity.heavy";
                case MODE_ULTRA_HEAVY -> "gui.typemoonworld.mode.gravity.ultra_heavy";
                default -> "gui.typemoonworld.mode.gravity.normal";
            };
            int durationSec = duration / 20;
            player.displayClientMessage(
                    Component.translatable("message.typemoonworld.magic.gravity.applied", targetComp, Component.translatable(modeKey), durationSec),
                    true
            );
        }

        vars.proficiency_gravity_magic = Math.min(100.0, vars.proficiency_gravity_magic + 0.4);
        vars.syncPlayerVariables(player);
    }

    private static LivingEntity resolveTarget(ServerPlayer player, int targetMode) {
        if (targetMode == 0) {
            return player;
        }

        HitResult hitResult = EntityUtils.getRayTraceTarget(player, TARGET_RANGE);
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            Entity hit = ((EntityHitResult) hitResult).getEntity();
            if (hit instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }
        return null;
    }

    private static int getDurationTicks(double proficiency) {
        double clamped = Math.max(0.0, Math.min(100.0, proficiency));
        double ratio = clamped / 100.0;
        return MIN_DURATION_TICKS + (int) Math.round((MAX_DURATION_TICKS - MIN_DURATION_TICKS) * ratio);
    }
}
