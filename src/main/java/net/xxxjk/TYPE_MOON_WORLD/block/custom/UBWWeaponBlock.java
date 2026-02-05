package net.xxxjk.TYPE_MOON_WORLD.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.ModBlockEntities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;

import net.xxxjk.TYPE_MOON_WORLD.block.entity.UBWWeaponBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

public class UBWWeaponBlock extends BaseEntityBlock {
    public static final MapCodec<UBWWeaponBlock> CODEC = simpleCodec(UBWWeaponBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    
    // Additional rotation properties for visual variety
    public static final BooleanProperty ROTATION_A = BooleanProperty.create("rot_a");
    public static final BooleanProperty ROTATION_B = BooleanProperty.create("rot_b");
    public static final BooleanProperty ROTATION_C = BooleanProperty.create("rot_c");
    public static final BooleanProperty ROTATION_D = BooleanProperty.create("rot_d");
    public static final BooleanProperty ROTATION_E = BooleanProperty.create("rot_e");
    public static final BooleanProperty ROTATION_F = BooleanProperty.create("rot_f");
    public static final BooleanProperty ROTATION_G = BooleanProperty.create("rot_g");

    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public UBWWeaponBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ROTATION_A, false)
                .setValue(ROTATION_B, false)
                .setValue(ROTATION_C, false)
                .setValue(ROTATION_D, false)
                .setValue(ROTATION_E, false)
                .setValue(ROTATION_F, false)
                .setValue(ROTATION_G, false));
    }

    @Override
    @NotNull
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROTATION_A, ROTATION_B, ROTATION_C, ROTATION_D, ROTATION_E, ROTATION_F, ROTATION_G);
    }

    @Override
    @NotNull
    public VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    @NotNull
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new UBWWeaponBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.UBW_WEAPON_BLOCK_ENTITY.get(), UBWWeaponBlock::tick);
    }

    private static void tick(Level level, BlockPos pos, BlockState state, UBWWeaponBlockEntity blockEntity) {
        if (level.isClientSide) return;
        
        // Don't despawn inside UBW dimension
        if (level.dimension().location().equals(ModDimensions.UBW_KEY.location())) return;
        
        // Check every 100 ticks (5 seconds)
        if (level.getGameTime() % 100 == 0) {
            // Check for nearby living entities (Radius 32)
            // If no one is nearby, despawn
            AABB searchBox = new AABB(pos).inflate(32);
            if (level.getEntitiesOfClass(LivingEntity.class, searchBox).isEmpty()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }
    }

    @Override
    @NotNull
    protected ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof UBWWeaponBlockEntity ubwTile) {
                // If player right-clicks with empty hand or anything
                // Retrieve the item
                ItemStack storedItem = ubwTile.getStoredItem();
                if (!storedItem.isEmpty()) {
                    applyUBWAttributes(storedItem, player);
                    // Give item to player (or drop if full)
                    if (!player.getInventory().add(storedItem)) {
                        player.drop(storedItem, false);
                    }
                    
                    // Clear and destroy block
                    ubwTile.setStoredItem(ItemStack.EMPTY);
                    level.destroyBlock(pos, false);
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private void applyUBWAttributes(ItemStack stack, Player player) {
        // Durability
        if (stack.isDamageableItem()) {
             boolean isSword = stack.getItem() instanceof SwordItem;
             int maxDmg = stack.getMaxDamage();
             if (isSword) {
                 stack.setDamageValue((int)(maxDmg * 0.333));
             } else {
                 stack.setDamageValue((int)(maxDmg * 0.9));
             }
        }
        
        // Tags
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("is_projected", true);
        tag.putBoolean("is_infinite_projection", true);
        tag.putLong("projection_time", player.level().getGameTime());
        
        CustomData existingData = stack.get(DataComponents.CUSTOM_DATA);
        if (existingData != null) {
            CompoundTag existingTag = existingData.copyTag();
            existingTag.merge(tag);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(existingTag));
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        
        // No Glint as requested
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }
}
