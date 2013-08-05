package org.agmip.translators.stics.output;

import org.agmip.translators.stics.util.UnitConverter;

public class SticsFileGenerator {
	private UnitConverter converter;

	public SticsFileGenerator() {
		converter = new UnitConverter();
	}

	public UnitConverter getConverter() {
		return converter;
	}

	public void setConverter(UnitConverter converter) {
		this.converter = converter;
	}
}
