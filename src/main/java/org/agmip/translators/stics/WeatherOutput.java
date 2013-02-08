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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.agmip.core.types.TranslatorOutput;
import org.agmip.translators.stics.util.SticsUtil;
import org.agmip.translators.stics.util.VelocityUtil;
import org.agmip.util.MapUtil;
import org.agmip.util.MapUtil.BucketEntry;
import org.apache.velocity.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeatherOutput implements TranslatorOutput {
	private static final Logger log = LoggerFactory.getLogger(WeatherOutput.class);
	public static String WEATHER_DATA_SEPARATOR = " ";
	public static String NEW_LINE = "\n";
	public static int DATE_INDEX = 1;
	public static String STATION_TEMPLATE_FILE = "/sta_template.vm";

	private BufferedWriter writer;
	private String previousYear;
	private HashMap<String, ArrayList<String>> weatherFilesById;
	private HashMap<String, String> stationFilesById;

	public HashMap<String, ArrayList<String>> getWeatherFilesById() {
		return weatherFilesById;
	}

	public HashMap<String, String> getStationFilesById() {
		return stationFilesById;
	}

	public WeatherOutput() {
		weatherFilesById = new HashMap<String, ArrayList<String>>();
		stationFilesById = new HashMap<String, String>();
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
					log.error(e.getMessage());
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
	 * 
	 * @param fileName
	 * @param year
	 * @throws IOException
	 */
	private void newClimaticFile(String stationId, String fileName, String year) throws IOException {
		File currentFile;
		currentFile = new File(fileName);
		log.info("Generating weather file : "+currentFile.getName());
		weatherFilesById.get(stationId).add(currentFile.getName());
		writer = new BufferedWriter(new FileWriter(currentFile));
		previousYear = year;
	}

	/**
	 * Write data, only 1 year by climatic file
	 * 
	 * @param stationId
	 * @param year
	 * @param data
	 * @throws IOException
	 */
	public void outputData(String filePath, String stationId, String year, String data) throws IOException {
		String fileName;
		String city;
		city = stationId.split("_")[0];
		fileName = filePath + File.separator + city + "." + year;
		if (writer == null) {
			newClimaticFile(stationId, fileName, year);
		} else if (!previousYear.equals(year)) {
			// we need a new file
			writer.close();
			newClimaticFile(stationId, fileName, year);
		}
		writer.write(data + NEW_LINE);
	}

	/**
	 * Where is my year ?
	 * 
	 * @param line
	 * @return
	 */
	private String getYear(String line) {
		String[] fields = line.split(" ");
		return fields[DATE_INDEX];
	}

	/**
	 * Main method for the converter
	 */
	public void writeFile(String filePath, Map data) {

		HashMap<String, String> firstLevelStationParameters;
		String stationId;
		String city;
		String content;
		Context context;
		String refht, fl_lat, flele, anga, angb;
		try {
			/** 1 - Generate climatic files **/
			// Extract weather values
			ArrayList<BucketEntry> listofWeather = MapUtil.getPackageContents(data, "weathers");
			for (BucketEntry weatherBucket : listofWeather) {
				// Extract station name first
				stationId = SticsUtil.toWeatherId(weatherBucket.getValues().get("wst_id"));
				weatherFilesById.put(stationId, new ArrayList<String>());
				city = stationId.split("_")[0];
				// Write values
				for (Map<String, String> record : weatherBucket.getDataList()) {
					String line = formatLine(stationId, record);
					String year = getYear(line);
					outputData(filePath, stationId, year, line);
				}
				/** 2 - Generate station files **/
				refht = getValueOr(weatherBucket.getValues(), "refht", SticsUtil.defaultValue("refht"));
				fl_lat = getValueOr(data, "fl_lat", SticsUtil.defaultValue("fl_lat"));
				flele = getValueOr(data, "flele", SticsUtil.defaultValue("flele"));
				anga = getValueOr(data, "anga", SticsUtil.defaultValue("anga"));
				angb = getValueOr(data, "angb", SticsUtil.defaultValue("angb"));
				firstLevelStationParameters = new HashMap<String, String>();
				firstLevelStationParameters.put("refht", refht);
				firstLevelStationParameters.put("fl_lat", fl_lat);
				firstLevelStationParameters.put("flele", flele);
				firstLevelStationParameters.put("anga", anga);
				firstLevelStationParameters.put("angb", angb);
				context = VelocityUtil.fillVelocityContext(firstLevelStationParameters, null);
				content = VelocityUtil.getInstance().runVelocity(context, STATION_TEMPLATE_FILE);
				File stationFile = SticsUtil.newFile(content, filePath, city + "_sta.xml");
				log.info("Generating station file : "+stationFile.getName());
				stationFilesById.put(stationId, stationFile.getName());
			}
		} catch (Exception e) {
			log.error(e.toString());
		} finally {
			try {
				if (writer != null) {
					writer.flush();
					writer.close();
				}
			} catch (IOException e) {
				log.error("Unable to generate climatic files");
				log.error(e.toString());
			}
		}
	}
}
