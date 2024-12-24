package org.zain;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/// Config Utility Class To Create Dynamic Config Files
public class Config {
	private final boolean useAppDir;
	private final CommentedProperties properties = new CommentedProperties();
	private String fileName;
	private boolean enableEvents = false;
	private boolean isConfigLoadedSet = false;
	private boolean isConfigNotLoadedSet = false;
	private boolean isPropertyCreatedSet = false;
	private boolean isConfigMigratedSet = false;
	private boolean isConfigReloadedSet = false;
	private boolean isConfigResetSet = false;
	private boolean isConfigRestoredSet = false;
	private boolean isDefaultConfigLoadedSet = false;
	private boolean isFileNameChangedSet = false;
	private boolean arePropertiesImportedSet = false;
	private boolean isPropertyRemovedSet = false;
	private boolean isPropertyNotRemovedSet = false;
	private boolean isSectionClearedSet = false;
	private boolean isSearchKeysSet = false;
	private boolean isConfigBackupSuccessSet = false;
	private boolean isConfigBackupFailSet = false;
	private boolean isConfigFileDeletedSet = false;
	private boolean isConfigFileDeleteFailedSet = false;
	private boolean isConfigFileMissingSet = false;
	private boolean isConfigLoadedPartiallySet = false;
	private LoadConfigEventFail onConfigNotLoaded = (IOException exception) -> System.err.println(
			"Error loading config at: " + this.getAbsolutePath() + ": " + exception.getMessage());
	private LoadConfigEventPass onConfigLoaded = (String path) -> System.out.println(
			"Config has been created at: " + path);
	private ReloadConfigEvent onConfigReloaded = () -> System.out.println("Config has been reloaded");
	private PropertyEvent onPropertyCreated = (key, value) -> System.out.println(
			"Property: '" + key + "', Value: '" + value + "' has been added");
	private PropertyEvent onPropertyRemoved = (key, value) -> System.out.println(
			"Property: '" + key + "' has been removed");
	private PropertyEvent onPropertyNotRemoved = (key, value) -> System.err.println(
			"Property: '" + key + "' could not be removed!");
	private ClearSectionEvent onSectionCleared = (String section) -> System.out.println("Cleared section: " + section);
	private RestoreConfigEvent onConfigRestored = (String path) -> System.out.println("Config restored from: " + path);
	private MigrateConfigEvent onConfigMigrated = (String path) -> System.out.println("Migrated to new file: " + path);
	private ImportedPropertiesEvent onPropertiesImported = () -> System.out.println("Properties Imported.");
	private ResetConfigEvent onConfigReset = () -> System.out.println("Config has been reset.");
	private LoadedDefaultConfigEvent onDefaultConfigLoaded = (String path) -> System.out.println(
			"Loaded default properties from: " + path);
	private FileNameChangeEvent onFileNameChanged = (String name) -> System.out.println("File name changed: " + name);
	private SearchKeyEvent onSearchKeys = (String key) -> System.out.println("Searching keys: " + key);
	private ConfigBackupSuccessEvent onConfigBackupSuccess = (String name) -> System.out.println(
			"Backed up config: " + name);
	private ConfigBackupFailEvent onConfigBackupFail = (IOException exception) -> System.err.println(
			"Failed to backup config: " + exception.getMessage());
	private ConfigFileDeletedEvent onConfigFileDeleted = (String path) -> System.out.println(
			"Config file deleted: " + path);
	private ConfigFileDeleteFailedEvent onConfigFileDeleteFailed = (String path) -> System.err.println(
			"Failed to delete config file: " + path);
	private ConfigFileMissingEvent onConfigFileMissing = (String path) -> System.err.println(
			"Config file doesn't exist: " + path);
	private ConfigLoadedPartiallyEvent onConfigLoadedPartially = (int loadedProperties, int totalProperties) -> System.out.println(
			"Partially loaded config: " + loadedProperties + "/" + totalProperties + " properties");
	
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
			properties.storeWithComments(output, header);
		} catch (IOException e) {
			System.err.println("Error saving properties to file: " + e.getMessage());
		}
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
		
		if (enableEvents || isSearchKeysSet) {
			onSearchKeys.onEvent(searchQuery);
		}
		
		return properties.stringPropertyNames().stream().filter(key -> key.contains(finalSearchQuery)).collect(
				Collectors.toList());
	}
	
	/**
	 * Clears all properties within a specific section.
	 *
	 * @param section the section name to clear
	 */
	public void clearSection(String section) {
		properties.stringPropertyNames().stream().filter(key -> key.startsWith(section + ".")).toList().forEach(key -> {
			properties.remove(key);
			removeProperty(key);
		});
		saveProperties("Cleared Section: " + section);
		if (enableEvents || isSectionClearedSet) {
			onSectionCleared.onEvent(section);
		}
	}
	
	/**
	 * Sets whether events will be enabled for Config object
	 *
	 * @param enableEvents whether events will be enabled
	 */
	public void setEnableEvents(boolean enableEvents) {
		this.enableEvents = enableEvents;
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
			if (enableEvents || isConfigMigratedSet) {
				onConfigMigrated.onEvent(newFileName);
			}
		}
		this.fileName = newFileName;
		if (enableEvents || isFileNameChangedSet) {
			onFileNameChanged.onEvent(newFileName);
		}
	}
	
	/**
	 * Loads properties from the configuration file into memory, if the file exists.
	 */
	public void loadProperties() {
		File configFile = getConfigFile();
		if (configFile.exists()) {
			try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile),
			                                                      StandardCharsets.UTF_8)) {
				Properties tempProperties = new Properties();
				tempProperties.load(reader);
				int loaded = tempProperties.size();
				int total = properties.stringPropertyNames().size();
				properties.putAll(tempProperties);
				if (loaded < total) {
					if (enableEvents || isConfigLoadedPartiallySet) {
						onConfigLoadedPartially.onEvent(loaded, total);
					}
				} else if (enableEvents || isConfigLoadedSet) {
					onConfigLoaded.onPass(configFile.getAbsolutePath());
				}
			} catch (IOException e) {
				if (enableEvents || isConfigNotLoadedSet) {
					onConfigNotLoaded.onFail(e);
				}
			}
		} else if (enableEvents || isConfigFileMissingSet) {
			onConfigFileMissing.onEvent(configFile.getAbsolutePath());
		}
	}
	
	/**
	 * Deletes the current config file
	 */
	public void deleteConfigFile() {
		File configFile = getConfigFile();
		if (configFile.exists() && configFile.delete()) {
			if (enableEvents || isConfigFileDeletedSet) {
				onConfigFileDeleted.onEvent(configFile.getAbsolutePath());
			}
		} else {
			if (enableEvents || isConfigFileDeleteFailedSet) {
				onConfigFileDeleteFailed.onEvent(configFile.getAbsolutePath());
			}
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
		if (enableEvents || isConfigReloadedSet) {
			onConfigReloaded.onEvent();
		}
	}
	
	/**
	 * Clears all properties and saves an empty configuration file with a reset message.
	 */
	public void resetConfiguration() {
		properties.clear();
		saveProperties("Reset Configuration");
		if (enableEvents || isConfigResetSet) {
			onConfigReset.onEvent();
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
		if (enableEvents || isPropertyCreatedSet) {
			onPropertyCreated.onEvent(key, value);
		}
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
			if (enableEvents || isPropertyCreatedSet) {
				onPropertyCreated.onEvent(key, defaultValue);
			}
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
	 */
	public boolean removeProperty(String key) {
		key = key.replaceAll(" ", "_");
		boolean removed = properties.remove(key) != null;
		if (removed) {
			if (enableEvents || isPropertyRemovedSet) {
				onPropertyRemoved.onEvent(key, properties.getProperty(key));
			}
			saveProperties("Configuration Updated");
		} else {
			if (enableEvents || isPropertyNotRemovedSet) {
				onPropertyNotRemoved.onEvent(key, properties.getProperty(key));
			}
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
				if (enableEvents || isDefaultConfigLoadedSet) {
					onDefaultConfigLoaded.onEvent(defaultFileName);
				}
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
			return new File(
					Config.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
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
			if (enableEvents || isConfigBackupSuccessSet) {
				onConfigBackupSuccess.onSuccess(backupFileName);
			}
		} catch (IOException e) {
			if (enableEvents || isConfigBackupFailSet) {
				onConfigBackupFail.onFail(e);
			}
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
			if (enableEvents || isConfigRestoredSet) {
				onConfigRestored.onEvent(backupFileName);
			}
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
			if (enableEvents || isPropertyCreatedSet) {
				onPropertyCreated.onEvent(entry.getKey(), entry.getValue());
			}
		}
		saveProperties("Imported Properties");
		if (enableEvents || arePropertiesImportedSet) {
			onPropertiesImported.onEvent();
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
	 * Sets the event handler for when the configuration is successfully loaded.
	 *
	 * @param onConfigLoaded the event handler to be triggered when the config is loaded
	 */
	public void setOnConfigLoaded(LoadConfigEventPass onConfigLoaded) {
		this.onConfigLoaded = onConfigLoaded;
		isConfigLoadedSet = true;
	}
	
	/**
	 * Sets the event handler for when the configuration fails to load.
	 *
	 * @param onConfigNotLoaded the event handler to be triggered when the config fails to load
	 */
	public void setOnConfigNotLoaded(LoadConfigEventFail onConfigNotLoaded) {
		this.onConfigNotLoaded = onConfigNotLoaded;
		isConfigNotLoadedSet = true;
	}
	
	/**
	 * Sets the event handler for when a property is created.
	 *
	 * @param onPropertyCreated the event handler to be triggered when a property is created
	 */
	public void setOnPropertyCreated(PropertyEvent onPropertyCreated) {
		this.onPropertyCreated = onPropertyCreated;
		isPropertyCreatedSet = true;
	}
	
	/**
	 * Sets the event handler for when the configuration is migrated to a new file.
	 *
	 * @param onConfigMigrated the event handler to be triggered when the config is migrated
	 */
	public void setOnConfigMigrated(MigrateConfigEvent onConfigMigrated) {
		this.onConfigMigrated = onConfigMigrated;
		isConfigMigratedSet = true;
	}
	
	/**
	 * Sets the event handler for when the configuration is reloaded.
	 *
	 * @param onConfigReloaded the event handler to be triggered when the config is reloaded
	 */
	public void setOnConfigReloaded(ReloadConfigEvent onConfigReloaded) {
		this.onConfigReloaded = onConfigReloaded;
		isConfigReloadedSet = true;
	}
	
	/**
	 * Sets the event handler for when the configuration is reset.
	 *
	 * @param onConfigReset the event handler to be triggered when the config is reset
	 */
	public void setOnConfigReset(ResetConfigEvent onConfigReset) {
		this.onConfigReset = onConfigReset;
		isConfigResetSet = true;
	}
	
	/**
	 * Sets the event handler for when the configuration is restored from a backup.
	 *
	 * @param onConfigRestored the event handler to be triggered when the config is restored
	 */
	public void setOnConfigRestored(RestoreConfigEvent onConfigRestored) {
		this.onConfigRestored = onConfigRestored;
		isConfigRestoredSet = true;
	}
	
	/**
	 * Sets the event handler for when the default configuration is loaded.
	 *
	 * @param onDefaultConfigLoaded the event handler to be triggered when the default config is loaded
	 */
	public void setOnDefaultConfigLoaded(LoadedDefaultConfigEvent onDefaultConfigLoaded) {
		this.onDefaultConfigLoaded = onDefaultConfigLoaded;
		isDefaultConfigLoadedSet = true;
	}
	
	/**
	 * Sets the event handler for when the file name is changed.
	 *
	 * @param onFileNameChanged the event handler to be triggered when the file name is changed
	 */
	public void setOnFileNameChanged(FileNameChangeEvent onFileNameChanged) {
		this.onFileNameChanged = onFileNameChanged;
		isFileNameChangedSet = true;
	}
	
	/**
	 * Sets the event handler for when properties are imported.
	 *
	 * @param onPropertiesImported the event handler to be triggered when properties are imported
	 */
	public void setOnPropertiesImported(ImportedPropertiesEvent onPropertiesImported) {
		this.onPropertiesImported = onPropertiesImported;
		arePropertiesImportedSet = true;
	}
	
	/**
	 * Sets the event handler for when a property is removed.
	 *
	 * @param onPropertyRemoved the event handler to be triggered when a property is removed
	 */
	public void setOnPropertyRemoved(PropertyEvent onPropertyRemoved) {
		this.onPropertyRemoved = onPropertyRemoved;
		isPropertyRemovedSet = true;
	}
	
	/**
	 * Sets the event handler for when a property fails to be removed.
	 *
	 * @param onPropertyNotRemoved the event handler to be triggered when a property fails to be removed
	 */
	public void setOnPropertyNotRemoved(PropertyEvent onPropertyNotRemoved) {
		this.onPropertyNotRemoved = onPropertyNotRemoved;
		isPropertyNotRemovedSet = true;
	}
	
	/**
	 * Sets the event handler for when a section of the configuration is cleared.
	 *
	 * @param onSectionCleared the event handler to be triggered when a section is cleared
	 */
	public void setOnSectionCleared(ClearSectionEvent onSectionCleared) {
		this.onSectionCleared = onSectionCleared;
		isSectionClearedSet = true;
	}
	
	/**
	 * Sets the event handler for when a search key is used.
	 *
	 * @param onSearchKeys the event handler to be triggered when a search key is used
	 */
	public void setOnSearchKeys(SearchKeyEvent onSearchKeys) {
		this.onSearchKeys = onSearchKeys;
		isSearchKeysSet = true;
	}
	
	/**
	 * Sets the event handler for when the configuration backup is successful.
	 *
	 * @param path the event handler to be triggered when the config backup is successful
	 */
	public void setOnConfigBackupSuccess(ConfigBackupSuccessEvent path) {
		this.onConfigBackupSuccess = path;
		isConfigBackupSuccessSet = true;
	}
	
	/**
	 * Sets the event handler for when the configuration backup fails.
	 *
	 * @param exception the event handler to be triggered when the config backup fails
	 */
	public void setOnConfigBackupFail(ConfigBackupFailEvent exception) {
		this.onConfigBackupFail = exception;
		isConfigBackupFailSet = true;
	}
	
	/**
	 * Sets the event handler for when the configuration file is deleted.
	 *
	 * @param path the event handler to be triggered when the config file is deleted
	 */
	public void setOnConfigFileDeleted(ConfigFileDeletedEvent path) {
		this.onConfigFileDeleted = path;
		isConfigFileDeletedSet = true;
	}
	
	/**
	 * Sets the event handler for when the configuration file deletion fails.
	 *
	 * @param path the event handler to be triggered when the config file deletion fails
	 */
	public void setOnConfigFileDeleteFailed(ConfigFileDeleteFailedEvent path) {
		this.onConfigFileDeleteFailed = path;
		isConfigFileDeleteFailedSet = true;
	}
	
	/**
	 * Sets the event handler for when the configuration file is missing.
	 *
	 * @param path the event handler to be triggered when the config file is missing
	 */
	public void setOnConfigFileMissing(ConfigFileMissingEvent path) {
		this.onConfigFileMissing = path;
		isConfigFileMissingSet = true;
	}
	
	/**
	 * Sets the event handler for when the configuration is partially loaded.
	 *
	 * @param path the event handler to be triggered when the config is partially loaded
	 */
	public void setOnConfigLoadedPartially(ConfigLoadedPartiallyEvent path) {
		this.onConfigLoadedPartially = path;
		isConfigLoadedPartiallySet = true;
	}
	
	@FunctionalInterface
	public interface LoadConfigEventFail {
		void onFail(IOException e);
	}
	
	@FunctionalInterface
	public interface LoadConfigEventPass {
		void onPass(String path);
	}
	
	@FunctionalInterface
	public interface ReloadConfigEvent {
		void onEvent();
	}
	
	@FunctionalInterface
	public interface PropertyEvent {
		void onEvent(String key, String value);
	}
	
	@FunctionalInterface
	public interface ClearSectionEvent {
		void onEvent(String clearedSection);
	}
	
	@FunctionalInterface
	public interface RestoreConfigEvent {
		void onEvent(String restorePath);
	}
	
	@FunctionalInterface
	public interface MigrateConfigEvent {
		void onEvent(String migratedPath);
	}
	
	@FunctionalInterface
	public interface ImportedPropertiesEvent {
		void onEvent();
	}
	
	@FunctionalInterface
	public interface ResetConfigEvent {
		void onEvent();
	}
	
	@FunctionalInterface
	public interface LoadedDefaultConfigEvent {
		void onEvent(String loadedDefaultPath);
	}
	
	@FunctionalInterface
	public interface FileNameChangeEvent {
		void onEvent(String newFile);
	}
	
	@FunctionalInterface
	public interface SearchKeyEvent {
		void onEvent(String searchKey);
	}
	
	@FunctionalInterface
	public interface ConfigBackupSuccessEvent {
		void onSuccess(String backupFileName);
	}
	
	@FunctionalInterface
	public interface ConfigBackupFailEvent {
		void onFail(IOException exception);
	}
	
	@FunctionalInterface
	public interface ConfigFileDeletedEvent {
		void onEvent(String path);
	}
	
	@FunctionalInterface
	public interface ConfigFileDeleteFailedEvent {
		void onEvent(String path);
	}
	
	@FunctionalInterface
	public interface ConfigFileMissingEvent {
		void onEvent(String path);
	}
	
	@FunctionalInterface
	public interface ConfigLoadedPartiallyEvent {
		void onEvent(int loadedProperties, int totalProperties);
	}
	
	/**
	 * Inner class for handling properties with comments and organizing them into sections.
	 * This class extends {@link Properties} and adds support for associating comments with keys
	 * and grouping keys by sections. It provides functionality to store properties along with
	 * their comments in a structured format.
	 */
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
