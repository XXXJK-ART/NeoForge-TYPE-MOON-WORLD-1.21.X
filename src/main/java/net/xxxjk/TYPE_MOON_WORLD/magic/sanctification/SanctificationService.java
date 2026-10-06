package net.xxxjk.TYPE_MOON_WORLD.magic.sanctification;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.DeadApostleEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import com.example.typemoonaddon.entity.SummonedSpiritEntity;

/** Shared item, projectile, block and combat rules for Sanctification. */
@EventBusSubscriber(modid = "typemoonworld")
public final class SanctificationService {
    public static final String SANCTIFIED = "Sanctified";
    public static final String PROJECTILE_TAG = "TypeMoonSanctifiedProjectile";
    private SanctificationService() {}

    public static boolean isSanctified(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(SANCTIFIED);
    }
    public static void markItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.getBoolean(SANCTIFIED)) tag.putBoolean(SANCTIFIED, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        int current = stack.getMaxDamage();
        if (current > 0) stack.set(DataComponents.MAX_DAMAGE, Math.max(current + current, current));
    }
    public static boolean isSanctifiedBlock(ServerLevel level, net.minecraft.core.BlockPos pos) {
        return SanctifiedBlockData.get(level).contains(pos);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LivingEntity living)) return;
        if (isSanctified(living.getMainHandItem()) || isSanctified(living.getOffhandItem())) {
            SanctifiedBlockData.get(level).add(event.getPos());
        } else {
            SanctifiedBlockData.get(level).remove(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) SanctifiedBlockData.get(level).remove(event.getPos());
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || event.getPosition().isEmpty()) return;
        net.minecraft.core.BlockPos pos = event.getPosition().get();
        if (!isSanctifiedBlock(level, pos)) return;
        float hardness = event.getState().getDestroySpeed(level, pos);
        if (hardness > 0.0F) event.setNewSpeed(event.getOriginalSpeed() * hardness / (hardness + 2.0F));
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof PrimedTnt tnt && tnt.level() instanceof ServerLevel level
                && isSanctifiedBlock(level, tnt.blockPosition())) {
            tnt.getPersistentData().putBoolean(PROJECTILE_TAG, true);
            tnt.getPersistentData().putBoolean("TypeMoonSanctifiedTnt", true);
            return;
        }
        if (!(event.getEntity() instanceof Projectile projectile)) return;
        Entity owner = projectile.getOwner();
        if (owner instanceof LivingEntity living && (isSanctified(living.getMainHandItem()) || isSanctified(living.getOffhandItem()))) {
            projectile.getPersistentData().putBoolean(PROJECTILE_TAG, true);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0.0F) return;
        Entity direct = event.getSource().getDirectEntity();
        Entity attacker = event.getSource().getEntity();
        boolean sanctifiedTnt = direct != null && direct.getPersistentData().getBoolean("TypeMoonSanctifiedTnt");
        if (sanctifiedTnt && isSpecialTarget(event.getEntity())) {
            event.setAmount(event.getAmount() * 1.5F);
            return;
        }
        boolean sanctified = direct != null && direct.getPersistentData().getBoolean(PROJECTILE_TAG);
        if (attacker instanceof LivingEntity living) sanctified |= isSanctified(living.getMainHandItem()) || isSanctified(living.getOffhandItem());
        if (direct instanceof ThrownPotion potion && isSanctified(potion.getItem())) {
            String key = "SanctificationPotion:" + event.getEntity().getUUID();
            if (!direct.getPersistentData().getBoolean(key) && isSpecialTarget(event.getEntity())) {
                direct.getPersistentData().putBoolean(key, true);
                event.setAmount(event.getAmount() + 20.0F);
            }
        }
        if (!sanctified) return;
        float amount = event.getAmount() + 3.0F;
        if (isSpecialTarget(event.getEntity())) amount *= 1.5F;
        event.setAmount(amount);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SanctifiedBlockData.get(level).cleanup(level);
        }
    }

    public static boolean isSpecialTarget(LivingEntity entity) {
        return entity instanceof DeadApostleEntity || entity instanceof SummonedSpiritEntity || entity instanceof ServantEntity;
    }
}
