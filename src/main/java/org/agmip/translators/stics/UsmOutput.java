package org.agmip.translators.stics;

import static org.agmip.translators.stics.util.SticsUtil.newFile;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.agmip.core.types.TranslatorOutput;
import org.agmip.translators.stics.util.SticsUtil;
import org.agmip.translators.stics.util.VelocityUtil;
import org.agmip.util.MapUtil;
import org.agmip.util.MapUtil.BucketEntry;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Class responsible of usm file generation (configuration file specific to stics).
 * @author jucufi
 *
 */
public class UsmOutput implements TranslatorOutput {
	private static final Logger log = LoggerFactory.getLogger(UsmOutput.class);
	public static String INIT_FILE = "init_file";
	public static String STATION_FILE = "station_file";
	public static String SOIL_FILE = "soil_file";
	public static String CLIM_FILES = "clim_files";
	public static String TEC_FILE = "tec_file";
	public static String PLANT_FILE = "plant_file";
	public static String DATE_FORMAT = "yyyyMMdd";
	public static int YEAR = 365;
	public static String USM_TEMPLATE_FILE = "/usms_template.vm";
	private HashMap<String, String> fileNames;
	private ArrayList<String> climaticFiles;

	public UsmOutput(HashMap<String, String> otherFiles, ArrayList<String> climFiles) {
		fileNames = otherFiles;
		climaticFiles = climFiles;
	}

	/**
	 * @see org.agmip.core.types.TranslatorOutput
	 */
	public void writeFile(String file, Map data) {
		String exname;
		String content;
		BucketEntry initialConditionBucket;
		initialConditionBucket = MapUtil.getBucket(data, "initial_condition");
		// init file, station file, clim files, tec file, plant file
		String icdat = initialConditionBucket.getValues().get("icdat");
		int icdatJulianDay;
		icdatJulianDay = 0;
		SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
		try {
			icdatJulianDay = SticsUtil.getJulianDay(formatter.parse(icdat));
		} catch (ParseException e) {
			log.error(e.toString());
			log.error("Unable to parse icdat");
		}
		exname = (String) data.get("exname");
		String soilId = (String) data.get("soil_id");
		VelocityContext context = VelocityUtil.newVelocitycontext();
		context.put("exname", exname);
		context.put("soil_id", soilId);
		context.put("icdat", String.valueOf(icdatJulianDay));
		if (climaticFiles.size() > 1) {
			context.put("dateFin", 2 * YEAR);
		} else {
			context.put("dateFin", YEAR);
		}
		context.put(INIT_FILE, fileNames.get(INIT_FILE));
		context.put(STATION_FILE, fileNames.get(STATION_FILE));
		context.put(SOIL_FILE, fileNames.get(SOIL_FILE));
		context.put(CLIM_FILES, climaticFiles);
		context.put(TEC_FILE, fileNames.get(TEC_FILE));
		context.put(PLANT_FILE, fileNames.get(PLANT_FILE));
		content = VelocityUtil.getInstance().runVelocity(context, USM_TEMPLATE_FILE);
		try {
			newFile(content, file, "usms.xml");
		} catch (IOException e) {
			log.error("Unable to generate usm file.");
			log.error(e.toString());
		}
	}

}
