package org.example;

public class Main {
	public static void main(String[] args) {
		// Specify the configuration file name
		String configFileName = "test_config.properties";
		
		// Create a Config object with file name and useAppDir set to false
		Config config = new Config(configFileName, false);
		
		// Set some properties with comments
		config.setProperty("app.name", "TestApp", "The name of the application");
		config.setProperty("app.version", "1.0.0", "The version of the application");
		config.setProperty("user.language", "en", "Default language for the application");
		config.setProperty("feature.enabled", "true", "Whether the new feature is enabled");
		
		// Retrieve properties
		System.out.println("App Name: " + config.getProperty("app.name", "DefaultApp"));
		System.out.println("App Version: " + config.getProperty("app.version", "0.0.0"));
		System.out.println("User Language: " + config.getProperty("user.language", "en"));
		System.out.println("Feature Enabled: " + config.getProperty("feature.enabled", "false"));
		
		// Retrieve a localized message
		System.out.println(
				"Localized Message: " + config.getLocalizedMessage("user.greeting", "Welcome to the application!"));
		
		// Update a property
		config.setProperty("user.language", "fr", "Changed default language");
		
		// Retrieve the updated property
		System.out.println("Updated User Language: " + config.getProperty("user.language", "en"));
		
		// Save the properties to the file with a header
		config.saveProperties("Test Configuration for Config Class");
		
		System.out.println("Properties saved successfully.");
	}
}