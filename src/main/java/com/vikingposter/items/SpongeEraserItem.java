package com.vikingposter.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class SpongeEraserItem extends Item {
    public SpongeEraserItem() {
        super(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16));
    }
}
