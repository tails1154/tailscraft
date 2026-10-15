package net.minecraft.item;

import java.util.List;
import javax.annotation.Nullable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.netty.buffer.Unpooled;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/** Grappling spear backed by the vanilla fishing-hook entity. */
public class ItemSpear extends ItemSword {
	private static final String CHANNEL = "TC|Grapple";
	private static final int MAX_RANGE = 32;
	private final float pullStrength;
	private final String materialName;

	public ItemSpear(Item.ToolMaterial baseMaterial, int durability, float damage, float attackSpeed,
			float pullStrength, int unusedActivationDelay, String materialName) {
		super(baseMaterial);
		this.setMaxDamage(durability);
		this.pullStrength = pullStrength;
		this.materialName = materialName;
		this.setUnlocalizedName(materialName + "_spear");
		this.setCreativeTab(net.minecraft.creativetab.CreativeTabs.TOOLS);
		this.addPropertyOverride(new ResourceLocation("cast"), (stack, world, entity) -> {
			if (!(entity instanceof EntityPlayer)) return 0.0F;
			EntityPlayer player = (EntityPlayer) entity;
			return player.fishEntity != null && (player.getHeldItemMainhand() == stack
					|| player.getHeldItemOffhand() == stack) ? 1.0F : 0.0F;
		});
	}

	public float getPullStrength() { return this.pullStrength; }

	public String getSpearMaterialName() { return this.materialName; }

	@Override
	public boolean isFull3D() { return true; }

	@Override
	public boolean shouldRotateAroundWhenRendering() { return true; }

	@Override
	public Multimap<String, net.minecraft.entity.ai.attributes.AttributeModifier> getItemAttributeModifiers(
			EntityEquipmentSlot slot) {
		Multimap<String, net.minecraft.entity.ai.attributes.AttributeModifier> result = HashMultimap.create();
		if (slot == EntityEquipmentSlot.MAINHAND) {
			result.put(SharedMonsterAttributes.ATTACK_DAMAGE.getAttributeUnlocalizedName(),
					new net.minecraft.entity.ai.attributes.AttributeModifier(ATTACK_DAMAGE_MODIFIER,
							"Grappling spear fallback", 1.0D, 0));
		}
		return result;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (player.fishEntity != null) {
			int damage = player.fishEntity.handleHookRetraction();
			stack.damageItem(damage, player);
			player.swingArm(hand);
			world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_BOBBER_RETRIEVE,
					SoundCategory.NEUTRAL, 1.0F, 0.8F);
			return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
		}

		world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_BOBBER_THROW,
				SoundCategory.NEUTRAL, 0.7F, 1.0F);
		if (world.isRemote && player instanceof EntityPlayerSP) {
			sendGrapplePacket((EntityPlayerSP) player, world, player);
		} else if (!world.isRemote) {
			world.spawnEntityInWorld(new EntityFishHook(world, player));
		}
		player.swingArm(hand);
		player.addStat(StatList.getObjectUseStats(this));
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
	}

	private void sendGrapplePacket(EntityPlayerSP player, World world, EntityPlayer owner) {
		Entity target = findTarget(owner, world, MAX_RANGE);
		Vec3d eyes = owner.getPositionEyes(1.0F);
		Vec3d look = owner.getLook(1.0F);
		RayTraceResult block = world.rayTraceBlocks(eyes,
				eyes.addVector(look.xCoord * MAX_RANGE, look.yCoord * MAX_RANGE, look.zCoord * MAX_RANGE), false, true,
					false);
		PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
		buffer.writeByte(1);
		buffer.writeInt(target == null ? -1 : target.getEntityId());
		if (block != null && block.typeOfHit == RayTraceResult.Type.BLOCK) {
			BlockPos pos = block.getBlockPos();
			buffer.writeInt(pos.getX());
			buffer.writeInt(pos.getY());
			buffer.writeInt(pos.getZ());
		} else {
			buffer.writeInt(Integer.MIN_VALUE);
			buffer.writeInt(Integer.MIN_VALUE);
			buffer.writeInt(Integer.MIN_VALUE);
		}
		buffer.writeInt(materialIndex());
		player.connection.sendPacket(new CPacketCustomPayload(CHANNEL, buffer));
	}

	/** Handles TC|Grapple on the integrated server. */
	public static void handleGrapplePacket(EntityPlayer player, PacketBuffer buffer) {
		if (!(player.getHeldItemMainhand().getItem() instanceof ItemSpear)
				&& !(player.getHeldItemOffhand().getItem() instanceof ItemSpear)) return;
		if (buffer.readableBytes() < 17 || buffer.readByte() != 1) return;
		Entity target = player.world.getEntityByID(buffer.readInt());
		int blockX = buffer.readInt();
		int blockY = buffer.readInt();
		int blockZ = buffer.readInt();
		buffer.readInt();
		if (target != null && target != player && target.canBeCollidedWith()
				&& player.getDistanceSqToEntity(target) <= MAX_RANGE * MAX_RANGE) {
			pull(player, target.posX, target.getEntityBoundingBox().minY + target.height * 0.5D, target.posZ);
			return;
		}
		if (blockX != Integer.MIN_VALUE && player.getDistanceSq(blockX + 0.5D, blockY + 0.5D, blockZ + 0.5D)
				<= MAX_RANGE * MAX_RANGE) {
			pull(player, blockX + 0.5D, blockY + 0.5D, blockZ + 0.5D);
		}
	}

	public static void pull(EntityPlayer player, double x, double y, double z) {
		Vec3d delta = new Vec3d(x - player.posX, y - (player.posY + player.getEyeHeight() * 0.5D), z - player.posZ);
		double distance = delta.lengthVector();
		if (distance < 1.0D || distance > MAX_RANGE + 1.0D) return;
		double strength = Math.min(0.85D, 0.22D + distance * 0.018D);
		player.addVelocity(delta.xCoord / distance * strength, delta.yCoord / distance * strength + 0.04D,
				delta.zCoord / distance * strength);
		player.velocityChanged = true;
	}

	private static Entity findTarget(EntityPlayer player, World world, double reach) {
		Vec3d eyes = player.getPositionEyes(1.0F);
		Vec3d look = player.getLook(1.0F);
		AxisAlignedBB search = player.getEntityBoundingBox()
				.addCoord(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach)
				.expand(1.0D, 1.0D, 1.0D);
		Entity best = null;
		double bestDistance = reach;
		for (Entity candidate : world.getEntitiesWithinAABBExcludingEntity(player, search)) {
			if (!candidate.canBeCollidedWith()) continue;
			RayTraceResult hit = candidate.getEntityBoundingBox().expand(0.3D, 0.3D, 0.3D)
					.calculateIntercept(eyes, eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach));
			if (hit != null) {
				double distance = eyes.distanceTo(hit.hitVec);
				if (distance < bestDistance) {
					best = candidate;
					bestDistance = distance;
				}
			}
		}
		return best;
	}

	private int materialIndex() {
		if ("wooden".equals(this.materialName)) return 0;
		if ("stone".equals(this.materialName)) return 1;
		if ("copper".equals(this.materialName)) return 2;
		if ("golden".equals(this.materialName)) return 4;
		if ("diamond".equals(this.materialName)) return 5;
		if ("netherite".equals(this.materialName)) return 6;
		return 3;
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		tooltip.add(I18n.translateToLocal("item.tailscraft.spear.grapple"));
	}
}
