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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.agmip.translators.stics.WeatherOutput;
import org.agmip.util.JSONAdapter;

public class SticsUtil {

	public static String UNKNOWN_DEFAULT_VALUE = "unknown-default-value";
	public static LinkedHashMap<String, String> defaultvalues;
	static {
		try {
			defaultvalues = SticsUtil.getDefaultValues();
		} catch (IOException e) {
			System.err.println("Default values cannot be loaded");
			e.printStackTrace();
		}
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
		InputStream inputStream = WeatherOutput.class.getResourceAsStream(file);
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

	public static String convert(String paramName, String paramValue) {
		Float result;
		result = null;
		if ("wind".equals(paramName)) {
			// convert in m/s
			result = Float.parseFloat(paramValue) / (24f * 60f * 60f) * 1000f;
		} else if ("vprs".equals(paramName)) {
			// convert kpascal in mb
			result = Float.parseFloat(paramValue) * 10f;
		} else if ("sksat".equals(paramName)) {
			result = Float.parseFloat(paramValue) * 10 * 24;
		} else if ("sldul".equals(paramName)) {
			result = Float.parseFloat(paramValue) * 10 * 24;
		} else if ("sloc".equals(paramName)) {
			result = Float.parseFloat(paramValue) / 1.8f / 9.52f;
		} else if ("caco3".equals(paramName)) {
			result = Float.parseFloat(paramValue) / 10f;
		} else if ("icrag".equals(paramName)) {
			// convert in tons
			result = Float.parseFloat(paramValue) / 1000f;
		}
		return (result == null) ? paramValue : result.toString();
	}

	public static String defaultValue(String key) {
		String value;
		if(defaultvalues.containsKey(key)){
			value = defaultvalues.get(key);
		}else{
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
	public static LinkedHashMap<String, String> getDefaultValues() throws IOException {
		InputStream inputStream = WeatherOutput.class.getResourceAsStream("/default-stics-values.properties");
		LinkedHashMap<String, String> defaultProperties;
		Properties properties;
		defaultProperties = new LinkedHashMap<String, String>();
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

	public static void fillDefault(LinkedHashMap<String, String> values) {
		// Fill default values
		for (String key : values.keySet()) {
			if (defaultvalues.containsKey(key)) {
				values.put(key, defaultValue(key));
			} else {
				values.put(key, convert(key, values.get(key)));
			}
		}

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

	public static void convertFirstLevelRecords(LinkedHashMap<String, String> values) {
		fillDefault(values);
	}

	public static void convertNestedRecords(ArrayList<LinkedHashMap<String, String>> dataList) {
		for (LinkedHashMap<String, String> data : dataList) {
			fillDefault(data);
		}
	}

	public static Map getDataFrom(String jsonFile) {
		Map data;
		data = null;
		try {
			data = JSONAdapter.fromJSON(getDataFromTestFile(jsonFile));
		} catch (IOException e) {
			System.err.println("Unable to read test data, " + jsonFile);
			e.printStackTrace();
		}
		return data;
	}

}
