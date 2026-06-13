package com.quickshulker;

import com.quickshulker.network.ShulkerNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickShulker implements ModInitializer {
    public static final String MOD_ID = "quickshulker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ShulkerNetworking.register();
        LOGGER.info("QuickShulker initialized");
    }
}
