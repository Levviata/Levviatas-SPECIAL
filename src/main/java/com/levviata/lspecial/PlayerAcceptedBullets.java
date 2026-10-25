package com.levviata.lspecial;

import com.hbm.items.weapon.sedna.BulletConfig;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;

/**
 * Per-player view of an NTM magazine's acceptedBullets list.<br>
 * <br>
 * NTM keeps one acceptedBullets list per magazine definition, shared by every copy of the gun on the
 * server. Adding alternate calibers to it would unlock them for everybody, so the native list stays
 * untouched (base calibers only) and this view appends the alternates only while the gun operation of a
 * player who passes the Intelligence gate is running. Outside a gun operation there is no player in
 * {@link SPECIALPlayerContext}, so the view shows the base calibers only (strict fallback).<br>
 * <br>
 * Writes (NTM's addConfigs, weapon mods) go to the base list, never to the alternates.
 */
public final class PlayerAcceptedBullets extends AbstractList<BulletConfig> {

    private final List<BulletConfig> base;

    public PlayerAcceptedBullets(List<BulletConfig> base) {
        this.base = base;
    }

    /**
     * Returns NTM's native list behind a view, or the list itself when it isn't a view.
     */
    public static List<BulletConfig> baseOf(List<BulletConfig> list) {
        if (list instanceof PlayerAcceptedBullets) {
            return ((PlayerAcceptedBullets) list).base;
        }
        return list;
    }

    private static List<BulletConfig> extras() {
        return CaliberAccess.getAllowedAlternates(SPECIALPlayerContext.getPlayer(), SPECIALPlayerContext.getStack());
    }

    @Override
    public int size() {
        int count = base.size();
        for (BulletConfig config : extras()) {
            if (config != null && !base.contains(config)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean isEmpty() {
        return base.isEmpty() && size() == 0;
    }

    @Override
    public BulletConfig get(int index) {
        int baseSize = base.size();
        if (index < baseSize) {
            return base.get(index);
        }
        int remaining = index - baseSize;
        for (BulletConfig config : extras()) {
            if (config == null || base.contains(config)) {
                continue;
            }
            if (remaining == 0) {
                return config;
            }
            remaining--;
        }
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
    }

    @Override
    public boolean contains(Object object) {
        return base.contains(object) || extras().contains(object);
    }

    @Override
    public void add(int index, BulletConfig element) {
        base.add(Math.min(index, base.size()), element);
        modCount++;
    }

    @Override
    public boolean addAll(Collection<? extends BulletConfig> collection) {
        // copying from another view must not bake its current alternates into this list
        if (collection instanceof PlayerAcceptedBullets) {
            return base.addAll(((PlayerAcceptedBullets) collection).base);
        }
        return base.addAll(collection);
    }

    @Override
    public BulletConfig set(int index, BulletConfig element) {
        if (index >= base.size()) {
            throw new UnsupportedOperationException("Alternate calibers can't be replaced");
        }
        return base.set(index, element);
    }

    @Override
    public BulletConfig remove(int index) {
        if (index >= base.size()) {
            throw new UnsupportedOperationException("Alternate calibers can't be removed");
        }
        modCount++;
        return base.remove(index);
    }
}
