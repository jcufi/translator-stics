package org.agmip.translators.stics.acmo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.agmip.translators.stics.util.Const;
import org.agmip.translators.stics.util.UnitConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class take the output of stics model and convert into ACMO format.
 * 
 * @author jucufi
 * 
 */
public class OutFileTranslator {
	private static final int DAYS_IN_REGULAR_YEAR = 365;
	private static final int DAYS_IN_LEAP_YEAR = 366;
	private static final int MAX_LINES = 10;
	private UnitConverter converter = new UnitConverter();
	private static final String IFLOS_COLUMN = "iflos";
	private static final String IMATS_COLUMN = "imats";
	private static final String IRECS_COLUMN = "irecs";
	private static final String NOMVERSION = "nomversion";
	private static final String IO_ERROR = "IO Error";
	// starting year
	private static final String ANSEMIS = "ansemis";
	private static final String ACMO_HEADER = "MODEL_VER;HWAH_S;CWAH_S;ADAT_S;MDAT_S;HADAT_S;LAIX_S;PRCP_S;ETCP_S;NUCM_S;NLCM_S";
	private static final String STICS_HEADER = "nomversion;Masec(n);mafruit;iflos;imats;irecs;laimax;cprecip;cet;Qnplante;qles";

	public static final String OUTPUT_CSV_SEPARATOR = ";";
	public static final String INPUT_CSV_SEPARATOR = ";";
	public static final String NEW_LINE = "\n";
	private static final String STICS_OUTPUT_FILE = "stics.out";

	private static final Logger log = LoggerFactory.getLogger(OutFileTranslator.class);

	/**
	 * Check if the directory exists
	 * 
	 * @param dir
	 * @return a status
	 */
	public boolean exists(String dir) {
		boolean result = false;
		File destDir = new File(dir);
		if (!destDir.exists()) {
			log.error("The file or directory " + dir + " doesn't exists");
		} else {
			result = true;
		}
		return result;
	}

	/**
	 * Entry point for processing stics model output
	 * 
	 * @param outputDir the directory used to saved converted files
	 * @param file the output file name
	 * @return a process status
	 * @throws IOException
	 */
	public boolean convert(String outputDir, String file) throws IOException {
		boolean status = false;
		boolean dirExists = exists(outputDir);
		boolean fileExists = exists(file);
		if (fileExists && dirExists) {
			status = processFile(outputDir, file);
		}
		return status;
	}

	/**
	 * Returns a map with csv column positions indexed by column name
	 * 
	 * @param header the csv header
	 * @returna map of positions
	 */
	public HashMap<String, Integer> getPositions(String header) {
		String[] keys = header.split(INPUT_CSV_SEPARATOR);
		HashMap<String, Integer> positions = new HashMap<String, Integer>();
		for (int i = 0; i < keys.length; i++) {
			positions.put(keys[i].toLowerCase(), i);
		}
		return positions;
	}

	/**
	 * Process the stics output file
	 * 
	 * @param dir The dir used to store the resulting file
	 * @param file The file to convert
	 * @return a process status
	 * @throws IOException 
	 */
	private boolean processFile(String dir, String file) throws IOException {
		FileReader reader;
		FileWriter writer;
		File output;
		BufferedReader buffer;
		StringBuilder strBuilder;
		String line;
		boolean status;
		int lineCount;
		HashMap<String, Integer> positions;
		List<String> values;

		writer = null;
		reader = null;
		positions = null;
		status = false;
		lineCount = 0;
		strBuilder = new StringBuilder();

		try {
			log.info("Processing stics output file : " + file);
			reader = new FileReader(new File(file));
			buffer = new BufferedReader(reader);
			output = new File(dir, STICS_OUTPUT_FILE);
			writer = new FileWriter(output);
			while ((line = buffer.readLine()) != null) {
				if (lineCount == 0) {
					// process header
					positions = getPositions(line);
					strBuilder.append(ACMO_HEADER).append(NEW_LINE);
				} else {
					values = Arrays.asList(line.split(INPUT_CSV_SEPARATOR));
					if (values.size() == 0) {
						log.error("Invalid stics file, check the separator.");
					} else if (NOMVERSION.equals(values.get(0))) {
						// the file can contain more than 1 header line, so we check the first column to see if the line 
						// match to the repeated header
						strBuilder.append(ACMO_HEADER).append(NEW_LINE);
					} else {
						processLine(positions, strBuilder, values);
						if (lineCount % MAX_LINES == 0) {
							appendToFile(writer, strBuilder);
							// clear the builder
							strBuilder.setLength(0);
						}
					}
				}
				lineCount += 1;
			}
			appendToFile(writer, strBuilder);
			status = true;
		} catch (IOException e) {
			log.error(IO_ERROR, e);
			if (reader != null) {
				reader.close();
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
			if (writer != null) {
				writer.close();
			}
			log.info("File " + STICS_OUTPUT_FILE + " generated under directory : " + dir);

		}
		return status;
	}
	
	/**
	 * Process a line of csv
	 * 
	 * @param positions a map with csv column positions indexed by column name
	 * @param strBuilder the string builder used to store converted data
	 * @param values the list of data to convert
	 */
	private void processLine(HashMap<String, Integer> positions, StringBuilder strBuilder, List<String> values) {
		List<String> orderredColumns = Arrays.asList(STICS_HEADER.split(INPUT_CSV_SEPARATOR));
		for (String column : orderredColumns) {
			Integer position = positions.get(column.toLowerCase());
			log.debug("column : " + column.toLowerCase() + ", position : " + position);
			if (position == null) {
				log.error("Missing value for " + column);
			} else {
				if (values.get(position) != null) {
					processValue(values.get(position), values.get(positions.get(ANSEMIS)), column, strBuilder);
				}
			}
		}
		strBuilder.append(NEW_LINE);
	}

	/**
	 * In the stics report integers and real have the same format, this method extract and return the integer part of
	 * the input value.
	 * 
	 * @param value The string value to parse, cannot be null
	 * @param column The column name
	 * @return int value of the string.
	 */
	public int readInt(String value, String column) {
		int result = 0;
		String strValue = value.trim();
		String[] splittedValues = strValue.split("\\.");
		try {
			if (splittedValues.length >= 1) {
				result = Integer.parseInt(splittedValues[0]);
			} else {
				logErrorValue(column, strValue);
			}

		} catch (NumberFormatException e) {
			logErrorValue(column, strValue);
		}
		return result;
	}

	/**
	 * Log an error value.
	 * 
	 * @param column the column name
	 * @param strValue the column value
	 */
	private void logErrorValue(String column, String strValue) {
		if ("".equals(strValue)) {
			log.error("Empty value for column " + column);
		} else {
			log.error("Unable to parse value : '" + strValue + "' for column " + column);
		}
	}

	/**
	 * Convert a single value
	 * 
	 * @param value the value to convert
	 * @param ansemis starting year (used for date convertion)
	 * @param column the column name
	 * @param strBuilder the string builder used to store converted data
	 */
	private void processValue(String value, String ansemis, String column, StringBuilder strBuilder) {
		String strDate;
		int julianDay;
		int year;
		if (IFLOS_COLUMN.equalsIgnoreCase(column) || IMATS_COLUMN.equalsIgnoreCase(column) || IRECS_COLUMN.equalsIgnoreCase(column)) {
			if (ansemis != null) {
				year = readInt(ansemis.trim(), ANSEMIS);
				julianDay = readInt(value.trim(), column);
				strDate = toDate(julianDay, year);
				strBuilder.append(strDate).append(OUTPUT_CSV_SEPARATOR);
			} else {
				log.error("Unable to read " + ANSEMIS);
			}
		} else {
			strBuilder.append(converter.convertToIcasaUnit(column, value.trim())).append(OUTPUT_CSV_SEPARATOR);
		}
	}

	/**
	 * Convert a julian day + the current year into a ACMO date
	 * 
	 * @param julianDay julian day
	 * @param year the starting year
	 * @return A string representation of the date
	 */
	public String toDate(int julianDay, int year) {
		SimpleDateFormat formatter = new SimpleDateFormat(Const.ACMO_DATE_FORMAT);
		GregorianCalendar calendar = new GregorianCalendar();
		int julianDaysInYear;
		int yearComputed;
		int julianDaysComputed;

		if (calendar.isLeapYear(year)) {
			julianDaysInYear = DAYS_IN_LEAP_YEAR;
		} else {
			julianDaysInYear = DAYS_IN_REGULAR_YEAR;
		}
		if (julianDay > julianDaysInYear) {
			// It's a two years crop, so the julian day is computed on 2 years
			yearComputed = year + 1;
			julianDaysComputed = julianDay - julianDaysInYear;
		} else {
			yearComputed = year;
			julianDaysComputed = julianDay;
		}
		calendar.set(GregorianCalendar.DAY_OF_YEAR, julianDaysComputed);
		calendar.set(GregorianCalendar.YEAR, yearComputed);
		return formatter.format(calendar.getTime());
	}

	/**
	 * Append the string at the end of the file
	 * 
	 * @param writer the writer used to create the result file
	 * @param strBuilder the string builder used to store converted data
	 * @throws IOException
	 */
	private void appendToFile(FileWriter writer, StringBuilder strBuilder) throws IOException {
		writer.append(strBuilder.toString());
		writer.flush();

	}

	/**
	 * Sample method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			OutFileTranslator translator = new OutFileTranslator();
			translator.convert(System.getProperty("user.dir"), System.getProperty("user.dir") + File.separator + "test.csv");
		} catch (IOException e) {
			log.error(IO_ERROR, e);
		}

	}
}
