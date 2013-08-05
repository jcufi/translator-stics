package org.agmip.translators.stics.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.agmip.util.JSONAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SticsUtil {

	private SticsUtil() {
		// utility class hide constructor
	}

	private static final String UNKNOWN_DEFAULT_VALUE = "unknown-default-value";
	private static final Logger log = LoggerFactory.getLogger(SticsUtil.class);
	private static HashMap<String, String> defaultvalues;
	static {
		try {
			defaultvalues = SticsUtil.getDefaultValues();
		} catch (IOException e) {
			log.error("Default values cannot be loaded");
			log.error(e.toString());
		}
	}

	public static void createDefaultListOfMap(List<HashMap<String, String>> list, String[] params, int size) {
		if (list.size() < size) {
			for (int i = list.size(); i < size; i++) {
				list.add(createDefaultMap(params));
			}
		}
	}

	/**
	 * Create a new map filled with default values for parameters set as input.
	 * 
	 * @param keys parameters to fill
	 * @return the new map
	 */
	public static HashMap<String, String> createDefaultMap(String[] keys) {
		HashMap<String, String> defaultMap = new HashMap<String, String>();
		for (String k : keys) {
			defaultMap.put(k, defaultValue(k));
		}
		return defaultMap;
	}

	/**
	 * Create a new list of map filled with default values for parameters set as input
	 * 
	 * @param keys parameters to fill
	 * @param listSize size
	 * @return the new list
	 */
	public static ArrayList<HashMap<String, String>> createDefaultListOfMap(String[] keys, int listSize) {
		ArrayList<HashMap<String, String>> defaultList = new ArrayList<HashMap<String, String>>();
		for (int i = 0; i < listSize; i++) {
			defaultList.add(createDefaultMap(keys));
		}
		return defaultList;
	}

	public static File newFile(String content, String filePath, String fileName) throws IOException {
		File currentFile;
		FileWriter fileWriter;
		currentFile = new File(filePath + File.separator + fileName);
		fileWriter = new FileWriter(currentFile);
		BufferedWriter writer = new BufferedWriter(fileWriter);
		writer.write(content);
		writer.close();
		fileWriter.close();
		return currentFile;
	}

	/**
	 * Return a string containing the data file
	 * 
	 * @param file
	 * @return a string containing the data file
	 */
	public static String getDataFromTestFile(String file) {
		InputStreamReader inputStreamReader;
		BufferedReader buffer;
		InputStream inputStream = SticsUtil.class.getResourceAsStream(file);
		inputStreamReader = null;
		StringBuffer strBuffer = new StringBuffer();
		if (inputStream != null) {
			try {
				inputStreamReader = new InputStreamReader(inputStream);
				buffer = new BufferedReader(inputStreamReader);
				while (buffer.ready()) {
					strBuffer.append(buffer.readLine());
				}
			} catch (IOException e) {
				log.error("IO Error", e);
			} finally {
				if (inputStreamReader != null) {
					try {
						inputStreamReader.close();
					} catch (IOException e) {
						log.error("IO Error", e);
					}
				}
			}
		}
		return strBuffer.toString();
	}

	public static String defaultValue(String key) {
		String value;
		// log.debug("Default value for "+key);
		if (defaultvalues.containsKey(key)) {
			value = defaultvalues.get(key);
		} else {
			value = UNKNOWN_DEFAULT_VALUE;
		}
		Report.addParamInfo(key, value);
		return value;
	}

	public static boolean isDefaultValue(String paramName, String value) {
		return value.equals(defaultvalues.get(paramName));
	}

	/**
	 * Parse the properties file and returns a Map filled with properties Property with the same value can have the
	 * following format : prop1,prop2=defaultvalue
	 * 
	 * @return
	 * @throws IOException
	 */
	public static HashMap<String, String> getDefaultValues() throws IOException {
		InputStream inputStream = SticsUtil.class.getResourceAsStream("/default-stics-values.properties");
		HashMap<String, String> defaultProperties;
		Properties properties;
		defaultProperties = new HashMap<String, String>();
		properties = new Properties();
		try {
			properties.load(inputStream);
		} catch (IOException e) {
			throw e;
		}finally{
			if(inputStream != null){
				inputStream.close();
			}
		}
		for (Object property : properties.keySet()) {
			String defaultValue = properties.getProperty((String) property);
			String[] subListOfProperties = ((String) property).split(",");
			for (String subProperty : subListOfProperties) {
				defaultProperties.put(subProperty, defaultValue);
			}
		}
		return defaultProperties;
	}

	public static void populate(String[] defaultParam, List<HashMap<String, String>> list) {
		if (list.size() == 0) {
			list.add(SticsUtil.createDefaultMap(defaultParam));
		} else {
			// Check for each map in the list if the keys doesn't exist put the key and the default value
			SticsUtil.defaultValueForList(defaultParam, list);
		}
	}

	public static void defaultValueForList(String[] params, List<HashMap<String, String>> values) {
		for (HashMap<String, String> e : values) {
			defaultValueForMap(params, e);
		}
	}

	/**
	 * If the map doesn't contain the parameter name add the parameter in the map with a default value.
	 * 
	 * @param params an array of parameter's name
	 * @param values the map
	 */
	public static void defaultValueForMap(String[] params, Map<String, String> values) {
		for (String param : params) {
			if (!values.containsKey(param)) {
				values.put(param, defaultValue(param));
			}
		}
	}

	/**
	 * Remove forbidden characters from the weather id
	 * 
	 * @param wstId The weather identifier
	 * @return A clean weather identifier
	 */
	public static String toWeatherId(String wstId) {
		String stationId;
		// Trick for stics , not allowed in station name
		stationId = wstId.replaceAll(",", "_");
		return stationId;
	}

	/**
	 * Convert date into a julian day
	 * 
	 * @param date
	 * @return a julian day
	 */
	public static int getJulianDay(Date date) {
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		return calendar.get(GregorianCalendar.DAY_OF_YEAR);
	}

	public static Map getDataFrom(String jsonFile) {
		Map data;
		data = null;
		try {
			data = JSONAdapter.fromJSON(getDataFromTestFile(jsonFile));
		} catch (IOException e) {
			log.info("Unable to read test data, " + jsonFile);
			log.error(e.toString());
		}
		return data;
	}

}
