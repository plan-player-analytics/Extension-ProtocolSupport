/*
    Copyright(c) 2019 Risto Lahtela (Rsl1122)

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

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.annotation.StringProvider;
import com.djrapitops.plan.extension.annotation.TableProvider;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import protocolsupport.api.ProtocolVersion;

import java.util.UUID;

/**
 * ProtocolSupport DataExtension.
 *
 * @author Rsl1122
 */
@PluginInfo(name = "ProtocolSupport", iconName = "gamepad", iconFamily = Family.SOLID, color = Color.CYAN)
public class ProtocolSupportExtension implements DataExtension {

    private final ProtocolSupportStorage storage;

    public ProtocolSupportExtension() {
        storage = new ProtocolSupportStorage();

        if (viaVersionIsNotInstalled()) {
            new ProtocolSupportVersionListener(storage).register();
        }
    }

    private boolean viaVersionIsNotInstalled() {
        try {
            Class.forName("us.myles.ViaVersion.api.ViaAPI");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    @Override
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{
                CallEvents.PLAYER_JOIN,
                CallEvents.PLAYER_LEAVE,
                CallEvents.SERVER_PERIODICAL
        };
    }

    @StringProvider(
            text = "Last Join Version",
            description = "Version used last time the player joined",
            iconName = "signal",
            iconColor = Color.CYAN,
            showInPlayerTable = true
    )
    public String protocolVersion(UUID playerUUID) {
        return getProtocolVersionString(storage.getProtocolVersion(playerUUID));
    }

    private String getProtocolVersionString(int number) {
        if (number == -1) {
            return "Not Yet Known";
        }
        ProtocolVersion[] versions = ProtocolVersion.getAllSupported();
        for (ProtocolVersion version : versions) {
            if (version.getId() == number) {
                String name = version.getName();
                if (name == null) {
                    continue; // Unknown name for the version
                }
                return name;
            }
        }
        return "Unknown (" + number + ')';
    }

    @TableProvider(tableColor = Color.CYAN)
    public Table protocolTable() {
        Table.Factory table = Table.builder()
                .columnOne("Version", Icon.called("signal").build())
                .columnTwo("Users", Icon.called("users").build());

        storage.getProtocolVersionCounts().entrySet()
                .stream()
                .sorted((one, two) -> Integer.compare(two.getValue(), one.getValue()))
                .forEach(entry -> table.addRow(getProtocolVersionString(entry.getKey()), entry.getValue()));

        return table.build();
    }
}