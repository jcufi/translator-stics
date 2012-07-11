package org.agmip.translators.stics;

import static org.agmip.util.MapUtil.getValueOr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
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
		String[] params = new String[] {"w_date", "tmin", "tmax", "srad", "eoaa", "rain", "wind", "vprs", "co2d" };
		StringBuffer buffer = new StringBuffer();
		SimpleDateFormat srcDateFormat;
		SimpleDateFormat newDateFormat;
		Date dateResult;
		String separator;
		int julianDay;
		dateResult = null;
		julianDay = 0;

		buffer.append(stationName);
		buffer.append(WEATHER_DATA_SEPARATOR);
		for (int i = 0; i < params.length; i++) {
			separator = ((i == params.length - 1) ? "" : WEATHER_DATA_SEPARATOR);
			if ("w_date".equals(params[i])) {
				// First write date in stics format
				srcDateFormat = new SimpleDateFormat("yyyyMMdd");
				newDateFormat = new SimpleDateFormat("yyyy MM dd");
				try {
					dateResult = srcDateFormat.parse((String) weatherRecord.get(params[i]));
					buffer.append(newDateFormat.format(dateResult) + separator);
				} catch (Exception e) {
					e.printStackTrace();
				}
				// Write julian day
				julianDay = getJulianDay(dateResult);
				buffer.append(julianDay + separator);
			} else {
				buffer.append((weatherRecord.get(params[i]) + separator));
			}
		}
		// Return the line and remove the last trailing extra space
		return buffer.toString();
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

	/**
	 * Main method for the converter
	 */
	public void writeFile(String filePath, Map data) {
		ArrayList<BucketEntry> weatherRecords;
		BufferedWriter writer;
		String stationName;
		try {
			// Extract station name first
			stationName = getValueOr(data, "wst_name", STATION_NAME);
			// Extract weather values
			weatherRecords = MapUtil.getBucket(data, "weather");
			// Write values
			writer = new BufferedWriter(new FileWriter(new File(CLIMATIC_FILENAME)));
			StringBuffer weatherRecordBuffer = new StringBuffer();
			int count = 1;
			for (BucketEntry bucket : weatherRecords) {
				for (Map<String, String> record : bucket.getDataList()) {
					weatherRecordBuffer.append(formatLine(stationName, record));
					weatherRecordBuffer.append(NEW_LINE);
				}
				if (count % 100 == 0) {
					System.out.println(weatherRecordBuffer.toString());
					writer.write(weatherRecordBuffer.toString());
					weatherRecordBuffer = new StringBuffer();
				}
				count++;
			}
			// Last write
			System.out.println(weatherRecordBuffer.toString());
			writer.write(weatherRecordBuffer.toString());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
