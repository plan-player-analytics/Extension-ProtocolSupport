/*
    Copyright(c) 2019 Risto Lahtela (AuroraLS3)

    The MIT License(MIT)

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files(the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions :
    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/
package com.djrapitops.extension;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import protocolsupport.api.ProtocolSupportAPI;
import protocolsupport.api.ProtocolVersion;

import java.util.concurrent.ExecutionException;

public class ProtocolSupportVersionListener implements Listener {

    private final ProtocolSupportStorage storage;

    private final Plugin plugin;

    ProtocolSupportVersionListener(
            ProtocolSupportStorage storage
    ) {
        this.storage = storage;
        plugin = Bukkit.getPluginManager().getPlugin("Plan");
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        try {
            ProtocolVersion protocolVersion = ProtocolSupportAPI.getProtocolVersion(player);
            int playerVersion = protocolVersion.getId();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> storeVersion(player, playerVersion));
        } catch (IllegalStateException accessBeforeDetect) {
            // Ignore
        }
    }

    private void storeVersion(Player player, int playerVersion) {
        try {
            storage.storeProtocolVersion(player.getUniqueId(), playerVersion);
        } catch (ExecutionException ignored) {
            // Ignore
        }
    }
}
