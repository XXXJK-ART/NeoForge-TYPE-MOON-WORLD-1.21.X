package com.example.typemoonaddon.servant;

import com.example.typemoonaddon.entity.GillesDeRaisEntity;
import com.example.typemoonaddon.registry.AddonEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantLifecycleContext;

public final class GillesDeRaisAddonEntrypoint implements IServantAddonEntrypoint {
    @Override
    public String providerId() {
        return "typemoonworld";
    }

    @Override
    public void registerServants(IServantAddonRegistry registry) {
        registry.registerEntityFactory("typemoonworld:" + GillesDeRaisEntity.SERVANT_KEY,
                level -> AddonEntities.GILLES_DE_RAIS_CASTER.get().create(level), providerId());
        registry.registerLifecycleHandler("gilles_de_rais_caster_tick", this::tickGilles, providerId());
        registry.registerCombatAction(GillesDeRaisCombatHelper.ACTION_SUMMON_SMALL, GillesDeRaisCombatHelper::executeCombatAction, providerId());
        registry.registerCombatAction(GillesDeRaisCombatHelper.ACTION_SUMMON_LARGE, GillesDeRaisCombatHelper::executeCombatAction, providerId());
        registry.registerCombatAction(GillesDeRaisCombatHelper.ACTION_ABYSSAL_GAZE, GillesDeRaisCombatHelper::executeCombatAction, providerId());
        registry.registerCombatAction(GillesDeRaisCombatHelper.ACTION_LIFE_ABSORB, GillesDeRaisCombatHelper::executeCombatAction, providerId());
        registry.registerCombatAction(GillesDeRaisCombatHelper.ACTION_SUMMON_HUGE, GillesDeRaisCombatHelper::executeCombatAction, providerId());
    }

    private ServantExecutionResult tickGilles(ServantLifecycleContext context) {
        if (context.entity() instanceof GillesDeRaisEntity gilles) {
            GillesDeRaisCombatHelper.tick(gilles);
        }
        return ServantExecutionResult.NOT_HANDLED;
    }
}
