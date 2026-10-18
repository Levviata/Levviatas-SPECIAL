package com.levviata.lspecial.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;

/**
 * Server-spawned grapple-chain visual; synchronized endpoint IDs make it visible to all tracking clients.
 */
public class EntityGrappleChain extends Entity {
    private static final DataParameter<Integer> OWNER_ID =
            EntityDataManager.createKey(EntityGrappleChain.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> TARGET_ID =
            EntityDataManager.createKey(EntityGrappleChain.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LIFE_TICKS =
            EntityDataManager.createKey(EntityGrappleChain.class, DataSerializers.VARINT);

    public EntityGrappleChain(World world) {
        super(world);
        setSize(0.05F, 0.05F);
        isImmuneToFire = true;
    }

    public void setEndpoints(EntityLivingBase owner, EntityLivingBase target, int lifeTicks) {
        dataManager.set(OWNER_ID, owner.getEntityId());
        dataManager.set(TARGET_ID, target.getEntityId());
        dataManager.set(LIFE_TICKS, Math.max(1, lifeTicks));
        setPosition(owner.posX, owner.posY + owner.getEyeHeight(), owner.posZ);
    }

    public int getOwnerId() {
        return dataManager.get(OWNER_ID);
    }

    public int getTargetId() {
        return dataManager.get(TARGET_ID);
    }

    @Override
    protected void entityInit() {
        dataManager.register(OWNER_ID, -1);
        dataManager.register(TARGET_ID, -1);
        dataManager.register(LIFE_TICKS, 0);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote && !isDead) {
            int life = dataManager.get(LIFE_TICKS) - 1;
            if (life <= 0 || world.getEntityByID(getOwnerId()) == null
                    || world.getEntityByID(getTargetId()) == null) {
                setDead();
            } else {
                dataManager.set(LIFE_TICKS, life);
            }
        }
        motionX = motionY = motionZ = 0.0D;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        dataManager.set(OWNER_ID, compound.getInteger("OwnerId"));
        dataManager.set(TARGET_ID, compound.getInteger("TargetId"));
        dataManager.set(LIFE_TICKS, compound.getInteger("LifeTicks"));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setInteger("OwnerId", getOwnerId());
        compound.setInteger("TargetId", getTargetId());
        compound.setInteger("LifeTicks", dataManager.get(LIFE_TICKS));
    }
}
