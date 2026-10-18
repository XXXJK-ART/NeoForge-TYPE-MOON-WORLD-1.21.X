package net.xxxjk.TYPE_MOON_WORLD.grail;

import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.HolyGrailItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningStrategy;
import net.xxxjk.TYPE_MOON_WORLD.network.GrailOpenPayload;
import net.neoforged.neoforge.network.PacketDistributor;

/** Validates and executes grail wishes entirely on the server. */
public final class GrailWishService {
    private static final String HAND_TAG = "TypeMoonGrailTriggerHand";
    private static final int MAIN_HAND = 0;
    private static final int OFF_HAND = 1;

    private GrailWishService() {}

    public static void rememberTriggerHand(ServerPlayer player, InteractionHand hand) {
        player.getPersistentData().putInt(HAND_TAG, hand == InteractionHand.OFF_HAND ? OFF_HAND : MAIN_HAND);
    }

    public static InteractionHand triggerHand(ServerPlayer player) {
        return player.getPersistentData().getInt(HAND_TAG) == OFF_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    public static void handleAction(ServerPlayer player, int action, String wish) {
        if (player == null) return;
        if (action == GrailOpenPayload.ACTION_THIRD_MAGIC) {
            var vars = player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (!MagicLearningStrategy.isLearned(vars, "third_magic")) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.holy_grail.third_magic_unlearned"), true);
            }
            return;
        }
        if (action == GrailOpenPayload.ACTION_WISH_OPEN) {
            if (!player.level().getGameRules().getBoolean(GrailGameRules.ALLOW_GRAIL_WISH)) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.holy_grail.denied"), true);
            } else {
                PacketDistributor.sendToPlayer(player, new GrailOpenPayload(null));
            }
            return;
        }
        if (action == GrailOpenPayload.ACTION_WISH_EXECUTE) executeWish(player, wish);
    }

    public static WishResult classify(String input) {
        if (input == null) return WishResult.INVALID;
        String command = input.trim();
        while (command.startsWith("/")) command = command.substring(1).trim();
        if (command.isEmpty()) return WishResult.INVALID;
        String lower = command.toLowerCase(Locale.ROOT);
        if (lower.startsWith("gamemode") || lower.equals("typemoon player max")) return WishResult.BLACKLISTED;
        return WishResult.ACCEPTED;
    }

    public static boolean executeCommand(ServerPlayer player, String input) {
        WishResult classification = classify(input);
        if (classification == WishResult.BLACKLISTED) return false;
        if (classification != WishResult.ACCEPTED) return false;
        String command = input.trim();
        while (command.startsWith("/")) command = command.substring(1).trim();
        MinecraftServer server = player.getServer();
        if (server == null) return false;
        try {
            CommandSourceStack source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            return server.getCommands().getDispatcher().execute(command, source) > 0;
        } catch (Exception exception) {
            return false;
        }
    }

    private static void executeWish(ServerPlayer player, String wish) {
        if (!player.level().getGameRules().getBoolean(GrailGameRules.ALLOW_GRAIL_WISH)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.holy_grail.denied"), true);
            return;
        }
        if (classify(wish) == WishResult.BLACKLISTED) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.holy_grail.blacklisted"), true);
            return;
        }
        if (!executeCommand(player, wish)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.holy_grail.unknown"), true);
            return;
        }
        InteractionHand hand = triggerHand(player);
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.HOLY_GRAIL.get())) {
            stack.shrink(1);
            player.getInventory().setChanged();
        }
    }

    public enum WishResult { ACCEPTED, BLACKLISTED, INVALID }
}
