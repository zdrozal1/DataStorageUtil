package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Utility class for managing configuration and localization files.
 * <p>
 * Supports properties with optional comments for localization purposes.
 */
public class Config {
	private final String fileName;
	private final boolean useAppDir;
	private final CommentedProperties properties = new CommentedProperties();
	
	public Config(String fileName, boolean useAppDir) {
		this.fileName = fileName;
		this.useAppDir = useAppDir;
		loadProperties();
	}
	
	private String getAppPath() {
		try {
			String jarPath = Config.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			return new File(jarPath).getParent();
		} catch (Exception e) {
			System.err.println("Error determining application path: " + e.getMessage());
			return null;
		}
	}
	
	private File getConfigFile() {
		if (useAppDir) {
			return new File(getAppPath() + File.separator + fileName);
		}
		return new File(fileName);
	}
	
	private void loadProperties() {
		File configFile = getConfigFile();
		if (configFile.exists()) {
			try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile),
			                                                      StandardCharsets.UTF_8)) {
				properties.load(reader);
			} catch (IOException e) {
				System.err.println("Unable to load properties from " + configFile.getAbsolutePath() + ": " + e.getMessage());
			}
		}
	}
	
	public void saveProperties(String header) {
		try (OutputStream output = new FileOutputStream(getConfigFile())) {
			properties.storeWithComments(output, header);
		} catch (IOException e) {
			System.err.println("Error saving properties to file: " + e.getMessage());
		}
	}
	
	public void setProperty(String key, String value, String comment) {
		properties.putWithComment(key, value, comment);
		saveProperties("Configuration and Localization");
	}
	
	public String getProperty(String key, String defaultValue) {
		String value = properties.getProperty(key);
		if (value == null) {
			properties.put(key, defaultValue);
			saveProperties("Configuration and Localization");
			return defaultValue;
		}
		return value;
	}
	
	public synchronized String getLocalizedMessage(String key, String defaultValue) {
		return getProperty(key, defaultValue);
	}
	
	public void resetProperties() {
		properties.clear();
		saveProperties("Reset Configuration");
	}
	
	public boolean removeProperty(String key) {
		if (properties.remove(key) != null) {
			saveProperties("Configuration Updated");
			return true;
		}
		return false;
	}
	
	public boolean hasProperty(String key) {
		return properties.containsKey(key);
	}
	
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
	
	public Map<String, String> exportAsMap() {
		Map<String, String> map = new HashMap<>();
		for (String key : properties.stringPropertyNames()) {
			map.put(key, properties.getProperty(key));
		}
		return map;
	}
	
	public void loadFromInputStream(InputStream inputStream) {
		try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
			properties.load(reader);
			saveProperties("Configuration Updated from Stream");
		} catch (IOException e) {
			System.err.println("Error loading properties from InputStream: " + e.getMessage());
		}
	}
	
	// CommentedProperties inner class for handling comments and sections
	private static class CommentedProperties extends Properties {
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
