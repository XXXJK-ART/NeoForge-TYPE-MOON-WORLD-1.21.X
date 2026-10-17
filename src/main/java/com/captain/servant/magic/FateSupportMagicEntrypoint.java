package com.captain.servant.magic;

import com.captain.servant.TypeMoonServant;
import com.captain.servant.voice.VoiceCommunicationService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import net.xxxjk.typemoonworld.api.MagicDefinitionData;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

public final class FateSupportMagicEntrypoint implements IMagicAddonEntrypoint {
    private static final ResourceLocation BASIC = ResourceLocation.fromNamespaceAndPath("typemoonworld", "basic");
    private static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "none");

    @Override
    public void registerMagics(IMagicRegistry registry) {
        registry.register(
                VoiceCommunicationService.MESSAGE_MAGIC_ID.toString(),
                context -> {
                    ServerPlayer player = context.asServerPlayer();
                    if (player == null) {
                        return MagicExecutionResult.NOT_HANDLED;
                    }
                    return VoiceCommunicationService.toggleMessageMagic(player)
                            ? MagicExecutionResult.SUCCESS
                            : MagicExecutionResult.FAILED;
                },
                providerId()
        );

        TypeMoonWorldApi.addon(TypeMoonServant.MOD_ID).magics().registerDefinition(new MagicDefinitionData(
                VoiceCommunicationService.MESSAGE_MAGIC_ID,
                "magic.fatesupport.message_magic.name",
                BASIC,
                NONE,
                0.0,
                0,
                false,
                true,
                false,
                false,
                false,
                0,
                0
        ));
    }

    @Override
    public String providerId() {
        return TypeMoonServant.MOD_ID;
    }
}
