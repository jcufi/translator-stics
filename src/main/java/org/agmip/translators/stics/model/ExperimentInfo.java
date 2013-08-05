package org.agmip.translators.stics.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.agmip.translators.stics.util.CropCycle;
import org.agmip.translators.stics.util.IcasaCode;

public class ExperimentInfo {
	private static final int YEAR = 365;
	private String latitude;
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

	public String getTwoYearsCrop() {
		return CropCycle.getCultureAn(cropId, latitude);
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

	private List<String> weatherFiles;

	public ExperimentInfo() {
		weatherFiles = new ArrayList<String>();
	}

	public List<String> getWeatherFiles() {
		return weatherFiles;
	}

	public void setWeatherFiles(List<String> weatherFiles) {
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
		if (CropCycle.isATwoYearsCrop(cropId, latitude)) {
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

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

}
