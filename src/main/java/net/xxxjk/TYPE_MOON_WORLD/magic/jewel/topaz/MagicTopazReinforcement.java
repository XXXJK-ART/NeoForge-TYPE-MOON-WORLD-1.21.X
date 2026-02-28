
package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.topaz;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.utils.GemUtils;

public class MagicTopazReinforcement {
    public static void execute(Entity entity) {
        if (!(entity instanceof Player player))
            return;

        int count = countFullGems(player);
        if (count < 3) {
            player.displayClientMessage(Component.translatable(MagicConstants.MSG_MAGIC_TOPAZ_REINFORCEMENT_NEED_GEM), true);
            return;
        }

        ItemStack gem1 = GemUtils.consumeGem(player, GemType.TOPAZ);
        ItemStack gem2 = GemUtils.consumeGem(player, GemType.TOPAZ);
        ItemStack gem3 = GemUtils.consumeGem(player, GemType.TOPAZ);

        float m1 = 1.0f;
        if (gem1.getItem() instanceof FullManaCarvedGemItem g) m1 = g.getQuality().getEffectMultiplier();
        float m2 = 1.0f;
        if (gem2.getItem() instanceof FullManaCarvedGemItem g) m2 = g.getQuality().getEffectMultiplier();
        float m3 = 1.0f;
        if (gem3.getItem() instanceof FullManaCarvedGemItem g) m3 = g.getQuality().getEffectMultiplier();

        applyReinforcement(player, (m1 + m2 + m3) / 3.0f);
    }

    public static void executeFromCombo(Player player, float multiplier) {
        if (player == null) {
            return;
        }
        applyReinforcement(player, Math.max(0.4f, multiplier));
    }

    private static int countFullGems(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof FullManaCarvedGemItem gemItem && gemItem.getType() == GemType.TOPAZ) {
                count += stack.getCount();
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof FullManaCarvedGemItem gemItem && gemItem.getType() == GemType.TOPAZ) {
            count += offhand.getCount();
        }
        return count;
    }

    private static void applyReinforcement(Player player, float multiplier) {
        int duration = Math.round(1200 * multiplier);
        if (!(player instanceof LivingEntity living)) {
            return;
        }

        living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1));
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1));
        living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1));
        living.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, 1));

        Level level = player.level();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 1, 0.5, 0.1);
            serverLevel.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 1, 0.5, 0.1);
        }
    }
}
