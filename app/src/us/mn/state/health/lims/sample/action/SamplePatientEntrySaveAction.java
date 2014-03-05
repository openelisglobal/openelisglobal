/**
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package us.mn.state.health.lims.sample.action;

import org.apache.commons.validator.GenericValidator;
import org.apache.struts.Globals;
import org.apache.struts.action.*;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import us.mn.state.health.lims.address.dao.OrganizationAddressDAO;
import us.mn.state.health.lims.address.daoimpl.AddressPartDAOImpl;
import us.mn.state.health.lims.address.daoimpl.OrganizationAddressDAOImpl;
import us.mn.state.health.lims.address.valueholder.AddressPart;
import us.mn.state.health.lims.address.valueholder.OrganizationAddress;
import us.mn.state.health.lims.analysis.dao.AnalysisDAO;
import us.mn.state.health.lims.analysis.daoimpl.AnalysisDAOImpl;
import us.mn.state.health.lims.analysis.valueholder.Analysis;
import us.mn.state.health.lims.common.action.BaseAction;
import us.mn.state.health.lims.common.action.BaseActionForm;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.common.formfields.FormFields;
import us.mn.state.health.lims.common.formfields.FormFields.Field;
import us.mn.state.health.lims.common.provider.validation.IAccessionNumberValidator;
import us.mn.state.health.lims.common.provider.validation.IAccessionNumberValidator.ValidationResults;
import us.mn.state.health.lims.common.services.ObservationHistoryService;
import us.mn.state.health.lims.common.services.ObservationHistoryService.ObservationType;
import us.mn.state.health.lims.common.services.SampleAddService;
import us.mn.state.health.lims.common.services.SampleAddService.SampleTestCollection;
import us.mn.state.health.lims.common.services.StatusService;
import us.mn.state.health.lims.common.services.StatusService.AnalysisStatus;
import us.mn.state.health.lims.common.services.StatusService.ExternalOrderStatus;
import us.mn.state.health.lims.common.services.StatusService.OrderStatus;
import us.mn.state.health.lims.common.util.ConfigurationProperties;
import us.mn.state.health.lims.common.util.ConfigurationProperties.Property;
import us.mn.state.health.lims.common.util.DateUtil;
import us.mn.state.health.lims.common.util.StringUtil;
import us.mn.state.health.lims.common.util.SystemConfiguration;
import us.mn.state.health.lims.common.util.validator.ActionError;
import us.mn.state.health.lims.dataexchange.order.dao.ElectronicOrderDAO;
import us.mn.state.health.lims.dataexchange.order.daoimpl.ElectronicOrderDAOImpl;
import us.mn.state.health.lims.dataexchange.order.valueholder.ElectronicOrder;
import us.mn.state.health.lims.hibernate.HibernateUtil;
import us.mn.state.health.lims.laborder.dao.LabOrderTypeDAO;
import us.mn.state.health.lims.laborder.daoimpl.LabOrderTypeDAOImpl;
import us.mn.state.health.lims.laborder.valueholder.LabOrderType;
import us.mn.state.health.lims.observationhistory.dao.ObservationHistoryDAO;
import us.mn.state.health.lims.observationhistory.daoimpl.ObservationHistoryDAOImpl;
import us.mn.state.health.lims.observationhistory.valueholder.ObservationHistory;
import us.mn.state.health.lims.observationhistory.valueholder.ObservationHistory.ValueType;
import us.mn.state.health.lims.organization.dao.OrganizationDAO;
import us.mn.state.health.lims.organization.dao.OrganizationOrganizationTypeDAO;
import us.mn.state.health.lims.organization.daoimpl.OrganizationDAOImpl;
import us.mn.state.health.lims.organization.daoimpl.OrganizationOrganizationTypeDAOImpl;
import us.mn.state.health.lims.organization.daoimpl.OrganizationTypeDAOImpl;
import us.mn.state.health.lims.organization.valueholder.Organization;
import us.mn.state.health.lims.organization.valueholder.OrganizationType;
import us.mn.state.health.lims.panel.valueholder.Panel;
import us.mn.state.health.lims.patient.action.IPatientUpdate;
import us.mn.state.health.lims.patient.action.PatientManagementUpdateAction;
import us.mn.state.health.lims.patient.action.bean.PatientManagementInfo;
import us.mn.state.health.lims.patient.util.PatientUtil;
import us.mn.state.health.lims.person.dao.PersonDAO;
import us.mn.state.health.lims.person.daoimpl.PersonDAOImpl;
import us.mn.state.health.lims.person.valueholder.Person;
import us.mn.state.health.lims.provider.dao.ProviderDAO;
import us.mn.state.health.lims.provider.daoimpl.ProviderDAOImpl;
import us.mn.state.health.lims.provider.valueholder.Provider;
import us.mn.state.health.lims.requester.dao.RequesterTypeDAO;
import us.mn.state.health.lims.requester.dao.SampleRequesterDAO;
import us.mn.state.health.lims.requester.daoimpl.RequesterTypeDAOImpl;
import us.mn.state.health.lims.requester.daoimpl.SampleRequesterDAOImpl;
import us.mn.state.health.lims.requester.valueholder.RequesterType;
import us.mn.state.health.lims.requester.valueholder.SampleRequester;
import us.mn.state.health.lims.sample.bean.SampleOrderItem;
import us.mn.state.health.lims.sample.dao.SampleDAO;
import us.mn.state.health.lims.sample.daoimpl.SampleDAOImpl;
import us.mn.state.health.lims.sample.util.AccessionNumberUtil;
import us.mn.state.health.lims.sample.valueholder.Sample;
import us.mn.state.health.lims.samplehuman.dao.SampleHumanDAO;
import us.mn.state.health.lims.samplehuman.daoimpl.SampleHumanDAOImpl;
import us.mn.state.health.lims.samplehuman.valueholder.SampleHuman;
import us.mn.state.health.lims.sampleitem.dao.SampleItemDAO;
import us.mn.state.health.lims.sampleitem.daoimpl.SampleItemDAOImpl;
import us.mn.state.health.lims.test.dao.TestDAO;
import us.mn.state.health.lims.test.dao.TestSectionDAO;
import us.mn.state.health.lims.test.daoimpl.TestDAOImpl;
import us.mn.state.health.lims.test.daoimpl.TestSectionDAOImpl;
import us.mn.state.health.lims.test.valueholder.Test;
import us.mn.state.health.lims.test.valueholder.TestSection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class SamplePatientEntrySaveAction extends BaseAction {

	private static final String DEFAULT_ANALYSIS_TYPE = "MANUAL";
	public static  long ORGANIZATION_REQUESTER_TYPE_ID;
	private static long PROVIDER_REQUESTER_TYPE_ID;
	private boolean savePatient = false;
	private Person providerPerson;
	private Provider provider;
	private String patientId;
	private String accessionNumber;

	private Sample sample;
	private SampleHuman sampleHuman;
	private SampleRequester requesterSite;
	private List<SampleTestCollection> sampleItemsTests;
	private SampleAddService sampleAddService;
	private ActionMessages patientErrors;
	private Organization newOrganization = null;
	private Organization currentOrganization = null;
	private ElectronicOrder electronicOrder = null;

	private boolean useReceiveDateForCollectionDate = false;
    private String collectionDateFromRecieveDate = null;
	private LabOrderTypeDAO labOrderTypeDAO = new LabOrderTypeDAOImpl();
	private OrganizationDAO orgDAO = new OrganizationDAOImpl();
	private OrganizationAddressDAO orgAddressDAO = new OrganizationAddressDAOImpl();
	private OrganizationOrganizationTypeDAO orgOrgTypeDAO = new OrganizationOrganizationTypeDAOImpl();
	private TestSectionDAO testSectionDAO = new TestSectionDAOImpl();
	private ElectronicOrderDAO electronicOrderDAO = new ElectronicOrderDAOImpl();
    private List<ObservationHistory> observations;
	private List<OrganizationAddress> orgAddressExtra;
	
	private static String REFERRING_ORG_TYPE_ID;
	private static String ADDRESS_COMMUNE_ID;
	private static String ADDRESS_FAX_ID;
	private static String ADDRESS_PHONE_ID;
	private static String ADDRESS_STREET_ID;

	static {

		RequesterTypeDAO requesterTypeDAO = new RequesterTypeDAOImpl();
		RequesterType type = requesterTypeDAO.getRequesterTypeByName("organization");
		if (type != null) {
			ORGANIZATION_REQUESTER_TYPE_ID = Long.parseLong(type.getId());
		}

		type = requesterTypeDAO.getRequesterTypeByName("provider");
		if (type != null) {
			PROVIDER_REQUESTER_TYPE_ID = Long.parseLong(type.getId());
		}

		
		OrganizationType orgType = new OrganizationTypeDAOImpl().getOrganizationTypeByName("referring clinic");
		if (orgType != null) {
			REFERRING_ORG_TYPE_ID = orgType.getId();
		}

		List<AddressPart> parts = new AddressPartDAOImpl().getAll();
		for (AddressPart part : parts) {
			if ("commune".equals(part.getPartName())) {
				ADDRESS_COMMUNE_ID = part.getId();
			} else if ("fax".equals(part.getPartName())) {
				ADDRESS_FAX_ID = part.getId();
			} else if ("phone".equals(part.getPartName())) {
				ADDRESS_PHONE_ID = part.getId();
			} else if ("street".equals(part.getPartName())) {
				ADDRESS_STREET_ID = part.getId();
			}
		}
	}


	@Override
	protected ActionForward performAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		String forward = FWD_SUCCESS;

		sampleAddService = null;

		orgAddressExtra = new ArrayList<OrganizationAddress>();
		observations = new ArrayList<ObservationHistory>();
		boolean useInitialSampleCondition = FormFields.getInstance().useField(Field.InitialSampleCondition);
		BaseActionForm dynaForm = (BaseActionForm) form;
		PatientManagementInfo patientInfo = (PatientManagementInfo ) dynaForm.get("patientProperties");
        SampleOrderItem sampleOrder = (SampleOrderItem)dynaForm.get("sampleOrderItems");

        ActionMessages errors = new ActionMessages();

        boolean useReferringSiteId = FormFields.getInstance().useField( Field.RequesterSiteList );
        boolean trackPayments = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.TRACK_PATIENT_PAYMENT, "true");

		String receivedDateForDisplay =  sampleOrder.getReceivedDateForDisplay();
		useReceiveDateForCollectionDate = !FormFields.getInstance().useField(Field.CollectionDate);

		if (!GenericValidator.isBlankOrNull(sampleOrder.getReceivedTime())) {
			receivedDateForDisplay += " " + sampleOrder.getReceivedTime();
		}else{
			receivedDateForDisplay += " 00:00";
		}

		if (useReceiveDateForCollectionDate) {
			collectionDateFromRecieveDate = receivedDateForDisplay;
		}
		
		requesterSite = null;
		if ( useReferringSiteId ) {
			requesterSite = initSampleRequester( sampleOrder );
		}

		IPatientUpdate patientUpdate = new PatientManagementUpdateAction();
		testAndInitializePatientForSaving(mapping, request, patientInfo, patientUpdate);

        accessionNumber = sampleOrder.getLabNo();
		initProvider(sampleOrder);
		initSampleData(dynaForm, receivedDateForDisplay, trackPayments, sampleOrder);
		initSampleHumanData();
		validateSample(errors);

		if (errors.size() > 0) {
			saveErrors(request, errors);
			request.setAttribute(Globals.ERROR_KEY, errors);
			return mapping.findForward(FWD_FAIL);
		}

		Transaction tx = HibernateUtil.getSession().beginTransaction();

		try {
			persistOrganizationData();
			
			if (savePatient) {
				patientUpdate.persistPatientData(patientInfo);
			}

			patientId = patientUpdate.getPatientId(dynaForm);

			persistProviderData();
			persistSampleData();
			persistRequesterData();
			if (useInitialSampleCondition) {
				persistInitialSampleConditions();
			}

			persistObservations();

			tx.commit();

		} catch (LIMSRuntimeException lre) {
			tx.rollback();

			ActionError error;
			if (lre.getException() instanceof StaleObjectStateException) {
				error = new ActionError("errors.OptimisticLockException", null, null);
			} else {
				lre.printStackTrace();
				error = new ActionError("errors.UpdateException", null, null);
			}

			errors.add(ActionMessages.GLOBAL_MESSAGE, error);
			saveErrors(request, errors);
			request.setAttribute(Globals.ERROR_KEY, errors);
			request.setAttribute(ALLOW_EDITS_KEY, "false");
			return mapping.findForward(FWD_FAIL);

		} finally {
			HibernateUtil.closeSession();
		}

		setSuccessFlag(request, forward);

		return mapping.findForward(forward);
	}

	private void persistObservations() {

        ObservationHistoryDAO observationDAO = new ObservationHistoryDAOImpl();
		for (ObservationHistory observation : observations) {
			observation.setSampleId(sample.getId());
			observation.setPatientId(patientId);
			observationDAO.insertData( observation );
		}
	}

	private SampleRequester initSampleRequester( SampleOrderItem orderItem) {
		SampleRequester requester = null;
		newOrganization = null;
		currentOrganization = null;
		
		String orgId = orderItem.getReferringSiteId();

		if (!GenericValidator.isBlankOrNull(orgId)) {
			requester = createSiteRequester(orgId);
			
			if( FormFields.getInstance().useField(Field.SampleEntryReferralSiteCode)){
				updateCurrentOrgIfNeeded( orderItem.getReferringSiteCode(), orgId);
			}
			
		} else if (!GenericValidator.isBlankOrNull(orderItem.getNewRequesterName())) {
			//will be corrected after newOrg is persisted
			requester = createSiteRequester("0"); 

			newOrganization = new Organization();
			if (FormFields.getInstance().useField(Field.SampleEntryHealthFacilityAddress)) {
				addOrgAddressExtra(orderItem.getFacilityPhone(), "T", ADDRESS_PHONE_ID);
				addOrgAddressExtra(orderItem.getFacilityFax(), "T", ADDRESS_FAX_ID);
				addOrgAddressExtra(orderItem.getFacilityAddressCommune(), "T", ADDRESS_COMMUNE_ID);
				addOrgAddressExtra(orderItem.getFacilityAddressStreet(), "T", ADDRESS_STREET_ID);
			}

			if( FormFields.getInstance().useField(Field.SampleEntryReferralSiteCode)){
				newOrganization.setCode(orderItem.getReferringSiteCode());
			}
			
			newOrganization.setIsActive("Y");
			newOrganization.setOrganizationName(orderItem.getNewRequesterName());
			
			// this was left as a warning for copy and paste -- it causes a null
			// pointer exception in session.flush()
			// newOrganization.setOrganizationTypes(ORG_TYPE_SET);
			newOrganization.setSysUserId(currentUserId);
			newOrganization.setMlsSentinelLabFlag("N");

		}

		return requester;
	}

	private void updateCurrentOrgIfNeeded(String code, String orgId){
		currentOrganization = orgDAO.getOrganizationById(orgId);
		if( StringUtil.compareWithNulls(code, currentOrganization.getCode()) != 0){
			currentOrganization.setCode(code);
			currentOrganization.setSysUserId(currentUserId);
		}else{
			currentOrganization = null;
		}
	}

	private SampleRequester createSiteRequester(String orgId) {
		SampleRequester requester;
		requester = new SampleRequester();
		requester.setRequesterId(orgId);
		requester.setRequesterTypeId(ORGANIZATION_REQUESTER_TYPE_ID);
		requester.setSysUserId(currentUserId);
		return requester;
	}

	private void addOrgAddressExtra(String value, String type, String addressPart) {
		if (!GenericValidator.isBlankOrNull(value)) {
			OrganizationAddress orgAddress = new OrganizationAddress();
			orgAddress.setSysUserId(currentUserId);
			orgAddress.setType(type);
			orgAddress.setValue(value);
			orgAddress.setAddressPartId(addressPart);
			orgAddressExtra.add(orgAddress);
		}
	}

	private void validateSample(ActionMessages errors) {
		// assure accession number
		ValidationResults result = AccessionNumberUtil.checkAccessionNumberValidity(accessionNumber, null, null, null);

		if (result != IAccessionNumberValidator.ValidationResults.SUCCESS) {
			String message = AccessionNumberUtil.getInvalidMessage(result);
			errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionError(message));
		}

		// assure that there is at least 1 sample
		if (sampleItemsTests.isEmpty()) {
			errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionError("errors.no.sample"));
		}

		// assure that all samples have tests
		if (!allSamplesHaveTests()) {
			errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionError("errors.samples.with.no.tests"));
		}

		// check patient errors
		if (patientErrors.size(ActionErrors.GLOBAL_MESSAGE) > 0) {
			errors.add(patientErrors);
		}

	}

	private boolean allSamplesHaveTests() {

		for (SampleTestCollection sampleTest : sampleItemsTests) {
			if (sampleTest.tests.size() == 0) {
				return false;
			}
		}

		return true;
	}

	private void initSampleData(BaseActionForm dynaForm, String receivedDate, boolean trackPayments, SampleOrderItem sampleOrder) {
		createPopulatedSample( receivedDate, sampleOrder);

		addObservations(sampleOrder, trackPayments);

		sampleAddService = new SampleAddService(dynaForm.getString("sampleXML"), currentUserId, sample, receivedDate);
		sampleItemsTests = sampleAddService.createSampleTestCollection();
	
	}

	private void addObservations(SampleOrderItem sampleOrder, boolean trackPayments) {
		if (trackPayments) {
			createObservation( sampleOrder.getPaymentOptionSelection(), ObservationHistoryService.getIdForType(ObservationType.PAYMENT_STATUS), ValueType.DICTIONARY);
		}

		createObservation( sampleOrder.getRequestDate(),  ObservationHistoryService.getIdForType(ObservationType.REQUEST_DATE), ValueType.LITERAL);
		createObservation( sampleOrder.getNextVisitDate(),  ObservationHistoryService.getIdForType(ObservationType.NEXT_VISIT_DATE), ValueType.LITERAL);
		createOrderTypeObservation(sampleOrder.getOrderType(),  ObservationHistoryService.getIdForType(ObservationType.PRIMARY_ORDER_TYPE), ValueType.LITERAL);
		createOrderTypeObservation(sampleOrder.getFollowupPeriodOrderType(),  ObservationHistoryService.getIdForType(ObservationType.SECONDARY_ORDER_TYPE), ValueType.LITERAL);
		createOrderTypeObservation(sampleOrder.getInitialPeriodOrderType(),  ObservationHistoryService.getIdForType(ObservationType.SECONDARY_ORDER_TYPE), ValueType.LITERAL);
		createObservation(sampleOrder.getOtherPeriodOrder(),  ObservationHistoryService.getIdForType(ObservationType.OTHER_SECONDARY_ORDER_TYPE), ValueType.LITERAL);
		createObservation(sampleOrder.getReferringPatientNumber(),  ObservationHistoryService.getIdForType(ObservationType.REFERRERS_PATIENT_ID), ValueType.LITERAL);
	}

    private void createOrderTypeObservation(String observationData, String observationType, ValueType valueType) {
        if (!GenericValidator.isBlankOrNull(observationData) && !GenericValidator.isBlankOrNull(observationType)) {
            LabOrderType labOrderType = labOrderTypeDAO.getLabOrderTypeById(observationData);
            // should notify end user if null
            if (labOrderType != null) {
                ObservationHistory observation = new ObservationHistory();
                observation.setObservationHistoryTypeId(observationType);
                observation.setSysUserId(currentUserId);
                observation.setValue(labOrderType.getType());
                observation.setValueType(valueType);
                observations.add(observation);
            }
        }
    }
    private void createObservation(String observationData, String observationType, ValueType valueType) {
        if (!GenericValidator.isBlankOrNull(observationData) && !GenericValidator.isBlankOrNull(observationType)) {
            ObservationHistory observation = new ObservationHistory();
            observation.setObservationHistoryTypeId(observationType);
            observation.setSysUserId(currentUserId);
            observation.setValue(observationData);
            observation.setValueType(valueType);
            observations.add(observation);
        }
    }
	private void createPopulatedSample( String receivedDate, SampleOrderItem sampleOrder) {
		sample = new Sample();
		sample.setSysUserId(currentUserId);
		sample.setAccessionNumber(accessionNumber);

		sample.setEnteredDate(DateUtil.getNowAsSqlDate());

        sample.setReceivedTimestamp(DateUtil.convertStringDateToTimestamp(receivedDate));
		sample.setReferringId( sampleOrder.getRequesterSampleID() );

		if (useReceiveDateForCollectionDate) {
			sample.setCollectionDateForDisplay(collectionDateFromRecieveDate);
		}

		sample.setDomain(SystemConfiguration.getInstance().getHumanDomain());
		sample.setStatusId(StatusService.getInstance().getStatusID(OrderStatus.Entered));
		
		setElectronicOrderIfNeeded( sampleOrder );
	}

	private void setElectronicOrderIfNeeded( SampleOrderItem sampleOrder ){
		electronicOrder = null;
		String externalOrderNumber =  sampleOrder.getExternalOrderNumber();
		if( !GenericValidator.isBlankOrNull(externalOrderNumber)){
			List<ElectronicOrder> orders = electronicOrderDAO.getElectronicOrdersByExternalId(externalOrderNumber);
			if( !orders.isEmpty()){
				electronicOrder = orders.get(orders.size() - 1);
				electronicOrder.setStatusId(StatusService.getInstance().getStatusID(ExternalOrderStatus.Realized));
				electronicOrder.setSysUserId(currentUserId);
				
				sample.setReferringId(externalOrderNumber);	
				sample.setClinicalOrderId(electronicOrder.getId());
			}
		}
	}

	private void initSampleHumanData() {
		sampleHuman = new SampleHuman();
		sampleHuman.setSysUserId(currentUserId);
	}

	private void testAndInitializePatientForSaving(ActionMapping mapping, HttpServletRequest request, PatientManagementInfo patientInfo,
			IPatientUpdate patientUpdate) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

		patientUpdate.setPatientUpdateStatus(patientInfo);
		savePatient = patientUpdate.getPatientUpdateStatus() != PatientManagementUpdateAction.PatientUpdateStatus.NO_ACTION;

		if (savePatient) {
			patientErrors = patientUpdate.preparePatientData(mapping, request, patientInfo);
		} else {
			patientErrors = new ActionMessages();
		}
	}

	private void initProvider(SampleOrderItem sampleOrder) {

		providerPerson = null;
		if (noRequesterInformation(sampleOrder)){
			provider = PatientUtil.getUnownProvider();
		} else {
			providerPerson = new Person();
			provider = new Provider();

			providerPerson.setFirstName(sampleOrder.getProviderFirstName());
			providerPerson.setLastName(sampleOrder.getProviderLastName());
			providerPerson.setWorkPhone(sampleOrder.getProviderWorkPhone());
			providerPerson.setFax(sampleOrder.getProviderFax());
			providerPerson.setEmail(sampleOrder.getProviderEmail());
			providerPerson.setSysUserId(currentUserId);
			provider.setExternalId(sampleOrder.getRequesterSampleID());
		}

		provider.setSysUserId(currentUserId);
	}

	
	private boolean noRequesterInformation( SampleOrderItem sampleOrder){
		return (GenericValidator.isBlankOrNull(sampleOrder.getProviderFirstName()) &&
                GenericValidator.isBlankOrNull(sampleOrder.getProviderWorkPhone()) &&
				GenericValidator.isBlankOrNull(sampleOrder.getProviderLastName()) &&
                GenericValidator.isBlankOrNull(sampleOrder.getRequesterSampleID()) &&
				GenericValidator.isBlankOrNull(sampleOrder.getProviderFax()) &&
                GenericValidator.isBlankOrNull(sampleOrder.getProviderEmail()));
	}

	private void persistOrganizationData() {


		
		if (newOrganization != null) {
			orgDAO.insertData(newOrganization);
			orgOrgTypeDAO.linkOrganizationAndType(newOrganization, REFERRING_ORG_TYPE_ID);
			if (requesterSite != null) {
				requesterSite.setRequesterId(newOrganization.getId());
			}

			for (OrganizationAddress address : orgAddressExtra) {
				address.setOrganizationId(newOrganization.getId());
				orgAddressDAO.insert(address);
			}
		}
		
		if( currentOrganization != null){
			orgDAO.updateData(currentOrganization);
		}

	}

	private void persistProviderData() {
		if (providerPerson != null && provider != null) {
			PersonDAO personDAO = new PersonDAOImpl();
			ProviderDAO providerDAO = new ProviderDAOImpl();

			personDAO.insertData(providerPerson);
			provider.setPerson(providerPerson);

			providerDAO.insertData(provider);
		}
	}

	private void persistSampleData() {
		SampleDAO sampleDAO = new SampleDAOImpl();
		SampleHumanDAO sampleHumanDAO = new SampleHumanDAOImpl();
		SampleItemDAO sampleItemDAO = new SampleItemDAOImpl();
		AnalysisDAO analysisDAO = new AnalysisDAOImpl();
		TestDAO testDAO = new TestDAOImpl();
		String analysisRevision = SystemConfiguration.getInstance().getAnalysisDefaultRevision();

		sampleDAO.insertDataWithAccessionNumber(sample);

	//	if (!GenericValidator.isBlankOrNull(projectId)) {
	//		persistSampleProject();
	//	}

		for (SampleTestCollection sampleTestCollection : sampleItemsTests) {

			sampleItemDAO.insertData(sampleTestCollection.item);

			for (Test test : sampleTestCollection.tests) {
				testDAO.getData(test);

				Analysis analysis = populateAnalysis(analysisRevision,
                                                     sampleTestCollection,
                                                     test,
                                                     sampleTestCollection.testIdToUserSectionMap.get(test.getId()),
                                                     sampleTestCollection.testIdToUserSampleTypeMap.get(test.getId()));
				analysisDAO.insertData(analysis, false); // false--do not check for duplicates
			}

		}

		sampleHuman.setSampleId(sample.getId());
		sampleHuman.setPatientId(patientId);
		if (provider != null) {
			sampleHuman.setProviderId(provider.getId());
		}
		sampleHumanDAO.insertData(sampleHuman);

		if(electronicOrder != null){
			electronicOrderDAO.updateData(electronicOrder);
		}
	}

/*	private void persistSampleProject() throws LIMSRuntimeException {
		SampleProjectDAO sampleProjectDAO = new SampleProjectDAOImpl();
		ProjectDAO projectDAO = new ProjectDAOImpl();
		Project project = new Project();
	//	project.setId(projectId);
		projectDAO.getData(project);

		SampleProject sampleProject = new SampleProject();
		sampleProject.setProject(project);
		sampleProject.setSample(sample);
		sampleProject.setSysUserId(currentUserId);
		sampleProjectDAO.insertData(sampleProject);
	}*/

	private void persistRequesterData() {
		SampleRequesterDAO sampleRequesterDAO = new SampleRequesterDAOImpl();
		if (providerPerson != null && !GenericValidator.isBlankOrNull(providerPerson.getId())) {
			SampleRequester sampleRequester = new SampleRequester();
			sampleRequester.setRequesterId(providerPerson.getId());
			sampleRequester.setRequesterTypeId(PROVIDER_REQUESTER_TYPE_ID);
			sampleRequester.setSampleId(Long.parseLong( sample.getId()));
			sampleRequester.setSysUserId(currentUserId);
			sampleRequesterDAO.insertData(sampleRequester);
		}

		if (requesterSite != null) {
			requesterSite.setSampleId(Long.parseLong( sample.getId()));
			if( newOrganization != null){
				requesterSite.setRequesterId(newOrganization.getId());
			}
			sampleRequesterDAO.insertData(requesterSite);
		}
	}

	private void persistInitialSampleConditions() {
		ObservationHistoryDAO ohDAO = new ObservationHistoryDAOImpl();

		for (SampleTestCollection sampleTestCollection : sampleItemsTests) {
			List<ObservationHistory> initialConditions = sampleTestCollection.initialSampleConditionIdList;

			if (initialConditions != null) {
				for (ObservationHistory observation : initialConditions) {
					observation.setSampleId(sampleTestCollection.item.getSample().getId());
					observation.setSampleItemId(sampleTestCollection.item.getId());
					observation.setPatientId(patientId);
					observation.setSysUserId(currentUserId);
					ohDAO.insertData(observation);
				}
			}
		}
	}

	private Analysis populateAnalysis(String analysisRevision, SampleTestCollection sampleTestCollection, Test test, String userSelectedTestSection, String sampleTypeName) {
	    java.sql.Date collectionDateTime = DateUtil.convertStringDateTimeToSqlDate(sampleTestCollection.collectionDate);
	    TestSection testSection = test.getTestSection();
		if( !GenericValidator.isBlankOrNull(userSelectedTestSection)){
			testSection = testSectionDAO.getTestSectionById( userSelectedTestSection);
		}
		
		Panel panel = sampleAddService.getPanelForTest(test);
		
		Analysis analysis = new Analysis();
		analysis.setTest(test);
		analysis.setPanel(panel);
		analysis.setIsReportable(test.getIsReportable());
		analysis.setAnalysisType(DEFAULT_ANALYSIS_TYPE);
		analysis.setSampleItem(sampleTestCollection.item);
		analysis.setSysUserId(sampleTestCollection.item.getSysUserId());
		analysis.setRevision(analysisRevision);
		analysis.setStartedDate(collectionDateTime == null ? DateUtil.getNowAsSqlDate() : collectionDateTime );
		analysis.setStatusId(StatusService.getInstance().getStatusID(AnalysisStatus.NotStarted));
        if( !GenericValidator.isBlankOrNull( sampleTypeName )){
            analysis.setSampleTypeName( sampleTypeName );
        }
		analysis.setTestSection(testSection);                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 
		return analysis;
	}

	@Override
	protected String getPageTitleKey() {
		return "sample.entry.title";
	}

	@Override
	protected String getPageSubtitleKey() {
		return "sample.entry.title";
	}
}
