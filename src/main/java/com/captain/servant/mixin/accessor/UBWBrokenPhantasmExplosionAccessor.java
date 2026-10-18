package com.captain.servant.mixin.accessor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm.UBWBrokenPhantasmExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = UBWBrokenPhantasmExplosion.class, remap = false)
public interface UBWBrokenPhantasmExplosionAccessor {
    @Invoker("spawnShellEffects")
    static void fatesupport$spawnShellEffects(ServerLevel level, Vec3 center, double radius) {
        throw new AssertionError();
    }
}
