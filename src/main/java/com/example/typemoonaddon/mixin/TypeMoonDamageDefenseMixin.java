package com.example.typemoonaddon.mixin;

import com.example.typemoonaddon.shadowlogic.magic.ShadowBindingService;
import com.example.typemoonaddon.shadowlogic.magic.ShadowArtService;
import com.example.typemoonaddon.magic.MagicOutputService;
import com.example.typemoonaddon.magic.SakuraSpiritualDamageService;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses a bound servant's defensive reactions to attacks from the binding source's allies. */
@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.event.CommonEvents", remap = false)
public abstract class TypeMoonDamageDefenseMixin {
    @Redirect(
        method = "onLivingDeath",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/nbt/CompoundTag;getBoolean(Ljava/lang/String;)Z"
        ),
        remap = false
    )
    private static boolean typemoonaddon$skipCollapseGodHand(
        CompoundTag tag,
        String key,
        LivingDeathEvent event
    ) {
        if ("GodHandActive".equals(key) && SakuraSpiritualDamageService.isCollapse(event.getSource())) {
            return false;
        }
        return tag.getBoolean(key);
    }

    @Inject(method = "handleServantDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void typemoonaddon$skipFriendlyBindingServantDefense(
        ServantEntity servant,
        LivingIncomingDamageEvent event,
        CallbackInfo callback
    ) {
        if (SakuraSpiritualDamageService.isCollapse(event.getSource())
            || MagicOutputService.isDamage(event.getSource())
            || ShadowArtService.isUnavoidableInBlackMud(servant, event.getSource())
            || ShadowBindingService.isFriendlyDamage(servant, event.getSource())) {
            callback.cancel();
        }
    }
}
