package com.levviata.lspecial;

/** exposes the native receiver refire flags so temporary perks can restore their exact original values. */
public interface ReceiverStateAccess {
    boolean lspecial$getNativeRefireOnHold();
    boolean lspecial$getNativeRefireAfterDry();
}