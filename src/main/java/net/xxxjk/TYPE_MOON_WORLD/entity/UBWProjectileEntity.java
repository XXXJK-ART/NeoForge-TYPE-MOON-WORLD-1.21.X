package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.neoforged.neoforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.UBWWeaponBlock;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.UBWWeaponBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler;
import net.minecraft.core.Direction;

@SuppressWarnings("null")
public class UBWProjectileEntity extends ThrowableItemProjectile {

    private List<Entity> hitEntities = new ArrayList<>();

    public UBWProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public UBWProjectileEntity(Level level, LivingEntity shooter, ItemStack stack) {
        super(ModEntities.UBW_PROJECTILE.get(), shooter, level);
        this.setItem(stack);
    }

    public UBWProjectileEntity(Level level, double x, double y, double z) {
        super(ModEntities.UBW_PROJECTILE.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_SWORD; // Fallback
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
             this.level().addParticle(ParticleTypes.ENCHANT, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }
    
    private float calculateDamage() {
        ItemStack stack = this.getItem();
        if (stack.isEmpty()) return 4.0f;
        
        ItemAttributeModifiers modifiers = stack.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double damage = modifiers.compute(1.0, EquipmentSlot.MAINHAND); 
        return (float)Math.max(4.0, damage);
    }

    @Override
    protected void onHit(HitResult result) {
        if (!this.level().isClientSide) {
            // Check if hit entity is player (shooter)
            if (result.getType() == HitResult.Type.ENTITY) {
                net.minecraft.world.phys.EntityHitResult entityHit = (net.minecraft.world.phys.EntityHitResult) result;
                Entity target = entityHit.getEntity();

                
                // Ignore shooter (Owner)
                if (target.equals(this.getOwner())) {
                    return; 
                }
                
                // Damage other entities but DO NOT STOP
                if (!hitEntities.contains(target)) {
                    Entity owner = this.getOwner();
                    if (owner instanceof ServerPlayer serverPlayer && this.level() instanceof ServerLevel serverLevel) {
                        FakePlayer fakePlayer = new FakePlayer(serverLevel, new GameProfile(UUID.randomUUID(), "[UBW_Proxy]"));
                        fakePlayer.setPos(this.getX(), this.getY(), this.getZ());
                        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, this.getItem());
                        
                        // Reset attack cooldown to ensure full damage
                        fakePlayer.resetAttackStrengthTicker();
                        
                        // Attack
                        fakePlayer.attack(target);
                        
                        // Remove Invulnerability Frames
                        target.invulnerableTime = 0;
                        
                        // Restore Aggro/Credit to real player
                        if (target instanceof LivingEntity livingTarget) {
                            livingTarget.setLastHurtByMob(serverPlayer);
                            if (livingTarget instanceof Mob mob) {
                                mob.setTarget(serverPlayer);
                            }
                        }
                        
                        // Cleanup
                        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        fakePlayer.discard();
                    } else {
                        float damage = calculateDamage();
                        target.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
                    }
                    hitEntities.add(target);
                }
                
                // Important: We do NOT call super.onHit() or discard() here.
                // This allows the projectile to continue moving.
                return; 
            }
            
            // If hit block
            if (result.getType() == HitResult.Type.BLOCK) {
                net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) result;
                BlockState state = this.level().getBlockState(blockHit.getBlockPos());
                
                // Pass through non-solid blocks (grass, flowers, etc.)
                if (state.getCollisionShape(this.level(), blockHit.getBlockPos()).isEmpty() || !state.canOcclude()) {
                     // Ignore non-solid hits
                     return; 
                }

                BlockPos hitPos = blockHit.getBlockPos();
                BlockPos placePos = hitPos.relative(blockHit.getDirection());
                
                // Check if placeable (Air or replaceable)
                BlockState placeState = this.level().getBlockState(placePos);
                if (!placeState.canBeReplaced()) {
                     this.discard();
                     return;
                }
                
                // Set Block
                Direction facing = Direction.fromYRot(this.getYRot()).getOpposite();
                if (facing == Direction.UP || facing == Direction.DOWN) facing = Direction.NORTH;
                
                BlockState newState = ModBlocks.UBW_WEAPON_BLOCK.get().defaultBlockState()
                        .setValue(UBWWeaponBlock.FACING, facing)
                        .setValue(UBWWeaponBlock.ROTATION_A, this.random.nextBoolean())
                        .setValue(UBWWeaponBlock.ROTATION_B, this.random.nextBoolean())
                        .setValue(UBWWeaponBlock.ROTATION_C, this.random.nextBoolean())
                        .setValue(UBWWeaponBlock.ROTATION_D, this.random.nextBoolean())
                        .setValue(UBWWeaponBlock.ROTATION_E, this.random.nextBoolean())
                        .setValue(UBWWeaponBlock.ROTATION_F, this.random.nextBoolean())
                        .setValue(UBWWeaponBlock.ROTATION_G, this.random.nextBoolean());
                
                if (this.level().setBlock(placePos, newState, 3)) {
                     // Set Tile Entity
                     if (this.level().getBlockEntity(placePos) instanceof UBWWeaponBlockEntity tile) {
                         tile.setStoredItem(this.getItem());
                     }
                     
                     // Register to ChantHandler if owner is player
                     if (this.getOwner() instanceof ServerPlayer player) {
                         ChantHandler.registerPlacedSword(player.getUUID(), placePos);
                     }
                }

                this.discard();
                return;
            }

            this.discard();
        }
    }
}