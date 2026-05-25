package com.vikingposter;

import com.vikingposter.network.NetworkHandler;
import com.vikingposter.registration.ModBlockEntities;
import com.vikingposter.registration.ModBlocks;
import com.vikingposter.registration.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(VikingPosterMod.MODID)
public class VikingPosterMod {

    public static final String MODID = "vikingposter";

    public VikingPosterMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);

        bus.addListener(this::commonSetup);
        bus.addListener(ClientSetup::init);

        MinecraftForge.EVENT_BUS.register(new com.vikingposter.events.PosterEventHandler());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::init);
    }
}
