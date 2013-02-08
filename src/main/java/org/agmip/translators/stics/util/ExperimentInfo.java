package org.agmip.translators.stics.util;

import java.util.ArrayList;
import java.util.Date;

public class ExperimentInfo {
	public static int YEAR = 365;
	private String expId;
	private String soilId;
	private String weatherId;
	private String cropId;
	private String icdat;
	private int duration;
	private Date startingDate;
	public Date getStartingDate() {
		return startingDate;
	}

	public void setStartingDate(Date startingDate) {
		this.startingDate = startingDate;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	private static String PLANT_FILE_SUFFIX = "-plant-file";

	public String getPlantFile() {
		return IcasaCode.toSticsCode(cropId + PLANT_FILE_SUFFIX);
	}

	private ArrayList<String> weatherFiles;

	public ExperimentInfo() {
		weatherFiles = new ArrayList<String>();
	}

	public ArrayList<String> getWeatherFiles() {
		return weatherFiles;
	}

	public void setWeatherFiles(ArrayList<String> weatherFiles) {
		this.weatherFiles = weatherFiles;
	}

	public String getIcdat() {
		return icdat;
	}

	public void setIcdat(String icdat) {
		this.icdat = icdat;
	}

	public String getEndDate() {
		String endDate;
		if (weatherFiles.size() > 1) {
			endDate = String.valueOf(2 * YEAR);
		} else {
			endDate = String.valueOf(YEAR);
		}
		return endDate;
	}

	private String mngmtFile;
	private String iniFile;
	private String stationFile;

	public String getIniFile() {
		return iniFile;
	}

	public String getStationFile() {
		return stationFile;
	}

	public void setStationFile(String weatherFile) {
		this.stationFile = weatherFile;
	}

	public void setIniFile(String iniFile) {
		this.iniFile = iniFile;
	}

	public String getSoilFile() {
		return soilFile;
	}

	public void setSoilFile(String soilFile) {
		this.soilFile = soilFile;
	}

	private String soilFile;

	public String getMngmtFile() {
		return mngmtFile;
	}

	public void setMngmtFile(String mngmtFile) {
		this.mngmtFile = mngmtFile;
	}

	public String getCropId() {
		return cropId;
	}

	public void setCropId(String cropId) {
		this.cropId = cropId;
	}

	public ExperimentInfo(String expId, String soilId, String weatherId) {
		this();
		this.expId = expId;
		this.soilId = soilId;
		this.weatherId = weatherId;
	}

	public String getExpId() {
		return expId;
	}

	public void setExpId(String expId) {
		this.expId = expId;
	}

	public String getSoilId() {
		return soilId;
	}

	public void setSoilId(String soilId) {
		this.soilId = soilId;
	}

	public String getWeatherId() {
		return weatherId;
	}

	public void setWeatherId(String weatherId) {
		this.weatherId = weatherId;
	}

}
