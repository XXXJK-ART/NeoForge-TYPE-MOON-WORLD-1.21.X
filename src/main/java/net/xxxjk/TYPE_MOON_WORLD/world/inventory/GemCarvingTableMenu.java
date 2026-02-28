package net.xxxjk.TYPE_MOON_WORLD.world.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.GemCarvingTableBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModMenus;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.CarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ChiselItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemCompatibilityService;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemEngravingService;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.MagicProjection;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.MagicStructuralAnalysis;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.jetbrains.annotations.NotNull;

public class GemCarvingTableMenu extends AbstractContainerMenu {
    private static final String ADVANCED_JEWEL_MAGIC_ID = "jewel_magic_release";
    private static final int MAX_MAGIC_ID_LENGTH = 64;
    public static final int PROJECTION_MODE_ITEM = 0;
    public static final int PROJECTION_MODE_STRUCTURE = 1;

    public static final int SLOT_GEM = GemCarvingTableBlockEntity.SLOT_GEM;
    public static final int SLOT_TOOL = GemCarvingTableBlockEntity.SLOT_TOOL;
    private static final int SLOT_COUNT = 2;

    private static final int PLAYER_INV_START = SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Level level;
    private final ContainerLevelAccess access;
    private final GemCarvingTableBlockEntity blockEntity;
    private final ItemStackHandler itemHandler;

    public record EngravePreview(
            boolean valid,
            String errorKey,
            Object[] errorArgs,
            int chance,
            double requiredMana,
            int capacity,
            int reinforcementPart,
            int reinforcementLevel,
            int projectionMode,
            ItemStack projectionItemTemplate,
            String projectionStructureId,
            String projectionStructureName
    ) {
        public static EngravePreview error(String errorKey, Object... errorArgs) {
            return new EngravePreview(false, errorKey, errorArgs, -1, 0.0, 0, 0, 1, PROJECTION_MODE_ITEM, ItemStack.EMPTY, "", "");
        }
    }

    private record MenuContext(BlockPos pos, GemCarvingTableBlockEntity blockEntity) {
    }

    public GemCarvingTableMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveMenuContext(playerInventory.player.level(), extraData));
    }

    private GemCarvingTableMenu(int containerId, Inventory playerInventory, MenuContext context) {
        this(containerId, playerInventory, context.blockEntity(), context.pos());
    }

    public GemCarvingTableMenu(int containerId, Inventory playerInventory, GemCarvingTableBlockEntity blockEntity, BlockPos pos) {
        super(TypeMoonWorldModMenus.GEM_CARVING_TABLE.get(), containerId);
        this.level = playerInventory.player.level();
        this.access = ContainerLevelAccess.create(this.level, pos);
        this.blockEntity = blockEntity;
        this.itemHandler = blockEntity.getItems();

        this.addSlot(new SlotItemHandler(this.itemHandler, SLOT_GEM, 26, 34) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof CarvedGemItem
                        && GemEngravingService.getEngravedMagicId(stack) == null;
            }
        });
        this.addSlot(new SlotItemHandler(this.itemHandler, SLOT_TOOL, 26, 65) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof ChiselItem;
            }
        });

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 13 + col * 18, 126 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 13 + col * 18, 184));
        }
    }

    private static MenuContext resolveMenuContext(Level level, FriendlyByteBuf extraData) {
        BlockPos pos = extraData == null ? BlockPos.ZERO : extraData.readBlockPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof GemCarvingTableBlockEntity tableBlockEntity) {
            return new MenuContext(pos, tableBlockEntity);
        }
        GemCarvingTableBlockEntity fallback = new GemCarvingTableBlockEntity(pos, ModBlocks.GEM_CARVING_TABLE.get().defaultBlockState());
        return new MenuContext(pos, fallback);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(this.access, player, ModBlocks.GEM_CARVING_TABLE.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof CarvedGemItem
                && GemEngravingService.getEngravedMagicId(stack) == null) {
            if (!this.moveItemStackTo(stack, SLOT_GEM, SLOT_GEM + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ChiselItem) {
            if (!this.moveItemStackTo(stack, SLOT_TOOL, SLOT_TOOL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INV_START && index < PLAYER_INV_END) {
            if (!this.moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= HOTBAR_START && index < HOTBAR_END) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return result;
    }

    public EngravePreview getPreview(
            Player player,
            String magicId,
            int reinforcementPart,
            int reinforcementLevel,
            int projectionMode,
            int projectionItemIndex,
            String projectionStructureIdInput
    ) {
        if (!isValidMagicId(magicId)) {
            return EngravePreview.error("message.typemoonworld.gem.engrave.not_supported");
        }

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!vars.learned_magics.contains(ADVANCED_JEWEL_MAGIC_ID) || !hasLearnedMagic(vars, magicId)) {
            return EngravePreview.error("message.typemoonworld.gem.engrave.locked");
        }
        if (!GemCompatibilityService.isWhitelistedMagic(magicId)) {
            return EngravePreview.error("message.typemoonworld.gem.engrave.not_supported");
        }

        ItemStack gemStack = this.itemHandler.getStackInSlot(SLOT_GEM);
        if (!(gemStack.getItem() instanceof CarvedGemItem carvedGem)) {
            return EngravePreview.error("message.typemoonworld.gem.engrave.need_gem");
        }
        if (gemStack.isEmpty() || GemEngravingService.getEngravedMagicId(gemStack) != null) {
            return EngravePreview.error("message.typemoonworld.gem.engrave.single_required");
        }

        int chance = GemCompatibilityService.calculateEngraveSuccessChance(
                carvedGem.getQuality(),
                carvedGem.getType(),
                magicId,
                getMagicProficiency(vars, magicId)
        );
        int capacity = carvedGem.getQuality().getCapacity(carvedGem.getType());

        int appliedPart = Math.max(0, Math.min(3, reinforcementPart));
        int appliedLevel = clampReinforcementLevel(vars, reinforcementLevel);
        int appliedProjectionMode = projectionMode == PROJECTION_MODE_STRUCTURE ? PROJECTION_MODE_STRUCTURE : PROJECTION_MODE_ITEM;

        double requiredMana = 0.0;
        ItemStack projectionTemplate = ItemStack.EMPTY;
        String projectionStructureId = "";
        String projectionStructureName = "";

        if ("reinforcement".equals(magicId)) {
            requiredMana = GemEngravingService.calculateReinforcementManaCost(appliedLevel);
        } else if ("projection".equals(magicId)) {
            if (appliedProjectionMode == PROJECTION_MODE_ITEM) {
                if (projectionItemIndex >= 0 && projectionItemIndex < vars.analyzed_items.size()) {
                    projectionTemplate = vars.analyzed_items.get(projectionItemIndex).copy();
                } else if (!vars.projection_selected_item.isEmpty()) {
                    projectionTemplate = vars.projection_selected_item.copy();
                }
                if (projectionTemplate.isEmpty()) {
                    return EngravePreview.error("message.typemoonworld.gem.engrave.projection_no_item");
                }
                projectionTemplate = GemEngravingService.sanitizeProjectionTemplate(projectionTemplate);
                requiredMana = MagicProjection.calculateCost(
                        projectionTemplate,
                        vars.player_magic_attributes_sword,
                        vars.proficiency_projection
                );
            } else {
                projectionStructureId = projectionStructureIdInput == null ? "" : projectionStructureIdInput;
                if (projectionStructureId.isEmpty()) {
                    projectionStructureId = vars.projection_selected_structure_id == null ? "" : vars.projection_selected_structure_id;
                }
                if (projectionStructureId.isEmpty()) {
                    return EngravePreview.error("message.typemoonworld.gem.engrave.projection_no_structure");
                }
                TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = vars.getStructureById(projectionStructureId);
                if (structure == null || !TypeMoonWorldModVariables.PlayerVariables.isTrustedStructure(structure)) {
                    return EngravePreview.error("message.typemoonworld.gem.engrave.projection_no_structure");
                }
                projectionStructureName = structure.name == null ? "" : structure.name;
                requiredMana = calculateStructureProjectionCost(structure, vars.player_magic_attributes_sword);
            }
        } else if ("gravity_magic".equals(magicId)) {
            requiredMana = 20.0;
        }

        if (requiredMana > capacity) {
            return EngravePreview.error(
                    "message.typemoonworld.gem.engrave.capacity_exceeded",
                    (int) Math.ceil(requiredMana),
                    capacity
            );
        }

        return new EngravePreview(
                true,
                "",
                new Object[0],
                chance,
                requiredMana,
                capacity,
                appliedPart,
                appliedLevel,
                appliedProjectionMode,
                projectionTemplate,
                projectionStructureId,
                projectionStructureName
        );
    }

    public void tryEngrave(
            ServerPlayer player,
            String magicId,
            int reinforcementPart,
            int reinforcementLevel,
            int projectionMode,
            int projectionItemIndex,
            String projectionStructureId
    ) {
        ItemStack toolStack = this.itemHandler.getStackInSlot(SLOT_TOOL);
        if (!(toolStack.getItem() instanceof ChiselItem)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.need_tool"), true);
            return;
        }

        EngravePreview preview = getPreview(
                player,
                magicId,
                reinforcementPart,
                reinforcementLevel,
                projectionMode,
                projectionItemIndex,
                projectionStructureId
        );
        if (!preview.valid()) {
            player.displayClientMessage(Component.translatable(preview.errorKey(), preview.errorArgs()), true);
            return;
        }

        ItemStack gemStack = this.itemHandler.getStackInSlot(SLOT_GEM);
        if (!(gemStack.getItem() instanceof CarvedGemItem carvedGem)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.need_gem"), true);
            return;
        }

        int inputCount = gemStack.getCount();
        int availableUses = getAvailableChiselUses(toolStack);
        if (availableUses <= 0) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.need_tool"), true);
            return;
        }

        int attempts = Math.min(inputCount, availableUses);
        int untouchedCount = inputCount - attempts;
        int successCount = 0;

        for (int i = 0; i < attempts; i++) {
            int roll = player.level().getRandom().nextInt(100) + 1;
            if (roll <= preview.chance()) {
                successCount++;
            }
            damageChisel(player, toolStack);
        }

        int failCount = attempts - successCount;

        if (successCount > 0) {
            Item normalizedGem = ModItems.getNormalizedCarvedGem(carvedGem.getType());
            ItemStack engravedStack = new ItemStack(normalizedGem, successCount);
            GemEngravingService.copyEngravingData(gemStack, engravedStack);
            GemEngravingService.setEngravedMagic(engravedStack, magicId);
            if ("reinforcement".equals(magicId)) {
                GemEngravingService.setReinforcementConfig(engravedStack, preview.reinforcementPart(), preview.reinforcementLevel(), preview.requiredMana());
            } else if ("projection".equals(magicId)) {
                if (preview.projectionMode() == PROJECTION_MODE_STRUCTURE) {
                    GemEngravingService.setProjectionStructureConfig(engravedStack, preview.projectionStructureId(), preview.projectionStructureName(), preview.requiredMana());
                } else {
                    GemEngravingService.setProjectionItemConfig(engravedStack, preview.projectionItemTemplate(), player.registryAccess(), preview.requiredMana());
                }
            } else {
                GemEngravingService.setEngravedManaCost(engravedStack, preview.requiredMana());
            }
            this.itemHandler.setStackInSlot(SLOT_GEM, engravedStack);
        } else {
            if (untouchedCount > 0) {
                ItemStack untouchedStack = gemStack.copy();
                untouchedStack.setCount(untouchedCount);
                this.itemHandler.setStackInSlot(SLOT_GEM, untouchedStack);
            } else {
                this.itemHandler.setStackInSlot(SLOT_GEM, ItemStack.EMPTY);
            }
        }

        if (successCount > 0 && untouchedCount > 0) {
            ItemStack untouchedStack = gemStack.copy();
            untouchedStack.setCount(untouchedCount);
            if (!player.addItem(untouchedStack.copy())) {
                player.drop(untouchedStack.copy(), false);
            }
        }

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.proficiency_jewel_magic_release = Math.min(100.0, vars.proficiency_jewel_magic_release + (successCount * 0.3));
        vars.syncPlayerVariables(player);

        if (attempts == 1) {
            if (successCount > 0) {
                player.displayClientMessage(
                        Component.translatable("message.typemoonworld.gem.engrave.success", GemEngravingService.getMagicName(magicId)),
                        true
                );
            } else {
                player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.failed"), true);
            }
        } else {
            player.displayClientMessage(
                    Component.translatable("message.typemoonworld.gem.engrave.batch.result", successCount, failCount),
                    true
            );
            if (untouchedCount > 0) {
                player.displayClientMessage(
                        Component.translatable("message.typemoonworld.gem.engrave.batch.remaining", untouchedCount),
                        true
                );
            }
        }

        if (successCount > 0) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.PLAYERS, 0.8F, 1.2F);
        } else {
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
        }

        this.blockEntity.setChanged();
        this.broadcastChanges();
    }

    private void damageChisel(ServerPlayer player, ItemStack toolStack) {
        toolStack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        if (toolStack.isEmpty()) {
            this.itemHandler.setStackInSlot(SLOT_TOOL, ItemStack.EMPTY);
        } else {
            this.itemHandler.setStackInSlot(SLOT_TOOL, toolStack);
        }
    }

    private static int getAvailableChiselUses(ItemStack toolStack) {
        if (toolStack.isEmpty()) {
            return 0;
        }
        if (!toolStack.isDamageableItem()) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, toolStack.getMaxDamage() - toolStack.getDamageValue());
    }

    private static int clampReinforcementLevel(TypeMoonWorldModVariables.PlayerVariables vars, int requestedLevel) {
        int maxLevel = Math.min(5, 1 + (int) (Math.max(0.0, vars.proficiency_reinforcement) / 20.0));
        int raw = requestedLevel <= 0 ? 1 : requestedLevel;
        return Math.max(1, Math.min(maxLevel, raw));
    }

    private static double getMagicProficiency(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
        return switch (magicId) {
            case "projection" -> vars.proficiency_projection;
            case "reinforcement" -> vars.proficiency_reinforcement;
            case "gravity_magic" -> vars.proficiency_gravity_magic;
            default -> 0.0;
        };
    }

    private static boolean hasLearnedMagic(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
        if ("reinforcement".equals(magicId)) {
            return vars.learned_magics.contains("reinforcement")
                    || vars.learned_magics.contains("reinforcement_self")
                    || vars.learned_magics.contains("reinforcement_other")
                    || vars.learned_magics.contains("reinforcement_item");
        }
        return vars.learned_magics.contains(magicId);
    }

    private static boolean isValidMagicId(String magicId) {
        return magicId != null
                && !magicId.isEmpty()
                && magicId.length() <= MAX_MAGIC_ID_LENGTH
                && magicId.matches("[a-z0-9_]+");
    }

    private static double calculateStructureProjectionCost(TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure, boolean hasSwordAttribute) {
        if (structure == null || structure.blocks == null || structure.blocks.isEmpty()) {
            return 0.0;
        }

        double totalCost = 0.0;
        for (TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock savedBlock : structure.blocks) {
            ResourceLocation blockId = ResourceLocation.tryParse(savedBlock.blockId);
            if (blockId == null) {
                continue;
            }
            BlockState defaultState = BuiltInRegistries.BLOCK.get(blockId).defaultBlockState();
            Item blockItem = defaultState.getBlock().asItem();
            if (blockItem == Items.AIR) {
                continue;
            }
            ItemStack costStack = blockItem.getDefaultInstance();
            costStack.setCount(1);
            totalCost += MagicStructuralAnalysis.calculateStructureCost(costStack, hasSwordAttribute);
        }
        return totalCost;
    }
}
