package org.agmip.translators.stics.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CropCycle {
	private CropCycle() {
	}
	public static final String TWO_YEARS_CROP = "0";
	public static final String ONE_YEARS_CROP = "1";
	public static final String NORTH = "north";
	public static final String SOUTH = "south";
	public static final String TWO_YEARS_CROP_MSG = "Translator has not be able to determine if the crop cycle is on 1 or 2 years, fill culturean manually";
	private static final Properties codeMap;
	private static final Logger log = LoggerFactory.getLogger(CropCycle.class);
	static {
		InputStream inputStream = CropCycle.class.getResourceAsStream("/crop-cycle-years.properties");
		codeMap = new Properties();
		try {
			codeMap.load(inputStream);
			log.debug(codeMap.toString());
		} catch (IOException e) {
			log.error(e.toString());
		}
	}

	public static String getCultureAn(String cropName, String latitude) {
		String key;
		String result = ONE_YEARS_CROP;
		if (checkInput(cropName, latitude)) {
			key = getKey(cropName, latitude);
			result = (codeMap.get(key) == null) ? ONE_YEARS_CROP : (String) codeMap.get(key);
		}
		return result;
	}

	public static boolean isATwoYearsCrop(String cropName, String latitude) {
		boolean result;
		String value;
		String key;
		// TODO use exceptions to avoid returning crap
		if (!checkInput(cropName, latitude)) {
			return false;
		}
		key = getKey(cropName, latitude);
		if (codeMap.contains(key)) {
			value = (String) codeMap.get(key);
			result = TWO_YEARS_CROP.equals(value);
		} else {
			log.debug(TWO_YEARS_CROP_MSG);
			result = false;
		}
		if (result) {
			log.debug("Crop " + cropName + ", (with latitude " + latitude + ") is a two years crop.");
		} else {
			log.debug("Crop " + cropName + ", (with latitude " + latitude + ") is a one year crop.");
		}
		return result;
	}

	private static boolean checkInput(String cropName, String latitude) {
		boolean result = true;
		if (cropName == null || latitude == null) {
			log.debug("cropName : " + cropName + " latitude : " + latitude);
			Report.addSummary(TWO_YEARS_CROP_MSG + " (crop name " + cropName + ").\n");
			result = false;
		}
		return result;
	}

	private static String getKey(String cropName, String latitude) {
		String key;
		if (latitude.charAt(0) != '-') {
			key = cropName + "-" + NORTH;
		} else {
			key = cropName + "-" + SOUTH;
		}
		return key;
	}

}
