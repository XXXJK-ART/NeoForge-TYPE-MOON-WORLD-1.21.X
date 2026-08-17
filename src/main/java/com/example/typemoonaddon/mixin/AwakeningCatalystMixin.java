package com.example.typemoonaddon.mixin;

import com.example.typemoonaddon.item.AwakeningCatalystItems;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Makes the upstream X-key awakening scan recognize the addon's explicit catalysts. */
@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.network.Lose_health_regain_mana_Message", remap = false)
public abstract class AwakeningCatalystMixin {
    @Redirect(
        method = "pressAction",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/DefaultedRegistry;getKey(Ljava/lang/Object;)Lnet/minecraft/resources/ResourceLocation;",
            ordinal = 0
        ),
        remap = false
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ResourceLocation typemoonaddon$recognizeAwakeningCatalyst(
        DefaultedRegistry registry,
        Object value
    ) {
        ResourceLocation original = registry.getKey(value);
        if (value instanceof Item item && AwakeningCatalystItems.isCatalyst(item)) {
            return ResourceLocation.fromNamespaceAndPath("typemoonworld", original.getPath());
        }
        return original;
    }
}
