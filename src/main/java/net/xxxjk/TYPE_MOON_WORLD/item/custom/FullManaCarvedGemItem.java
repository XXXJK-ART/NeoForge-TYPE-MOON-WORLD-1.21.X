package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemManaStorageService;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemEngravingService;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemUseService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class FullManaCarvedGemItem extends Item {
    private final Supplier<Item> emptyGemSupplier;
    private final GemQuality quality;
    private final GemType type;

    public FullManaCarvedGemItem(Properties properties, Supplier<Item> emptyGemSupplier, GemQuality quality, GemType type) {
        super(properties);
        this.emptyGemSupplier = emptyGemSupplier;
        this.quality = quality;
        this.type = type;
    }

    public GemQuality getQuality() {
        return quality;
    }
    
    public GemType getType() {
        return type;
    }
    
    public Item getEmptyGemItem() {
        return emptyGemSupplier.get();
    }

    public double getManaAmount(ItemStack stack) {
        int manaAmount = quality.getCapacity(type);
        if (stack == null || stack.isEmpty()) {
            return manaAmount;
        }

        if (GemEngravingService.getEngravedMagicId(stack) != null) {
            double engravedCost = GemEngravingService.getEngravedManaCost(stack);
            if (engravedCost > 0.0D) {
                manaAmount = Math.max(1, (int) Math.ceil(engravedCost));
            }
        }
        return manaAmount;
    }

    @Deprecated(forRemoval = false)
    public double getManaAmount() {
        return quality.getCapacity(type);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        if (GemEngravingService.getEngravedMagicId(stack) == null) {
            return super.getName(stack);
        }
        return Component.translatable(
                "item.typemoonworld.engraved_gem_full",
                Component.translatable(getGemColorKey(this.type))
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.addAll(GemEngravingService.getEngravingDetailLines(stack));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String engravedMagicId = GemEngravingService.getEngravedMagicId(stack);
        boolean shiftGravitySelfCast = player.isShiftKeyDown() && "gravity_magic".equals(engravedMagicId);

        if (world.isClientSide) {
            if (shiftGravitySelfCast && net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.openGravitySelector(hand);
            }
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        stack = normalizeEngravedVariant(serverPlayer, hand, stack);

        if (shiftGravitySelfCast) {
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        if (player.isShiftKeyDown()) {
            ItemStack result = GemManaStorageService.withdrawFromGem(serverPlayer, hand, stack, emptyGemSupplier.get(), type, quality);
            return InteractionResultHolder.sidedSuccess(result, false);
        }

        ItemStack result = GemUseService.useFilledGem(world, player, hand, stack, type);
        return InteractionResultHolder.sidedSuccess(result, false);
    }

    private static String getGemColorKey(GemType type) {
        return switch (type) {
            case RUBY -> "item.typemoonworld.gem_color.ruby";
            case SAPPHIRE -> "item.typemoonworld.gem_color.sapphire";
            case EMERALD -> "item.typemoonworld.gem_color.emerald";
            case TOPAZ -> "item.typemoonworld.gem_color.topaz";
            case WHITE_GEMSTONE -> "item.typemoonworld.gem_color.white";
            case CYAN -> "item.typemoonworld.gem_color.cyan";
            case BLACK_SHARD -> "item.typemoonworld.gem_color.black";
        };
    }

    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void openGravitySelector(InteractionHand hand) {
            net.xxxjk.TYPE_MOON_WORLD.client.GemGravityModeClient.openSelector(hand);
        }
    }

    private ItemStack normalizeEngravedVariant(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        if (GemEngravingService.getEngravedMagicId(stack) == null) {
            return stack;
        }
        Item normalizedItem = ModItems.getNormalizedFullCarvedGem(this.type);
        if (stack.getItem() == normalizedItem) {
            return stack;
        }

        ItemStack normalized = new ItemStack(normalizedItem, stack.getCount());
        GemEngravingService.copyEngravingData(stack, normalized);
        player.setItemInHand(hand, normalized);
        return normalized;
    }
}
