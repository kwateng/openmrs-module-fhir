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
package org.openmrs.module.fhir.api.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Bundle;
import ca.uhn.fhir.model.api.BundleEntry;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.composite.AddressDt;
import ca.uhn.fhir.model.dstu.composite.ContactDt;
import ca.uhn.fhir.model.dstu.composite.HumanNameDt;
import ca.uhn.fhir.model.dstu.composite.NarrativeDt;
import ca.uhn.fhir.model.dstu.resource.Patient;
import ca.uhn.fhir.model.dstu.valueset.AddressUseEnum;
import ca.uhn.fhir.model.dstu.valueset.AdministrativeGenderCodesEnum;
import ca.uhn.fhir.model.dstu.valueset.IdentifierUseEnum;
import ca.uhn.fhir.model.dstu.valueset.NameUseEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationFailureException;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;

import java.util.ArrayList;
import java.util.List;

public class FHIRPatientUtil {

	public static Patient generatePatient(org.openmrs.Patient omrsPatient) {

		Patient patient = new Patient();
		IdDt uuid = new IdDt();

		uuid.setValue(omrsPatient.getUuid());
		patient.setId(uuid);

		for (PatientIdentifier identifier : omrsPatient.getActiveIdentifiers()) {
			String uri = Context.getAdministrationService().getGlobalProperty("webservices.rest.uriPrefix")
			             + "/ws/rest/v1/patientidentifiertype/" + identifier.getIdentifierType().getUuid();
			if (identifier.isPreferred()) {

				patient.addIdentifier().setUse(IdentifierUseEnum.USUAL).setSystem(uri).setValue(identifier.getIdentifier())
						.setLabel(identifier.getIdentifierType().getName());
			} else {
				patient.addIdentifier().setUse(IdentifierUseEnum.SECONDARY).setSystem(uri).setValue(
						identifier.getIdentifier()).setLabel(identifier.getIdentifierType().getName());
			}
		}

		List<HumanNameDt> humanNameDts = new ArrayList<HumanNameDt>();

		for (PersonName name : omrsPatient.getNames()) {

			if (name.isPreferred()) {

				HumanNameDt fhirName = new HumanNameDt();
				StringDt familyName = new StringDt();
				familyName.setValue(name.getFamilyName());
				List<StringDt> familyNames = new ArrayList<StringDt>();
				familyNames.add(familyName);

				fhirName.setFamily(familyNames);

				StringDt givenName = new StringDt();
				givenName.setValue(name.getGivenName());
				List<StringDt> givenNames = new ArrayList<StringDt>();
				givenNames.add(givenName);

				fhirName.setGiven(givenNames);

				if (name.getFamilyNameSuffix() != null) {
					StringDt suffix = fhirName.addSuffix();
					suffix.setValue(name.getFamilyNameSuffix());
				}

				if (name.getPrefix() != null) {
					StringDt prefix = fhirName.addPrefix();
					prefix.setValue(name.getPrefix());
				}

				if (name.isPreferred()) {
					fhirName.setUse(NameUseEnum.USUAL);
				} else {
					fhirName.setUse(NameUseEnum.NICKNAME);
				}

				humanNameDts.add(fhirName);
			}

		}

		patient.setName(humanNameDts);

		if (omrsPatient.getGender().equals("M")) {
			patient.setGender(AdministrativeGenderCodesEnum.M);

		}
		if (omrsPatient.getGender().equals("F")) {
			patient.setGender(AdministrativeGenderCodesEnum.F);

		}

		List<AddressDt> fhirAddresses = patient.getAddress();

		for (PersonAddress address : omrsPatient.getAddresses()) {

			if (address.isPreferred()) {

				AddressDt fhirAddress = new AddressDt();
				fhirAddress.setCity(address.getCityVillage());
				fhirAddress.setCountry(address.getCountry());

				List<StringDt> addressStrings = new ArrayList<StringDt>();

				addressStrings.add(new StringDt(address.getAddress1()));
				addressStrings.add(new StringDt(address.getAddress2()));
				addressStrings.add(new StringDt(address.getAddress3()));
				addressStrings.add(new StringDt(address.getAddress4()));
				addressStrings.add(new StringDt(address.getAddress5()));

				fhirAddress.setLine(addressStrings);

				if (address.isPreferred()) {
					fhirAddress.setUse(AddressUseEnum.HOME);
				} else {
					fhirAddress.setUse(AddressUseEnum.OLD);
				}

				fhirAddresses.add(fhirAddress);
			}
		}

		NarrativeDt dt = new NarrativeDt();

		patient.setText(dt);
		patient.setAddress(fhirAddresses);

		DateTimeDt fhirBirthDate = patient.getBirthDate();
		fhirBirthDate.setValue(omrsPatient.getBirthdate());

		patient.setActive(!omrsPatient.isVoided());

		if (omrsPatient.isDead()) {
			DateTimeDt fhirDeathDate = (DateTimeDt) patient.getDeceased();
			fhirDeathDate.setValue(omrsPatient.getDeathDate());
			patient.setDeceased(fhirDeathDate);
		} else {
			BooleanDt isDeceased = new BooleanDt();
			isDeceased.setValue(omrsPatient.getDead());
			patient.setDeceased(isDeceased);
		}

		List<ContactDt> dts = new ArrayList<ContactDt>();

		// Add global property for telephone / email address. These properties will be used to identify the name of the
		// person attribute (if any) being used to store a phone number and/or email.
		ContactDt telecom = new ContactDt();
		if (omrsPatient.getAttribute("Telephone Number") != null) {
			telecom.setValue(omrsPatient.getAttribute("Telephone Number").getValue());
		} else {
			telecom.setValue("None");
		}
		dts.add(telecom);
		patient.setTelecom(dts);

		validate(patient);

		return patient;

	}

	public static void validate(Patient patient) {
		FhirContext ctx = new FhirContext();

		// Request a validator and apply it
		FhirValidator val = ctx.newValidator();
		try {

			val.validate(patient);
		} catch (ValidationFailureException e) {
			// We failed validation
			String results = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(e.getOperationOutcome());
		}

	}

	public static String parsePatient(Patient patient, String contentType) {
		FhirContext ctx = new FhirContext();
		ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());

		IParser jsonParser = ctx.newJsonParser();
		IParser xmlParser = ctx.newXmlParser();

		String encoded = null;

		if (contentType != null) {
			if (contentType.equals("application/xml+fhir")) {

				xmlParser.setPrettyPrint(true);
				encoded = xmlParser.encodeResourceToString(patient);
			} else {
				jsonParser.setPrettyPrint(true);
				encoded = jsonParser.encodeResourceToString(patient);
			}
		} else {

			jsonParser.setPrettyPrint(true);
			encoded = jsonParser.encodeResourceToString(patient);
		}

		return encoded;
	}

	public static Bundle generateBundle(List<org.openmrs.Patient> patientList) {
		Bundle bundle = new Bundle();
		StringDt title = bundle.getTitle();
		title.setValue("Search result");

		IdDt id = new IdDt();
		id.setValue("the request uri");
		bundle.setId(id);

		for (org.openmrs.Patient patient : patientList) {
			BundleEntry bundleEntry = new BundleEntry();

			IdDt entryId = new IdDt();
			entryId.setValue(Context.getAdministrationService().getGlobalProperty("webservices.rest.uriPrefix")
			                 + "/ws/fhir/Patient/" + patient.getUuid());

			bundleEntry.setId(entryId);

			StringDt entryTitle = bundleEntry.getTitle();
			entryTitle.setValue("Patient'/" + patient.getUuid());

			IResource resource = new Patient();
			resource = generatePatient(patient);

			bundleEntry.setResource(resource);
			InstantDt dt = new InstantDt();
			if (patient.getDateChanged() != null) {
				dt.setValue(patient.getDateChanged());
			} else {
				dt.setValue(patient.getDateCreated());
			}
			bundleEntry.setUpdated(dt);

			bundle.addEntry(bundleEntry);
		}

		return bundle;
	}

	public static String parseBundle(Bundle bundle) {
		FhirContext ctx = new FhirContext();
		IParser jsonParser = ctx.newJsonParser();
		jsonParser.setPrettyPrint(true);
		String encoded = jsonParser.encodeBundleToString(bundle);
		return encoded;
	}

}
