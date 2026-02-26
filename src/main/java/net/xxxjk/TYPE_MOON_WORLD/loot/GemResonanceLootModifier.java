package net.xxxjk.TYPE_MOON_WORLD.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.event.GemTerrainEventHandler;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.util.ModTags;

public class GemResonanceLootModifier extends LootModifier {
    public static final MapCodec<GemResonanceLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            inst -> LootModifier.codecStart(inst).apply(inst, GemResonanceLootModifier::new)
    );

    public GemResonanceLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!(context.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return generatedLoot;
        var blockState = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (blockState == null || !blockState.is(ModTags.Blocks.GEM_ORES)) return generatedLoot;

        var entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer player)) return generatedLoot;
        if (!GemTerrainEventHandler.isResonanceActive(level, player.blockPosition())) return generatedLoot;
        if (context.getRandom().nextDouble() >= Config.gemBonusDropChance) return generatedLoot;

        ItemStack bonusDrop = getBonusDrop(blockState.getBlock());
        if (bonusDrop.isEmpty()) return generatedLoot;

        ItemStack brokenBlockAsItem = new ItemStack(blockState.getBlock().asItem());
        for (ItemStack loot : generatedLoot) {
            if (ItemStack.isSameItemSameComponents(loot, brokenBlockAsItem)) {
                return generatedLoot;
            }
        }

        generatedLoot.add(bonusDrop);
        return generatedLoot;
    }

    private ItemStack getBonusDrop(Block block) {
        if (block == ModBlocks.EMERALD_MINE.get()) return new ItemStack(ModItems.RAW_EMERALD.get());
        if (block == ModBlocks.RUBY_MINE.get()) return new ItemStack(ModItems.RAW_RUBY.get());
        if (block == ModBlocks.SAPPHIRE_MINE.get()) return new ItemStack(ModItems.RAW_SAPPHIRE.get());
        if (block == ModBlocks.TOPAZ_MINE.get()) return new ItemStack(ModItems.RAW_TOPAZ.get());
        if (block == ModBlocks.WHITE_GEMSTONE_MINE.get()) return new ItemStack(ModItems.RAW_WHITE_GEMSTONE.get());
        if (block == ModBlocks.CYAN_GEMSTONE_MINE.get()) return new ItemStack(ModItems.RAW_CYAN_GEMSTONE.get());
        return ItemStack.EMPTY;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}

