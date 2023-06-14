package com.qendolin.betterclouds.compat;

public class SodiumExtraCompat {
    public static final boolean IS_LOADED = false;// FabricLoader.getInstance().isModLoaded("sodium-extra");
//
    public static float getCloudsHeight() {
        return 100;//SodiumExtraClientMod.options().extraSettings.cloudHeight;
    }
}
