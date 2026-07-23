package net.xxxjk.typemoonworld.api;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Stable access to master/servant contracts and command-spell state. */
public interface MasterAccess {
   boolean active();
   int commandSpells();
   String commandSpellStyle();
   UUID boundServant();
   boolean activate();
   boolean release();
   boolean bind(ServerPlayer servant);
}
