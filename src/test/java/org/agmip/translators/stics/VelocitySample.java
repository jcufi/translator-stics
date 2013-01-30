package org.agmip.translators.stics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.agmip.translators.stics.util.SticsUtil;
import org.agmip.translators.stics.util.VelocityUtil;
import org.agmip.util.MapUtil;
import org.agmip.util.MapUtil.BucketEntry;
import org.apache.velocity.VelocityContext;

public class VelocitySample {

	public static void main(String[] args) {
		BucketEntry initialConditionBucket;
		LinkedHashMap<String, String> firstLevelSoilData;
		ArrayList<LinkedHashMap<String, String>> aggregatedSoilData;
		LinkedHashMap<String, String> firstLevelData;
		List<LinkedHashMap<String, String>> aggregatedData;
		
		
		Map data = SticsUtil.getDataFrom("/test-management.json");
		initialConditionBucket = MapUtil.getBucket(data, "initial_condition");
		
		firstLevelData = initialConditionBucket.getValues();
		aggregatedData = initialConditionBucket.getDataList();
		
		VelocityContext velocityContext = VelocityUtil.fillVelocityContext(firstLevelData, aggregatedData);
		VelocityUtil.runVelocity(velocityContext, "test.vm");
		
		
	}
	
}
