package org.agmip.translators.stics;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;

import org.agmip.translators.soil.LayerReducerUtil;
import org.junit.Test;

public class SoilAndInitOutputTest {

	@Test
	public void testMergeSoilData() {
		// soil info
		//[{sllb=15, slll=.072, sldul=.225, slsat=.275, slrgf=1, slbdm=1.15, sloc=.61}, {sllb=30, slll=.07, sldul=.24, slsat=.29, slrgf=.7, slbdm=1.16, sloc=.61}, {sllb=60, slll=.04, sldul=.154, slsat=.194, slrgf=.2, slbdm=1.21, sloc=.59}, {sllb=90, slll=.032, sldul=.091, slsat=.141, slrgf=.05, slbdm=1.23, sloc=.29}, {sllb=120, slll=.032, sldul=.087, slsat=.137, slrgf=.03, slbdm=1.31, sloc=.24}, {sllb=150, slll=.032, sldul=.087, slsat=.137, slrgf=.01, slbdm=1.31, sloc=.2}, {sllb=180, slll=.032, sldul=.087, slsat=.137, slrgf=.01, slbdm=1.31, sloc=.2}]
		// ini info
		//[{icbl=15, ich2o=.205, icnh4=3.4, icno3=9.8}, {icbl=30, ich2o=.17, icnh4=3.2, icno3=7.3}, {icbl=60, ich2o=.092, icnh4=2.5, icno3=5.1}, {icbl=90, ich2o=.065, icnh4=2.2, icno3=4.7}, {icbl=120, ich2o=.066, icnh4=2.7, icno3=4.3}, {icbl=150, ich2o=.066, icnh4=2.7, icno3=4.3}, {icbl=180, ich2o=.066, icnh4=2.7, icno3=4.3}]
	
		ArrayList<HashMap<String, String>> soilsData = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> soilInfo1 = new HashMap<String, String>();
		HashMap<String, String> soilInfo2 = new HashMap<String, String>();
		soilInfo1.put("sllb", "15");
		soilInfo1.put("slbdm", "1");
		soilInfo2.put("sllb", "30");
		soilInfo2.put("slbdm", "2");
		soilsData.add(soilInfo1);
		soilsData.add(soilInfo2);
		
		ArrayList<HashMap<String, String>> iniData = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> iniInfo1 = new HashMap<String, String>();
		HashMap<String, String> iniInfo2 = new HashMap<String, String>();
		
		iniInfo1.put("icbl", "15");
		iniInfo1.put("icno3", "9.8");
		iniInfo2.put("icbl", "30");
		iniInfo2.put("icno3", "7.6");
		
		iniData.add(iniInfo1);
		iniData.add(iniInfo2);
		
		SoilAndInitOutput o = new SoilAndInitOutput(System.getProperty("user.dir"));
		LayerReducerUtil.mergeSoilAndInitializationData(soilsData, iniData);
		for(HashMap<String, String> s: soilsData){
			assertTrue(s.containsKey("icno3"));
			assertTrue(s.containsKey("sllb"));
			assertTrue(s.containsKey("slbdm"));
			assertEquals(s.get("sllb"), s.get("icbl"));
		}
		
	
	}

}
