package org.zain.DataStorage;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A utility class that simplifies the management of in-memory and file-based data storage.
 */
public class DataStorageUtil {
	private static final Map<String, Object> dataStorage = new HashMap<>();
	
	/**
	 * Stores the given value in memory associated with the provided key.
	 *
	 * @param key   the key to store the value under.
	 * @param value the value to be stored (must be serializable).
	 */
	public static void store(String key, Object value) {
		dataStorage.put(key, value);
	}
	
	/**
	 * Retrieves the stored value associated with the provided key.
	 *
	 * @param key   the key of the stored value.
	 * @param clazz the expected type of the stored value.
	 * @param <T>   the type of the stored value.
	 * @return the stored value, or null if not found or type mismatch.
	 * Example:
	 * User user1 = DataStorageUtil.retrieve("user1", User.class);
	 * if (user1 != null) {
	 * System.out.println("User1's name: " + user1.getName());
	 * }
	 */
	public static <T> T retrieve(String key, Class<T> clazz) {
		Object value = dataStorage.get(key);
		return clazz.isInstance(value) ? clazz.cast(value) : null;
	}
	
	/**
	 * Saves all in-memory data to a file.
	 *
	 * @param fileName the name of the file to save the data to.
	 */
	public static void saveToFile(String fileName) {
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName))) {
			out.writeObject(dataStorage);
			System.out.println("Data saved to " + fileName);
		} catch (IOException e) {
			System.err.println("Failed to save data to file: " + e.getMessage());
		}
	}
	
	/**
	 * Loads data from a file and replaces the current in-memory data.
	 *
	 * @param fileName the name of the file to load the data from.
	 */
	public static void loadFromFile(String fileName) {
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName))) {
			Map<String, Object> loadedData = (Map<String, Object>) in.readObject();
			dataStorage.clear();
			dataStorage.putAll(loadedData);
			System.out.println("Data loaded from " + fileName);
		} catch (IOException | ClassNotFoundException e) {
			System.err.println("Failed to load data from file: " + e.getMessage());
		}
	}
	
	/**
	 * Clears all in-memory data.
	 */
	public static void clear() {
		dataStorage.clear();
	}
	
	/**
	 * Searches for keys that contain the given query string.
	 *
	 * @param query the query string to search for.
	 * @return a list of keys containing the query string.
	 */
	public static List<String> searchKeys(String query) {
		return dataStorage.keySet().stream().filter(key -> key.contains(query)).collect(Collectors.toList());
	}
	
	/**
	 * Generates a human-readable report of the current in-memory data.
	 *
	 * @return a string representing the data report.
	 */
	public static String generateReport() {
		StringBuilder reportBuilder = new StringBuilder();
		for (Map.Entry<String, Object> entry : dataStorage.entrySet()) {
			reportBuilder.append("Key: ").append(entry.getKey()).append("\nValue: ").append(entry.getValue()).append("\n\n");
		}
		return reportBuilder.toString();
	}
	
	/**
	 * Saves the current data report to a text file.
	 *
	 * @param fileName the name of the file to write the report to.
	 */
	public static void saveReportToTextFile(String fileName) {
		String reportContent = generateReport();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
			writer.write(reportContent);
			System.out.println("Report saved to " + fileName);
		} catch (IOException e) {
			System.err.println("Failed to write report to text file: " + e.getMessage());
		}
	}
}
