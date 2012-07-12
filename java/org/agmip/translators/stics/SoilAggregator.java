package org.agmip.translators.stics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SoilAggregator {

	// slll sldul slbdm sksat
	float THRESHOLD = 0.02f;
	static String SLLL = "slll";
	static String SLDUL = "sldul";
	static String SLBDM = "slbdm";
	static String SKSAT = "sksat";
	private String[] mergeParams = new String[] { SLLL, SLDUL, SLBDM };

	public boolean shouldAggregate(String soilValue, String previousSoilValue) {
		float delta = Float.parseFloat(soilValue) - Float.parseFloat(previousSoilValue);
		return delta < THRESHOLD;
	}

	public List<HashMap<String, String>> merge(ArrayList<LinkedHashMap<String, String>> soilsData) {
		HashMap<String, String> previousSoil;
		List<HashMap<String, String>> aggregatedSoilsData;
		HashMap<String, String> aggregatedSoil;
		String[] keys = new String[] {SKSAT, SLLL, SLDUL, SLBDM };
		HashMap<String, String> fullCurrentSoil = new HashMap<String, String>();
		HashMap<String, String> referenceSoil = new HashMap<String, String>(soilsData.get(0));
		boolean aggregate;
		previousSoil = null;
		aggregate = true;
		aggregatedSoilsData = new ArrayList<HashMap<String, String>>();
		for (HashMap<String, String> currentSoil : soilsData) {
			if (previousSoil != null) {
				// create a new soil with previous parameters
				fullCurrentSoil = new HashMap<String, String>(referenceSoil);
				for (String key : keys) {
					if(currentSoil.containsKey(key)){
						fullCurrentSoil.put(key, currentSoil.get(key));
					}
				}
				aggregate &= shouldAggregate(fullCurrentSoil.get(SLLL), previousSoil.get(SLLL));
				aggregate &= shouldAggregate(fullCurrentSoil.get(SLDUL), previousSoil.get(SLDUL));
				aggregate &= shouldAggregate(fullCurrentSoil.get(SLBDM), previousSoil.get(SLBDM));

				if (aggregate) {
					// Compute the new map
					aggregatedSoil = computeSoil(fullCurrentSoil, previousSoil);
					if (aggregatedSoilsData.contains(previousSoil)) {
						aggregatedSoilsData.remove(previousSoil);
					}
					// Set as previous soil treated
					previousSoil = aggregatedSoil;
					aggregatedSoilsData.add(aggregatedSoil);
				} else {
					previousSoil = fullCurrentSoil;
					aggregatedSoilsData.add(fullCurrentSoil);
				}
			} else {
				previousSoil = currentSoil;
			}
		}
		return aggregatedSoilsData;
	}

	private HashMap<String, String> computeSoil(Map<String, String> fullCurrentSoil, Map<String, String> previousSoil) {
		HashMap<String, String> aggregatedSoil;
		aggregatedSoil = new HashMap<String, String>();
		for (String p : mergeParams) {
			Float newValue = (Float.parseFloat(fullCurrentSoil.get(p)) + Float.parseFloat(previousSoil.get(p))) / 2f;
			aggregatedSoil.put(p, newValue.toString());
		}
		System.out.println(Float.parseFloat(fullCurrentSoil.get(SKSAT))  +" "+ Float.parseFloat(previousSoil.get(SKSAT)) );
		aggregatedSoil.put(SKSAT, String.valueOf(Float.parseFloat(fullCurrentSoil.get(SKSAT)) + Float.parseFloat(previousSoil.get(SKSAT))));
		return aggregatedSoil;
	}

}
