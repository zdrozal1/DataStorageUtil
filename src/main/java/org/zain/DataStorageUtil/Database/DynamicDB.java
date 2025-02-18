package org.zain.DataStorageUtil.Database;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A dynamic database manager that lets the user define a custom table (its name and schema)
 * at runtime. Records are represented as a Map from column name to value.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * // Define the table schema.
 * Map<String, String> schema = new LinkedHashMap<>();
 * schema.put("id", "TEXT");      // Primary key column
 * schema.put("name", "TEXT");
 * schema.put("value", "REAL");
 *
 * // Create an instance of DynamicDB.
 * DynamicDB dbManager = new DynamicDB("mydatabase", "custom_data", "id", schema);
 *
 * dbManager.initDB();
 *
 * // Create a new record.
 * Map<String, Object> record = new HashMap<>();
 * record.put("id", "R001");
 * record.put("name", "Example");
 * record.put("value", 123.45);
 *
 * // Insert or replace the record.
 * dbManager.addOrReplaceRecord(record);
 *
 * // Retrieve the record.
 * Map<String, Object> retrieved = dbManager.getRecord("R001");
 * System.out.println("Retrieved: " + retrieved);
 *
 * // Update the record.
 * record.put("value", 543.21);
 * dbManager.modifyRecord(record);
 *
 * // Execute a custom query.
 * List<Object> params = Arrays.asList(543.21);
 * List<Map<String, Object>> results = dbManager.executeCustomQuery(
 *     "SELECT * FROM custom_data WHERE value = ?", params);
 *
 * // Export the table data to CSV.
 * dbManager.exportToCSV("data_export.csv");
 *
 * // Drop the table.
 * // dbManager.dropTable();
 *
 * // Close the manager when done.
 * dbManager.close();
 * }</pre>
 */
public class DynamicDB {

    private final String dbFilePath;
    private final String tableName;
    private final String primaryKeyColumn;
    private final Map<String, String> columnsDefinition;
    private final DynamicDBEventManager eventManager = new DynamicDBEventManager();
    private Connection connection;

    /**
     * Constructs a DynamicDB instance.
     *
     * @param dbFilePath        The SQLite database file path.
     * @param tableName         The name of the table to manage.
     * @param primaryKeyColumn  The column that acts as the primary key.
     * @param columnsDefinition A map of column names to SQLite data types.
     *                          The primary key column must be included.
     */
    public DynamicDB(String dbFilePath, String tableName, String primaryKeyColumn, Map<String, String> columnsDefinition) {
        this.dbFilePath = dbFilePath + ".db";
        this.tableName = tableName;
        this.primaryKeyColumn = primaryKeyColumn;
        // Preserve insertion order for consistent SQL generation.
        this.columnsDefinition = new LinkedHashMap<>(columnsDefinition);
        if (!this.columnsDefinition.containsKey(primaryKeyColumn)) {
            eventManager.triggerDatabaseError(
                    "Columns definition must include the primary key column: " + primaryKeyColumn,
                    new IllegalArgumentException());
        }
    }

    /**
     * Checks if a database file exists at the given file path.
     *
     * @param dbFilePath The database file path (with or without the ".db" extension).
     * @return True if the database file exists, false otherwise.
     */
    public static boolean databaseExists(String dbFilePath) {
        String fullPath = dbFilePath.endsWith(".db") ? dbFilePath : dbFilePath + ".db";
        File dbFile = new File(fullPath);
        return dbFile.exists();
    }

    /**
     * Checks if a specific table exists in a given SQLite database file.
     *
     * <p>This method connects to the SQLite database specified by {@code dbFilePath} and checks if a table with
     * the provided {@code tableName} exists. The database file extension ".db" is appended if it is not present.
     * It queries the {@code sqlite_master} table to determine whether the table exists.</p>
     *
     * @param dbFilePath the path to the SQLite database file (with or without the ".db" extension)
     * @param tableName  the name of the table to check for existence
     * @return {@code true} if the table exists in the database, {@code false} otherwise
     * @throws SQLException if a database access error occurs or the connection fails
     */
    public static boolean tableExists(String dbFilePath, String tableName) throws SQLException {
        String fullPath = dbFilePath.endsWith(".db") ? dbFilePath : dbFilePath + ".db";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + fullPath);
             PreparedStatement ps = conn.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name=?")) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Checks if a table with the specified name exists in the connected database.
     *
     * @param tableName The name of the table to check.
     * @return True if the table exists, false otherwise.
     * @throws SQLException if the connection is not established or a database error occurs.
     */
    public boolean tableExists(String tableName) throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Connection is not established.");
        }
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Deletes the entire database file, including the file itself.
     * Closes the connection if open and then deletes the database file.
     *
     * @throws IOException if the database file cannot be deleted.
     */
    public void deleteDB() throws IOException {
        try {
            close(); // Close the connection to release any file locks
        } catch (SQLException e) {
            eventManager.triggerDatabaseError("Error closing connection during database deletion", e);
        }

        File dbFile = new File(dbFilePath);
        if (dbFile.exists()) {
            boolean isDeleted = dbFile.delete();
            if (!isDeleted) {
                throw new IOException("Failed to delete database file: " + dbFilePath);
            }
            eventManager.triggerDatabaseDeleted(dbFilePath);
        }
    }

    /**
     * Initializes the SQLite database connection.
     */
    public boolean initDB() {
        try {
            Class.forName("org.sqlite.JDBC"); // Ensure the driver is loaded.
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
            eventManager.triggerDatabaseConnected(dbFilePath);
            createTableIfNotExists();
            return true;
        } catch (Exception e) {
            eventManager.triggerDatabaseError("Error connecting to database", e);
            return false;
        }
    }

    /**
     * Creates the table if it does not already exist using the provided schema.
     */
    private void createTableIfNotExists() {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sql.append(tableName).append(" (");
        // Build each column definition.
        for (Map.Entry<String, String> entry : columnsDefinition.entrySet()) {
            sql.append(entry.getKey()).append(" ").append(entry.getValue());
            if (entry.getKey().equals(primaryKeyColumn)) {
                sql.append(" PRIMARY KEY");
            }
            sql.append(", ");
        }
        // Remove the trailing comma and space.
        sql.setLength(sql.length() - 2);
        sql.append(");");

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql.toString());
            eventManager.triggerTableReady(tableName);
        } catch (SQLException e) {
            eventManager.triggerDatabaseError("Error creating table: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a new record or replaces an existing one (matching on the primary key).
     *
     * @param record A map representing the record (must include the primary key).
     * @throws SQLException if a database error occurs.
     */
    public void addOrReplaceRecord(Map<String, Object> record) throws SQLException {
        // Validate that the types match the schema before proceeding.
        validateRecord(record);
        Object pkValue = record.get(primaryKeyColumn);
        if (pkValue == null) {
            eventManager.triggerDatabaseError("Record must contain a value for primary key column: " + primaryKeyColumn,
                    new IllegalArgumentException());
        }

        String checkSQL = "SELECT COUNT(*) FROM " + tableName + " WHERE " + primaryKeyColumn + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(checkSQL)) {
            ps.setObject(1, pkValue);
            ResultSet rs = ps.executeQuery();
            boolean exists = false;
            if (rs.next()) {
                exists = rs.getInt(1) > 0;
            }
            rs.close();
            if (exists) {
                updateRecord(record);
            } else {
                insertRecord(record);
            }
        }
        eventManager.triggerRecordAddedOrReplaced(pkValue);
    }

    /**
     * Inserts a new record into the table.
     *
     * @param record A map representing the record.
     * @throws SQLException if a database error occurs.
     */
    private void insertRecord(Map<String, Object> record) throws SQLException {
        StringBuilder columnsPart = new StringBuilder();
        StringBuilder valuesPart = new StringBuilder();
        List<Object> values = new ArrayList<>();

        // Loop over defined columns and include those present in the record.
        for (String col : columnsDefinition.keySet()) {
            if (record.containsKey(col)) {
                columnsPart.append(col).append(", ");
                valuesPart.append("?, ");
                values.add(record.get(col));
            }
        }
        if (columnsPart.length() > 0) {
            columnsPart.setLength(columnsPart.length() - 2);
            valuesPart.setLength(valuesPart.length() - 2);
        }
        String sql = "INSERT INTO " + tableName + " (" + columnsPart + ") VALUES (" + valuesPart + ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            ps.executeUpdate();
        }
    }

    /**
     * Updates an existing record in the table (all columns except the primary key).
     *
     * @param record A map representing the updated record.
     * @throws SQLException if a database error occurs.
     */
    private void updateRecord(Map<String, Object> record) throws SQLException {
        StringBuilder setClause = new StringBuilder();
        List<Object> values = new ArrayList<>();

        // Build the SET clause (skip the primary key).
        for (String col : columnsDefinition.keySet()) {
            if (col.equals(primaryKeyColumn)) {
                continue;
            }
            if (record.containsKey(col)) {
                setClause.append(col).append(" = ?, ");
                values.add(record.get(col));
            }
        }
        if (setClause.length() > 0) {
            setClause.setLength(setClause.length() - 2);
        }
        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + primaryKeyColumn + " = ?";
        values.add(record.get(primaryKeyColumn));
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            int affected = ps.executeUpdate();
            if (affected == 0) {
                eventManager.triggerDatabaseError(
                        "No record updated; record not found for primary key: " + primaryKeyColumn, new SQLException());
            }
        }
    }

    /**
     * Modifies (updates) an existing record in the table.
     *
     * @param record A map representing the updated record.
     * @throws SQLException if a database error occurs.
     */
    public void modifyRecord(Map<String, Object> record) throws SQLException {
        // Validate that the types match the schema before proceeding.
        validateRecord(record);

        Object pkValue = record.get(primaryKeyColumn);
        if (pkValue == null) {
            eventManager.triggerDatabaseError("Record must contain a value for primary key column: " + primaryKeyColumn,
                    new IllegalArgumentException());
        }
        String checkSQL = "SELECT COUNT(*) FROM " + tableName + " WHERE " + primaryKeyColumn + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(checkSQL)) {
            ps.setObject(1, pkValue);
            ResultSet rs = ps.executeQuery();
            boolean exists = false;
            if (rs.next()) {
                exists = rs.getInt(1) > 0;
            }
            rs.close();
            if (exists) {
                updateRecord(record);
                eventManager.triggerRecordModified(pkValue);
            } else {
                eventManager.triggerRecordNotFound("modifyRecord", pkValue);
            }
        }
    }

    /**
     * Deletes a record from the table using its primary key.
     *
     * @param pkValue The primary key value identifying the record to delete.
     * @throws SQLException if a database error occurs.
     */
    public void deleteRecord(Object pkValue) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE " + primaryKeyColumn + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, pkValue);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                eventManager.triggerRecordDeleted(pkValue);
            } else {
                eventManager.triggerRecordNotFound("deleteRecord", pkValue);
            }
        }
    }

    /**
     * Retrieves a record from the table using its primary key.
     *
     * @param pkValue The primary key value.
     * @return A map representing the record, or null if not found.
     * @throws SQLException if a database error occurs.
     */
    public Map<String, Object> getRecord(Object pkValue) throws SQLException {
        String sql = "SELECT * FROM " + tableName + " WHERE " + primaryKeyColumn + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, pkValue);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> record = new LinkedHashMap<>();
                for (String col : columnsDefinition.keySet()) {
                    record.put(col, rs.getObject(col));
                }
                rs.close();
                return record;
            }
            rs.close();
            return null;
        }
    }

    /**
     * Retrieves all records from the table.
     *
     * @return A list of maps, each representing a record.
     * @throws SQLException if a database error occurs.
     */
    public List<Map<String, Object>> getAllRecords() throws SQLException {
        List<Map<String, Object>> records = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName;
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> record = new LinkedHashMap<>();
                for (String col : columnsDefinition.keySet()) {
                    record.put(col, rs.getObject(col));
                }
                records.add(record);
            }
        }
        return records;
    }

    /**
     * Validates that the types of the values in the given record match the expected types
     * based on the columnsDefinition.
     *
     * @param record The record to validate.
     * @throws IllegalArgumentException if a value's type does not match the expected type.
     */
    private void validateRecord(Map<String, Object> record) {
        for (Map.Entry<String, String> entry : columnsDefinition.entrySet()) {
            String column = entry.getKey();
            String expectedType = entry.getValue().toUpperCase();
            Object value = record.get(column);

            // Allow null values (adjust if non-null enforcement is needed)
            if (value == null) {
                continue;
            }

            switch (expectedType) {
                case "INTEGER":
                    if (!(value instanceof Number)) {
                        eventManager.triggerDatabaseError(
                                "Column '" + column + "' expects an INTEGER, but got " + value.getClass().getSimpleName(),
                                new IllegalArgumentException());
                    }
                    break;
                case "REAL":
                    if (!(value instanceof Number)) {
                        eventManager.triggerDatabaseError(
                                "Column '" + column + "' expects a REAL, but got " + value.getClass().getSimpleName(),
                                new IllegalArgumentException());
                    }
                    break;
                case "TEXT":
                    if (!(value instanceof String)) {
                        eventManager.triggerDatabaseError(
                                "Column '" + column + "' expects TEXT, but got " + value.getClass().getSimpleName(),
                                new IllegalArgumentException());
                    }
                    break;
                case "BLOB":
                    if (!(value instanceof byte[])) {
                        eventManager.triggerDatabaseError(
                                "Column '" + column + "' expects a BLOB (byte[]), but got " + value.getClass().getSimpleName(),
                                new IllegalArgumentException());
                    }
                    break;
                default:
                    // Additional type checks can be implemented here if needed.
                    break;
            }
        }
    }

    /// --- Transaction Management ---

    /**
     * Begins a database transaction.
     *
     * @throws SQLException if a database error occurs.
     */
    public void beginTransaction() throws SQLException {
        connection.setAutoCommit(false);
        eventManager.triggerTransactionStarted();
    }

    /**
     * Commits the current database transaction.
     *
     * @throws SQLException if a database error occurs.
     */
    public void commitTransaction() throws SQLException {
        connection.commit();
        connection.setAutoCommit(true);
        eventManager.triggerTransactionCommitted();
    }

    /**
     * Rolls back the current database transaction.
     *
     * @throws SQLException if a database error occurs.
     */
    public void rollbackTransaction() throws SQLException {
        connection.rollback();
        connection.setAutoCommit(true);
        eventManager.triggerTransactionRolledBack();
    }

    /// --- Batch Operations ---

    /**
     * Adds or replaces multiple records in a batch operation.
     *
     * @param records A list of records to process.
     * @throws SQLException if a database error occurs.
     */
    public void addOrReplaceRecords(List<Map<String, Object>> records) throws SQLException {
        if (records == null || records.isEmpty()) {
            return;
        }
        beginTransaction();
        try {
            for (Map<String, Object> record : records) {
                addOrReplaceRecord(record);
            }
            commitTransaction();
        } catch (SQLException e) {
            rollbackTransaction();
            throw e;
        }
    }

    /**
     * Retrieves a list of all table names in the database
     *
     * @return A list of table names
     * @throws SQLException if a database error occurs
     */
    public List<String> getTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'";

        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tables.add(rs.getString("name"));
            }
        }
        return tables;
    }

    /// --- Custom Query Execution ---

    /**
     * Executes a custom SQL query with parameters and returns the result as a list of records.
     *
     * @param sql        The SQL query string.
     * @param parameters The list of parameters for the query.
     * @return A list of records (each record is a map).
     * @throws SQLException if a database error occurs.
     */
    public List<Map<String, Object>> executeCustomQuery(String sql, List<Object> parameters) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (parameters != null) {
                for (int i = 0; i < parameters.size(); i++) {
                    ps.setObject(i + 1, parameters.get(i));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String colName = meta.getColumnName(i);
                        row.put(colName, rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        }
        return results;
    }

    /// --- Table Modification Methods ---

    /**
     * Drops the table from the database.
     *
     * @throws SQLException if a database error occurs.
     */
    public void dropTable() throws SQLException {
        String sql = "DROP TABLE IF EXISTS " + tableName;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            eventManager.triggerTableDropped(tableName);
        }
    }

    /**
     * Clears all records from the table.
     *
     * @throws SQLException if a database error occurs.
     */
    public void clearTable() throws SQLException {
        String sql = "DELETE FROM " + tableName;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            eventManager.triggerTableCleared(tableName);
        }
    }

    /**
     * Alters the table to add a new column.
     *
     * @param columnName The name of the new column.
     * @param columnType The SQLite data type for the new column.
     * @throws SQLException if a database error occurs.
     */
    public void alterTableAddColumn(String columnName, String columnType) throws SQLException {
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            // Update the local schema definition.
            columnsDefinition.put(columnName, columnType);
            eventManager.triggerColumnAdded(columnName, columnType);
        }
    }

    /**
     * Retrieves the current table schema (column names and types).
     *
     * @return A map representing the table schema.
     * @throws SQLException if a database error occurs.
     */
    public Map<String, String> getTableSchema() throws SQLException {
        Map<String, String> schema = new LinkedHashMap<>();
        String sql = "PRAGMA table_info(" + tableName + ")";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("name");
                String type = rs.getString("type");
                schema.put(name, type);
            }
        }
        return schema;
    }

    /// --- CSV Export ---

    /**
     * Exports all records from the table to a CSV file.
     *
     * @param filePath The file path where the CSV will be written.
     * @throws SQLException if a database error occurs.
     * @throws IOException  if an I/O error occurs.
     */
    public void exportToCSV(String filePath) throws SQLException, IOException {
        List<Map<String, Object>> records = getAllRecords();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Write CSV header.
            String header = String.join(",", columnsDefinition.keySet());
            writer.write(header);
            writer.newLine();
            // Write each record.
            for (Map<String, Object> record : records) {
                List<String> values = new ArrayList<>();
                for (String col : columnsDefinition.keySet()) {
                    Object value = record.get(col);
                    values.add(value != null ? value.toString() : "");
                }
                writer.write(String.join(",", values));
                writer.newLine();
            }
        }
        eventManager.triggerDataExported(filePath);
    }

    /// --- Closing the Connection ---

    /**
     * Closes the database connection.
     *
     * @throws SQLException if a database error occurs.
     */
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            eventManager.triggerConnectionClosed();
        }
    }

    public DynamicDBEventManager getEventManager() {
        return eventManager;
    }

    public String getTableName() {
        return tableName;
    }
}
