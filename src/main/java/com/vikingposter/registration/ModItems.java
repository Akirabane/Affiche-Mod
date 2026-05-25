package com.vikingposter.registration;

import com.vikingposter.VikingPosterMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, VikingPosterMod.MODID);

    public static final RegistryObject<Item> POSTER = ITEMS.register("poster",
        () -> new BlockItem(ModBlocks.POSTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> SPONGE_ERASER = ITEMS.register("sponge_eraser",
        com.vikingposter.items.SpongeEraserItem::new);
}
