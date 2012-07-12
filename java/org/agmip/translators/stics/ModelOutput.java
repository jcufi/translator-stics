package org.agmip.translators.stics;

import static org.agmip.util.MapUtil.getValueOr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.agmip.core.types.TranslatorOutput;
import org.agmip.util.MapUtil;
import org.agmip.util.MapUtil.BucketEntry;

public class ModelOutput implements TranslatorOutput {

	public static String ZERO_VALUE = "0.0";
	public static String NEGATIVE_VALUE = "-999";
	public static String DATE_VALUE = "20110101";
	public static String STATION_NAME = "notGiven";
	public static String WEATHER_DATA_SEPARATOR = " ";
	public static String NEW_LINE = "\n";
	public static String CLIMATIC_FILENAME = "a.tmp";
	public static String UNKNOWN_DEFAULT_VALUE = "unknown-default-value";
	public static LinkedHashMap<String, String> defaultvalues;

	public ModelOutput() throws IOException {
		defaultvalues = SticsUtil.getDefaultValues();
	}

	public String defaultValue(String key) {
		return defaultvalues.containsKey(key) ? defaultvalues.get(key) : UNKNOWN_DEFAULT_VALUE;
	}

	public boolean isDefaultValue(String paramName, String value){
		return value.equals(defaultvalues.get(paramName));
	}
	
	/**
	 * Format one line of stics weather file
	 * 
	 * @param firstLevel
	 *            first level parameters
	 * @param weatherRecord
	 *            data corresponding to a weather record
	 * @return a line of stics weather file
	 */
	public String formatLine(String stationName, Map<String, String> weatherRecord) {
		String[] params = new String[] { "w_date", "tmin", "tmax", "srad", "eoaa", "rain", "wind", "vprs", "co2d" };
		List<String> requiresConvertion = Arrays.asList(new String[]{"wind", "vprs"});
		StringBuffer buffer = new StringBuffer();
		SimpleDateFormat srcDateFormat;
		SimpleDateFormat newDateFormat;
		Date dateResult;
		String separator;
		String paramValue;
		int julianDay;
		dateResult = null;
		julianDay = 0;
		
		buffer.append(stationName);
		buffer.append(WEATHER_DATA_SEPARATOR);
		for (int i = 0; i < params.length; i++) {
			separator = ((i == params.length - 1) ? "" : WEATHER_DATA_SEPARATOR);
			paramValue = MapUtil.getObjectOr(weatherRecord, params[i], defaultValue(params[i]));
			if(requiresConvertion.contains(params[i]) && !isDefaultValue(params[i], paramValue)){
				buffer.append((convert(paramValue) + separator));
			}else if ("w_date".equals(params[i])) {
				// First write date in stics format
				srcDateFormat = new SimpleDateFormat("yyyyMMdd");
				newDateFormat = new SimpleDateFormat("yyyy MM dd");
				try {
					dateResult = srcDateFormat.parse(paramValue);
					buffer.append(newDateFormat.format(dateResult) + separator);
				} catch (Exception e) {
					e.printStackTrace();
				}
				// Write julian day
				julianDay = getJulianDay(dateResult);
				buffer.append(julianDay + separator);
			} else {
				buffer.append((paramValue + separator));
			}
		}
		// Return the line and remove the last trailing extra space
		return buffer.toString();
	}

	private String convert(String param) {
		Float result;
		result = null;
		if("wind".equals(param)){
			// convert in m/s
			result = Float.parseFloat(param) / (24f*60f*60f) * 1000f;
		}else if ("vprs".equals(param)){
			// convert kpascal in mb
			result = Float.parseFloat(param) * 10f;
		}
		return (result == null) ? "requires-convertion" : result.toString();
	}

	/**
	 * Convert date into a julian day
	 * 
	 * @param date
	 * @return a julian day
	 */
	public int getJulianDay(Date date) {
		System.out.println(date.toString());
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		return calendar.get(GregorianCalendar.DAY_OF_YEAR);
	}

	/**
	 * Return a new LinkedList filled with values taken from the input
	 * parameters
	 * 
	 * @param listOfDefaultList
	 *            A list of list to merge
	 * @return a new LinkedList filled with values taken from the input
	 *         parameters
	 */
	public LinkedList<Map<String, Object>> mergeList(LinkedList<List<Map<String, Object>>> listOfDefaultList) {
		// Merge all previous default values list
		LinkedList<Map<String, Object>> mergedWeatherRecords;
		mergedWeatherRecords = new LinkedList<Map<String, Object>>();
		for (List<Map<String, Object>> list : listOfDefaultList) {
			for (Map<String, Object> m : list) {
				mergedWeatherRecords.add(m);
			}
		}
		return mergedWeatherRecords;
	}

	ArrayList<File> climaticFiles = new ArrayList<File>();
	BufferedWriter writer;
	String previousYear;

	private void newClimaticFile(String fileName, String year) throws IOException {
		File currentFile;
		currentFile = new File(fileName);
		climaticFiles.add(currentFile);
		writer = new BufferedWriter(new FileWriter(currentFile));
		previousYear = year;
	}

	public void outputData(String stationName, String year, String data) throws IOException {
		String fileName;
		String city;
		city = stationName.split(",")[0];
		fileName = System.getProperty("user.dir") + File.separator + city + "." + year;
		if (writer == null) {
			newClimaticFile(fileName, year);
		} else if (!previousYear.equals(year)) {
			// we need a new file
			writer.close();
			newClimaticFile(fileName, year);
		}
		writer.write(data + NEW_LINE);
	}

	int DATE_INDEX = 1;

	private String getYear(String line) {
		String[] fields = line.split(" ");
		return fields[DATE_INDEX];
	}

	/**
	 * Main method for the converter
	 */
	public void writeFile(String filePath, Map data) {
		ArrayList<BucketEntry> weatherRecords;
		String stationName;
		try {
			// Extract weather values
			weatherRecords = MapUtil.getBucket(data, "weather");
			// Extract station name first
			stationName = getValueOr(weatherRecords.get(0).getValues(), "wst_name", defaultValue("wst_name"));
			System.out.println(stationName);

			// Write values
			for (BucketEntry bucket : weatherRecords) {
				for (Map<String, String> record : bucket.getDataList()) {
					String line = formatLine(stationName, record);
					String year = getYear(line);
					outputData(stationName, year, line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.flush();
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
