package org.vivecraft.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class XplatImpl {

    public static boolean isModLoaded(String name) {
        return FabricLoader.getInstance().isModLoaded(name);
    }

    public static Path getConfigPath(String fileName) {
        return FabricLoader.getInstance().getConfigDir().resolve(fileName);
    }
    public static boolean isDedicatedServer() {
       return FabricLoader.getInstance().getEnvironmentType().equals(EnvType.SERVER);
    }

    public static String getModloader() {
        return "fabric";
    }

    public static String getModVersion() {
        return FabricLoader.getInstance().getModContainer("vivecraft").get().getMetadata().getVersion().getFriendlyString();
    }
}
