package org.agmip.translators.stics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Properties;

public class SticsUtil {

	/**
	 * Return a string containing the data file
	 * 
	 * @param file
	 * @return a string containing the data file
	 */
	public static String getDataFromTestFile(String file) {
		InputStream inputStream = ModelOutput.class.getResourceAsStream(file);
		StringBuffer strBuffer = new StringBuffer();
		if (inputStream != null) {
			BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));
			try {
				while (buffer.ready()) {
					strBuffer.append(buffer.readLine());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return strBuffer.toString();
	}

	/**
	 * Parse the properties file and returns a Map filled with properties
	 * Property with the same value can have the following format :
	 * prop1,prop2=defaultvalue
	 * @return
	 * @throws IOException
	 */
	public static LinkedHashMap<String, String> getDefaultValues() throws IOException{
		InputStream inputStream = ModelOutput.class.getResourceAsStream("/default-stics-values.properties");
		LinkedHashMap<String, String> defaultProperties;
		Properties properties;
		defaultProperties = new LinkedHashMap<String, String>();
		properties = new Properties();
		properties.load(inputStream);
		for(Object property : properties.keySet()){
			String defaultValue = properties.getProperty((String) property);
			String[] subListOfProperties = ((String)property).split(",");
			for(String subProperty : subListOfProperties){
				defaultProperties.put(subProperty, defaultValue);
			}
		}
		return defaultProperties;
	}
}
