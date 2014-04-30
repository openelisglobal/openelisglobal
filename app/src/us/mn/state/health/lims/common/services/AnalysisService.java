/*
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
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package us.mn.state.health.lims.common.services;

import org.apache.commons.validator.GenericValidator;
import org.json.simple.JSONObject;
import us.mn.state.health.lims.analysis.dao.AnalysisDAO;
import us.mn.state.health.lims.analysis.daoimpl.AnalysisDAOImpl;
import us.mn.state.health.lims.analysis.valueholder.Analysis;
import us.mn.state.health.lims.dictionary.dao.DictionaryDAO;
import us.mn.state.health.lims.dictionary.daoimpl.DictionaryDAOImpl;
import us.mn.state.health.lims.dictionary.valueholder.Dictionary;
import us.mn.state.health.lims.result.dao.ResultDAO;
import us.mn.state.health.lims.result.daoimpl.ResultDAOImpl;
import us.mn.state.health.lims.result.valueholder.Result;
import us.mn.state.health.lims.test.valueholder.Test;
import us.mn.state.health.lims.typeofsample.util.TypeOfSampleUtil;
import us.mn.state.health.lims.typeofsample.valueholder.TypeOfSample;
import us.mn.state.health.lims.typeoftestresult.valueholder.TypeOfTestResult.ResultType;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 */
public class AnalysisService{
    private static final AnalysisDAO analysisDAO = new AnalysisDAOImpl();
    private static final DictionaryDAO dictionaryDAO = new DictionaryDAOImpl();
    private static final ResultDAO resultDAO = new ResultDAOImpl();
    private final Analysis analysis;

    public AnalysisService(Analysis analysis){
        this.analysis = analysis;
    }

    public AnalysisService(String analysisId){
        analysis = analysisDAO.getAnalysisById( analysisId );
    }

    public Analysis getAnalysis(){
        return analysis;
    }

    public String getTestDisplayName( ){
        if( analysis == null){return ""; }
        Test test = getTest();
        String name = test.getDescription();

        TypeOfSample typeOfSample = TypeOfSampleUtil.getTypeOfSampleForTest( test.getId() );

        if( typeOfSample != null && typeOfSample.getId().equals( TypeOfSampleUtil.getTypeOfSampleIdForLocalAbbreviation( "Variable" ))){
            name += "(" + analysis.getSampleTypeName() + ")";
        }

        String parentResultType = analysis.getParentResult() != null ? analysis.getParentResult().getResultType() : "";
        if(  ResultType.isMultiSelectVariant( parentResultType ) ){
            Dictionary dictionary = dictionaryDAO.getDictionaryById( analysis.getParentResult().getValue() );
            if( dictionary != null){
                String parentResult = dictionary.getLocalAbbreviation();
                if( GenericValidator.isBlankOrNull( parentResult )){
                    parentResult = dictionary.getDictEntry();
                }
                name = parentResult + " &rarr; " + name;
            }
        }

        return name;
    }

    public String getCSVMultiselectResults(){
        if( analysis == null){return ""; }
        List<Result> existingResults = resultDAO.getResultsByAnalysis( analysis );
        StringBuilder multiSelectBuffer = new StringBuilder();
        for( Result existingResult : existingResults ){
            if( ResultType.isMultiSelectVariant( existingResult.getResultType() )){
                multiSelectBuffer.append( existingResult.getValue() );
                multiSelectBuffer.append( ',' );
            }
        }

        // remove last ','
        multiSelectBuffer.setLength( multiSelectBuffer.length() - 1 );

        return multiSelectBuffer.toString();
    }

    public String getJSONMultiSelectResults(){
        if( analysis == null){return ""; }
        List<Result> existingResults = resultDAO.getResultsByAnalysis( analysis );

        Collections.sort( existingResults, new Comparator<Result>(){
            @Override
            public int compare( Result o1, Result o2 ){
                return o1.getGrouping() - o2.getGrouping();
            }
        });

        JSONObject jsonRep =new JSONObject();

        int currentGrouping = -1;
        StringBuilder currentString = new StringBuilder( );

        for(Result result : existingResults){
            if( ResultType.isMultiSelectVariant( result.getResultType() )){
                if( currentGrouping != result.getGrouping()){
                    if( currentString.length() > 1 ){
                        currentString.setLength( currentString.length() - 1 );
                        jsonRep.put( String.valueOf( currentGrouping ), currentString.toString() );
                    }

                    currentGrouping = result.getGrouping();
                    currentString = new StringBuilder( );
                }

                currentString.append( result.getValue() );
                currentString.append( "," );
            }
        }

        if( currentString.length() > 1 ){
            currentString.setLength( currentString.length() - 1 );
            jsonRep.put( String.valueOf( currentGrouping ), currentString.toString() );
        }

        return jsonRep.toJSONString();
    }
    public Result getQuantifiedResult(){
        if( analysis == null){return null; }
        List<Result> existingResults = resultDAO.getResultsByAnalysis( analysis );
        List<String> quantifiableResultsIds = new ArrayList<String>(  );
        for( Result existingResult : existingResults ){
            if( ResultType.isDictionaryType( existingResult.getResultType() ) ){
                quantifiableResultsIds.add( existingResult.getId() );
            }
        }

        for( Result existingResult : existingResults ){
            if( !ResultType.isDictionaryType( existingResult.getResultType()) &&
                    existingResult.getParentResult() != null &&
                    quantifiableResultsIds.contains( existingResult.getParentResult().getId()) &&
                    !GenericValidator.isBlankOrNull(existingResult.getValue())){
            return existingResult;
            }
        }

        return null;
    }
    public String getCompletedDateForDisplay(){
        return analysis == null ? "" : analysis.getCompletedDateForDisplay();
    }

    public String getAnalysisType(){
        return analysis == null ? "" :analysis.getAnalysisType();
    }

    public String getStatusId(){
        return analysis == null ? "" :analysis.getStatusId();
    }

    public Boolean getTriggeredReflex(){
        return analysis == null ? false :analysis.getTriggeredReflex();
    }

    public boolean resultIsConclusion(Result currentResult){
        if( analysis == null){return false; }
        List<Result> results = resultDAO.getResultsByAnalysis(analysis);
        if (results.size() == 1) {
            return false;
        }

        Long testResultId = Long.parseLong(currentResult.getId());
        // This based on the fact that the conclusion is always added
        // after the shared result so if there is a result with a larger id
        // then this is not a conclusion
        for (Result result : results) {
            if (Long.parseLong(result.getId()) > testResultId) {
                return false;
            }
        }

        return true;
    }

    public boolean isParentNonConforming(){
        return analysis == null ? false :QAService.isAnalysisParentNonConforming(analysis);
    }

    public Test getTest(){
        return analysis == null ? null :analysis.getTest();
    }

    public static List<Analysis> getAnalysisStartedOrCompletedInDateRange(Date lowDate, Date highDate){
        return analysisDAO.getAnalysisStartedOrCompletedInDateRange(lowDate, highDate);
    }

    public List<Result> getResults(){
        return analysis == null ? new ArrayList<Result>(  ) : resultDAO.getResultsByAnalysis( analysis );
    }

    public boolean hasBeenCorrectedSinceLastPatientReport(){
        return analysis == null ? false : analysis.isCorrectedSincePatientReport();
    }

    public boolean patientReportHasBeenDone(){
        return analysis == null ? false : new ReportTrackingService().getLastReportForSample( analysis.getSampleItem().getSample(), ReportTrackingService.ReportType.PATIENT ) != null;
    }
}
