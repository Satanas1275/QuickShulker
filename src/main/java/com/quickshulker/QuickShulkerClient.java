package com.quickshulker;

import com.quickshulker.network.ShulkerNetworking;
import net.fabricmc.api.ClientModInitializer;

public class QuickShulkerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ShulkerNetworking.registerClient();
    }
}
