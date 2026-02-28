package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.CarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.gravity.GemGravityFieldMagic;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGravity;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGravityEffectHandler;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.StructureProjectionBuildHandler;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

import java.util.ArrayList;
import java.util.List;

public final class GemEngravingService {
    public enum CastResult {
        NOT_ENGRAVED,
        SUCCESS,
        FAILED
    }

    public static final String TAG_ENGRAVED_MAGIC = "TypeMoonGemEngravedMagic";
    private static final String TAG_ENGRAVED_AT = "TypeMoonGemEngravedAt";
    public static final String TAG_ENGRAVED_MANA_COST = "TypeMoonGemEngravedManaCost";
    public static final String TAG_ENGRAVED_CONTENT_KEY = "TypeMoonGemEngravedContentKey";
    public static final String TAG_REINFORCEMENT_PART = "TypeMoonGemReinforcementPart";
    public static final String TAG_REINFORCEMENT_LEVEL = "TypeMoonGemReinforcementLevel";
    public static final String TAG_PROJECTION_KIND = "TypeMoonGemProjectionKind";
    public static final String TAG_PROJECTION_ITEM = "TypeMoonGemProjectionItem";
    public static final String TAG_PROJECTION_ITEM_NAME = "TypeMoonGemProjectionItemName";
    public static final String TAG_PROJECTION_STRUCTURE_ID = "TypeMoonGemProjectionStructureId";
    public static final String TAG_PROJECTION_STRUCTURE_NAME = "TypeMoonGemProjectionStructureName";

    public static final String PROJECTION_KIND_ITEM = "item";
    public static final String PROJECTION_KIND_STRUCTURE = "structure";

    private static final String ADVANCED_JEWEL_MAGIC_ID = "jewel_magic_release";
    private static final int GRAVITY_MIN_DURATION_TICKS = 20 * 30;
    private static final int GRAVITY_MAX_DURATION_TICKS = 20 * 180;

    private GemEngravingService() {
    }

    public static boolean tryEngraveWithChisel(ServerPlayer player, InteractionHand chiselHand, ItemStack chiselStack, ItemStack offhandStack) {
        if (!(offhandStack.getItem() instanceof CarvedGemItem carvedGem)) {
            return false;
        }
        if (!player.isShiftKeyDown()) {
            return false;
        }
        if (offhandStack.getCount() != 1) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.single_required"), true);
            return true;
        }

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!vars.learned_magics.contains(ADVANCED_JEWEL_MAGIC_ID)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.locked"), true);
            return true;
        }

        String selectedMagic = getSelectedMagic(vars);
        if (selectedMagic == null) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.no_magic_selected"), true);
            return true;
        }

        if (!GemCompatibilityService.isWhitelistedMagic(selectedMagic)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.not_supported"), true);
            return true;
        }

        if (getEngravedMagicId(offhandStack) != null) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.already"), true);
            return true;
        }

        GemQuality quality = carvedGem.getQuality();
        GemType type = carvedGem.getType();
        int chance = GemCompatibilityService.calculateEngraveSuccessChance(quality, type, selectedMagic, vars.proficiency_jewel_magic_release);
        int roll = player.level().getRandom().nextInt(100) + 1;
        boolean success = roll <= chance;

        damageChisel(player, chiselHand, chiselStack);
        if (success) {
            if (offhandStack.getItem() != ModItems.getNormalizedCarvedGem(type)) {
                ItemStack normalized = new ItemStack(ModItems.getNormalizedCarvedGem(type), offhandStack.getCount());
                copyEngravingData(offhandStack, normalized);
                player.setItemInHand(InteractionHand.OFF_HAND, normalized);
                offhandStack = normalized;
            }

            setEngravedMagic(offhandStack, selectedMagic);
            vars.proficiency_jewel_magic_release = Math.min(100.0, vars.proficiency_jewel_magic_release + 0.3);
            vars.syncPlayerVariables(player);
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.success", getMagicName(selectedMagic)), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.PLAYERS, 0.8F, 1.2F);
        } else {
            offhandStack.shrink(1);
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.failed"), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        return true;
    }

    public static CastResult tryCastEngravedMagic(ServerPlayer player, InteractionHand hand, ItemStack gemStack) {
        String magicId = getEngravedMagicId(gemStack);
        if (magicId == null) {
            return CastResult.NOT_ENGRAVED;
        }

        boolean success = switch (magicId) {
            case "projection" -> castProjection(player, gemStack);
            case "reinforcement" -> castReinforcement(player, gemStack);
            case "gravity_magic" -> castGravity(player, gemStack);
            default -> false;
        };

        if (!success) {
            return CastResult.FAILED;
        }

        consumeHeldGem(player, hand, gemStack);
        return CastResult.SUCCESS;
    }

    public static String getEngravedMagicId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(TAG_ENGRAVED_MAGIC, 8)) {
            return null;
        }
        String id = tag.getString(TAG_ENGRAVED_MAGIC);
        return id.isEmpty() ? null : id;
    }

    public static void copyEngravingData(ItemStack from, ItemStack to) {
        CustomData customData = from.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            to.remove(DataComponents.CUSTOM_DATA);
            return;
        }
        CompoundTag tag = customData.copyTag();
        tag.remove(TAG_ENGRAVED_AT);
        ensureContentKey(tag);
        to.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void setEngravedMagic(ItemStack stack, String magicId) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(TAG_ENGRAVED_MAGIC, magicId);
        tag.remove(TAG_ENGRAVED_AT);
        tag.remove(TAG_ENGRAVED_MANA_COST);
        tag.remove(TAG_ENGRAVED_CONTENT_KEY);
        tag.remove(TAG_REINFORCEMENT_PART);
        tag.remove(TAG_REINFORCEMENT_LEVEL);
        tag.remove(TAG_PROJECTION_KIND);
        tag.remove(TAG_PROJECTION_ITEM);
        tag.remove(TAG_PROJECTION_ITEM_NAME);
        tag.remove(TAG_PROJECTION_STRUCTURE_ID);
        tag.remove(TAG_PROJECTION_STRUCTURE_NAME);
        tag.putString(TAG_ENGRAVED_CONTENT_KEY, magicId == null ? "" : magicId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void setEngravedManaCost(ItemStack stack, double manaCost) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putDouble(TAG_ENGRAVED_MANA_COST, Math.max(0.0, manaCost));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static double getEngravedManaCost(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 0.0;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(TAG_ENGRAVED_MANA_COST)) {
            return 0.0;
        }
        return Math.max(0.0, tag.getDouble(TAG_ENGRAVED_MANA_COST));
    }

    public static void setReinforcementConfig(ItemStack stack, int part, int level, double manaCost) {
        int clampedPart = Math.max(0, Math.min(3, part));
        int clampedLevel = Math.max(1, Math.min(5, level));
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TAG_REINFORCEMENT_PART, clampedPart);
        tag.putInt(TAG_REINFORCEMENT_LEVEL, clampedLevel);
        tag.putDouble(TAG_ENGRAVED_MANA_COST, Math.max(0.0, manaCost));
        tag.putString(TAG_ENGRAVED_CONTENT_KEY, "reinforcement:" + clampedPart + ":" + clampedLevel);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void setProjectionItemConfig(ItemStack stack, ItemStack selectedItem, HolderLookup.Provider registries, double manaCost) {
        ItemStack sanitized = sanitizeProjectionTemplate(selectedItem);
        String itemId = BuiltInRegistries.ITEM.getKey(sanitized.getItem()).toString();
        int payloadHash = sanitized.save(registries).hashCode();
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(TAG_PROJECTION_KIND, PROJECTION_KIND_ITEM);
        tag.put(TAG_PROJECTION_ITEM, sanitized.save(registries));
        tag.putString(TAG_PROJECTION_ITEM_NAME, sanitized.getHoverName().getString());
        tag.putDouble(TAG_ENGRAVED_MANA_COST, Math.max(0.0, manaCost));
        tag.putString(TAG_ENGRAVED_CONTENT_KEY, "projection:item:" + itemId + ":" + Integer.toHexString(payloadHash));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void setProjectionStructureConfig(ItemStack stack, String structureId, String structureName, double manaCost) {
        String normalizedStructureId = structureId == null ? "" : structureId;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(TAG_PROJECTION_KIND, PROJECTION_KIND_STRUCTURE);
        tag.putString(TAG_PROJECTION_STRUCTURE_ID, normalizedStructureId);
        tag.putString(TAG_PROJECTION_STRUCTURE_NAME, structureName == null ? "" : structureName);
        tag.putDouble(TAG_ENGRAVED_MANA_COST, Math.max(0.0, manaCost));
        tag.putString(TAG_ENGRAVED_CONTENT_KEY, "projection:structure:" + normalizedStructureId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isProjectionStructure(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        return PROJECTION_KIND_STRUCTURE.equals(customData.copyTag().getString(TAG_PROJECTION_KIND));
    }

    public static int getReinforcementPart(ItemStack stack, int fallback) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return fallback;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(TAG_REINFORCEMENT_PART)) {
            return fallback;
        }
        return Math.max(0, Math.min(3, tag.getInt(TAG_REINFORCEMENT_PART)));
    }

    public static int getReinforcementLevel(ItemStack stack, int fallback) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return fallback;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(TAG_REINFORCEMENT_LEVEL)) {
            return fallback;
        }
        return Math.max(1, Math.min(5, tag.getInt(TAG_REINFORCEMENT_LEVEL)));
    }

    public static String getProjectionStructureId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return "";
        }
        return customData.copyTag().getString(TAG_PROJECTION_STRUCTURE_ID);
    }

    public static String getProjectionStructureName(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return "";
        }
        return customData.copyTag().getString(TAG_PROJECTION_STRUCTURE_NAME);
    }

    public static String getProjectionItemName(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return "";
        }
        return customData.copyTag().getString(TAG_PROJECTION_ITEM_NAME);
    }

    public static ItemStack getProjectionItemTemplate(ItemStack stack, HolderLookup.Provider registries) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return ItemStack.EMPTY;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(TAG_PROJECTION_ITEM, 10)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parseOptional(registries, tag.getCompound(TAG_PROJECTION_ITEM));
    }

    public static List<Component> getEngravingDetailLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        String magicId = getEngravedMagicId(stack);
        if (magicId == null) {
            return lines;
        }

        lines.add(Component.translatable("tooltip.typemoonworld.gem.engraved.magic", getMagicName(magicId)));
        switch (magicId) {
            case "reinforcement" -> {
                int part = getReinforcementPart(stack, 0);
                int level = getReinforcementLevel(stack, 1);
                lines.add(Component.translatable(
                        "tooltip.typemoonworld.gem.engraved.reinforcement",
                        getReinforcementPartName(part),
                        level
                ));
            }
            case "projection" -> {
                if (isProjectionStructure(stack)) {
                    String name = getProjectionStructureName(stack);
                    lines.add(Component.translatable("tooltip.typemoonworld.gem.engraved.projection.mode",
                            Component.translatable("tooltip.typemoonworld.gem.engraved.projection.mode.structure")));
                    if (!name.isEmpty()) {
                        lines.add(Component.translatable("tooltip.typemoonworld.gem.engraved.projection.target", Component.literal(name)));
                    }
                } else {
                    String itemName = getProjectionItemName(stack);
                    lines.add(Component.translatable("tooltip.typemoonworld.gem.engraved.projection.mode",
                            Component.translatable("tooltip.typemoonworld.gem.engraved.projection.mode.item")));
                    if (!itemName.isEmpty()) {
                        lines.add(Component.translatable("tooltip.typemoonworld.gem.engraved.projection.target", Component.literal(itemName)));
                    }
                }
            }
            case "gravity_magic" -> lines.add(Component.translatable("tooltip.typemoonworld.gem.engraved.gravity.self_cast_hint"));
            default -> {
            }
        }

        double manaCost = getEngravedManaCost(stack);
        if (manaCost > 0.0) {
            lines.add(Component.translatable("tooltip.typemoonworld.gem.engraved.mana_cost", (int) Math.ceil(manaCost)));
        }
        return lines;
    }

    public static double calculateReinforcementManaCost(int level) {
        return 20.0 * Math.max(1, Math.min(5, level));
    }

    public static Component getReinforcementPartName(int part) {
        return switch (Math.max(0, Math.min(3, part))) {
            case 1 -> Component.translatable("gui.typemoonworld.mode.hand");
            case 2 -> Component.translatable("gui.typemoonworld.mode.leg");
            case 3 -> Component.translatable("gui.typemoonworld.mode.eye");
            default -> Component.translatable("gui.typemoonworld.mode.body");
        };
    }

    public static ItemStack sanitizeProjectionTemplate(ItemStack source) {
        ItemStack projected = source.copy();
        projected.setCount(1);
        projected.remove(DataComponents.CONTAINER);
        projected.remove(DataComponents.BUNDLE_CONTENTS);
        projected.remove(DataComponents.BLOCK_ENTITY_DATA);
        return projected;
    }

    private static String getSelectedMagic(TypeMoonWorldModVariables.PlayerVariables vars) {
        if (vars.selected_magics.isEmpty()) {
            return null;
        }
        if (vars.current_magic_index < 0 || vars.current_magic_index >= vars.selected_magics.size()) {
            return null;
        }
        return vars.selected_magics.get(vars.current_magic_index);
    }

    private static void ensureContentKey(CompoundTag tag) {
        if (tag == null || !tag.contains(TAG_ENGRAVED_MAGIC, 8)) {
            return;
        }
        String key = tag.getString(TAG_ENGRAVED_CONTENT_KEY);
        if (!key.isEmpty()) {
            return;
        }

        String magicId = tag.getString(TAG_ENGRAVED_MAGIC);
        if ("reinforcement".equals(magicId)) {
            int part = Math.max(0, Math.min(3, tag.getInt(TAG_REINFORCEMENT_PART)));
            int level = Math.max(1, Math.min(5, tag.getInt(TAG_REINFORCEMENT_LEVEL)));
            tag.putString(TAG_ENGRAVED_CONTENT_KEY, "reinforcement:" + part + ":" + level);
            return;
        }

        if ("projection".equals(magicId)) {
            String kind = tag.getString(TAG_PROJECTION_KIND);
            if (PROJECTION_KIND_STRUCTURE.equals(kind)) {
                tag.putString(TAG_ENGRAVED_CONTENT_KEY, "projection:structure:" + tag.getString(TAG_PROJECTION_STRUCTURE_ID));
            } else if (PROJECTION_KIND_ITEM.equals(kind)) {
                tag.putString(TAG_ENGRAVED_CONTENT_KEY, "projection:item:" + tag.getString(TAG_PROJECTION_ITEM_NAME));
            } else {
                tag.putString(TAG_ENGRAVED_CONTENT_KEY, "projection");
            }
            return;
        }

        tag.putString(TAG_ENGRAVED_CONTENT_KEY, magicId);
    }

    public static Component getMagicName(String magicId) {
        return switch (magicId) {
            case "projection" -> Component.translatable("magic.typemoonworld.projection.name");
            case "reinforcement" -> Component.translatable("magic.typemoonworld.reinforcement.name");
            case "gravity_magic" -> Component.translatable("magic.typemoonworld.gravity_magic.name");
            default -> Component.literal(magicId);
        };
    }

    private static void damageChisel(ServerPlayer player, InteractionHand chiselHand, ItemStack chiselStack) {
        EquipmentSlot slot = chiselHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        chiselStack.hurtAndBreak(1, player, slot);
    }

    private static void consumeHeldGem(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        stack.shrink(1);
        if (stack.isEmpty()) {
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
    }

    private static boolean castProjection(ServerPlayer player, ItemStack gemStack) {
        if (isProjectionStructure(gemStack)) {
            return castProjectionStructure(player, gemStack);
        }
        return castProjectionItem(player, gemStack);
    }

    private static boolean castProjectionItem(ServerPlayer player, ItemStack gemStack) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        ItemStack selected = getProjectionItemTemplate(gemStack, player.registryAccess());
        if (selected.isEmpty()) {
            selected = vars.projection_selected_item;
        }
        if (selected.isEmpty()) {
            return false;
        }

        ItemStack projected = sanitizeProjectionTemplate(selected);
        if (projected.isDamageableItem()) {
            projected.setDamageValue(Math.max(0, projected.getMaxDamage() - 1));
        }

        CompoundTag tag = projected.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean("is_projected", true);
        tag.putLong("projection_time", player.level().getGameTime());
        projected.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        projected.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        if (!player.addItem(projected.copy())) {
            player.drop(projected.copy(), false);
        }
        return true;
    }

    private static boolean castProjectionStructure(ServerPlayer player, ItemStack gemStack) {
        String structureId = getProjectionStructureId(gemStack);
        if (structureId.isEmpty()) {
            return false;
        }

        BlockPos anchor = player.blockPosition().above();
        HitResult hitResult = player.pick(32.0D, 1.0F, false);
        if (hitResult instanceof BlockHitResult blockHit) {
            anchor = blockHit.getBlockPos().relative(blockHit.getDirection());
        }

        if (!StructureProjectionBuildHandler.startProjectionFromGem(player, structureId, anchor, 0)) {
            return false;
        }
        return true;
    }

    private static boolean castReinforcement(ServerPlayer player, ItemStack gemStack) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        int part = getReinforcementPart(gemStack, vars.reinforcement_mode);
        int level = getReinforcementLevel(gemStack, 1);
        int amplifier = Math.max(0, level - 1);
        int duration = (600 + (int) Math.round(Math.max(0, Math.min(100, vars.proficiency_reinforcement)) * 10.0)) * level;

        switch (part) {
            case 1 -> {
                player.addEffect(new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_STRENGTH, duration, amplifier, false, false, true));
            }
            case 2 -> {
                player.addEffect(new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_AGILITY, duration, amplifier, false, false, true));
            }
            case 3 -> {
                player.addEffect(new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_SIGHT, duration, amplifier, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, false));
            }
            default -> {
                player.addEffect(new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_DEFENSE, duration, amplifier, false, false, true));
            }
        }
        return true;
    }

    private static boolean castGravity(ServerPlayer player, ItemStack gemStack) {
        boolean thrown = GemGravityFieldMagic.throwGravityFieldProjectile(player, gemStack);
        return thrown;
    }

    public static boolean castGravitySelfFromGem(ServerPlayer player, InteractionHand hand, int mode) {
        if (player == null) {
            return false;
        }
        if (mode < MagicGravity.MODE_ULTRA_LIGHT || mode > MagicGravity.MODE_ULTRA_HEAVY) {
            return false;
        }

        ItemStack heldStack = player.getItemInHand(hand);
        if (heldStack.isEmpty() || getEngravedMagicId(heldStack) == null || !"gravity_magic".equals(getEngravedMagicId(heldStack))) {
            return false;
        }

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        int duration = calculateGravityDurationTicks(vars.proficiency_gravity_magic);

        if (mode == MagicGravity.MODE_NORMAL) {
            MagicGravityEffectHandler.clearGravityState(player);
        } else {
            long until = player.level().getGameTime() + duration;
            MagicGravityEffectHandler.applyGravityState(player, mode, until);
        }

        vars.proficiency_gravity_magic = Math.min(100.0, vars.proficiency_gravity_magic + 0.2);
        vars.syncPlayerVariables(player);
        consumeHeldGem(player, hand, heldStack);
        return true;
    }

    private static int calculateGravityDurationTicks(double proficiency) {
        double clamped = Math.max(0.0, Math.min(100.0, proficiency));
        double ratio = clamped / 100.0;
        return GRAVITY_MIN_DURATION_TICKS + (int) Math.round((GRAVITY_MAX_DURATION_TICKS - GRAVITY_MIN_DURATION_TICKS) * ratio);
    }
}
