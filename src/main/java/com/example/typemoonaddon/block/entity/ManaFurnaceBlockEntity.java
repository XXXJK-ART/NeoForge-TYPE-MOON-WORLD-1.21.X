package com.example.typemoonaddon.block.entity;

import com.example.typemoonaddon.registry.AddonBlocks;
import com.example.typemoonaddon.registry.AddonItems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/** Three-foundations mana furnace: persistent owner and explicit authorization. */
public final class ManaFurnaceBlockEntity extends BlockEntity {
    public static final int BIND_TICKS = 60;
    public static final int AUTHORIZATION_TICKS = 200;
    public static final double RANGE = 37.5D;
    private UUID owner;
    private UUID bindingPlayer;
    private int bindProgress;
    private UUID pendingAuthorization;
    private long pendingAuthorizationUntil;
    private final Set<UUID> authorized = new HashSet<>();
    private static final Map<ServerLevel, Set<BlockPos>> INSTANCES = new WeakHashMap<>();

    public ManaFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(AddonBlockEntities.MANA_FURNACE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            synchronized (INSTANCES) {
                INSTANCES.computeIfAbsent(serverLevel, ignored -> new HashSet<>()).add(worldPosition.immutable());
            }
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            synchronized (INSTANCES) {
                Set<BlockPos> positions = INSTANCES.get(serverLevel);
                if (positions != null) {
                    positions.remove(worldPosition);
                    if (positions.isEmpty()) INSTANCES.remove(serverLevel);
                }
            }
        }
        super.setRemoved();
    }

    public static void tick(ServerLevel level, ManaFurnaceBlockEntity furnace) {
        if (furnace.bindingPlayer != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(furnace.bindingPlayer);
            if (player == null || !player.isAlive() || !player.isShiftKeyDown()
                    || player.distanceToSqr(Vec3.atCenterOf(furnace.worldPosition)) > 36.0D) {
                furnace.bindingPlayer = null;
                furnace.bindProgress = 0;
            } else if (++furnace.bindProgress >= BIND_TICKS) {
                furnace.owner = player.getUUID();
                furnace.authorized.clear();
                furnace.authorized.add(player.getUUID());
                furnace.bindingPlayer = null;
                furnace.bindProgress = 0;
                player.displayClientMessage(Component.translatable("message.typemoonworld.mana_furnace.bound"), true);
                furnace.setChanged();
            }
        }
        if (furnace.pendingAuthorization != null && level.getGameTime() > furnace.pendingAuthorizationUntil) {
            furnace.pendingAuthorization = null;
            furnace.setChanged();
        }
    }

    public boolean isBound() {
        return owner != null;
    }

    public boolean isOwner(ServerPlayer player) {
        return player != null && player.getUUID().equals(owner);
    }

    public boolean hasPendingAuthorization() {
        return pendingAuthorization != null;
    }

    public void beginBinding(ServerPlayer player) {
        if (isBound() || player == null || player.isSpectator()) {
            return;
        }
        bindingPlayer = player.getUUID();
        bindProgress = 0;
        player.displayClientMessage(Component.translatable("message.typemoonworld.mana_furnace.binding"), true);
        setChanged();
    }

    public void requestAuthorization(ServerPlayer player) {
        if (!isBound() || player == null || isOwner(player)) {
            return;
        }
        pendingAuthorization = player.getUUID();
        pendingAuthorizationUntil = level == null ? 0L : level.getGameTime() + AUTHORIZATION_TICKS;
        ServerPlayer ownerPlayer = level instanceof ServerLevel serverLevel && owner != null
                ? serverLevel.getServer().getPlayerList().getPlayer(owner) : null;
        if (ownerPlayer != null) {
            ownerPlayer.displayClientMessage(Component.translatable(
                    "message.typemoonworld.mana_furnace.authorization_request", player.getDisplayName()), true);
        }
        player.displayClientMessage(Component.translatable("message.typemoonworld.mana_furnace.authorization_pending"), true);
        setChanged();
    }

    public void confirmPending(ServerPlayer ownerPlayer) {
        if (!isOwner(ownerPlayer) || pendingAuthorization == null) {
            return;
        }
        authorized.add(pendingAuthorization);
        ServerPlayer granted = level instanceof ServerLevel serverLevel
                ? serverLevel.getServer().getPlayerList().getPlayer(pendingAuthorization) : null;
        if (granted != null) {
            granted.displayClientMessage(Component.translatable("message.typemoonworld.mana_furnace.authorized"), true);
        }
        pendingAuthorization = null;
        pendingAuthorizationUntil = 0L;
        setChanged();
    }

    public boolean canSupply(ServerPlayer player) {
        return isInRange(player) && player != null && authorized.contains(player.getUUID());
    }

    public boolean isInRange(ServerPlayer player) {
        return player != null && level != null && player.level().dimension().equals(level.dimension())
                && Math.abs(player.getX() - (worldPosition.getX() + 0.5D)) <= RANGE
                && Math.abs(player.getY() - (worldPosition.getY() + 0.5D)) <= RANGE
                && Math.abs(player.getZ() - (worldPosition.getZ() + 0.5D)) <= RANGE;
    }

    public void returnToOwner(ServerPlayer player) {
        if (!isOwner(player) || level == null) {
            return;
        }
        level.setBlock(worldPosition, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        player.getInventory().add(new ItemStack(AddonItems.MANA_FURNACE.get()));
    }

    public void clearAuthorization(ServerLevel level) {
        authorized.clear();
        owner = null;
        pendingAuthorization = null;
        setChanged();
    }

    public static List<ManaFurnaceBlockEntity> nearby(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) {
            return List.of();
        }
        List<ManaFurnaceBlockEntity> result = new ArrayList<>();
        Set<BlockPos> positions;
        synchronized (INSTANCES) {
            positions = INSTANCES.get(level) == null ? Set.of() : Set.copyOf(INSTANCES.get(level));
        }
        for (BlockPos position : positions) {
            if (Math.abs(position.getX() + 0.5D - player.getX()) <= RANGE
                    && Math.abs(position.getY() + 0.5D - player.getY()) <= RANGE
                    && Math.abs(position.getZ() + 0.5D - player.getZ()) <= RANGE
                    && level.getBlockEntity(position) instanceof ManaFurnaceBlockEntity furnace
                    && furnace.canSupply(player)) {
                    result.add(furnace);
            }
        }
        return result;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
        if (bindingPlayer != null) tag.putUUID("BindingPlayer", bindingPlayer);
        if (pendingAuthorization != null) tag.putUUID("PendingAuthorization", pendingAuthorization);
        tag.putInt("BindProgress", bindProgress);
        tag.putLong("PendingAuthorizationUntil", pendingAuthorizationUntil);
        int i = 0;
        for (UUID uuid : authorized) {
            tag.putUUID("Authorized" + i++, uuid);
        }
        tag.putInt("AuthorizedCount", i);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        bindingPlayer = tag.hasUUID("BindingPlayer") ? tag.getUUID("BindingPlayer") : null;
        pendingAuthorization = tag.hasUUID("PendingAuthorization") ? tag.getUUID("PendingAuthorization") : null;
        bindProgress = tag.getInt("BindProgress");
        pendingAuthorizationUntil = tag.getLong("PendingAuthorizationUntil");
        authorized.clear();
        for (int i = 0; i < Math.min(64, tag.getInt("AuthorizedCount")); i++) {
            if (tag.hasUUID("Authorized" + i)) authorized.add(tag.getUUID("Authorized" + i));
        }
    }
}
