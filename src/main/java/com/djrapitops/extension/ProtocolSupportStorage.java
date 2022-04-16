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

import com.djrapitops.plan.extension.NotReadyException;
import com.djrapitops.plan.query.QueryService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProtocolSupportStorage {

    private final QueryService queryService;

    public ProtocolSupportStorage() {
        queryService = QueryService.getInstance();
        createTable();
        queryService.subscribeDataClearEvent(this::recreateTable);
        queryService.subscribeToPlayerRemoveEvent(this::removePlayer);
    }

    private void createTable() {
        String dbType = queryService.getDBType();
        boolean sqlite = dbType.equalsIgnoreCase("SQLITE");

        String sql = "CREATE TABLE IF NOT EXISTS plan_version_protocol (" +
                "id int " + (sqlite ? "PRIMARY KEY" : "NOT NULL AUTO_INCREMENT") + ',' +
                "uuid varchar(36) NOT NULL UNIQUE," +
                "protocol_version int NOT NULL" +
                (sqlite ? "" : ",PRIMARY KEY (id)") +
                ')';

        queryService.execute(sql, PreparedStatement::execute);
    }

    private void dropTable() {
        queryService.execute("DROP TABLE IF EXISTS plan_version_protocol", PreparedStatement::execute);
    }

    private void recreateTable() {
        dropTable();
        createTable();
    }

    private void removePlayer(UUID playerUUID) {
        queryService.execute(
                "DELETE FROM plan_version_protocol WHERE uuid=?",
                statement -> {
                    statement.setString(1, playerUUID.toString());
                    statement.execute();
                }
        );
    }

    public void storeProtocolVersion(UUID uuid, int version) throws ExecutionException {
        String update = "UPDATE plan_version_protocol SET protocol_version=? WHERE uuid=?";
        String insert = "INSERT INTO plan_version_protocol (protocol_version, uuid) VALUES (?, ?)";

        AtomicBoolean updated = new AtomicBoolean(false);
        try {
            queryService.execute(update, statement -> {
                statement.setInt(1, version);
                statement.setString(2, uuid.toString());
                updated.set(statement.executeUpdate() > 0);
            }).get(); // Wait
            if (!updated.get()) {
                queryService.execute(insert, statement -> {
                    statement.setInt(1, version);
                    statement.setString(2, uuid.toString());
                    statement.execute();
                });
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getProtocolVersion(UUID uuid) {
        String sql = "SELECT protocol_version FROM plan_version_protocol WHERE uuid=?";

        return queryService.query(sql, statement -> {
            statement.setString(1, uuid.toString());
            try (ResultSet set = statement.executeQuery()) {
                return set.next() ? set.getInt("protocol_version") : -1;
            }
        });
    }

    public Map<Integer, Integer> getProtocolVersionCounts() {
        UUID serverUUID = queryService.getServerUUID()
                .orElseThrow(NotReadyException::new);
        final String sql = "SELECT protocol_version, COUNT(1) as count" +
                " FROM plan_version_protocol" +
                " INNER JOIN plan_users on plan_version_protocol.uuid=plan_users.uuid" +
                " INNER JOIN plan_user_info on plan_user_info.user_id=plan_users.id" +
                " WHERE plan_user_info.server_id=(SELECT id FROM plan_servers WHERE uuid=?)" +
                " GROUP BY protocol_version";
        return queryService.query(sql, statement -> {
            statement.setString(1, serverUUID.toString());
            try (ResultSet set = statement.executeQuery()) {
                Map<Integer, Integer> versions = new HashMap<>();
                while (set.next()) {
                    versions.put(set.getInt("protocol_version"), set.getInt("count"));
                }
                return versions;
            }
        });
    }
}
