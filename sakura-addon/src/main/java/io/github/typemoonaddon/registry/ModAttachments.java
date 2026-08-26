package io.github.typemoonaddon.registry;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.data.ImaginarySpaceData;
import io.github.typemoonaddon.data.PollutionData;
import io.github.typemoonaddon.data.CursedArmorViewData;
import io.github.typemoonaddon.data.SpiritualDamageData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(
        NeoForgeRegistries.ATTACHMENT_TYPES,
        TypeMoonAddon.MOD_ID
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ImaginarySpaceData>> IMAGINARY_SPACE = ATTACHMENTS.register(
        "imaginary_space",
        () -> AttachmentType.serializable(ImaginarySpaceData::new)
            .copyOnDeath()
            .sync((holder, player) -> holder == player, ImaginarySpaceData.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PollutionData>> POLLUTION = ATTACHMENTS.register(
        "pollution",
        () -> AttachmentType.serializable(PollutionData::new)
            .copyOnDeath()
            .sync((holder, player) -> true, PollutionData.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CursedArmorViewData>> CURSED_ARMOR_VIEW = ATTACHMENTS.register(
        "cursed_armor_view",
        () -> AttachmentType.builder(CursedArmorViewData::new)
            .sync((holder, player) -> true, CursedArmorViewData.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SpiritualDamageData>> SPIRITUAL_DAMAGE = ATTACHMENTS.register(
        "spiritual_damage",
        () -> AttachmentType.serializable(SpiritualDamageData::new)
            .copyOnDeath()
            .sync((holder, player) -> true, SpiritualDamageData.STREAM_CODEC)
            .build()
    );

    private ModAttachments() {
    }
}
