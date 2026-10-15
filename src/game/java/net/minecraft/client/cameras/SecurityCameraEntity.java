package net.minecraft.client.cameras;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;

/** Non-rendered client-only entity used solely as a fixed render viewpoint. */
final class SecurityCameraEntity extends Entity {
    SecurityCameraEntity(World world) { super(world); noClip = true; width = 0.1F; height = 0.1F; }
    protected void entityInit() { }
    protected void readEntityFromNBT(NBTTagCompound compound) { }
    protected void writeEntityToNBT(NBTTagCompound compound) { }
    public boolean isInRangeToRenderDist(double distance) { return false; }
}
