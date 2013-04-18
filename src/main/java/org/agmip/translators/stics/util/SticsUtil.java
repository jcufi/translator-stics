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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.agmip.translators.stics.SticsOutput;
import org.agmip.translators.stics.WeatherOutput;
import org.agmip.util.JSONAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SticsUtil {
	private static String UNKNOWN_DEFAULT_VALUE = "unknown-default-value";
	private static final Logger log = LoggerFactory.getLogger(SticsUtil.class);
	public static HashMap<String, String> defaultvalues;
	static {
		try {
			defaultvalues = SticsUtil.getDefaultValues();
		} catch (IOException e) {
			log.error("Default values cannot be loaded");
			log.error(e.toString());
		}
	}
	
	public static void fill(ArrayList<HashMap<String, String>> list, String[] params, int size){
		if(list.size() < size){
			for(int i=list.size(); i < size; i++){
				list.add(createDefaultMap(params));
			}
		}
	}
	

	public static HashMap<String, String> createDefaultMap(String[] keys) {
		HashMap<String, String> defaultMap = new HashMap<String, String>();
		for (String k : keys) {
			defaultMap.put(k, defaultValue(k));
		}
		return defaultMap;
	}

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
		InputStream inputStream = SticsOutput.class.getResourceAsStream(file);
		StringBuffer strBuffer = new StringBuffer();
		if (inputStream != null) {
			BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));
			try {
				while (buffer.ready()) {
					strBuffer.append(buffer.readLine());
				}
			} catch (IOException e) {
				log.error(e.toString());
			}
		}
		return strBuffer.toString();
	}

	public static String convert(String paramName, String paramValue) {
		Float result;
		result = null;
		if ("wind".equals(paramName)) {
			// convert in m/s
			result = Float.parseFloat(paramValue) / (24f * 60f * 60f) * 1000f;
		} else if ("vprs".equals(paramName)) {
			// convert vapor pressure from kpascal to millibar
			result = Float.parseFloat(paramValue) * 10f;
		} else if ("sksat".equals(paramName)) {
			// convert infil
			result = Float.parseFloat(paramValue) * 10 * 24;
		} else if ("sloc".equals(paramName)) {
			result = Float.parseFloat(paramValue) / 1.8f / 9.52f;
		} else if ("caco3".equals(paramName)) {
			result = Float.parseFloat(paramValue) / 10f;
		} else if ("icrag".equals(paramName)) {
			// convert in tons
			result = Float.parseFloat(paramValue) / 1000f;
		} else if ("pldp".equals(paramName)) {
			// convert in mm
			result = Float.parseFloat(paramValue) / 10f;
			//TODO pldp in mm in the test data
		} else if ("plrs".equals(paramName)) {
			// convert in cm
			result = Float.parseFloat(paramValue) / 10f;
		}
		return (result == null) ? paramValue : result.toString();
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
	 * Parse the properties file and returns a Map filled with properties
	 * Property with the same value can have the following format :
	 * prop1,prop2=defaultvalue
	 * 
	 * @return
	 * @throws IOException
	 */
	public static HashMap<String, String> getDefaultValues() throws IOException {
		InputStream inputStream = WeatherOutput.class.getResourceAsStream("/default-stics-values.properties");
		HashMap<String, String> defaultProperties;
		Properties properties;
		defaultProperties = new HashMap<String, String>();
		properties = new Properties();
		properties.load(inputStream);
		for (Object property : properties.keySet()) {
			String defaultValue = properties.getProperty((String) property);
			String[] subListOfProperties = ((String) property).split(",");
			for (String subProperty : subListOfProperties) {
				defaultProperties.put(subProperty, defaultValue);
			}
		}
		return defaultProperties;
	}
	
	
	public static void populate(String[] defaultParam, ArrayList<HashMap<String, String>> list){
		if(list.size() == 0){
			list.add(SticsUtil.createDefaultMap(defaultParam));
		}else{
			// Check for each map in the list if the keys doesn't exist put the key and the default value
			SticsUtil.defaultValueForList(defaultParam, list);
		}
	}
	
	public static void defaultValueForList(String[] params, ArrayList<HashMap<String, String>> values){
		for (HashMap<String, String> e : values) {
			defaultValueForMap(params, e);
		}
	}

	public static void defaultValueForMap(String[] params, HashMap<String, String> values) {
		for (String param : params) {
			if (!values.containsKey(param)) {
				values.put(param, defaultValue(param));
			}
		}
	}

	public static void convertValues(HashMap<String, String> values) {
		for (String key : values.keySet()) {
			values.put(key, convert(key, values.get(key)));
		}

	}

	public static String toWeatherId(String wstId) {
		String stationId;
		// = getValueOr(weatherBucket.getValues(), "wst_id",
		// SticsUtil.defaultValue("wst_id"));
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

	public static void convertFirstLevelRecords(HashMap<String, String> values) {
		convertValues(values);

	}

	public static void convertNestedRecords(ArrayList<HashMap<String, String>> dataList, String[] paramWithDefaultValues) {
		for (HashMap<String, String> data : dataList) {
			convertValues(data);
			SticsUtil.defaultValueForMap(paramWithDefaultValues, data);
		}
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
