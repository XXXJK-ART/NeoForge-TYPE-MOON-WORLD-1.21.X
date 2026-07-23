package net.xxxjk.typemoonworld.api;

import net.minecraft.server.level.ServerPlayer;

public record MasterProfileContext(
   ServerPlayer player,
   MasterProfileData profile,
   ManaAccess mana,
   MagicKnowledge magicKnowledge,
   MasterAccess master
) { }
