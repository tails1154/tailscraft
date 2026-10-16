package net.minecraft.block;

import javax.annotation.Nullable;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Lantern block shared by the normal and soul variants. */
public class BlockLantern extends Block {
	public static final PropertyBool HANGING = PropertyBool.create("hanging");
	private static final AxisAlignedBB BOX = new AxisAlignedBB(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.4375D, 0.6875D);
	private static final AxisAlignedBB HANGING_BOX = new AxisAlignedBB(0.3125D, 0.0625D, 0.3125D, 0.6875D, 0.5D, 0.6875D);

	public BlockLantern() {
		super(Material.IRON);
		setDefaultState(blockState.getBaseState().withProperty(HANGING, Boolean.FALSE));
		setHardness(3.5F);
		setLightLevel(1.0F);
		setCreativeTab(CreativeTabs.DECORATIONS);
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return ((Boolean) state.getValue(HANGING)).booleanValue() ? HANGING_BOX : BOX;
	}

	@Override
	@Nullable
	public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) { return getBoundingBox(state, world, pos); }

	@Override
	public boolean isOpaqueCube(IBlockState state) { return false; }

	@Override
	public boolean isFullCube(IBlockState state) { return false; }

	@Override
	public boolean canPlaceBlockAt(World world, BlockPos pos) {
		return !world.getBlockState(pos).getMaterial().isLiquid();
	}

	@Override
	public IBlockState onBlockPlaced(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
			float hitZ, int meta, EntityLivingBase placer) {
		return getDefaultState().withProperty(HANGING, Boolean.valueOf((meta & 1) != 0));
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(HANGING, Boolean.valueOf((meta & 1) != 0));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return ((Boolean) state.getValue(HANGING)).booleanValue() ? 1 : 0;
	}

	@Override
	protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, new IProperty[] { HANGING }); }

	@Override
	public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }
}
