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
	public String formatLine(FirstLevelValues firstLevel, Map<String, Object> weatherRecord) {
		String[] parametersName = new String[] { "wst_name", "w_date", "tmin", "tmax", "srad", "eoaa", "rain", "wind", "vprs", "co2d" };
		StringBuffer buffer = new StringBuffer();
		Date result;
		int julianDay;
		result = null;
		julianDay = 0;
		for (String param : parametersName) {
			if ("w_ date".equals(parametersName)) {
				// First the date
				SimpleDateFormat format = new SimpleDateFormat("mmmm dd yy");
				try {
					result = format.parse((String) weatherRecord.get(param));
					buffer.append(result.toString());
					buffer.append(WEATHER_DATA_SEPARATOR);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				// Write julian day
				julianDay = getJulianDay(result);
				buffer.append(julianDay);
				buffer.append(WEATHER_DATA_SEPARATOR);

			} else {
				buffer.append(weatherRecord.get(param));
				buffer.append(WEATHER_DATA_SEPARATOR);
			}
		}
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
		BufferedWriter writer;
		try {
			FirstLevelValues firstLevel = new FirstLevelValues();
			// Extract first level values
			firstLevel.stationName = (String) getValueOr(data, "wsta_name", STATION_NAME);
			firstLevel.latitude = (String) getValueOr(data, "wsta_lat", ZERO_VALUE);
			firstLevel.altitude = (String) getValueOr(data, "elev", ZERO_VALUE);

			// Extract weather values
			List<Map<String, Object>> weatherRecords = getValueOr(data, "weather", new ArrayList<Map<String, Object>>());
			List<Map<String, Object>> negativeDefault = extractFromList(weatherRecords, new String[] { "tmin", "tmax", "eoaa", "vprs", "co2d", "srad", "rain", "wind" }, NEGATIVE_VALUE);
			List<Map<String, Object>> zeroDefault = extractFromList(weatherRecords, new String[] { "elev" }, ZERO_VALUE);
			List<Map<String, Object>> dateDefault = extractFromList(weatherRecords, new String[] { "w_date" }, DATE_VALUE);
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
				weatherRecordBuffer.append(formatLine(firstLevel, record));
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
