package org.zain.DataStorageUtil.Config;

/// Class to create a Config object for localization
public class Locale extends Config {

	/**
	 * Initializes the Localization object with the specified file name and directory usage flag.
	 *
	 * @param fileName  the name of the configuration file
	 * @param useAppDir if true, the configuration file is stored in the application directory
	 */
	public Locale(String fileName, boolean useAppDir) {
		super(fileName, useAppDir);
	}

	public String getLocalization(String key, String defaultValue) {
		return super.getProperty(key, defaultValue);
	}

	public void setLocalization(String key, String defaultValue) {
		super.setProperty(key, defaultValue, null);
	}
}
