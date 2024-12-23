package org.zain;

public class Testing {
	public static void main(String[] args) {
		String configFileName = "configuration/test_config.conf";
		String localeFileName = "locale/locale.conf";
		
		Config config = new Config(configFileName, true);
		Locale locale = new Locale(localeFileName, true);
		
		config.setProperty("Application Settings.name", "TestApp", "The name of the application");
		config.setProperty("Application Settings.version", "1.0.0", "The version of the application");
		config.setProperty("User Settings.language", "en", "Default language for the application");
		config.setProperty("Feature Settings.enabled", "true", "Whether the new feature is enabled");
		config.setProperty("Feature Settings.testFeature1,5", "false", null);
		config.setProperty("Feature Settings.testFeature2", "true", null);
		
		System.out.println("Test Property: " + config.getProperty("Test Settings.testOne", "blah blah value"));
		
		System.out.println("User settings search: " + config.searchKeys("User Settings"));
		
		System.out.println(
				"Localized Message: " + locale.getLocalization("user.greeting", "Welcome to the application!"));
		System.out.println("Doesnt exist: " + locale.getLocalization("test.value", "This is the text"));
		
		locale.backupConfiguration("locale/backupLocale.conf", true);
	}
}