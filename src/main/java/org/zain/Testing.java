package org.zain;

public class Testing {
	public static void main(String[] args) {
		String localeFileName = "locale/locale.conf";
		
		Locale locale = new Locale(localeFileName, true);
		locale.setEnableEvents(true);
		locale.setOnConfigLoaded(path -> System.out.println("loaded config!"));
		locale.loadProperties();
		
		locale.setProperty("User Defaults.greeting", "Hello World", "Hello world locale sentence");
		locale.setProperty("User Defaults.goodbye", "goodbye", null);
		
		System.out.println("Test Property: " + locale.getLocalization("Test Settings.testOne", "Test Value 1"));
		
		locale.removeProperty("User Defaults.greeting");
		
		locale.backupConfiguration("locale/backupLocale.conf", true);
	}
}