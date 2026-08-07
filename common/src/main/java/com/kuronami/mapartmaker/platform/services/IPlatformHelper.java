package com.kuronami.mapartmaker.platform.services;

public interface IPlatformHelper {

    /**
     * BlockEntityType を生成する（vanilla builder は common から触れないため loader に委譲）。
     * NeoForge は {@code BlockEntityType.Builder}、Fabric は {@code FabricBlockEntityTypeBuilder}。
     */
    <T extends net.minecraft.world.level.block.entity.BlockEntity>
            net.minecraft.world.level.block.entity.BlockEntityType<T> createBlockEntityType(
                    ModBlockEntitySupplier<T> supplier, net.minecraft.world.level.block.Block block);

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {

        return isDevelopmentEnvironment() ? "development" : "production";
    }
}