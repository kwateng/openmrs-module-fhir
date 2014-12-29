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
package org.openmrs.module.fhir.resources;

import ca.uhn.fhir.model.dstu.resource.Location;
import ca.uhn.fhir.model.api.Bundle;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir.api.LocationService;
import org.openmrs.module.fhir.api.util.FHIRLocationUtil;
import org.openmrs.module.fhir.util.Parser;

public class FHIRLocationResource extends Resource {

	public Object retrieve(String uuid) throws Exception {

		Object delegate = getByUniqueId(uuid, null);
		System.out.println(delegate);
		if (delegate == null) {
			throw new Exception();
		}

		return delegate;
	}

    public String searchById(String id, String contentType) {

        LocationService locationService = Context.getService(LocationService.class);
        Bundle locationBundle = locationService.getLocationsById(id);
        return FHIRLocationUtil.parseBundle(locationBundle);
    }

	public String getByUniqueId(String uuid, String contentType) {

		LocationService locationService = Context.getService(LocationService.class);
		Location fhirLocation = locationService.getLocation(uuid);

		return Parser.parse(fhirLocation, contentType);
	}

}
