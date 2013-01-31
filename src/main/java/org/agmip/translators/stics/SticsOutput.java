package org.agmip.translators.stics;

import static org.agmip.translators.stics.util.SticsUtil.newFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.agmip.core.types.TranslatorOutput;
import org.agmip.translators.stics.util.IcasaCode;
import org.agmip.translators.stics.util.Report;
import org.agmip.translators.stics.util.SticsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for Stics file generation
 * @author jucufi
 *
 */
public class SticsOutput implements TranslatorOutput {
	private static final Logger log = LoggerFactory.getLogger(SticsOutput.class);
	private static String PLANT_FILE_SUFFIX = "-plant-file";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDir;
		String jsonFile;
		SticsOutput sticsOut;
		Map data;
		//jsonFile = "/new_version.json";
		jsonFile = "/KSAS8101WH_1.json";
		// jsonFile = "/MACH0001MZ.json";
		// jsonFile = "/MACH0004MZ.json";
		// Get JSON data
		data = SticsUtil.getDataFrom(jsonFile);
		outputDir = System.getProperty("user.dir") + File.separator + "workspace";
		sticsOut = new SticsOutput();
		sticsOut.writeFile(outputDir, data);
	}

	public void writeFile(String outputDir, Map data) {
		WeatherOutput weatherOutput;
		SoilAndInitOutput soilOutput;
		ManagementOutput mgmtOutput;
		HashMap<String, String> files;
		ArrayList<String> climaticFiles;

		weatherOutput = new WeatherOutput();
		soilOutput = new SoilAndInitOutput();
		mgmtOutput = new ManagementOutput();

		// Run weather
		weatherOutput.writeFile(outputDir, data);
		// Run Soil
		soilOutput.writeFile(outputDir, data);
		// Run management
		mgmtOutput.writeFile(outputDir, data);
		// Run USM generation
		climaticFiles = weatherOutput.getClimaticFiles();
		files = new HashMap<String, String>();
		files.put(UsmOutput.INIT_FILE, soilOutput.getInitializationFile().getName());
		files.put(UsmOutput.SOIL_FILE, soilOutput.getSoilFile().getName());
		files.put(UsmOutput.STATION_FILE, weatherOutput.getStationFile().getName());
		files.put(UsmOutput.TEC_FILE, mgmtOutput.getManagementFile().getName());
		files.put(UsmOutput.PLANT_FILE, IcasaCode.toSticsCode(mgmtOutput.getCrid() + PLANT_FILE_SUFFIX));
		UsmOutput usm = new UsmOutput(files, climaticFiles);
		usm.writeFile(outputDir, data);
		// Report generation
		try {
			newFile(Report.getContent(), outputDir, "README.txt");
		} catch (IOException e) {
			log.error("Unable to generate report");
			log.error(e.toString());
		}

	}

}
