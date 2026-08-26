package io.github.typemoonaddon.data;

import io.github.typemoonaddon.registry.ModAttachments;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

public final class ImaginarySpaceContainer implements Container {
    private final ImaginarySpaceData data;
    private final ServerPlayer owner;
    private int page;

    public ImaginarySpaceContainer(ServerPlayer owner) {
        this.owner = owner;
        this.data = owner.getData(ModAttachments.IMAGINARY_SPACE.get());
        this.page = data.selectedPage();
    }

    @Override
    public int getContainerSize() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return data.getItem(page, slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = data.removeItem(page, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return data.removeItemNoUpdate(page, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        data.setItem(page, slot, stack);
        setChanged();
    }

    @Override
    public void setChanged() {
        owner.syncData(ModAttachments.IMAGINARY_SPACE.get());
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && data.learned();
    }

    @Override
    public void clearContent() {
        data.clear();
        page = 0;
        setChanged();
    }

    public int page() {
        return page;
    }

    public int pageCount() {
        return data.pageCount();
    }

    public boolean selectPage(int requestedPage) {
        if (!data.selectPage(requestedPage)) {
            return false;
        }
        page = requestedPage;
        setChanged();
        return true;
    }
}
