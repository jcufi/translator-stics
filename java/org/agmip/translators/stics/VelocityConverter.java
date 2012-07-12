package org.agmip.translators.stics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

public abstract class VelocityConverter {

	public void runVelocity(Context context, String templateFile) {
		try {
			Template template = null;
			try {
				template = Velocity.getTemplate(templateFile);
			} catch (ResourceNotFoundException rnfe) {
				System.out.println("Example : error : cannot find template " + templateFile);
			} catch (ParseErrorException pee) {
				System.out.println("Example : Syntax error in template " + templateFile + ":" + pee);
			}
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));
			if (template != null) {
				template.merge(context, writer);
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public VelocityContext fillVelocityContext(LinkedHashMap<String, String> firstLevelValues, List<LinkedHashMap<String, String>> nestedValues) {
		VelocityContext context = new VelocityContext();
		SoilAggregator aggregator = new SoilAggregator();
		Velocity.init(System.getProperty("user.dir") + File.separator + "velocity.properties");
		context.put("listOfMaps", nestedValues);
		//context.put("listOfMaps", aggregator.merge(nestedValues));
		context.put("firstLevelFields", firstLevelValues);
		return context;
	}

}
