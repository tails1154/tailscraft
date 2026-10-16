package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.NonNullList;

/** Client-side item representation for lantern stacks sent by the 1.12 server. */
public class ItemLantern extends ItemBlock {
	public ItemLantern(Block block, String name) {
		super(block);
		setUnlocalizedName(name);
		setCreativeTab(CreativeTabs.DECORATIONS);
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if (this.func_194125_a(tab)) {
			items.add(new ItemStack(this));
		}
	}
}
