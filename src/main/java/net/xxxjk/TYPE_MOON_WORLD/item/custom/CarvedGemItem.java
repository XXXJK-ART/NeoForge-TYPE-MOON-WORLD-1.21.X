package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemManaStorageService;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemEngravingService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class CarvedGemItem extends Item {
    private final GemType type;
    private final GemQuality quality;
    private final Supplier<Item> fullGemSupplier;

    public CarvedGemItem(Properties properties, GemType type, GemQuality quality, Supplier<Item> fullGemSupplier) {
        super(properties);
        this.type = type;
        this.quality = quality;
        this.fullGemSupplier = fullGemSupplier;
    }

    private Item fullGemItem() {
        return fullGemSupplier.get();
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return false;
    }
    
    public GemQuality getQuality() {
        return quality;
    }

    public GemType getType() {
        return type;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        if (GemEngravingService.getEngravedMagicId(stack) == null) {
            return super.getName(stack);
        }
        return Component.translatable(
                "item.typemoonworld.engraved_gem",
                Component.translatable(getGemColorKey(this.type))
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.addAll(GemEngravingService.getEngravingDetailLines(stack));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        stack = normalizeEngravedVariant(serverPlayer, hand, stack);

        if (player.isShiftKeyDown()) {
            ItemStack result = GemManaStorageService.storeIntoGem(serverPlayer, hand, stack, fullGemItem(), type, quality);
            return InteractionResultHolder.sidedSuccess(result, false);
        }

        player.displayClientMessage(Component.translatable("message.typemoonworld.gem.use.empty"), true);
        return InteractionResultHolder.fail(stack);
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

    private ItemStack normalizeEngravedVariant(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        if (GemEngravingService.getEngravedMagicId(stack) == null) {
            return stack;
        }
        Item normalizedItem = ModItems.getNormalizedCarvedGem(this.type);
        if (stack.getItem() == normalizedItem) {
            return stack;
        }

        ItemStack normalized = new ItemStack(normalizedItem, stack.getCount());
        GemEngravingService.copyEngravingData(stack, normalized);
        player.setItemInHand(hand, normalized);
        return normalized;
    }
}
