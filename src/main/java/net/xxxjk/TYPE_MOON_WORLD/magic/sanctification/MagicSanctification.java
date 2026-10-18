package net.xxxjk.TYPE_MOON_WORLD.magic.sanctification;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.typemoonworld.api.MagicDefinitionData;

/** Third-party-free implementation of the Sanctification miracle. */
public final class MagicSanctification {
    public static final String MAGIC_ID = "sanctification";
    public static final int CHANT_TICKS = 400;
    public static final double MANA_COST = 100.0D;
    static final String TAG_UNTIL = "TypeMoonSanctificationUntil";
    static final String TAG_MODE = "TypeMoonSanctificationMode";
    static final String TAG_POS = "TypeMoonSanctificationPos";
    static final String TAG_DIMENSION = "TypeMoonSanctificationDimension";
    static final String TAG_ITEM = "TypeMoonSanctificationItem";

    private MagicSanctification() {}

    public static void registerDefinition() {
        if (MagicDefinitionRegistry.get(MAGIC_ID) != null) return;
        MagicDefinitionRegistry.register(new MagicDefinitionData(
                ResourceLocation.fromNamespaceAndPath("typemoonworld", MAGIC_ID),
                "magic.typemoonworld.sanctification.name",
                ResourceLocation.fromNamespaceAndPath("typemoonworld", "church"),
                ResourceLocation.fromNamespaceAndPath("typemoonworld", "church"),
                60, CHANT_TICKS, MANA_COST, 0.0D, 0,
                true, true, true, true, false, true, 0, 20,
                null, 0.0D, List.of()), "typemoonworld");
    }

    public static MagicExecutionResult execute(MagicExecutionContext context) {
        if (!(context.entity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return MagicExecutionResult.FAILED;
        }
        if (isChanting(player)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.sanctification.already_chanting"), true);
            return MagicExecutionResult.FAILED;
        }
        TypeMoonWorldModVariables.PlayerVariables vars = context.vars();
        if (vars == null || vars.magicState().mana() < MANA_COST) return MagicExecutionResult.FAILED;
        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty()) {
            player.getPersistentData().putString(TAG_MODE, "item");
            player.getPersistentData().put(TAG_ITEM, main.save(player.registryAccess()));
        } else {
            HitResult hit = player.pick(10.0D, 0.0F, false);
            if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
                if (hit instanceof EntityHitResult) player.displayClientMessage(Component.translatable("message.typemoonworld.magic.sanctification.no_entity"), true);
                return MagicExecutionResult.FAILED;
            }
            BlockPos pos = blockHit.getBlockPos();
            if (!level.getBlockState(pos).isAir()) {
                player.getPersistentData().putString(TAG_MODE, "block");
                player.getPersistentData().putLong(TAG_POS, pos.asLong());
                player.getPersistentData().putString(TAG_DIMENSION, level.dimension().location().toString());
            } else return MagicExecutionResult.FAILED;
        }
        long now = level.getGameTime();
        player.getPersistentData().putLong(TAG_UNTIL, now + CHANT_TICKS);
        player.displayClientMessage(Component.translatable("message.typemoonworld.magic.sanctification.start"), true);
        return MagicExecutionResult.started(MANA_COST, 0);
    }

    public static boolean isChanting(ServerPlayer player) {
        return player != null && player.getPersistentData().getLong(TAG_UNTIL) > player.level().getGameTime();
    }

    public static void tick(ServerPlayer player) {
        long until = player.getPersistentData().getLong(TAG_UNTIL);
        if (until <= 0L) return;
        if (player.level().getGameTime() < until) return;
        finish(player);
    }

    private static void finish(ServerPlayer player) {
        String mode = player.getPersistentData().getString(TAG_MODE);
        boolean success = false;
        if ("item".equals(mode)) {
            ItemStack stack = player.getMainHandItem();
            ItemStack locked = ItemStack.parseOptional(player.registryAccess(), player.getPersistentData().getCompound(TAG_ITEM));
            if (!stack.isEmpty() && !locked.isEmpty() && ItemStack.isSameItemSameComponents(stack, locked)
                    && stack.getCount() == locked.getCount()) {
                SanctificationService.markItem(stack);
                success = true;
            }
        } else if ("block".equals(mode) && player.level() instanceof ServerLevel level
                && level.dimension().location().toString().equals(player.getPersistentData().getString(TAG_DIMENSION))) {
            BlockPos pos = BlockPos.of(player.getPersistentData().getLong(TAG_POS));
            if (!level.getBlockState(pos).isAir()) { SanctifiedBlockData.get(level).add(pos); success = true; }
        }
        clear(player);
        player.displayClientMessage(Component.translatable(success
                ? "message.typemoonworld.magic.sanctification.success"
                : "message.typemoonworld.magic.sanctification.lost_target"), true);
    }

    private static void clear(ServerPlayer player) {
        player.getPersistentData().remove(TAG_UNTIL);
        player.getPersistentData().remove(TAG_MODE);
        player.getPersistentData().remove(TAG_POS);
        player.getPersistentData().remove(TAG_DIMENSION);
        player.getPersistentData().remove(TAG_ITEM);
    }
}
