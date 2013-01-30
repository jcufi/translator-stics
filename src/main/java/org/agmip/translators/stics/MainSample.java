package org.agmip.translators.stics;

import static org.agmip.translators.stics.util.SticsUtil.newFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.agmip.translators.stics.util.IcasaCode;
import org.agmip.translators.stics.util.Report;
import org.agmip.translators.stics.util.SticsUtil;

public class MainSample {

	private static String PLANT_FILE_SUFFIX = "-plant-file";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		WeatherOutput weatherOutput;
		SoilAndInitOutput soilOutput;
		ManagementOutput mgmtOutput;
		Map data;
		HashMap<String, String> files;
		ArrayList<String> climaticFiles;
		String jsonFile;
		String outputDir;
		outputDir = System.getProperty("user.dir") + File.separator + "workspace";
		// jsonFile = "/new_version.json";
		jsonFile = "/KSAS8101WH_1.json";
		// jsonFile = "/MACH0001MZ.json";
		// jsonFile = "/MACH0004MZ.json";
		weatherOutput = new WeatherOutput();
		soilOutput = new SoilAndInitOutput();
		mgmtOutput = new ManagementOutput();

		// Get JSON data
		data = SticsUtil.getDataFrom(jsonFile);
		// Run weather
		weatherOutput.writeFile(outputDir, data);
		// Run Soil
		soilOutput.writeFile(outputDir, data);
		// Run management
		mgmtOutput.writeFile(outputDir, data);
		// Run USM generation
		climaticFiles = weatherOutput.getClimaticFiles();
		files = new HashMap<String, String>();
		files.put(UsmFile.INIT_FILE, soilOutput.getInitializationFile().getName());
		files.put(UsmFile.SOIL_FILE, soilOutput.getSoilFile().getName());
		files.put(UsmFile.STATION_FILE, weatherOutput.getStationFile().getName());
		files.put(UsmFile.TEC_FILE, mgmtOutput.getManagementFile().getName());
		files.put(UsmFile.PLANT_FILE, IcasaCode.toSticsCode(mgmtOutput.getCrid() + PLANT_FILE_SUFFIX));
		UsmFile usm = new UsmFile(files, climaticFiles);
		usm.writeFile(outputDir, data);
		// Report generation
		try {
			newFile(Report.getContent(), outputDir, "README.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
