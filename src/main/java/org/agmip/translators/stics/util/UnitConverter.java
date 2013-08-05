package org.agmip.translators.stics.util;

import static java.lang.Float.parseFloat;
import static org.agmip.translators.stics.util.Const.SLBDM;
import static org.agmip.translators.stics.util.Const.SLLB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to centralize unit convertion
 * 
 * @author jucufi
 * 
 */
public class UnitConverter {
	private static final Logger log = LoggerFactory.getLogger(UnitConverter.class);

	public UnitConverter() {
	}

	/**
	 * Convert initialization parameters
	 * 
	 * @param soilsData soil layers
	 * @param initParamToConvert list of parameters to convert
	 */
	public void convertInitValues(List<HashMap<String, String>> soilsData, String[] initParamToConvert) {
		for (HashMap<String, String> currentSoil : soilsData) {
			for (String param : initParamToConvert) {
				if (currentSoil.containsKey(param)) {
					currentSoil.put(param, new Float(parseFloat(currentSoil.get(param)) * parseFloat(currentSoil.get(SLBDM)) * parseFloat(currentSoil.get(SLLB)) / 10f).toString());
				}
			}
		}
	}

	/**
	 * Convert soil parameters
	 * 
	 * @param soilsData soil layers
	 * @param soilParamToConvert list of parameters to convert
	 */
	public void convertSoil(List<HashMap<String, String>> soilsData, String[] soilParamToConvert) {
		for (HashMap<String, String> currentSoil : soilsData) {
			// Convertion
			for (String param : soilParamToConvert) {
				if (currentSoil.containsKey(param)) {
					currentSoil.put(param, new Float(parseFloat(currentSoil.get(param)) / parseFloat(currentSoil.get(SLBDM)) * 100f).toString());
				}
			}
		}
	}

	/**
	 * Convert stics parameter value to icasa parameter value
	 * 
	 * @param paramName parameter name
	 * @param paramValue parameter value
	 * @return the converted value
	 */
	public String convertToIcasaUnit(String paramName, String paramValue) {
		Float result;
		Float n;
		result = null;
		if ("masec(n)".equalsIgnoreCase(paramName) || "mafruit".equalsIgnoreCase(paramName)) {
			try {
				n = Float.valueOf(paramValue.trim());
			} catch (NumberFormatException e) {
				log.error("Unable to convert " + paramName + ", value : " + paramValue, e);
				n = new Float(0);
			}
			result = n * 1000f;
		}
		return (result == null) ? paramValue : result.toString();
	}

	/**
	 * Convert icasa parameter value to stics parameter value
	 * 
	 * @param paramName icasa parameter name
	 * @param paramValue parameter value
	 * @return the converted value
	 */
	public String convertToSticsUnit(String paramName, String paramValue) {
		Float result;
		result = null;
		if ("wind".equals(paramName)) {
			// convert in m/s
			result = Float.parseFloat(paramValue) / (24f * 60f * 60f) * 1000f;
		} else if ("vprsd".equals(paramName)) {
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
			// TODO pldp in mm in the test data
		} else if ("plrs".equals(paramName)) {
			// convert in cm
			result = Float.parseFloat(paramValue) / 10f;
		}
		return (result == null) ? paramValue : result.toString();
	}

	/**
	 * For each parameters in the map check if it should be converted and do it if necessary.
	 * 
	 * @param values the map
	 */
	public void convertFirstLevelRecords(Map<String, String> values) {
		for (Entry<String, String> entry : values.entrySet()) {
			values.put(entry.getKey(), convertToSticsUnit(entry.getKey(), entry.getValue()));
		}

	}

	/**
	 * For each map in the dataList : - check if something needs to be converted - fill parameters (in the
	 * paramWithDefault) with their defaut values if they are missing.
	 * 
	 * @param dataList
	 * @param paramWithDefault
	 */
	public void convertNestedRecords(List<HashMap<String, String>> dataList, String[] paramWithDefault) {
		for (HashMap<String, String> data : dataList) {
			convertFirstLevelRecords(data);
			SticsUtil.defaultValueForMap(paramWithDefault, data);
		}
	}
}
