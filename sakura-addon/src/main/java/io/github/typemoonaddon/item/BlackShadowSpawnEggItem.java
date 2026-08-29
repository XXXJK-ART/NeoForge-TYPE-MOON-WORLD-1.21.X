package io.github.typemoonaddon.item;

import io.github.typemoonaddon.registry.ModAttachments;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

/** Carries the player context through vanilla's synchronous spawn-egg pipeline. */
public final class BlackShadowSpawnEggItem extends DeferredSpawnEggItem {
    private static final ThreadLocal<SpawnOwner> CURRENT_OWNER = new ThreadLocal<>();

    public BlackShadowSpawnEggItem(
        Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> type,
        int backgroundColor,
        int highlightColor,
        Item.Properties properties
    ) {
        super(type, backgroundColor, highlightColor, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return withOwner(context.getPlayer(), () -> super.useOn(context));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return withOwner(player, () -> super.use(level, player, hand));
    }

    @Nullable
    public static SpawnOwner currentOwner() {
        return CURRENT_OWNER.get();
    }

    private static <T> T withOwner(@Nullable Player player, Supplier<T> action) {
        SpawnOwner previous = CURRENT_OWNER.get();
        if (player != null) {
            CURRENT_OWNER.set(new SpawnOwner(
                player.getUUID(),
                player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()
            ));
        }
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT_OWNER.remove();
            } else {
                CURRENT_OWNER.set(previous);
            }
        }
    }

    public record SpawnOwner(UUID ownerId, boolean grailAscended) {
    }
}
