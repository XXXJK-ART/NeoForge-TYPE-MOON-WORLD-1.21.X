package net.xxxjk.typemoonworld.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public record NpcMagicContext(LivingEntity caster, LivingEntity target, Level level, String magicId, double proficiency, long gameTick) {
}
