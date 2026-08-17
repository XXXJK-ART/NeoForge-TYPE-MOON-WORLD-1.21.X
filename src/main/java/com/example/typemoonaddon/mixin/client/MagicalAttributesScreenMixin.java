package com.example.typemoonaddon.mixin.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.registry.AddonAttachments;
import net.minecraft.client.Minecraft;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.client.gui.Magical_attributes_Screen", remap = false)
public abstract class MagicalAttributesScreenMixin {
    @Unique private static final String SAKURA_STORAGE_ID = "imaginary_absorption";
    @Unique private static final String SAKURA_ABSORPTION_ID = "imaginary_absorption_evolved";
    @Unique private static final String SAKURA_MATERIALIZATION_ID = "shadow_materialization";
    @Unique private static final String SAKURA_BLACK_MUD_CONTROL_ID = "black_mud_control";
    @Unique private static final String SAKURA_SUMMON_BLACK_MUD_ID = "summon_black_mud";
    @Unique private static final String SAKURA_SHADOW_BINDING_ID = "shadow_binding";
    @Unique private static final String SAKURA_SHADOW_TRANSFER_ID = "shadow_transfer";
    @Unique private static final String SAKURA_HEROIC_SPIRIT_DEVOURER_ID = "heroic_spirit_devourer";
    @Unique private static final String SAKURA_SHADOW_ART_ID = "shadow_art";
    @Unique private static final String SAKURA_FORBIDDEN_MAGIC_ID = "forbidden_magic";

    @Shadow
    private void addMagic(String id, String nameKey, String categories, int color) {
        throw new AssertionError();
    }

    @Inject(method = "initMagicCatalog", at = @At("TAIL"))
    private void typemoonworld$addSakuraImaginaryBranch(CallbackInfo callback) {
        addMagic(SAKURA_STORAGE_ID, "key.typemoonworld.magic.imaginary_storage.short", "basic,imaginary", 0xFF7C4DA3);
        addMagic(SAKURA_ABSORPTION_ID, "key.typemoonworld.magic.imaginary_absorption.short", "basic,imaginary", 0xFF3A163F);
        addMagic(SAKURA_MATERIALIZATION_ID, "key.typemoonworld.magic.shadow_materialization.short", "basic,imaginary", 0xFFEEE8F6);
        addMagic(SAKURA_BLACK_MUD_CONTROL_ID, "key.typemoonworld.magic.black_mud_control.short", "basic,imaginary", 0xFF24122E);
        addMagic(SAKURA_SUMMON_BLACK_MUD_ID, "key.typemoonworld.magic.summon_black_mud.short", "basic,imaginary", 0xFF36104D);
        addMagic(SAKURA_SHADOW_BINDING_ID, "key.typemoonworld.magic.shadow_binding.short", "basic,imaginary", 0xFF7A1621);
        addMagic(SAKURA_SHADOW_TRANSFER_ID, "key.typemoonworld.magic.shadow_transfer.short", "basic,imaginary", 0xFF8B1A24);
        addMagic(SAKURA_HEROIC_SPIRIT_DEVOURER_ID, "key.typemoonworld.magic.heroic_spirit_devourer.short", "basic,imaginary", 0xFFB01722);
        addMagic(SAKURA_SHADOW_ART_ID, "key.typemoonworld.magic.shadow_art.short", "basic,imaginary", 0xFF5A1028);
        addMagic(SAKURA_FORBIDDEN_MAGIC_ID, "key.typemoonworld.magic.forbidden_magic.short", "basic,imaginary", 0xFFFF7FA6);
    }

    @Inject(method = "getMagicProficiency(Ljava/lang/String;)D", at = @At("HEAD"), cancellable = true)
    private void typemoonworld$showSakuraProficiency(String magicId, CallbackInfoReturnable<Double> callback) {
        if (!typemoonworld$isSakuraMagic(magicId) || Minecraft.getInstance().player == null) {
            return;
        }
        TypeMoonWorldModVariables.PlayerVariables vars = Minecraft.getInstance().player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        double proficiency = Math.max(
                vars.magic_proficiencies.getOrDefault(magicId, 0.0D),
                vars.magic_proficiencies.getOrDefault(TypeMoonAddon.MOD_ID + ":" + magicId, 0.0D)
        );
        callback.setReturnValue(proficiency);
    }

    @Unique
    private static boolean typemoonworld$isSakuraMagic(String magicId) {
        return SAKURA_STORAGE_ID.equals(magicId)
                || SAKURA_ABSORPTION_ID.equals(magicId)
                || SAKURA_MATERIALIZATION_ID.equals(magicId)
                || SAKURA_BLACK_MUD_CONTROL_ID.equals(magicId)
                || SAKURA_SUMMON_BLACK_MUD_ID.equals(magicId)
                || SAKURA_SHADOW_BINDING_ID.equals(magicId)
                || SAKURA_SHADOW_TRANSFER_ID.equals(magicId)
                || SAKURA_HEROIC_SPIRIT_DEVOURER_ID.equals(magicId)
                || SAKURA_SHADOW_ART_ID.equals(magicId)
                || SAKURA_FORBIDDEN_MAGIC_ID.equals(magicId);
    }
}
