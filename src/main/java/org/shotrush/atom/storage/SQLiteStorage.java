package org.shotrush.atom.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.shotrush.atom.model.PlayerSkillData;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class SQLiteStorage implements Storage.Provider {
    
    private final Path databasePath;
    private final ExecutorService executor;
    private HikariDataSource dataSource;
    
    public SQLiteStorage(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath cannot be null");
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread thread = new Thread(r, "Atom-Storage");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl("jdbc:sqlite:" + databasePath.toAbsolutePath());
                config.setMaximumPoolSize(2);
                config.setConnectionTimeout(5000);
                config.setLeakDetectionThreshold(60000);
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                
                dataSource = new HikariDataSource(config);
                
                createTables();
            } catch (Exception e) {
                throw new Storage.Exception("Failed to initialize database", e);
            }
        }, executor);
    }
    
    private void createTables() throws SQLException {
        String createPlayersTable = """
            CREATE TABLE IF NOT EXISTS atom_players (
                player_id TEXT PRIMARY KEY,
                last_modified INTEGER NOT NULL
            )
        """;
        
        String createSkillsTable = """
            CREATE TABLE IF NOT EXISTS atom_skills (
                player_id TEXT NOT NULL,
                skill_id TEXT NOT NULL,
                intrinsic_xp INTEGER NOT NULL,
                PRIMARY KEY (player_id, skill_id),
                FOREIGN KEY (player_id) REFERENCES atom_players(player_id) ON DELETE CASCADE
            )
        """;
        
        String createIndexes = """
            CREATE INDEX IF NOT EXISTS idx_player_modified ON atom_players(last_modified);
            CREATE INDEX IF NOT EXISTS idx_skill_lookup ON atom_skills(player_id, skill_id);
        """;
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createPlayersTable);
            stmt.execute(createSkillsTable);
            stmt.execute(createIndexes);
        }
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (dataSource != null && !dataSource.isClosed()) {
                    dataSource.close();
                }
                executor.shutdown();
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (Exception e) {
                throw new Storage.Exception("Failed to shutdown database", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Optional<PlayerSkillData>> loadPlayerData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                String checkQuery = "SELECT last_modified FROM atom_players WHERE player_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
                    stmt.setString(1, playerId.toString());
                    ResultSet rs = stmt.executeQuery();
                    
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                }
                
                String skillQuery = "SELECT skill_id, intrinsic_xp FROM atom_skills WHERE player_id = ?";
                Map<String, Long> skills = new HashMap<>();
                
                try (PreparedStatement stmt = conn.prepareStatement(skillQuery)) {
                    stmt.setString(1, playerId.toString());
                    ResultSet rs = stmt.executeQuery();
                    
                    while (rs.next()) {
                        String skillId = rs.getString("skill_id");
                        long xp = rs.getLong("intrinsic_xp");
                        skills.put(skillId, xp);
                    }
                }
                
                return Optional.of(new PlayerSkillData(playerId, skills));
            } catch (SQLException e) {
                throw new Storage.Exception("Failed to load player data for " + playerId, e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> savePlayerData(PlayerSkillData playerData) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                
                try {
                    String upsertPlayer = "INSERT OR REPLACE INTO atom_players (player_id, last_modified) VALUES (?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(upsertPlayer)) {
                        stmt.setString(1, playerData.playerId().toString());
                        stmt.setLong(2, System.currentTimeMillis());
                        stmt.executeUpdate();
                    }
                    
                    String deleteSkills = "DELETE FROM atom_skills WHERE player_id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteSkills)) {
                        stmt.setString(1, playerData.playerId().toString());
                        stmt.executeUpdate();
                    }
                    
                    String insertSkill = "INSERT INTO atom_skills (player_id, skill_id, intrinsic_xp) VALUES (?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(insertSkill)) {
                        for (Map.Entry<String, Long> entry : playerData.getAllIntrinsicXp().entrySet()) {
                            stmt.setString(1, playerData.playerId().toString());
                            stmt.setString(2, entry.getKey());
                            stmt.setLong(3, entry.getValue());
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                    
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new Storage.Exception("Failed to save player data for " + playerData.playerId(), e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> deletePlayerData(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                String deleteQuery = "DELETE FROM atom_players WHERE player_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
                    stmt.setString(1, playerId.toString());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                throw new Storage.Exception("Failed to delete player data for " + playerId, e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Boolean> playerDataExists(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                String query = "SELECT 1 FROM atom_players WHERE player_id = ? LIMIT 1";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, playerId.toString());
                    ResultSet rs = stmt.executeQuery();
                    return rs.next();
                }
            } catch (SQLException e) {
                throw new Storage.Exception("Failed to check player data existence for " + playerId, e);
            }
        }, executor);
    }
}
