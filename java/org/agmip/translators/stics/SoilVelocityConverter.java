package org.agmip.translators.stics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.agmip.util.JSONAdapter;
import org.agmip.util.MapUtil;
import org.agmip.util.MapUtil.BucketEntry;
import org.apache.velocity.context.Context;

public class SoilVelocityConverter extends VelocityConverter {

	/*
	 * public VelocityContext fillVelocityContext() { LinkedList<Map<String,
	 * String>> soilList; String[] soilFirstLevel = new String[] { "SOIL_ID",
	 * "CLAY", "SAOC", "CACO3", "SLPHW", "SALB" };
	 * Velocity.init(System.getProperty("user.dir") + File.separator +
	 * "velocity.properties");
	 * 
	 * VelocityContext context = new VelocityContext(); for (String key :
	 * soilFirstLevel) { context.put(key, "test"); } soilList = new
	 * LinkedList<Map<String, String>>(); Map<String, String> soilMap = new
	 * HashMap<String, String>(); soilMap.put("SLLB", "test_ellb");
	 * soilList.push(soilMap); context.put("listOfMaps", soilList); return
	 * context; }
	 */

	public static void main(String[] args) {
		SoilVelocityConverter velocityConverter = new SoilVelocityConverter();
		ArrayList<BucketEntry> weatherRecords;
		String templateFile = "/soil_template.vm";
		try {
			//JSONAdapter adapter = new JSONAdapter();
			Map data = JSONAdapter.fromJSON(SticsUtil.getDataFromTestFile("/ufga8201_mzx.json"));
			LinkedHashMap<String, String> firstLevelRecords;
			weatherRecords = MapUtil.getBucket(data, "soil");
			for (BucketEntry bucket : weatherRecords) {
				firstLevelRecords = weatherRecords.get(0).getValues();
				Context velocityContext = velocityConverter.fillVelocityContext(firstLevelRecords, bucket.getDataList());
				velocityConverter.runVelocity(velocityContext, templateFile);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String[] getMultivaluedFields() {
		return new String[] { "SLLB", "SLDUL", "SLLL", "SLBDM", "SKSAT" };
	}

	public String[] getSinglevaluedFields() {
		return new String[] { "SOIL_ID", "CLAY", "SAOC", "CACO3", "SLPHW", "SALB" };
	}

}
