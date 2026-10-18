package com.levviata.lspecial;

import com.levviata.lspecial.entity.EntityGrappleChain;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Defers grapple-chain spawning until the end of the server tick so the entity tracker
 * is not modified while its tracked-entity map is being iterated.
 */
public final class GrappleChainSpawnHandler {
    private static final List<EntityGrappleChain> PENDING = new ArrayList<>();

    public static void queue(EntityGrappleChain chain) {
        if (chain != null && !chain.world.isRemote) {
            synchronized (PENDING) {
                PENDING.add(chain);
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        synchronized (PENDING) {
            Iterator<EntityGrappleChain> iterator = PENDING.iterator();
            while (iterator.hasNext()) {
                EntityGrappleChain chain = iterator.next();
                iterator.remove();

                if (chain.isDead || !(chain.world instanceof WorldServer)) {
                    continue;
                }

                WorldServer world = (WorldServer) chain.world;
                if (!world.isBlockLoaded(chain.getPosition())) {
                    continue;
                }

                world.spawnEntity(chain);
            }
        }
    }
}
