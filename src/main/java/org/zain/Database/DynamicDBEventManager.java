package org.zain.Database;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DynamicDBEventManager {
	private Consumer<String> onDatabaseConnected = dbPath -> System.out.println("Connectjred to database at: " + dbPath);
	private Consumer<String> onTableReady = tableName -> System.out.println("Table " + tableName + " is ready.");
	private Consumer<Object> onRecordAddedOrReplaced = pkValue -> System.out.println("Record with primary key " + pkValue + " added or replaced successfully.");
	private Consumer<Object> onRecordModified = pkValue -> System.out.println("Record with primary key " + pkValue + " modified successfully.");
	private BiConsumer<String, Object> onRecordNotFound = (operation, pkValue) -> System.out.println("No record found for operation " + operation + " with primary key " + pkValue);
	private Consumer<Object> onRecordDeleted = pkValue -> System.out.println("Record with primary key " + pkValue + " deleted.");
	private Runnable onTransactionStarted = () -> System.out.println("Transaction started.");
	private Runnable onTransactionCommitted = () -> System.out.println("Transaction committed.");
	private Runnable onTransactionRolledBack = () -> System.out.println("Transaction rolled back.");
	private Consumer<String> onTableDropped = tableName -> System.out.println("Table " + tableName + " dropped.");
	private Consumer<String> onTableCleared = tableName -> System.out.println("All records cleared from table " + tableName + ".");
	private BiConsumer<String, String> onColumnAdded = (columnName, tableName) -> System.out.println("Column " + columnName + " added to table " + tableName + ".");
	private Consumer<String> onDataExported = filePath -> System.out.println("Data exported to CSV file at " + filePath);
	private Runnable onConnectionClosed = () -> System.out.println("Database connection closed.");
	private BiConsumer<String, Exception> onDatabaseError = (message, exception) -> System.out.println("Database error: " + message + " Exception: " + exception.getMessage());
	private BiConsumer<String, Exception> onValidationError = (message, exception) -> System.out.println("Validation error: " + message + " Exception: " + exception.getMessage());
	
	public void setOnDatabaseConnected(Consumer<String> handler) {
		this.onDatabaseConnected = handler;
	}
	
	public void setOnTableReady(Consumer<String> handler) {
		this.onTableReady = handler;
	}
	
	public void setOnRecordAddedOrReplaced(Consumer<Object> handler) {
		this.onRecordAddedOrReplaced = handler;
	}
	
	public void setOnRecordModified(Consumer<Object> handler) {
		this.onRecordModified = handler;
	}
	
	public void setOnRecordNotFound(BiConsumer<String, Object> handler) {
		this.onRecordNotFound = handler;
	}
	
	public void setOnRecordDeleted(Consumer<Object> handler) {
		this.onRecordDeleted = handler;
	}
	
	public void setOnTransactionStarted(Runnable handler) {
		this.onTransactionStarted = handler;
	}
	
	public void setOnTransactionCommitted(Runnable handler) {
		this.onTransactionCommitted = handler;
	}
	
	public void setOnTransactionRolledBack(Runnable handler) {
		this.onTransactionRolledBack = handler;
	}
	
	public void setOnTableDropped(Consumer<String> handler) {
		this.onTableDropped = handler;
	}
	
	public void setOnTableCleared(Consumer<String> handler) {
		this.onTableCleared = handler;
	}
	
	public void setOnColumnAdded(BiConsumer<String, String> handler) {
		this.onColumnAdded = handler;
	}
	
	public void setOnDataExported(Consumer<String> handler) {
		this.onDataExported = handler;
	}
	
	public void setOnConnectionClosed(Runnable handler) {
		this.onConnectionClosed = handler;
	}
	
	public void setOnDatabaseError(BiConsumer<String, Exception> handler) {
		this.onDatabaseError = handler;
	}
	
	public void setOnValidationError(BiConsumer<String, Exception> handler) {
		this.onValidationError = handler;
	}
	
	public void triggerDatabaseConnected(String dbFilePath) {
		onDatabaseConnected.accept(dbFilePath);
	}
	
	public void triggerTableReady(String tableName) {
		onTableReady.accept(tableName);
	}
	
	public void triggerRecordAddedOrReplaced(Object pkValue) {
		onRecordAddedOrReplaced.accept(pkValue);
	}
	
	public void triggerRecordModified(Object pkValue) {
		onRecordModified.accept(pkValue);
	}
	
	public void triggerRecordNotFound(String operation, Object pkValue) {
		onRecordNotFound.accept(operation, pkValue);
	}
	
	public void triggerRecordDeleted(Object pkValue) {
		onRecordDeleted.accept(pkValue);
	}
	
	public void triggerTransactionStarted() {
		onTransactionStarted.run();
	}
	
	public void triggerTransactionCommitted() {
		onTransactionCommitted.run();
	}
	
	public void triggerTransactionRolledBack() {
		onTransactionRolledBack.run();
	}
	
	public void triggerTableDropped(String tableName) {
		onTableDropped.accept(tableName);
	}
	
	public void triggerTableCleared(String tableName) {
		onTableCleared.accept(tableName);
	}
	
	public void triggerColumnAdded(String columnName, String tableName) {
		onColumnAdded.accept(columnName, tableName);
	}
	
	public void triggerDataExported(String filePath) {
		onDataExported.accept(filePath);
	}
	
	public void triggerConnectionClosed() {
		onConnectionClosed.run();
	}
	
	public void triggerDatabaseError(String message, Exception exception) {
		onDatabaseError.accept(message, exception);
	}
	
	public void triggerValidationError(String message, Exception exception) {
		onValidationError.accept(message, exception);
	}
}
