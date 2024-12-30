package org.zain.Config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

// TODO: ADD SUPPORT FOR MULTIPLE PARAMETERS

/// Config Utility Class To Create Dynamic Config Files
public class Config {
	private static final EventManager eventManager = new EventManager();
	private static boolean enableEvents = false;
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
	}
	
	/**
	 * Writes the current properties and their associated comments to the configuration file.
	 *
	 * @param header an optional header comment to include in the file
	 */
	public void saveProperties(String header) {
		try (OutputStream output = new FileOutputStream(getConfigFile())) {
			// If header is provided, write it in the section header format
			if (header != null && !header.isEmpty()) {
				properties.storeWithComments(output, "[" + header + "]");
			}
			properties.storeWithComments(output, null); // Save properties as usual
		} catch (IOException e) {
			System.err.println("Error saving properties to file: " + e.getMessage());
		}
	}
	
	public void clearSection(String section) {
		properties.stringPropertyNames().stream().filter(key -> key.startsWith(section + ".")).toList().forEach(key -> {
			properties.remove(key);
			removeProperty(key);
		});
		saveProperties(section);  // Now saves with section header formatted as [Section]
		eventManager.triggerEvent(Event.SECTION_CLEARED, section);
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
		
		eventManager.triggerEvent(Event.SEARCH_KEYS, searchQuery);
		
		return properties.stringPropertyNames().stream().filter(key -> key.contains(finalSearchQuery)).collect(Collectors.toList());
	}
	
	/**
	 * Sets whether events will be enabled for Config object
	 *
	 * @param enableEvents whether events will be enabled
	 */
	public void setEnableEvents(boolean enableEvents) {
		Config.enableEvents = enableEvents;
	}
	
	/**
	 * Changes the configuration file name and optionally migrates the current data to the new file.
	 *
	 * @param newFileName the new file name for the configuration
	 * @param migrateData if true, the current properties are saved to the new file
	 */
	public void changeFileName(String newFileName, boolean migrateData) {
		if (migrateData) {
			saveProperties("Migrated data to new file: " + newFileName);
			eventManager.triggerEvent(Event.CONFIG_MIGRATED, newFileName);
		}
		this.fileName = newFileName;
		eventManager.triggerEvent(Event.FILE_NAME_CHANGED, newFileName);
	}
	
	/**
	 * Loads properties from the configuration file into memory, if the file exists.
	 */
	public void loadProperties() {
		File configFile = getConfigFile();
		if (configFile.exists()) {
			try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
				Properties tempProperties = new Properties();
				tempProperties.load(reader);
				int loaded = tempProperties.size();
				int total = properties.stringPropertyNames().size();
				properties.putAll(tempProperties);
				if (loaded < total) {
				} else {
					eventManager.triggerEvent(Event.CONFIG_LOADED, configFile.getAbsolutePath());
				}
			} catch (IOException e) {
				eventManager.triggerEvent(Event.CONFIG_NOT_LOADED, e);
			}
		} else {
			eventManager.triggerEvent(Event.CONFIG_FILE_MISSING, configFile.getAbsolutePath());
		}
	}
	
	/**
	 * Deletes the current config file
	 */
	public void deleteConfigFile() {
		File configFile = getConfigFile();
		if (configFile.exists() && configFile.delete()) {
			eventManager.triggerEvent(Event.CONFIG_FILE_DELETED, configFile.getAbsolutePath());
		} else {
			eventManager.triggerEvent(Event.CONFIG_FILE_DELETE_FAILED, configFile.getAbsolutePath());
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
	
	/**
	 * Reloads the configuration file into memory, overwriting any unsaved changes.
	 */
	public synchronized void reloadProperties() {
		properties.clear();
		loadProperties();
		eventManager.triggerEvent(Event.CONFIG_RELOADED, "");
	}
	
	/**
	 * Clears all properties and saves an empty configuration file with a reset message.
	 */
	public void resetConfiguration() {
		properties.clear();
		saveProperties("Reset Configuration");
		eventManager.triggerEvent(Event.CONFIG_RESET, "");
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
		eventManager.triggerEvent(Event.PROPERTY_CREATED, key);
		saveProperties("Configuration and Localization");
	}
	
	/**
	 * Sets a property with an optional comment and immediately saves the updated configuration.
	 *
	 * @param key   the key of the property
	 * @param value the value of the property
	 */
	public void setProperty(String key, String value) {
		setProperty(key, value, null);
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
			eventManager.triggerEvent(Event.PROPERTY_CREATED, key);
			saveProperties("Configuration and Localization");
			return defaultValue;
		}
		return value;
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
	 * Removes a property and saves the updated configuration.
	 *
	 * @param key the key of the property to remove
	 * @return true if the property was successfully removed, false otherwise
	 */
	public boolean removeProperty(String key) {
		key = key.replaceAll(" ", "_");
		boolean removed = properties.remove(key) != null;
		if (removed) {
			eventManager.triggerEvent(Event.PROPERTY_REMOVED, properties.getProperty(key));
			saveProperties("Configuration Updated");
		} else {
			eventManager.triggerEvent(Event.PROPERTY_NOT_REMOVED, properties.getProperty(key));
			
		}
		return removed;
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
				eventManager.triggerEvent(Event.DEFAULT_CONFIG_LOADED, defaultFileName);
			}
		} catch (IOException e) {
			System.err.println("Error loading default properties: " + e.getMessage());
		}
	}
	
	/**
	 * Retrieves the application's directory path based on the executing JAR file's location.
	 *
	 * @return the absolute path to the application's directory, or null if the path cannot be determined
	 */
	private String getAppPath() {
		try {
			return new File(Config.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
		} catch (Exception e) {
			System.err.println("Error determining application path: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * File name getter
	 *
	 * @return the file name of the config file
	 */
	public String getFileName() {
		return fileName;
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
	 * Retrieve Config Path
	 *
	 * @return path of the config file
	 */
	private String getPath() {
		return getConfigFile().getPath();
	}
	
	/**
	 * Retrieve absolute config path
	 *
	 * @return the absolute path of the config file
	 */
	private String getAbsolutePath() {
		return getConfigFile().getAbsolutePath();
	}
	
	/**
	 * Backs up the current configuration to a specified file.
	 *
	 * @param backupFileName the file name to save the backup
	 * @param useAppDir      use the location from the jar directory
	 */
	public void backupConfiguration(String backupFileName, boolean useAppDir) {
		if (useAppDir) {
			backupFileName = getAppPath() + File.separator + backupFileName;
		}
		try (OutputStream output = new FileOutputStream(backupFileName)) {
			properties.store(output, "Backup Configuration");
			eventManager.triggerEvent(Event.CONFIG_BACKUP_SUCCESS, backupFileName);
		} catch (IOException e) {
			eventManager.triggerEvent(Event.CONFIG_BACKUP_FAIL, e);
		}
	}
	
	/**
	 * Restores the configuration from a backup file.
	 *
	 * @param backupFileName the file name of the backup to restore
	 * @param useAppDir      use the location from the jar directory
	 */
	public void restoreConfiguration(String backupFileName, boolean useAppDir) {
		if (useAppDir) {
			backupFileName = getAppPath() + File.separator + backupFileName;
		}
		try (InputStream input = new FileInputStream(backupFileName)) {
			properties.clear();
			properties.load(input);
			saveProperties("Restored from Backup");
			eventManager.triggerEvent(Event.CONFIG_RESTORED, backupFileName);
		} catch (IOException e) {
			System.err.println("Error restoring configuration: " + e.getMessage());
		}
	}
	
	/**
	 * Backs up the current configuration to a specified file.
	 *
	 * @param backupFileName the file name to save the backup
	 */
	public void backupConfiguration(String backupFileName) {
		backupConfiguration(backupFileName, false);
	}
	
	/**
	 * Restores the configuration from a backup file.
	 *
	 * @param backupFileName the file name of the backup to restore
	 */
	public void restoreConfiguration(String backupFileName) {
		restoreConfiguration(backupFileName, false);
	}
	
	/**
	 * Imports properties from a Map and saves the updated configuration.
	 *
	 * @param map the Map containing key-value pairs to add or update in the configuration
	 */
	public void importFromMap(Map<String, String> map) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			properties.put(entry.getKey(), entry.getValue());
			eventManager.triggerEvent(Event.PROPERTY_CREATED, entry.getKey());
		}
		saveProperties("Imported Properties");
		eventManager.triggerEvent(Event.PROPERTIES_IMPORTED, "");
	}
	
	/**
	 * Exports all configuration properties as a JSON-like string.
	 *
	 * @return a JSON-formatted string containing all properties
	 */
	public String exportAsJson() {
		return properties.stringPropertyNames().stream().collect(Collectors.toMap(key -> key, properties::getProperty)).toString().replace("=", ":");
	}
	
	/**
	 * Enum representing various events that can be triggered within the system.
	 */
	public enum Event {
		CONFIG_NOT_LOADED, CONFIG_LOADED, CONFIG_RELOADED, PROPERTY_CREATED, PROPERTY_REMOVED, PROPERTY_NOT_REMOVED, SECTION_CLEARED, CONFIG_RESTORED, CONFIG_MIGRATED, PROPERTIES_IMPORTED, CONFIG_RESET, DEFAULT_CONFIG_LOADED, FILE_NAME_CHANGED, SEARCH_KEYS, CONFIG_BACKUP_SUCCESS, CONFIG_BACKUP_FAIL, CONFIG_FILE_DELETED, CONFIG_FILE_DELETE_FAILED, CONFIG_FILE_MISSING, CONFIG_LOADED_PARTIALLY
	}
	
	@FunctionalInterface
	public interface EventHandler<T> {
		void accept(T parameter);
	}
	
	public static class EventManager {
		private static final Map<Event, EventHandler<?>> registeredHandlers = new HashMap<>();
		private final Map<Event, EventHandler<?>> defaultHandlers = new HashMap<>();
		
		/**
		 * Constructor that initializes the default handlers for various events.
		 */
		public EventManager() {
			// Default event handlers initialization
			defaultHandlers.put(Event.CONFIG_NOT_LOADED, (IOException exception) -> System.err.println("Error loading config: " + exception.getMessage()));
			defaultHandlers.put(Event.CONFIG_LOADED, (path) -> System.out.println("Config has been created at: " + path));
			defaultHandlers.put(Event.CONFIG_RELOADED, (String _) -> System.out.println("Config has been reloaded"));
			defaultHandlers.put(Event.PROPERTY_CREATED, (key) -> System.out.println("Property: '" + key + "', has been added"));
			defaultHandlers.put(Event.PROPERTY_REMOVED, (key) -> System.out.println("Property: '" + key + "' has been removed"));
			defaultHandlers.put(Event.PROPERTY_NOT_REMOVED, (key) -> System.err.println("Property: '" + key + "' could not be removed!"));
			defaultHandlers.put(Event.SECTION_CLEARED, (section) -> System.out.println("Cleared section: " + section));
			defaultHandlers.put(Event.CONFIG_RESTORED, (path) -> System.out.println("Config restored from: " + path));
			defaultHandlers.put(Event.CONFIG_MIGRATED, (path) -> System.out.println("Migrated to new file: " + path));
			defaultHandlers.put(Event.PROPERTIES_IMPORTED, (String _) -> System.out.println("Properties Imported."));
			defaultHandlers.put(Event.CONFIG_RESET, (String _) -> System.out.println("Config has been reset."));
			defaultHandlers.put(Event.DEFAULT_CONFIG_LOADED, (path) -> System.out.println("Loaded default properties from: " + path));
			defaultHandlers.put(Event.FILE_NAME_CHANGED, (name) -> System.out.println("File name changed: " + name));
			defaultHandlers.put(Event.SEARCH_KEYS, (key) -> System.out.println("Searching keys: " + key));
			defaultHandlers.put(Event.CONFIG_BACKUP_SUCCESS, (name) -> System.out.println("Backed up config: " + name));
			defaultHandlers.put(Event.CONFIG_BACKUP_FAIL, (IOException exception) -> System.err.println("Failed to backup config: " + exception.getMessage()));
			defaultHandlers.put(Event.CONFIG_FILE_DELETED, (path) -> System.out.println("Config file deleted: " + path));
			defaultHandlers.put(Event.CONFIG_FILE_DELETE_FAILED, (path) -> System.err.println("Failed to delete config file: " + path));
			defaultHandlers.put(Event.CONFIG_FILE_MISSING, (path) -> System.err.println("Config file doesn't exist: " + path));
		}
		
		/**
		 * Registers a custom event handler for an event with a single parameter.
		 *
		 * @param event        the event to register the handler for
		 * @param eventHandler the event handler to handle the event
		 * @param <T>          the type of the parameter for the event
		 */
		public static <T> void registerEvent(Event event, EventHandler<T> eventHandler) {
			registeredHandlers.put(event, eventHandler);
		}
		
		/**
		 * Triggers an event with a single parameter and calls the corresponding event handler.
		 *
		 * @param event     the event to trigger
		 * @param parameter the parameter to pass to the event handler
		 * @param <T>       the type of the parameter
		 */
		private <T> void triggerEvent(Event event, T parameter) {
			if (!enableEvents) {
				if (registeredHandlers.containsKey(event)) {
					EventHandler<T> handler = (EventHandler<T>) registeredHandlers.get(event);
					if (handler != null) {
						handler.accept(parameter);
					}
				}
				return;
			}
			
			if (registeredHandlers.containsKey(event)) {
				EventHandler<T> handler = (EventHandler<T>) registeredHandlers.get(event);
				if (handler != null) {
					handler.accept(parameter);
				}
			} else if (defaultHandlers.containsKey(event)) {
				EventHandler<T> handler = (EventHandler<T>) defaultHandlers.get(event);
				if (handler != null) {
					handler.accept(parameter);
				}
			} else {
				System.out.println("No handler for event: " + event);
			}
		}
	}
	
	private static final class CommentedProperties extends Properties {
		
		// Stores comments associated with keys and sections
		private final LinkedHashMap<String, String> comments = new LinkedHashMap<>();
		
		/**
		 * Adds a key-value pair to the properties with an optional comment.
		 * If the key is being added for the first time, a section header comment is created for its section.
		 *
		 * @param key     the property key
		 * @param value   the property value
		 * @param comment an optional comment for the key
		 */
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
		
		/**
		 * Extracts the section name from a given key. If the key contains a period ('.'),
		 * the substring before the first period is used as the section name. Otherwise, "default" is returned.
		 *
		 * @param key the property key
		 * @return the section name
		 */
		private String getSection(String key) {
			return key.contains(".") ? key.split("\\.")[0] : "default";
		}
		
		/**
		 * Capitalizes the first letter of the given text.
		 *
		 * @param text the text to capitalize
		 * @return the capitalized text
		 */
		private String capitalize(String text) {
			return text.substring(0, 1).toUpperCase() + text.substring(1);
		}
		
		/**
		 * Stores the properties and their associated comments to an output stream.
		 * The properties are organized by sections, with comments and a timestamp header included.
		 *
		 * @param out    the output stream to write to
		 * @param header an optional header to include in the output
		 * @throws IOException if an I/O error occurs
		 */
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
		
		/**
		 * Organizes property keys into sections based on their names.
		 * Each section groups keys that share the same section name (derived from the key's prefix).
		 *
		 * @return a map where the keys are section names and the values are lists of keys in each section
		 */
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
