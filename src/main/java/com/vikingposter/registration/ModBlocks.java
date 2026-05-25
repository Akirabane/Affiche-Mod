package com.vikingposter.registration;

import com.vikingposter.VikingPosterMod;
import com.vikingposter.blocks.PosterBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, VikingPosterMod.MODID);

    public static final RegistryObject<Block> POSTER = BLOCKS.register("poster", PosterBlock::new);

    public static void registerBlockItems() {
        ModItems.ITEMS.register("poster", () ->
            new BlockItem(POSTER.get(), new Item.Properties()));
    }
}
