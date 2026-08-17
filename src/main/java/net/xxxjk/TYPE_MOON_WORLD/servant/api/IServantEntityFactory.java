package net.xxxjk.TYPE_MOON_WORLD.servant.api;

import net.minecraft.server.level.ServerLevel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

@FunctionalInterface
public interface IServantEntityFactory {
   ServantEntity create(ServerLevel level);
}
