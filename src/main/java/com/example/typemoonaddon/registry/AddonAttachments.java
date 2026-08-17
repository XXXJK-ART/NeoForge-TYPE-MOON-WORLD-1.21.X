package com.example.typemoonaddon.registry;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.data.CursedArmorViewData;
import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.data.PollutionData;
import com.example.typemoonaddon.data.SpiritualDamageData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class AddonAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TypeMoonAddon.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ImaginarySpaceData>> IMAGINARY_SPACE =
            ATTACHMENTS.register("imaginary_space", () -> AttachmentType.serializable(ImaginarySpaceData::new)
                    .copyOnDeath()
                    .sync((holder, player) -> holder == player, ImaginarySpaceData.STREAM_CODEC)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PollutionData>> POLLUTION =
            ATTACHMENTS.register("pollution", () -> AttachmentType.serializable(PollutionData::new)
                    .copyOnDeath()
                    .sync((holder, player) -> true, PollutionData.STREAM_CODEC)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CursedArmorViewData>> CURSED_ARMOR_VIEW =
            ATTACHMENTS.register("cursed_armor_view", () -> AttachmentType.builder(CursedArmorViewData::new)
                    .sync((holder, player) -> true, CursedArmorViewData.STREAM_CODEC)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SpiritualDamageData>> SPIRITUAL_DAMAGE =
            ATTACHMENTS.register("spiritual_damage", () -> AttachmentType.serializable(SpiritualDamageData::new)
                    .copyOnDeath()
                    .sync((holder, player) -> true, SpiritualDamageData.STREAM_CODEC)
                    .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }

    private AddonAttachments() {
    }
}
