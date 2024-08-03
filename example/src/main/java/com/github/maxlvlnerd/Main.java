package com.github.maxlvlnerd;

import com.github.maxlvlnerd.simplegen.OceanGen;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.SimpleCommand;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.InstanceContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        var minecraftServer = MinecraftServer.init();
        var instanceManager = MinecraftServer.getInstanceManager();
        Supplier<InstanceContainer> instance = () -> {
            var instanceContainer = instanceManager.createInstanceContainer();
            // has really bad performance
            // instanceContainer.setChunkSupplier(LightingChunk::new);
            instanceContainer.setGenerator(new OceanGen(123));
            return instanceContainer;
        };

        var globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            var player = event.getPlayer();
            event.setSpawningInstance(instance.get());
            player.setRespawnPoint(new Pos(0, 120, 0));
        });
        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            event.getPlayer().setGameMode(GameMode.CREATIVE);
            event.getPlayer().setFlyingSpeed(1f);
        });
        MinecraftServer.getCommandManager().register(new SimpleCommand("reload") {
            @Override
            public boolean process(@NotNull CommandSender sender, @NotNull String command, @NotNull String[] args) {
                var last = sender.asPlayer().getInstance();
                sender.asPlayer().setInstance(instance.get()).thenRunAsync(() -> instanceManager.unregisterInstance(last));
                return true;
            }

            @Override
            public boolean hasAccess(@NotNull CommandSender sender, @Nullable String commandString) {
                return true;
            }
        });

        minecraftServer.start("0.0.0.0", 25565);
    }
}
