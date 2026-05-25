package com.vikingposter.registration;

import com.vikingposter.VikingPosterMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VikingPosterMod.MODID);

    public static final RegistryObject<CreativeModeTab> VIKING_TAB = TABS.register("viking_poster",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.vikingposter"))
            .icon(() -> ModItems.POSTER.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(ModItems.POSTER.get());
                output.accept(ModItems.SPONGE_ERASER.get());
            })
            .build());
}
