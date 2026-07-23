package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public interface DamageTypeRegistry {
   ResourceKey<DamageType> key(String id);
   DamageSource source(Level level, String id, Entity direct, Entity owner);
}
