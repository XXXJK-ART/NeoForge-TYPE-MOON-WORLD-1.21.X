package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public class CommonEvents {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;

        if (event.getEntity() instanceof Monster monster) {
            // Add goal to target Ryougi Shiki
            // Priority 2 or 3, usually after Player (which is typically 1 or 2)
            // But we want them to actively target her too.
            try {
                monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, RyougiShikiEntity.class, true));
            } catch (Exception e) {
                // Ignore if AI is locked or incompatible (e.g. Slimes might not have standard goal selector)
            }
        }
    }
}
