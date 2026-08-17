package com.example.typemoonaddon.mixin;

import javax.annotation.Nullable;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity", remap = false)
public interface GilgameshGateProjectileAccessor {
    @Accessor("ownerUuid")
    @Nullable
    UUID typemoonaddon$getOwnerUuid();
}
