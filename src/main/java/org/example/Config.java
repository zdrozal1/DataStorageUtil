package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/// Config Utility Class To Create Dynamic Config Files
public class Config {
	private final boolean useAppDir;
	private final CommentedProperties properties = new CommentedProperties();
	private String fileName;
	
	/**
	 * Initializes the Config object with the specified file name and directory usage flag.
	 *
	 * @param fileName  the name of the configuration file
	 * @param useAppDir if true, the configuration file is stored in the application directory
	 */
	public Config(String fileName, boolean useAppDir) {
		this.fileName = fileName;
		this.useAppDir = useAppDir;
		loadProperties();
	}
	
	/**
	 * Retrieves the application's directory path based on the executing JAR file's location.
	 *
	 * @return the absolute path to the application's directory, or null if the path cannot be determined
	 */
	private String getAppPath() {
		try {
			return new File(
					Config.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
		} catch (Exception e) {
			System.err.println("Error determining application path: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Retrieves the configuration file object and ensures parent directories exist.
	 *
	 * @return a File object representing the configuration file
	 */
	private File getConfigFile() {
		String path = useAppDir ? getAppPath() + File.separator + fileName : fileName;
		File configFile = new File(path);
		
		if (configFile.getParentFile() != null && !configFile.getParentFile().exists()) {
			configFile.getParentFile().mkdirs();
		}
		
		return configFile;
	}
	
	/**
	 * Loads properties from the configuration file into memory, if the file exists.
	 */
	private void loadProperties() {
		File configFile = getConfigFile();
		if (configFile.exists()) {
			try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile),
			                                                      StandardCharsets.UTF_8)) {
				properties.load(reader);
			} catch (IOException e) {
				System.err.println("Unable to load properties: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Exports all configuration properties as a JSON-like string.
	 *
	 * @return a JSON-formatted string containing all properties
	 */
	public String exportAsJson() {
		return properties.stringPropertyNames().stream().collect(
				Collectors.toMap(key -> key, properties::getProperty)).toString().replace("=", ":");
	}
	
	/**
	 * Searches for property keys containing the specified substring.
	 *
	 * @param searchQuery the substring to search for in keys
	 * @return a List of matching keys
	 */
	public List<String> searchKeys(String searchQuery) {
		searchQuery = searchQuery.replaceAll(" ", "_");
		String finalSearchQuery = searchQuery;
		return properties.stringPropertyNames().stream().filter(key -> key.contains(finalSearchQuery)).collect(
				Collectors.toList());
	}
	
	/**
	 * Clears all properties within a specific section.
	 *
	 * @param section the section name to clear
	 */
	public void clearSection(String section) {
		properties.stringPropertyNames().stream().filter(key -> key.startsWith(section + ".")).collect(
				Collectors.toList()).forEach(properties::remove);
		saveProperties("Cleared Section: " + section);
	}
	
	/**
	 * Backs up the current configuration to a specified file.
	 *
	 * @param backupFileName the file name to save the backup
	 */
	public void backupConfiguration(String backupFileName) {
		try (OutputStream output = new FileOutputStream(backupFileName)) {
			properties.store(output, "Backup Configuration");
		} catch (IOException e) {
			System.err.println("Error backing up configuration: " + e.getMessage());
		}
	}
	
	/**
	 * Restores the configuration from a backup file.
	 *
	 * @param backupFileName the file name of the backup to restore
	 */
	public void restoreConfiguration(String backupFileName) {
		try (InputStream input = new FileInputStream(backupFileName)) {
			properties.clear();
			properties.load(input);
			saveProperties("Restored from Backup");
		} catch (IOException e) {
			System.err.println("Error restoring configuration: " + e.getMessage());
		}
	}
	
	/**
	 * Changes the configuration file name and optionally migrates the current data to the new file.
	 *
	 * @param newFileName the new file name for the configuration
	 * @param migrateData if true, the current properties are saved to the new file
	 */
	public void changeFileName(String newFileName, boolean migrateData) {
		if (migrateData) {
			saveProperties("Migrated to new file: " + newFileName);
		}
		this.fileName = newFileName;
	}
	
	/**
	 * Imports properties from a Map and saves the updated configuration.
	 *
	 * @param map the Map containing key-value pairs to add or update in the configuration
	 */
	public void importFromMap(Map<String, String> map) {
		properties.putAll(map);
		saveProperties("Imported Properties");
	}
	
	/**
	 * Reloads the configuration file into memory, overwriting any unsaved changes.
	 */
	public synchronized void reloadProperties() {
		properties.clear();
		loadProperties();
	}
	
	/**
	 * Writes the current properties and their associated comments to the configuration file.
	 *
	 * @param header an optional header comment to include in the file
	 */
	public void saveProperties(String header) {
		try (OutputStream output = new FileOutputStream(getConfigFile())) {
			properties.storeWithComments(output, header);
		} catch (IOException e) {
			System.err.println("Error saving properties to file: " + e.getMessage());
		}
	}
	
	/**
	 * Sets a property with an optional comment and immediately saves the updated configuration.
	 *
	 * @param key     the key of the property
	 * @param value   the value of the property
	 * @param comment an optional comment describing the property
	 */
	public void setProperty(String key, String value, String comment) {
		key = key.replaceAll(" ", "_");
		properties.putWithComment(key, value, comment);
		saveProperties("Configuration and Localization");
	}
	
	/**
	 * Retrieves the value of a property or sets and returns the default value if the property does not exist.
	 *
	 * @param key          the key of the property
	 * @param defaultValue the default value to return and save if the property does not exist
	 * @return the value of the property
	 */
	public String getProperty(String key, String defaultValue) {
		String value = properties.getProperty(key);
		if (value == null) {
			key = key.replaceAll(" ", "_");
			properties.put(key, defaultValue);
			saveProperties("Configuration and Localization");
			return defaultValue;
		}
		return value;
	}
	
	/**
	 * Retrieves a localized message for a given key or sets and returns a default value if the key does not exist.
	 *
	 * @param key          the key of the localized message
	 * @param defaultValue the default value to return if the key does not exist
	 * @return the localized message
	 */
	public synchronized String getLocalizedMessage(String key, String defaultValue) {
		return getProperty(key, defaultValue);
	}
	
	/**
	 * Clears all properties and saves an empty configuration file with a reset message.
	 */
	public void resetProperties() {
		properties.clear();
		saveProperties("Reset Configuration");
	}
	
	/**
	 * Removes a property and saves the updated configuration.
	 *
	 * @param key the key of the property to remove
	 * @return true if the property was removed, false otherwise
	 */
	public boolean removeProperty(String key) {
		boolean removed = properties.remove(key) != null;
		if (removed) {
			saveProperties("Configuration Updated");
		}
		return removed;
	}
	
	/**
	 * Checks whether a property exists in the configuration.
	 *
	 * @param key the key of the property to check
	 * @return true if the property exists, false otherwise
	 */
	public boolean hasProperty(String key) {
		return properties.containsKey(key);
	}
	
	/**
	 * Loads default properties from a specified file in the classpath and saves the configuration.
	 *
	 * @param defaultFileName the name of the default properties file
	 */
	public void loadDefaultProperties(String defaultFileName) {
		try (InputStream defaultStream = getClass().getClassLoader().getResourceAsStream(defaultFileName)) {
			if (defaultStream != null) {
				properties.load(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
				saveProperties("Loaded Default Configuration");
			}
		} catch (IOException e) {
			System.err.println("Error loading default properties: " + e.getMessage());
		}
	}
	
	/**
	 * Exports all properties as a map of key-value pairs.
	 *
	 * @return a Map containing all properties
	 */
	public Map<String, String> exportAsMap() {
		return properties.stringPropertyNames().stream().collect(Collectors.toMap(key -> key, properties::getProperty));
	}
	
	/// CommentedProperties inner class for handling comments and sections
	private static final class CommentedProperties extends Properties {
		private final LinkedHashMap<String, String> comments = new LinkedHashMap<>();
		
		public synchronized void putWithComment(String key, String value, String comment) {
			if (!containsKey(key)) {
				String section = getSection(key);
				comments.putIfAbsent(section, "# " + capitalize(section));
			}
			if (comment != null) {
				comments.put(key, "# " + comment);
			}
			super.put(key, value);
		}
		
		private String getSection(String key) {
			return key.contains(".") ? key.split("\\.")[0] : "default";
		}
		
		private String capitalize(String text) {
			return text.substring(0, 1).toUpperCase() + text.substring(1);
		}
		
		public void storeWithComments(OutputStream out, String header) throws IOException {
			try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
				writer.println("# " + new Date());
				if (header != null) {
					writer.println("# " + header);
					writer.println();
				}
				
				// Organize properties by sections
				Map<String, List<String>> sections = organizeBySections();
				boolean firstSection = true;
				
				for (Map.Entry<String, List<String>> entry : sections.entrySet()) {
					String section = entry.getKey();
					List<String> keys = entry.getValue();
					
					if (!firstSection) {
						writer.println(); // Add spacing between sections
					}
					firstSection = false;
					
					// Write section header
					if (comments.containsKey(section)) {
						writer.println(comments.get(section)); // Section comment
					} else {
						writer.println("# " + capitalize(section)); // Default section header
					}
					
					// Write keys and their comments/values
					for (String key : keys) {
						if (comments.containsKey(key)) {
							writer.println(comments.get(key)); // Write key-specific comment
						}
						writer.println(key + "=" + getProperty(key)); // Write key=value
					}
				}
			}
		}
		
		private Map<String, List<String>> organizeBySections() {
			Map<String, List<String>> sections = new LinkedHashMap<>();
			for (String key : stringPropertyNames()) {
				String section = getSection(key);
				sections.computeIfAbsent(section, k -> new ArrayList<>()).add(key);
			}
			return sections;
		}
	}
}
