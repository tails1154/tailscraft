package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** Decorative chain carried over the 1.12 iron-bars wire slot. */
public class BlockChain extends Block {
    private static final AxisAlignedBB BOX = new AxisAlignedBB(0.375D, 0.0D, 0.375D, 0.625D, 1.0D, 0.625D);
    public BlockChain() { super(Material.IRON); setHardness(5.0F); setCreativeTab(CreativeTabs.DECORATIONS); }
    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) { return BOX; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }
}
