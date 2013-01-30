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
import org.agmip.translators.stics.util.SticsUtil;
import org.agmip.translators.stics.util.VelocityUtil;
import org.agmip.util.MapUtil;
import org.agmip.util.MapUtil.BucketEntry;
import org.apache.velocity.context.Context;

public class WeatherOutput implements TranslatorOutput {

	public static String WEATHER_DATA_SEPARATOR = " ";
	public static String NEW_LINE = "\n";
	public static int DATE_INDEX = 1;
	public static String STATION_TEMPLATE_FILE = "/sta_template.vm";
	
	private BufferedWriter writer;
	private String previousYear;
	private ArrayList<String> climaticFiles;
	private File stationFile;

	public ArrayList<String> getClimaticFiles() {
		return climaticFiles;
	}

	public WeatherOutput() {
		climaticFiles = new ArrayList<String>();
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
		List<String> requiresConvertion = Arrays.asList(new String[] { "wind", "vprs" });
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
			paramValue = MapUtil.getObjectOr(weatherRecord, params[i], SticsUtil.defaultValue(params[i]));
			if (requiresConvertion.contains(params[i]) && !SticsUtil.isDefaultValue(params[i], paramValue)) {
				buffer.append((SticsUtil.convert(params[i], paramValue) + separator));
			} else if ("w_date".equals(params[i])) {
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
				julianDay = SticsUtil.getJulianDay(dateResult);
				buffer.append(julianDay + separator);
			} else {
				buffer.append((paramValue + separator));
			}
		}
		// Return the line
		return buffer.toString();
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

	/**
	 * Create a new clim file
	 * @param fileName
	 * @param year
	 * @throws IOException
	 */
	private void newClimaticFile(String fileName, String year) throws IOException {
		File currentFile;
		currentFile = new File(fileName);
		climaticFiles.add(fileName);
		writer = new BufferedWriter(new FileWriter(currentFile));
		previousYear = year;
	}

	/**
	 * Write data, only 1 year by climatic file
	 * @param stationName
	 * @param year
	 * @param data
	 * @throws IOException
	 */
	public void outputData(String filePath, String stationName, String year, String data) throws IOException {
		String fileName;
		String city;
		city = stationName.split("_")[0];
		fileName = filePath + File.separator + city + "." + year;
		if (writer == null) {
			newClimaticFile(fileName, year);
		} else if (!previousYear.equals(year)) {
			// we need a new file
			writer.close();
			newClimaticFile(fileName, year);
		}
		writer.write(data + NEW_LINE);
	}

	/**
	 * Where is my year ?
	 * @param line
	 * @return
	 */
	private String getYear(String line) {
		String[] fields = line.split(" ");
		return fields[DATE_INDEX];
	}

	public File getStationFile(){
		return stationFile;
	}
	
	/**
	 * Main method for the converter
	 */
	public void writeFile(String filePath, Map data) {
		BucketEntry weatherBucket;
		LinkedHashMap<String, String> firstLevelStationParameters;
		String stationName;
		String city;
		String content;
		Context context;
		String refht, fl_lat, flele, anga, angb;
		try {
			/** 1 - Generate climatic files **/
			// Extract weather values
			weatherBucket = MapUtil.getBucket(data, "weather");
			// Extract station name first
			stationName = getValueOr(weatherBucket.getValues(), "wst_name", SticsUtil.defaultValue("wst_name"));
			// Trick for stics , not allowed in station name
			stationName = stationName.replaceAll(",", "_");
			city = stationName.split("_")[0];
			// Write values
			for (Map<String, String> record : weatherBucket.getDataList()) {
				String line = formatLine(stationName, record);
				String year = getYear(line);
				outputData(filePath, stationName, year, line);
			}
			/** 2 - Generate station files **/
			refht = getValueOr(weatherBucket.getValues(), "refht", SticsUtil.defaultValue("refht"));
			fl_lat = getValueOr(data, "fl_lat", SticsUtil.defaultValue("fl_lat"));
			flele = getValueOr(data, "flele", SticsUtil.defaultValue("flele"));
			anga = getValueOr(data, "anga", SticsUtil.defaultValue("anga"));
			angb = getValueOr(data, "angb", SticsUtil.defaultValue("angb"));
			firstLevelStationParameters = new LinkedHashMap<String, String>();
			firstLevelStationParameters.put("refht", refht);
			firstLevelStationParameters.put("fl_lat", fl_lat);
			firstLevelStationParameters.put("flele", flele);
			firstLevelStationParameters.put("anga", anga);
			firstLevelStationParameters.put("angb", angb);
			context = VelocityUtil.fillVelocityContext(firstLevelStationParameters, null);
			content = VelocityUtil.runVelocity(context, STATION_TEMPLATE_FILE);
			stationFile = SticsUtil.newFile(content, filePath, city+ "_sta.xml");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.flush();
				writer.close();
			} catch (IOException e) {
				System.err.println("Unable to generate climatic files");
				e.printStackTrace();
			}
		}
	}
}
