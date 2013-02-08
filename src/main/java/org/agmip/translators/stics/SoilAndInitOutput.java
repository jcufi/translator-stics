package org.agmip.translators.stics;

import static java.lang.Float.parseFloat;
import static org.agmip.translators.stics.util.SticsUtil.convertFirstLevelRecords;
import static org.agmip.translators.stics.util.SticsUtil.convertNestedRecords;
import static org.agmip.translators.stics.util.SticsUtil.newFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.agmip.translators.soil.LayerReducer;
import org.agmip.translators.soil.LayerReducerUtil;
import org.agmip.translators.soil.SAReducerDecorator;
import org.agmip.translators.stics.util.SticsUtil;
import org.agmip.translators.stics.util.VelocityUtil;
import org.agmip.util.MapUtil;
import org.agmip.util.MapUtil.BucketEntry;
import org.apache.velocity.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible of soil file generation and initialization file generation.
 * These two kind of informations are linked and we need to process these at the
 * same time.
 * 
 * @author jucufi
 * 
 */
public class SoilAndInitOutput  {
	public static final Logger log = LoggerFactory.getLogger(SoilAndInitOutput.class);
	public static String SOIL_TEMPLATE_FILE = "/soil_template.vm";
	public static String INI_TEMPLATE_FILE = "/ini_template.vm";
	public File initFile;
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

	private static String[] SOILS_PARAMS = new String[] { SLLL, SLDUL, SLBDM, SKSAT, SLLB, SLOC };
	private StringBuffer soilBuffer;
	HashMap<String, String> soilAdded;
	private String filePath;
	public SoilAndInitOutput(String filePath) {
		soilAdded = new HashMap<String, String>();
		soilBuffer = new StringBuffer();
		soilBuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><sols>");
		this.filePath = filePath;
	}

	public void convertInitValues(ArrayList<HashMap<String, String>> soilsData) {
		for (HashMap<String, String> currentSoil : soilsData) {
			String[] paramToConvert = new String[] { ICNO3, ICNH4 };
			for (String param : paramToConvert) {
				if (currentSoil.containsKey(param)) {
					currentSoil.put(param, new Float(parseFloat(currentSoil.get(param)) * parseFloat(currentSoil.get(SLBDM)) * parseFloat(currentSoil.get(SLLB)) / 10f).toString());
				}
			}
		}
	}
	
	public void convertSoil(ArrayList<HashMap<String, String>> soilsData) {
		for (HashMap<String, String> currentSoil : soilsData) {
			// Convertion
			String[] paramToConvert = { SLDUL, SLLL, ICH2O };
			for (String param : paramToConvert) {
				if (currentSoil.containsKey(param)) {
					currentSoil.put(param, new Float(parseFloat(currentSoil.get(param)) / parseFloat(currentSoil.get(SLBDM)) * 100f).toString());
				}
			}
		}
	}

	/**
	 * @see org.agmip.core.types.TranslatorOutput
	 */
	public void writeFile(Map data, String expId, BucketEntry soilBucket) {
		BucketEntry iniBucket;
		LayerReducer soilAgg;
		HashMap<String, String> firstLevelSoilData;
		ArrayList<HashMap<String, String>> nestedSoilData;
		HashMap<String, String> firstLevelInitData;
		ArrayList<HashMap<String, String>> nestedInitData;
		ArrayList<HashMap<String, String>> aggregatedSoilData;
		
		soilAgg = new LayerReducer(new SAReducerDecorator());

		try {
			iniBucket = MapUtil.getBucket(data, "initial_conditions");
			// Soil structure
			firstLevelSoilData = new HashMap<String, String>(soilBucket.getValues());
			nestedSoilData = new ArrayList<HashMap<String, String>>(soilBucket.getDataList());
			// Initialization structure
			firstLevelInitData = soilBucket.getValues();
			nestedInitData = iniBucket.getDataList();
			// Put soil information in the same map
			LayerReducerUtil.mergeSoilAndInitializationData(nestedSoilData, nestedInitData);
			// Merge soil layers
			// 1 - compute layer thickness instead of layer deep
			LayerReducerUtil.computeSoilLayerSize(nestedSoilData);
			// 2 - convertion
			String soilId = firstLevelSoilData.get("soil_id");
			convertInitValues(nestedSoilData);
			if(!soilAdded.containsKey(soilId)){
				convertSoil(nestedSoilData);
			}
			// 3 - reduce soil layers
			aggregatedSoilData = soilAgg.process(nestedSoilData);
			// 4 - Fill to 5 layers
			SticsUtil.fill(aggregatedSoilData, new String[]{"ich2o", "icno3", "icnh4", "sllb", "sldul", "slbdm", "slll"}, 5);
			// Generate initialization file
			String content = generateInitializationFile(firstLevelInitData, aggregatedSoilData);
			String iniFileName = soilId + "_" + expId + "_ini" + ".xml";
			initFile = newFile(content, filePath, iniFileName);
			log.info("Generating initialization file : "+iniFileName);
			if(!soilAdded.containsKey(soilId)){
				// Generate soil file
				log.info("Generating soil file for : "+soilId);
				content = generateSoilFile(firstLevelSoilData, aggregatedSoilData);
				soilBuffer.append(content);
				soilAdded.put(soilId, soilId);
			}
		} catch (IOException e) {
			log.error("Unable to generate soil file.");
			log.error(e.toString());
		}

	}

	public void generateSoilsFile() throws IOException {
		File soilFile = newFile(soilBuffer.append("</sols>").toString(), filePath, "sols.xml");
		log.info("Generating soil file : "+soilFile.getName());
	}

	/**
	 * Starts initialization file generation
	 * 
	 * @param firstLevelInitData
	 * @param aggregatedSoilData
	 * @return
	 */
	public String generateInitializationFile(HashMap<String, String> firstLevelInitData, ArrayList<HashMap<String, String>> aggregatedSoilData) {
		Context velocityContext;
		// Convert and put default values
		// these params have default values : icnh4,icno3,ich2o
		convertFirstLevelRecords(firstLevelInitData);
		// SticsUtil.defaultValueFor(Arrays.asList(, firstLevelInitData);
		convertNestedRecords(aggregatedSoilData,new String[] { "icnh4", "icno3", "ich2o" });
		velocityContext = VelocityUtil.fillVelocityContext(firstLevelInitData, aggregatedSoilData);
		return VelocityUtil.getInstance().runVelocity(velocityContext, INI_TEMPLATE_FILE);
	}

	/**
	 * Starts soil file generation
	 * 
	 * @param firstLevelSoilData
	 * @param aggregatedSoilData
	 * @return
	 */
	public String generateSoilFile(HashMap<String, String> firstLevelSoilData, ArrayList<HashMap<String, String>> aggregatedSoilData) {
		Context velocityContext;
		// Convert and put default values
		// these params have default values : slcly, salb, slphw ,sksat, caco3,
		// sloc
		convertFirstLevelRecords(firstLevelSoilData);
		SticsUtil.defaultValueForMap(new String[] { "slcly", "salb", "slphw", "sksat", "caco3" }, firstLevelSoilData);
		convertNestedRecords(aggregatedSoilData, new String[] { "sksat" });
		// for the sloc param we'll use only the first layer parameter
		firstLevelSoilData.put("sloc", SticsUtil.convert("sloc", MapUtil.getValueOr(aggregatedSoilData.get(0), "sloc", "0.0")));
		velocityContext = VelocityUtil.fillVelocityContext(firstLevelSoilData, aggregatedSoilData);
		return VelocityUtil.getInstance().runVelocity(velocityContext, SOIL_TEMPLATE_FILE);
	}

	/**
	 * Return initialization file
	 * 
	 * @return
	 */
	public File getInitializationFile() {
		return initFile;
	}

	public static void main(String[] args) {
		// new SoilVelocityConverter().perform();
	}
}
