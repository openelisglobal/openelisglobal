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
 * Copyright (C) CIRG, University of Washington, Seattle WA.  All Rights Reserved.
 *
 */
package us.mn.state.health.lims.result.action;

import org.apache.commons.validator.GenericValidator;
import org.apache.struts.Globals;
import org.apache.struts.action.*;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import us.mn.state.health.lims.analysis.dao.AnalysisDAO;
import us.mn.state.health.lims.analysis.daoimpl.AnalysisDAOImpl;
import us.mn.state.health.lims.analysis.valueholder.Analysis;
import us.mn.state.health.lims.common.action.BaseAction;
import us.mn.state.health.lims.common.action.BaseActionForm;
import us.mn.state.health.lims.common.action.IActionConstants;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.common.formfields.FormFields;
import us.mn.state.health.lims.common.formfields.FormFields.Field;
import us.mn.state.health.lims.common.services.*;
import us.mn.state.health.lims.common.services.DisplayListService.ListType;
import us.mn.state.health.lims.common.services.NoteService.NoteType;
import us.mn.state.health.lims.common.services.StatusService.AnalysisStatus;
import us.mn.state.health.lims.common.services.StatusService.OrderStatus;
import us.mn.state.health.lims.common.services.beanAdapters.ResultSaveBeanAdapter;
import us.mn.state.health.lims.common.services.registration.ResultUpdateRegister;
import us.mn.state.health.lims.common.services.registration.interfaces.IResultUpdate;
import us.mn.state.health.lims.common.services.serviceBeans.ResultSaveBean;
import us.mn.state.health.lims.common.util.ConfigurationProperties;
import us.mn.state.health.lims.common.util.ConfigurationProperties.Property;
import us.mn.state.health.lims.common.util.DateUtil;
import us.mn.state.health.lims.common.util.IdValuePair;
import us.mn.state.health.lims.common.util.StringUtil;
import us.mn.state.health.lims.common.util.validator.ActionError;
import us.mn.state.health.lims.hibernate.HibernateUtil;
import us.mn.state.health.lims.note.dao.NoteDAO;
import us.mn.state.health.lims.note.daoimpl.NoteDAOImpl;
import us.mn.state.health.lims.note.valueholder.Note;
import us.mn.state.health.lims.patient.valueholder.Patient;
import us.mn.state.health.lims.referral.dao.ReferralDAO;
import us.mn.state.health.lims.referral.dao.ReferralResultDAO;
import us.mn.state.health.lims.referral.dao.ReferralTypeDAO;
import us.mn.state.health.lims.referral.daoimpl.ReferralDAOImpl;
import us.mn.state.health.lims.referral.daoimpl.ReferralResultDAOImpl;
import us.mn.state.health.lims.referral.daoimpl.ReferralTypeDAOImpl;
import us.mn.state.health.lims.referral.valueholder.Referral;
import us.mn.state.health.lims.referral.valueholder.ReferralResult;
import us.mn.state.health.lims.referral.valueholder.ReferralType;
import us.mn.state.health.lims.result.action.util.*;
import us.mn.state.health.lims.result.dao.ResultDAO;
import us.mn.state.health.lims.result.dao.ResultInventoryDAO;
import us.mn.state.health.lims.result.dao.ResultSignatureDAO;
import us.mn.state.health.lims.result.daoimpl.ResultDAOImpl;
import us.mn.state.health.lims.result.daoimpl.ResultInventoryDAOImpl;
import us.mn.state.health.lims.result.daoimpl.ResultSignatureDAOImpl;
import us.mn.state.health.lims.result.valueholder.Result;
import us.mn.state.health.lims.result.valueholder.ResultInventory;
import us.mn.state.health.lims.result.valueholder.ResultSignature;
import us.mn.state.health.lims.resultlimits.dao.ResultLimitDAO;
import us.mn.state.health.lims.resultlimits.daoimpl.ResultLimitDAOImpl;
import us.mn.state.health.lims.resultlimits.valueholder.ResultLimit;
import us.mn.state.health.lims.sample.dao.SampleDAO;
import us.mn.state.health.lims.sample.daoimpl.SampleDAOImpl;
import us.mn.state.health.lims.sample.valueholder.Sample;
import us.mn.state.health.lims.test.beanItems.TestResultItem;
import us.mn.state.health.lims.testreflex.action.util.TestReflexBean;
import us.mn.state.health.lims.testreflex.action.util.TestReflexUtil;
import us.mn.state.health.lims.typeoftestresult.valueholder.TypeOfTestResult.ResultType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.*;

public class ResultsLogbookUpdateAction extends BaseAction implements IResultSaveService{

	private List<TestResultItem> modifiedItems;
	private List<ResultSet> modifiedResults;
	private List<ResultSet> newResults;
	private List<Analysis> modifiedAnalysis;
	private List<Result> deletableResults;
	private AnalysisDAO analysisDAO = new AnalysisDAOImpl();
	private ResultDAO resultDAO = new ResultDAOImpl();
	private ResultSignatureDAO resultSigDAO = new ResultSignatureDAOImpl();
	private ResultInventoryDAO resultInventoryDAO = new ResultInventoryDAOImpl();
	private NoteDAO noteDAO = new NoteDAOImpl();
	private SampleDAO sampleDAO = new SampleDAOImpl();
	private ReferralDAO referralDAO = new ReferralDAOImpl();
	private ReferralResultDAO referralResultDAO = new ReferralResultDAOImpl();
	private ResultLimitDAO resultLimitDAO = new ResultLimitDAOImpl();

	private static final String RESULT_SUBJECT = "Result Note";
	private static String REFERRAL_CONFORMATION_ID;

	private boolean useTechnicianName = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.resultTechnicianName, "true");
	private boolean useRejected = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.allowResultRejection, "true");
	private boolean alwaysValidate = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.ALWAYS_VALIDATE_RESULTS, "true");
	private boolean supportReferrals = FormFields.getInstance().useField( Field.ResultsReferral );
	private String statusRuleSet = ConfigurationProperties.getInstance().getPropertyValueUpperCase( Property.StatusRules );
	private Analysis previousAnalysis;
	private ResultsValidation resultValidation = new ResultsValidation();

	static{
		ReferralTypeDAO referralTypeDAO = new ReferralTypeDAOImpl();
		ReferralType referralType = referralTypeDAO.getReferralTypeByName("Confirmation");
		if(referralType != null){
			REFERRAL_CONFORMATION_ID = referralType.getId();
		}
	}

	protected ActionForward performAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
			throws Exception{

		String forward = FWD_SUCCESS;
		List<IResultUpdate> updaters = ResultUpdateRegister.getRegisteredUpdaters();

		BaseActionForm dynaForm = (BaseActionForm)form;

		resultValidation.setSupportReferrals(supportReferrals);
		resultValidation.setUseTechnicianName(useTechnicianName);
		resultValidation.setUseRejected(useRejected);

		ResultsPaging paging = new ResultsPaging();
		paging.updatePagedResults(request, dynaForm);
		List<TestResultItem> tests = paging.getResults(request);

		setModifiedItems(tests);

		ActionMessages errors = resultValidation.validateModifiedItems(modifiedItems);

		if(errors.size() > 0){
			saveErrors(request, errors);
			request.setAttribute(Globals.ERROR_KEY, errors);

			return mapping.findForward(FWD_VALIDATION_ERROR);
		}

		initializeLists();
        List<Note> noteList = new ArrayList<Note>(  );                                                                  
        createResultsFromItems(noteList);
        
		Transaction tx = HibernateUtil.getSession().beginTransaction();

		try{
            for(Note note: noteList){
                noteDAO.insertData( note );
            }

			for(ResultSet resultSet : newResults){

				resultDAO.insertData(resultSet.result);

				if(resultSet.signature != null){
					resultSet.signature.setResultId(resultSet.result.getId());   
					resultSigDAO.insertData(resultSet.signature);
				}

				if(resultSet.testKit != null && resultSet.testKit.getInventoryLocationId() != null){
					resultSet.testKit.setResultId(resultSet.result.getId());
					resultInventoryDAO.insertData(resultSet.testKit);
				}

				if(resultSet.newReferral != null){
					insertNewReferralAndReferralResult(resultSet);
				}
			}

			for(ResultSet resultSet : modifiedResults){
				resultDAO.updateData(resultSet.result);

				if(resultSet.signature != null){
					resultSet.signature.setResultId(resultSet.result.getId());
					if(resultSet.alwaysInsertSignature){
						resultSigDAO.insertData(resultSet.signature);
					}else{
						resultSigDAO.updateData(resultSet.signature);
					}
				}

				if(resultSet.testKit != null && resultSet.testKit.getInventoryLocationId() != null){
					resultSet.testKit.setResultId(resultSet.result.getId());
					if(resultSet.testKit.getId() == null){
						resultInventoryDAO.insertData(resultSet.testKit);
					}else{
						resultInventoryDAO.updateData(resultSet.testKit);
					}
				}

				if(resultSet.newReferral != null){
					// we can't just create a referral with a blank result,
					// because referral page assumes a referralResult and a
					// result.
					insertNewReferralAndReferralResult(resultSet);
				}

				if(resultSet.existingReferral != null){
					referralDAO.updateData(resultSet.existingReferral);
				}
			}

			for(Analysis analysis : modifiedAnalysis){
				analysisDAO.updateData(analysis);
			}

            ResultSaveService.removeDeletedResultsInTransaction( deletableResults,currentUserId);

			setTestReflexes();

			setSampleStatus();

			for(IResultUpdate updater : updaters){
				updater.transactionalUpdate(this);
			}

			tx.commit();

		}catch(LIMSRuntimeException lre){
			tx.rollback();

			ActionError error;
			if(lre.getException() instanceof StaleObjectStateException){
				error = new ActionError("errors.OptimisticLockException", null, null);
			}else{
				lre.printStackTrace();
				error = new ActionError("errors.UpdateException", null, null);
			}

			errors.add(ActionMessages.GLOBAL_MESSAGE, error);
			saveErrors(request, errors);
			request.setAttribute(Globals.ERROR_KEY, errors);

			return mapping.findForward(FWD_FAIL);

		}

		for(IResultUpdate updater : updaters){
			updater.postTransactionalCommitUpdate(this);
		}

		setSuccessFlag(request, forward);

		if(GenericValidator.isBlankOrNull(dynaForm.getString("logbookType"))){
			return mapping.findForward(forward);
		}else{
			Map<String, String> params = new HashMap<String, String>();
			params.put("type", dynaForm.getString("logbookType"));
			params.put("forward", forward);
			return getForwardWithParameters(mapping.findForward(forward), params);
		}
	}

	private void insertNewReferralAndReferralResult(ResultSet resultSet){
		referralDAO.insertData(resultSet.newReferral);
		ReferralResult referralResult = new ReferralResult();
		referralResult.setReferralId(resultSet.newReferral.getId());
		referralResult.setSysUserId(currentUserId);
		referralResultDAO.insertData(referralResult);
	}



	protected void setTestReflexes(){
		TestReflexUtil testReflexUtil = new TestReflexUtil();
		testReflexUtil.setCurrentUserId( currentUserId );
		testReflexUtil.addNewTestsToDBForReflexTests( convertToTestReflexBeanList( newResults ) );
		testReflexUtil.updateModifiedReflexes( convertToTestReflexBeanList( modifiedResults ) );
	}

    private List<TestReflexBean> convertToTestReflexBeanList(List<ResultSet> resultSetList){
        List<TestReflexBean> reflexBeanList = new ArrayList<TestReflexBean>();

		for(ResultSet resultSet : resultSetList){
			TestReflexBean reflex = new TestReflexBean();
			reflex.setPatient(resultSet.patient);

            if( resultSet.triggersToSelectedReflexesMap.size() > 0 && resultSet.multipleResultsForAnalysis){
                for( String trigger : resultSet.triggersToSelectedReflexesMap.keySet()){
                    if( trigger.equals( resultSet.result.getValue() )){
                        HashMap<String, List<String>> reducedMap = new HashMap<String, List<String>>( 1 );
                        reducedMap.put( trigger, resultSet.triggersToSelectedReflexesMap.get( trigger ) );
                        reflex.setTriggersToSelectedReflexesMap( reducedMap );
                    }
                }
                if( reflex.getTriggersToSelectedReflexesMap() == null){
                    reflex.setTriggersToSelectedReflexesMap( new HashMap<String, List<String>>(  ) );
                }
            }else{
                reflex.setTriggersToSelectedReflexesMap( resultSet.triggersToSelectedReflexesMap );
            }

			reflex.setResult(resultSet.result);
			reflex.setSample(resultSet.sample);
			reflexBeanList.add(reflex);
        }

        return reflexBeanList;
    }
	private void setSampleStatus(){
		Set<Sample> sampleSet = new HashSet<Sample>();

		for(ResultSet resultSet : newResults){
			sampleSet.add(resultSet.sample);
		}

		String sampleTestingStartedId = StatusService.getInstance().getStatusID(OrderStatus.Started);
		String sampleNonConformingId = StatusService.getInstance().getStatusID(OrderStatus.NonConforming_depricated);

		for(Sample sample : sampleSet){
			if(!(sample.getStatusId().equals(sampleNonConformingId) || sample.getStatusId().equals(sampleTestingStartedId))){
				Sample newSample = new Sample();
				newSample.setId(sample.getId());
				sampleDAO.getData(newSample);

				newSample.setStatusId(sampleTestingStartedId);
				newSample.setSysUserId(currentUserId);
				sampleDAO.updateData(newSample);
			}
		}
	}

	private void setModifiedItems(List<TestResultItem> allItems){
		modifiedItems = new ArrayList<TestResultItem>();

		for(TestResultItem item : allItems){
			if(isModified(item)){
				modifiedItems.add(item);
			}
		}
	}

	private boolean isModified(TestResultItem item){
		return item.getIsModified()
				&& (ResultUtil.areResults(item) || ResultUtil.areNotes(item) || ResultUtil.isReferred(item) || ResultUtil.isForcedToAcceptance(item) || ResultUtil.isRejected(item));
	}

	private void createResultsFromItems( List<Note> noteList ){

		for(TestResultItem testResultItem : modifiedItems){
		    
            AnalysisService analysisService = new AnalysisService( testResultItem.getAnalysisId() );
            analysisService.getAnalysis().setStatusId( getStatusForTestResult( testResultItem ) );
            analysisService.getAnalysis().setSysUserId( currentUserId );
            modifiedAnalysis.add( analysisService.getAnalysis() );

            NoteService noteService = new NoteService( analysisService.getAnalysis() );
            Note note = noteService.createSavableNote( NoteType.INTERNAL, testResultItem.getNote(), RESULT_SUBJECT, currentUserId);
            if( note != null){
                noteList.add( note );
            }

            if (testResultItem.isRejected()) {
                String rejectedReasonId = testResultItem.getRejectReasonId();
                for (IdValuePair rejectReason : DisplayListService.getList(ListType.REJECTION_REASONS)) {
                    if (rejectedReasonId.equals(rejectReason.getId())) {
                        String reason = rejectReason.getValue();
                        Note rejectNote = noteService.createSavableNote( NoteType.REJECTION_REASON, reason.substring(reason.indexOf(".") + 1), RESULT_SUBJECT, currentUserId);
                        noteList.add(rejectNote);
                        break;
                    }
                }
                
            }

            ResultSaveBean bean = ResultSaveBeanAdapter.fromTestResultItem(testResultItem);
            ResultSaveService resultSaveService = new ResultSaveService(analysisService.getAnalysis(),currentUserId);
            //deletable Results will be written to, not read
			List<Result> results = resultSaveService.createResultsFromTestResultItem( bean, deletableResults );

            analysisService.getAnalysis().setCorrectedSincePatientReport( resultSaveService.isUpdatedResult() &&
                                                                          analysisService.patientReportHasBeenDone()  );

            //If there is more than one result then each user selected reflex gets mapped to that result
            for(Result result : results){
                addResult(result, testResultItem, analysisService.getAnalysis(), results.size() > 1);

                if(analysisShouldBeUpdated(testResultItem, result)){
                    updateAnalysis( testResultItem, testResultItem.getTestDate(), analysisService.getAnalysis() );
                }
            }
        }
	}

	protected void initializeLists(){
		modifiedResults = new ArrayList<ResultSet>();
		newResults = new ArrayList<ResultSet>();
		modifiedAnalysis = new ArrayList<Analysis>();
		deletableResults = new ArrayList<Result>();
	}

	protected boolean analysisShouldBeUpdated(TestResultItem testResultItem, Result result){
		return result != null && !GenericValidator.isBlankOrNull(result.getValue())
				|| (supportReferrals && ResultUtil.isReferred(testResultItem))
				|| ResultUtil.isForcedToAcceptance(testResultItem) || testResultItem.isRejected();
	}

	private void addResult(Result result, TestResultItem testResultItem, Analysis analysis, boolean multipleResultsForAnalysis){
		boolean newResult = result.getId() == null;
		boolean newAnalysisInLoop = analysis != previousAnalysis;

		ResultSignature technicianResultSignature = null;

		if(useTechnicianName && newAnalysisInLoop){
			technicianResultSignature = createTechnicianSignatureFromResultItem(testResultItem);
		}

		ResultInventory testKit = createTestKitLinkIfNeeded(testResultItem, ResultsLoadUtility.TESTKIT);

		analysis.setReferredOut(testResultItem.isReferredOut());
		analysis.setEnteredDate(DateUtil.getNowAsTimestamp());

		if(newResult){
			analysis.setEnteredDate(DateUtil.getNowAsTimestamp());
			analysis.setRevision("1");
		}else if(newAnalysisInLoop){
			analysis.setRevision(String.valueOf(Integer.parseInt(analysis.getRevision()) + 1));
		}

        SampleService sampleService = new SampleService( testResultItem.getAccessionNumber() );
        Patient patient = sampleService.getPatient();

		Referral referral = null;
		Referral existingReferral = null;

		if(supportReferrals){
			// referredOut means the referral checkbox was checked, repeating
			// analysis means that we have multi-select results, so only do one.
			if(testResultItem.isReferredOut() && newAnalysisInLoop){
				// If it is a new result or there is no referral ID that means
				// that a new referral has to be created if it was checked and
				// it was canceled then we are un-canceling a canceled referral
				if(newResult || GenericValidator.isBlankOrNull(testResultItem.getReferralId())){
					referral = new Referral();
					referral.setReferralTypeId(REFERRAL_CONFORMATION_ID);
					referral.setSysUserId(currentUserId);
					referral.setRequestDate(new Timestamp(new Date().getTime()));
					referral.setRequesterName(testResultItem.getTechnician());
					referral.setAnalysis(analysis);
					referral.setReferralReasonId(testResultItem.getReferralReasonId());
				}else if(testResultItem.isReferralCanceled()){
					existingReferral = referralDAO.getReferralById(testResultItem.getReferralId());
					existingReferral.setCanceled(false);
					existingReferral.setSysUserId(currentUserId);
					existingReferral.setRequesterName(testResultItem.getTechnician());
					existingReferral.setReferralReasonId(testResultItem.getReferralReasonId());
				}
			}
		}

        Map<String,List<String>> triggersToReflexesMap = new HashMap<String, List<String>>(  );

        getSelectedReflexes( testResultItem.getReflexJSONResult(), triggersToReflexesMap );

        if(newResult){
			newResults.add(new ResultSet(result, technicianResultSignature, testKit, patient, sampleService.getSample(), triggersToReflexesMap, referral,
					existingReferral, multipleResultsForAnalysis));
		}else{
			modifiedResults.add(new ResultSet(result, technicianResultSignature, testKit, patient, sampleService.getSample(), triggersToReflexesMap,
					referral, existingReferral, multipleResultsForAnalysis));
		}

		previousAnalysis = analysis;
	}

    private void getSelectedReflexes( String reflexJSONResult, Map<String, List<String>> triggersToReflexesMap ){
        if( !GenericValidator.isBlankOrNull( reflexJSONResult )){
            JSONParser parser=new JSONParser();
            try{
                JSONObject jsonResult = ( JSONObject ) parser.parse( reflexJSONResult.replaceAll( "'", "\"" ) );

                for(Object compoundReflexes : jsonResult.values()){
                    if( compoundReflexes != null){
                        String triggerIds = ( String ) ( ( JSONObject ) compoundReflexes ).get( "triggerIds" );
                        List<String> selectedReflexIds = new ArrayList<String>();
                        JSONArray selectedReflexes = ( JSONArray ) ( ( JSONObject ) compoundReflexes ).get( "selected" );
                        for( Object selectedReflex : selectedReflexes ){
                            selectedReflexIds.add( ( ( String ) selectedReflex ) );
                        }
                        triggersToReflexesMap.put( triggerIds.trim(), selectedReflexIds );
                    }
                }
            }catch( ParseException e ){
                e.printStackTrace();
            }
        }
    }

    private String getStatusForTestResult(TestResultItem testResult){
        if (testResult.isRejected()) {
            return StatusService.getInstance().getStatusID(AnalysisStatus.TechnicalRejected);
        }else if(alwaysValidate || !testResult.isValid() || ResultUtil.isForcedToAcceptance(testResult)){
			return StatusService.getInstance().getStatusID(AnalysisStatus.TechnicalAcceptance);
		}else if(noResults(testResult.getResultValue(), testResult.getMultiSelectResultValues(), testResult.getResultType())){
			return StatusService.getInstance().getStatusID(AnalysisStatus.NotStarted);
		}else{
			ResultLimit resultLimit = resultLimitDAO.getResultLimitById(testResult.getResultLimitId());
			if(resultLimit != null && resultLimit.isAlwaysValidate()){
				return StatusService.getInstance().getStatusID(AnalysisStatus.TechnicalAcceptance);
			}

			return StatusService.getInstance().getStatusID(AnalysisStatus.Finalized);
		}
	}

	private boolean noResults(String value, String multiSelectValue, String type){

		return (GenericValidator.isBlankOrNull(value) && GenericValidator.isBlankOrNull(multiSelectValue)) ||
				( ResultType.DICTIONARY.matches(type) && "0".equals(value));
	}

	private ResultInventory createTestKitLinkIfNeeded(TestResultItem testResult, String testKitName){
		ResultInventory testKit = null;

		if((TestResultItem.ResultDisplayType.SYPHILIS == testResult.getRawResultDisplayType() || TestResultItem.ResultDisplayType.HIV == testResult
				.getRawResultDisplayType()) && ResultsLoadUtility.TESTKIT.equals(testKitName)){
		    
			testKit = createTestKit( testResult, testKitName, testResult.getTestKitId() );
		}
        return testKit;
    }

	private ResultInventory createTestKit( TestResultItem testResult, String testKitName, String testKitId ) throws LIMSRuntimeException{
		ResultInventory testKit;
		testKit = new ResultInventory();

		if(!GenericValidator.isBlankOrNull(testKitId)){
			testKit.setId(testKitId);
			resultInventoryDAO.getData(testKit);
		}

		testKit.setInventoryLocationId(testResult.getTestKitInventoryId());
		testKit.setDescription(testKitName);
		testKit.setSysUserId(currentUserId);
		return testKit;
	}

	private void updateAnalysis( TestResultItem testResultItem, String testDate, Analysis analysis ){
		String testMethod = testResultItem.getAnalysisMethod();
		analysis.setAnalysisType(testMethod);
		analysis.setStartedDateForDisplay(testDate);

		// This needs to be refactored -- part of the logic is in
		// getStatusForTestResult. RetroCI over rides to whatever was set before
		if(statusRuleSet.equals(IActionConstants.STATUS_RULES_RETROCI)){
			if( !StatusService.getInstance().getStatusID(AnalysisStatus.Canceled).equals(analysis.getStatusId() )){
				analysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(testDate));
				analysis.setStatusId(StatusService.getInstance().getStatusID(AnalysisStatus.TechnicalAcceptance));
			}
		}else if(StatusService.getInstance().matches(analysis.getStatusId(), AnalysisStatus.Finalized) ||
				StatusService.getInstance().matches( analysis.getStatusId(), AnalysisStatus.TechnicalAcceptance ) ||
				(analysis.isReferredOut() && !GenericValidator.isBlankOrNull(testResultItem.getResultValue()))){
			analysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(testDate));
		}

	}


	private ResultSignature createTechnicianSignatureFromResultItem(TestResultItem testResult){
		ResultSignature sig = null;

		// The technician signature may be blank if the user changed a
		// conclusion and then changed it back. It will be dirty
		// but will not need a signature
		if(!GenericValidator.isBlankOrNull(testResult.getTechnician())){
			sig = new ResultSignature();

			if(!GenericValidator.isBlankOrNull(testResult.getTechnicianSignatureId())){
				sig.setId(testResult.getTechnicianSignatureId());
				resultSigDAO.getData(sig);
			}

			sig.setIsSupervisor(false);
			sig.setNonUserName(testResult.getTechnician());

			sig.setSysUserId(currentUserId);
		}
		return sig;
	}

	protected ActionForward getForward(ActionForward forward, String accessionNumber){
		ActionRedirect redirect = new ActionRedirect(forward);
		if(!StringUtil.isNullorNill(accessionNumber))
			redirect.addParameter(ACCESSION_NUMBER, accessionNumber);

		return redirect;

	}

	protected ActionForward getForward(ActionForward forward, String accessionNumber, String analysisId){
		ActionRedirect redirect = new ActionRedirect(forward);
		if(!StringUtil.isNullorNill(accessionNumber))
			redirect.addParameter(ACCESSION_NUMBER, accessionNumber);

		if(!StringUtil.isNullorNill(analysisId))
			redirect.addParameter(ANALYSIS_ID, analysisId);

		return redirect;

	}

	@Override
	protected String getPageSubtitleKey(){
		return "banner.menu.results";
	}

	@Override
	protected String getPageTitleKey(){
		return "banner.menu.results";
	}

	@Override
	public String getCurrentUserId(){
		return currentUserId;
	}

	@Override
	public List<ResultSet> getNewResults(){
		return newResults;
	}

	@Override
	public List<ResultSet> getModifiedResults(){
		return modifiedResults;
	}
}
