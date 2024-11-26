package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/// Config Utility Class To Create Dynamic Config Files
public class Config {
	private final String fileName;
	private final boolean useAppDir;
	private final CommentedProperties properties = new CommentedProperties();
	
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
	 * Determines the application's directory path based on the location of the executing JAR file.
	 *
	 * @return the absolute path to the application's directory, or null if the path cannot be determined
	 */
	private String getAppPath() {
		try {
			String jarPath = Config.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			return new File(jarPath).getParent();
		} catch (Exception e) {
			System.err.println("Error determining application path: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Retrieves the configuration file object based on the application's directory or a specified file path.
	 *
	 * @return a File object representing the configuration file
	 */
	private File getConfigFile() {
		if (useAppDir) {
			return new File(getAppPath() + File.separator + fileName);
		}
		return new File(fileName);
	}
	
	/**
	 * Loads properties from the configuration file into memory.
	 * If the file does not exist, no properties are loaded.
	 */
	private void loadProperties() {
		File configFile = getConfigFile();
		if (configFile.exists()) {
			try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile),
			                                                      StandardCharsets.UTF_8)) {
				properties.load(reader);
			} catch (IOException e) {
				System.err.println(
						"Unable to load properties from " + configFile.getAbsolutePath() + ": " + e.getMessage());
			}
		}
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
	 * Removes a property from the configuration and saves the updated configuration.
	 *
	 * @param key the key of the property to remove
	 * @return true if the property was removed, false if it did not exist
	 */
	public boolean removeProperty(String key) {
		if (properties.remove(key) != null) {
			saveProperties("Configuration Updated");
			return true;
		}
		return false;
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
	
	/**
	 * Loads properties from an InputStream and saves the updated configuration.
	 *
	 * @param inputStream the InputStream containing properties data
	 */
	public void loadFromInputStream(InputStream inputStream) {
		try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
			properties.load(reader);
			saveProperties("Configuration Updated from Stream");
		} catch (IOException e) {
			System.err.println("Error loading properties from InputStream: " + e.getMessage());
		}
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
				}
				writer.println();
				
				Map<String, List<String>> sections = organizeBySections();
				for (Map.Entry<String, List<String>> entry : sections.entrySet()) {
					String section = entry.getKey();
					if (comments.containsKey(section)) {
						writer.println();
						writer.println(comments.get(section));
					}
					for (String key : entry.getValue()) {
						if (comments.containsKey(key)) {
							writer.println(comments.get(key));
						}
						writer.println(key + "=" + getProperty(key));
					}
				}
			}
		}
		
		private Map<String, List<String>> organizeBySections() {
			Map<String, List<String>> sections = new TreeMap<>();
			for (String key : stringPropertyNames()) {
				String section = getSection(key);
				sections.computeIfAbsent(section, k -> new ArrayList<>()).add(key);
			}
			return sections;
		}
	}
}
