package org.agmip.translators.stics;

import static org.agmip.util.MapUtil.extractFromList;
import static org.agmip.util.MapUtil.getValueOr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.agmip.core.types.TranslatorOutput;
import org.agmip.util.JSONAdapter;

public class ModelOutput implements TranslatorOutput {

	public static String ZERO_VALUE = "0.0";
	public static String NEGATIVE_VALUE = "-999";
	public static String DATE_VALUE = "20110101";
	public static String STATION_NAME = "notGiven";
	public static String WEATHER_DATA_SEPARATOR = " ";
	public static String NEW_LINE = "\n";
	public static String CLIMATIC_FILENAME = "a.tmp";

	public static void main(String[] args) {
		ModelOutput modelOutput;
		modelOutput = new ModelOutput();
		try {
			Map jsonMap = JSONAdapter.fromJSON(getDataFromTestFile("ufga8201_mzx.json"));
			modelOutput.writeFile("", jsonMap);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Return a string containing the data file
	 * 
	 * @param file
	 * @return a string containing the data file
	 */
	public static String getDataFromTestFile(String file) {
		InputStream inputStream = ModelOutput.class.getResourceAsStream(file);
		BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));
		StringBuffer strBuffer = new StringBuffer();
		try {
			while (buffer.ready()) {
				strBuffer.append(buffer.readLine());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return strBuffer.toString();
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
	public String formatLine(String stationName, Map<String, Object> weatherRecord) {
		String[] params = new String[] { "w_date", "tmin", "tmax", "srad", "eoaa", "rain", "wind", "vprs", "co2d" };
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
			separator = (i == params.length - 1 ? "" : WEATHER_DATA_SEPARATOR);
			if ("w_date".equals(params[i])) {
				// First write date in stics format
				srcDateFormat = new SimpleDateFormat("yyyyMMdd");
				newDateFormat = new SimpleDateFormat("yyyy MM dd");
				try {
					dateResult = srcDateFormat.parse((String) weatherRecord.get(params[i]));
					buffer.append(newDateFormat.format(dateResult) + separator);
				} catch (ParseException e) {
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

	public void writeFile(String filePath, Map data) {
		List<Map<String, Object>> weatherRecords;
		List<Map<String, Object>> negativeDefault;
		List<Map<String, Object>> zeroDefault;
		List<Map<String, Object>> dateDefault;

		BufferedWriter writer;
		String stationName;
		try {
			// Extract station name first
			stationName = (String) getValueOr(data, "wsta_name", STATION_NAME);

			// Extract weather values
			weatherRecords = getValueOr(data, "weather", new ArrayList<Map<String, Object>>());
			negativeDefault = extractFromList(weatherRecords, new String[] { "tmin", "tmax", "eoaa", "vprs", "co2d", "srad", "rain", "wind" }, NEGATIVE_VALUE);
			zeroDefault = extractFromList(weatherRecords, new String[] { "elev" }, ZERO_VALUE);
			dateDefault = extractFromList(weatherRecords, new String[] { "w_date" }, DATE_VALUE);
			LinkedList<List<Map<String, Object>>> listOfDefaultList = new LinkedList<List<Map<String, Object>>>();
			listOfDefaultList.push(negativeDefault);
			listOfDefaultList.push(zeroDefault);
			listOfDefaultList.push(dateDefault);

			// Merge all previous default values list
			LinkedList<Map<String, Object>> mergedWeatherRecords;
			mergedWeatherRecords = new LinkedList<Map<String, Object>>();
			for (List<Map<String, Object>> list : listOfDefaultList) {
				for (Map<String, Object> m : list) {
					mergedWeatherRecords.add(m);
				}
			}
			// Write the file
			writer = new BufferedWriter(new FileWriter(new File(CLIMATIC_FILENAME)));
			StringBuffer weatherRecordBuffer = new StringBuffer();
			int count = 0;
			for (Map<String, Object> record : mergedWeatherRecords) {
				weatherRecordBuffer.append(formatLine(stationName, record));
				weatherRecordBuffer.append(NEW_LINE);
				if (count % 100 == 0) {
					writer.write(weatherRecordBuffer.toString());
					weatherRecordBuffer = new StringBuffer();
				}
				count++;
			}
			// Last write
			writer.write(weatherRecordBuffer.toString());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
