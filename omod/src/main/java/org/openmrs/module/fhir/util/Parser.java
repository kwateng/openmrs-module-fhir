/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.fhir.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.parser.IParser;
import org.openmrs.module.fhir.api.manager.FHIRContextFactory;

public class Parser {

	public static String parse(IResource resource, String contentType) {

		FhirContext ctx = FHIRContextFactory.getFHIRContext();
		ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		IParser jsonParser = ctx.newJsonParser();
		IParser xmlParser = ctx.newXmlParser();
		String encoded = null;
		if (contentType != null) {
			if (contentType.equals("application/xml+fhir")) {
				xmlParser.setPrettyPrint(true);
				encoded = xmlParser.encodeResourceToString(resource);
			} else {
				jsonParser.setPrettyPrint(true);
				encoded = jsonParser.encodeResourceToString(resource);
			}
		} else {
			jsonParser.setPrettyPrint(true);
			encoded = jsonParser.encodeResourceToString(resource);
		}

		return encoded;
	}

	public static String parseObs(Observation observation, String contentType) {

		FhirContext ctx = FHIRContextFactory.getFHIRContext();
		ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator()
		);

		IParser jsonParser = ctx.newJsonParser();
		IParser xmlParser = ctx.newXmlParser();
		String encoded = null;
		if (contentType != null)
		{
			if (contentType.equals("application/xml+fhir")) {
				xmlParser.setPrettyPrint(true);
				encoded = xmlParser.encodeResourceToString(observation);
			} else {
				jsonParser.setPrettyPrint(true);
				encoded = jsonParser.encodeResourceToString(observation);
			}
		} else {
			jsonParser.setPrettyPrint(true);
			encoded = jsonParser.encodeResourceToString(observation);
		}
		return encoded;
	}

}
