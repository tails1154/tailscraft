package net.minecraft.item;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.NonNullList;
public class ItemChain extends ItemBlock {
    public ItemChain(Block block) { super(block); setUnlocalizedName("chain"); setCreativeTab(CreativeTabs.DECORATIONS); }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.func_194125_a(tab)) {
            items.add(new ItemStack(this));
        }
    }
}
