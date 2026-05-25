package com.vikingposter.registration;

import com.vikingposter.VikingPosterMod;
import com.vikingposter.blockentity.PosterBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, VikingPosterMod.MODID);

    public static final RegistryObject<BlockEntityType<PosterBlockEntity>> POSTER =
        BLOCK_ENTITIES.register("poster", () ->
            BlockEntityType.Builder.of(PosterBlockEntity::new, ModBlocks.POSTER.get()).build(null));
}
