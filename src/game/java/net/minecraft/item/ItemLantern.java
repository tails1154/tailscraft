package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;

/** Client-side item representation for lantern stacks sent by the 1.12 server. */
public class ItemLantern extends ItemBlock {
	public ItemLantern(Block block, String name) {
		super(block);
		setUnlocalizedName(name);
		setCreativeTab(CreativeTabs.DECORATIONS);
	}
}
