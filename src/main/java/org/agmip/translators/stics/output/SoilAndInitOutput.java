package org.agmip.translators.stics.output;

import static org.agmip.translators.stics.util.Const.ICH2O;
import static org.agmip.translators.stics.util.Const.ICNH4;
import static org.agmip.translators.stics.util.Const.ICNO3;
import static org.agmip.translators.stics.util.Const.SLBDM;
import static org.agmip.translators.stics.util.Const.SLDUL;
import static org.agmip.translators.stics.util.Const.SLLB;
import static org.agmip.translators.stics.util.Const.SLLL;
import static org.agmip.translators.stics.util.SticsUtil.createDefaultListOfMap;
import static org.agmip.translators.stics.util.SticsUtil.newFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.agmip.translators.soil.LayerReducer;
import org.agmip.translators.soil.LayerReducerUtil;
import org.agmip.translators.soil.SAReducerDecorator;
import org.agmip.translators.stics.model.ExperimentInfo;
import org.agmip.translators.stics.util.SticsUtil;
import org.agmip.translators.stics.util.VelocityUtil;
import org.agmip.util.MapUtil;
import org.agmip.util.MapUtil.BucketEntry;
import org.apache.velocity.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible of soil file generation and initialization file generation. These two kind of informations are
 * linked and we need to process these at the same time.
 * 
 * @author jucufi
 * 
 */
public class SoilAndInitOutput extends SticsFileGenerator {
	private static final int MAX_STICS_SOIL_LAYERS = 5;
	public static final Logger log = LoggerFactory.getLogger(SoilAndInitOutput.class);
	public static final String SOIL_TEMPLATE_FILE = "/soil_template.vm";
	public static final String INI_TEMPLATE_FILE = "/ini_template.vm";
	public static final String XML_START = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><sols>";
	public static final String XML_END = "</sols>";
	public static final String STICS_SOIL_FILE_NAME = "sols.xml";
	private static final String[] INIT_PARAM_TO_CONVERT = { ICNO3, ICNH4 };
	private static final String[] SOIL_PARAM_TO_CONVERT = { SLDUL, SLLL, ICH2O };
	private static final String[] SOIL_LAYER_PARAMS = new String[] { ICH2O, ICNO3, ICNH4, SLLB, SLDUL, SLBDM, SLLL };
	private StringBuffer soilBuffer;
	private HashMap<String, String> soilAdded;
	private String filePath;

	public SoilAndInitOutput() {
		super();
		soilAdded = new HashMap<String, String>();
		soilBuffer = new StringBuffer();
		soilBuffer.append(XML_START);
	}

	public SoilAndInitOutput(String path) {
		this();
		this.filePath = path;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	/**
	 * Starts stics soil file generation
	 * @param data
	 * @param expInfo experiment information
	 * @param soilBucket bucket of soil data
	 */
	public void writeFile(Map data, ExperimentInfo expInfo, BucketEntry soilBucket) {
		Map<String, String> firstLevelSoilData;
		ArrayList<HashMap<String, String>> nestedSoilData;
		Map<String, String> firstLevelInitData;
		ArrayList<HashMap<String, String>> nestedInitData;
		ArrayList<HashMap<String, String>> aggregatedSoilData;
		BucketEntry iniBucket;
		LayerReducer soilAgg;
		File initFile;
		String soilId;

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
			soilId = firstLevelSoilData.get("soil_id");
			getConverter().convertInitValues(nestedSoilData, INIT_PARAM_TO_CONVERT);
			if (!soilAdded.containsKey(soilId)) {
				getConverter().convertSoil(nestedSoilData, SOIL_PARAM_TO_CONVERT);
			}
			// 3 - reduce soil layers
			aggregatedSoilData = soilAgg.process(nestedSoilData);
			// 4 - fill to 5 layers
			createDefaultListOfMap(aggregatedSoilData, SOIL_LAYER_PARAMS, MAX_STICS_SOIL_LAYERS);
			// Convert first level data 
			getConverter().convertFirstLevelRecords(firstLevelInitData);
			// Convert nested data or fill with default values if they are missing 
			getConverter().convertNestedRecords(aggregatedSoilData, new String[] { "icnh4", "icno3", "ich2o" });
			// Generate initialization file
			initFile = generateInitializationFile(soilId, expInfo.getExpId(), firstLevelInitData, aggregatedSoilData);
			expInfo.setIniFile(initFile.getName());
			if (!soilAdded.containsKey(soilId)) {
				// Generate soil file
				generateSoilData(soilId, firstLevelSoilData, aggregatedSoilData);
				soilAdded.put(soilId, soilId);
			}
		} catch (IOException e) {
			log.error("Unable to generate soil file.");
			log.error(e.toString());
		}
	}

	public void generateSoilsFile() throws IOException {
		log.info("Generating soil file : " + STICS_SOIL_FILE_NAME);
		newFile(soilBuffer.append(XML_END).toString(), filePath, STICS_SOIL_FILE_NAME);
	}

	/**
	 * Starts initialization file generation
	 * 
	 * @param firstLevelInitData
	 * @param aggregatedSoilData
	 * @return
	 * @throws IOException
	 */
	public File generateInitializationFile(String soilId, String expId, Map<String, String> firstLevelInitData, List<HashMap<String, String>> aggregatedSoilData)
			throws IOException {
		Context velocityContext;
		String content;
		String iniFileName;
		File initFile;
		iniFileName = soilId + "_" + expId + "_ini" + ".xml";
		log.info("Generating initialization file : " + iniFileName);
		velocityContext = VelocityUtil.fillVelocityContext(firstLevelInitData, aggregatedSoilData);
		content = VelocityUtil.getInstance().runVelocity(velocityContext, INI_TEMPLATE_FILE);
		initFile = newFile(content, filePath, iniFileName);
		return initFile;
	}

	/**
	 * Starts soil file generation
	 * 
	 * @param firstLevelSoilData
	 * @param aggregatedSoilData
	 * @return
	 */
	public void generateSoilData(String soilId, Map<String, String> firstLevelSoilData, List<HashMap<String, String>> aggregatedSoilData) {
		Context velocityContext;
		String content;
		log.info("Processing soil data for : " + soilId);
		// Convert and put default values these params have default values : slcly, salb, slphw ,sksat, caco3, sloc
		getConverter().convertFirstLevelRecords(firstLevelSoilData);
		SticsUtil.defaultValueForMap(new String[] { "slcly", "salb", "slphw", "sksat", "caco3" }, firstLevelSoilData);
		getConverter().convertNestedRecords(aggregatedSoilData, new String[] { "sksat" });
		// for the sloc param we'll use only the first layer parameter
		firstLevelSoilData.put("sloc", getConverter().convertToSticsUnit("sloc", MapUtil.getValueOr(aggregatedSoilData.get(0), "sloc", "0.0")));
		velocityContext = VelocityUtil.fillVelocityContext(firstLevelSoilData, aggregatedSoilData);
		content = VelocityUtil.getInstance().runVelocity(velocityContext, SOIL_TEMPLATE_FILE);
		soilBuffer.append(content);
	}

}
