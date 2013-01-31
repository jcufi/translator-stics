package org.agmip.translators.stics.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to reduce number of soil layers.
 * 
 * @author jucufi
 * @author dripoche
 * 
 */
public class SoilAggregationTool {

	private static final Logger log = LoggerFactory.getLogger(SoilAggregationTool.class);

	// slll sldul slbdm sksat
	// Thresholds used to merge soil layers
	public float FIRST_THRESHOLD = 5f;
	public float SECOND_THRESHOLD = 0.05f;
	// Soil information under soil section
	public static String SLLL = "slll";
	public static String SLDUL = "sldul";
	public static String SLBDM = "slbdm";
	public static String SKSAT = "sksat";
	public static String SLLB = "sllb";
	public static String SLOC = "sloc";

	// Soil information under init section
	public static String ICBL = "icbl";
	public static String ICH2O = "ich2o";
	public static String ICNO3 = "icno3";
	public static String ICNH4 = "icnh4";

	private String[] allParams = new String[] { SLLL, SLDUL, SLBDM, SKSAT, SLLB, SLOC, ICH2O, ICNO3, ICNH4 };

	/**
	 * Round float
	 * 
	 * @param r
	 * @return
	 */
	public Float round(Float r) {
		return Math.round(r * 100.0) / 100f;
	}

	/**
	 * Criteria for merging soils ru is the maximum available water reserve
	 * (reserve utile) sdul is the field capacity slll is the wilting point
	 * (point de fletrissement permanent) slbdm is the bulk density
	 * 
	 * @param currentSoil
	 * @param previousSoil
	 * @return
	 */
	private boolean dirkRaesAndDomiTest(LinkedHashMap<String, String> currentSoil, LinkedHashMap<String, String> previousSoil) {
		float sdulCurrent_no_convert;
		float slllCurrent_no_convert;
		float sdulPrevious_no_convert;
		float slllPrevious_no_convert;
		float ruCurrent;
		float ruPrevious;
		float resultFirstRule;
		float resultSecRule;
		boolean firstRule;
		boolean secRule;

		// Unit change for stics only
		sdulCurrent_no_convert = Float.parseFloat(currentSoil.get(SLDUL)) * Float.parseFloat(currentSoil.get(SLBDM)) / 100f;
		slllCurrent_no_convert = Float.parseFloat(currentSoil.get(SLLL)) * Float.parseFloat(currentSoil.get(SLBDM)) / 100f;
		sdulPrevious_no_convert = Float.parseFloat(previousSoil.get(SLDUL)) * Float.parseFloat(previousSoil.get(SLBDM)) / 100f;
		slllPrevious_no_convert = Float.parseFloat(previousSoil.get(SLLL)) * Float.parseFloat(previousSoil.get(SLBDM)) / 100f;
		// end of unit change

		// ru in mm/m
		ruCurrent = (sdulCurrent_no_convert - slllCurrent_no_convert) * 1000f;
		ruPrevious = (sdulPrevious_no_convert - slllPrevious_no_convert) * 1000f;
		resultFirstRule = round(Math.abs(ruCurrent - ruPrevious));
		firstRule = (Float.parseFloat(String.valueOf(resultFirstRule)) <= FIRST_THRESHOLD);

		/**
		 * First rule : (currentRu - previousRu) <= 5 mm / m Second rule :
		 * (currentBdm - previousBdm) <= 0.05 Soil layers are aggregated if the
		 * rules below are both true
		 * */
		resultSecRule = round(Math.abs(Float.parseFloat(currentSoil.get(SLBDM)) - Float.parseFloat(previousSoil.get(SLBDM))));
		secRule = (round(resultSecRule) <= SECOND_THRESHOLD);

		log.debug("*********************");
		log.debug("First rule : " + resultFirstRule + " <= " + FIRST_THRESHOLD + " ? " + firstRule);
		log.debug("Sec rule : " + resultSecRule + " <= " + SECOND_THRESHOLD + " ? " + secRule);
		log.debug("*********************");

		return firstRule && secRule;
	}

	/**
	 * Process all the soil layers and return a new structure with aggregated
	 * data
	 * 
	 * @param soilsData
	 * @return
	 */
	public ArrayList<LinkedHashMap<String, String>> mergeSoilLayers(ArrayList<LinkedHashMap<String, String>> soilsData) {
		LinkedHashMap<String, String> previousSoil;
		ArrayList<LinkedHashMap<String, String>> aggregatedSoilsData;
		LinkedHashMap<String, String> aggregatedSoil;
		boolean aggregate;
		previousSoil = null;
		aggregate = true;
		aggregatedSoilsData = new ArrayList<LinkedHashMap<String, String>>();
		ArrayList<LinkedHashMap<String, String>> formattedSoilsData = formatSoilLayers(soilsData);
		log.debug("Formated soil data : " + formattedSoilsData);
		int i = 0;
		for (LinkedHashMap<String, String> currentSoil : formattedSoilsData) {
			i++;
			if (previousSoil != null) {
				aggregate = dirkRaesAndDomiTest(currentSoil, previousSoil);
				if (aggregate) {
					log.debug("Aggregating soil layers... " + i + " and " + (i - 1));
					log.debug("soil " + i + " " + currentSoil);
					log.debug("soil " + (i - 1) + " " + previousSoil);
					// Compute the new map
					aggregatedSoil = computeSoil(currentSoil, previousSoil);
					if (aggregatedSoilsData.contains(previousSoil)) {
						aggregatedSoilsData.remove(previousSoil);
					}
					// Set as previous soil treated
					previousSoil = aggregatedSoil;
					aggregatedSoilsData.add(aggregatedSoil);
				} else {
					previousSoil = currentSoil;
					log.debug("Adding soil layer ...");
					log.debug("soil " + i + " " + currentSoil);
					aggregatedSoilsData.add(currentSoil);
				}
			} else {
				previousSoil = currentSoil;
				aggregatedSoilsData.add(currentSoil);
			}
		}
		log.info("Information about soil aggregation");
		log.info("Soil layers before : " + soilsData.size());
		log.info("Soil layers after  : " + aggregatedSoilsData.size());
		return aggregatedSoilsData;
	}

	/**
	 * Create a new soil filled with aggregated data coming from soils set as
	 * input parameters.
	 * 
	 * @param fullCurrentSoil
	 * @param previousSoil
	 * @return
	 */
	private LinkedHashMap<String, String> computeSoil(Map<String, String> fullCurrentSoil, Map<String, String> previousSoil) {
		LinkedHashMap<String, String> aggregatedSoil;
		String fullCurrentValue;
		String previousValue;
		Float newValue;
		aggregatedSoil = new LinkedHashMap<String, String>();
		for (String p : allParams) {
			if (!SLLB.equals(p) && !ICNH4.equals(p) && !ICNO3.equals(p)) {
				fullCurrentValue = fullCurrentSoil.get(p) == null ? SticsUtil.defaultValue(p) : fullCurrentSoil.get(p);
				previousValue = previousSoil.get(p) == null ? SticsUtil.defaultValue(p) : previousSoil.get(p);
				newValue = (Float.parseFloat(fullCurrentValue) + Float.parseFloat(previousValue)) / 2f;
			} else {
				newValue = (Float.parseFloat(fullCurrentSoil.get(p)) + Float.parseFloat(previousSoil.get(p)));
			}
			aggregatedSoil.put(p, newValue.toString());
		}
		return aggregatedSoil;
	}

	/**
	 * Parse soil layer and convert in a stics format where each layer contrains
	 * all parameters and where the sslb is not computed from 0 origin.
	 * 
	 * @param soilsData
	 * @return
	 */
	private ArrayList<LinkedHashMap<String, String>> formatSoilLayers(ArrayList<LinkedHashMap<String, String>> soilsData) {
		LinkedHashMap<String, String> referenceSoil;
		ArrayList<LinkedHashMap<String, String>> newSoilsData;
		float deep = 0.0f;
		referenceSoil = soilsData.get(0);
		newSoilsData = new ArrayList<LinkedHashMap<String, String>>();
		if (referenceSoil != null) {
			for (LinkedHashMap<String, String> currentSoil : soilsData) {
				// create a new soil with reference parameters
				LinkedHashMap<String, String> fullCurrentSoil = new LinkedHashMap<String, String>(referenceSoil);
				for (String key : allParams) {
					if (currentSoil.containsKey(key)) {
						fullCurrentSoil.put(key, currentSoil.get(key));
					}
				}
				// Convertion
				String[] paramToConvert = { SLDUL, SLLL, ICH2O };
				for (String param : paramToConvert) {
					if (fullCurrentSoil.containsKey(param)) {
						fullCurrentSoil.put(param, new Float(Float.parseFloat(fullCurrentSoil.get(param)) / Float.parseFloat(fullCurrentSoil.get(SLBDM)) * 100f).toString());
					}
				}
				// Specific for stics soil data representation
				fullCurrentSoil.put(SLLB, new Float(Float.parseFloat(currentSoil.get(SLLB)) - deep).toString());
				newSoilsData.add(fullCurrentSoil);
				deep = Float.parseFloat(currentSoil.get(SLLB));
			}
		}
		return newSoilsData;
	}

}
