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

package us.mn.state.health.lims.reports.action.implementation;


import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.validator.GenericValidator;
import us.mn.state.health.lims.common.action.BaseActionForm;
import us.mn.state.health.lims.common.services.PatientService;
import us.mn.state.health.lims.common.services.ResultService;
import us.mn.state.health.lims.common.services.SampleOrderService;
import us.mn.state.health.lims.common.services.SampleService;
import us.mn.state.health.lims.common.util.ConfigurationProperties;
import us.mn.state.health.lims.common.util.ConfigurationProperties.Property;
import us.mn.state.health.lims.common.util.DateUtil;
import us.mn.state.health.lims.common.util.IdValuePair;
import us.mn.state.health.lims.common.util.StringUtil;
import us.mn.state.health.lims.reports.action.implementation.reportBeans.ActivityReportBean;
import us.mn.state.health.lims.result.valueholder.Result;
import us.mn.state.health.lims.sample.util.AccessionNumberUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class ActivityReport extends Report implements IReportCreator{
    private int PREFIX_LENGTH = AccessionNumberUtil.getAccessionNumberValidator().getInvarientLength();
    protected List<ActivityReportBean> testsResults;
    protected String reportPath = "";
    protected DateRange dateRange;

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException{
        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JRBeanCollectionDataSource(testsResults);
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();
        reportParameters.put( "activityLabel", getActivityLabel() );
        reportParameters.put( "accessionPrefix", AccessionNumberUtil.getAccessionNumberValidator().getPrefix() );
        reportParameters.put( "labNumberTitle", StringUtil.getContextualMessageForKey( "quick.entry.accession.number" ) );
        reportParameters.put( "labName", ConfigurationProperties.getInstance().getPropertyValue( Property.SiteName ) );
        reportParameters.put( "siteLogo", getSiteLogo() );
        reportParameters.put( "SUBREPORT_DIR", reportPath );
        reportParameters.put( "startDate", dateRange.getLowDateStr() );
        reportParameters.put( "endDate", dateRange.getHighDateStr() );
        reportParameters.put( "splitNameAndTest", getSplitNameAndTest() );

    }

    protected abstract String getActivityLabel();

    protected abstract boolean getSplitNameAndTest();

    protected abstract void buildReportContent( ReportSpecificationList testSelection );

    protected String getSiteLogo(){
        if( ConfigurationProperties.getInstance().isPropertyValueEqual(Property.configurationName, "Haiti LNSP")){
            return "images" + File.separator + "HaitiLNSP.jpg";
        }

        return null;
    }

    @Override
    public void initializeReport( BaseActionForm dynaForm ){
        initialized = true;
        ReportSpecificationList selection = ( ReportSpecificationList ) dynaForm.get( "selectList" );
        String lowDateStr = dynaForm.getString( "lowerDateRange" );
        String highDateStr = dynaForm.getString( "upperDateRange" );
        dateRange = new DateRange( lowDateStr, highDateStr );

        errorFound = !validateSubmitParameters(selection);
        if ( errorFound ) {
            return;
        }

        buildReportContent( selection );
        if ( testsResults.size() == 0 ) {
            add1LineErrorMessage("report.error.message.noPrintableItems");
        }
    }


    private boolean validateSubmitParameters(ReportSpecificationList selectList) {

        return (dateRange.validateHighLowDate("report.error.message.date.received.missing") &&
                validateSelection(selectList));
    }

    private boolean validateSelection( ReportSpecificationList selectList ){
        boolean complete = !GenericValidator.isBlankOrNull( selectList.getSelection() ) && !"0".equals( selectList.getSelection() );

        if( !complete){
            add1LineErrorMessage("report.error.message.activity.missing");
        }

        return complete;
    }


    protected ActivityReportBean createActivityReportBean( Result result, boolean useTestName ){
        ActivityReportBean item = new ActivityReportBean();

        ResultService resultService = new ResultService( result );
        SampleService sampleService = new SampleService( result.getAnalysis().getSampleItem().getSample() );
        PatientService patientService = new PatientService( sampleService.getSample() );
        item.setResultValue( resultService.getResultValue( "\n", true, true ) );
        item.setTechnician( resultService.getSignature() );
        item.setAccessionNumber( sampleService.getAccessionNumber().substring( PREFIX_LENGTH ) );
        item.setReceivedDate( sampleService.getReceivedDateWithTwoYearDisplay() );
        item.setResultDate( DateUtil.convertTimestampToTwoYearStringDate( result.getLastupdated() ) );
        item.setCollectionDate( DateUtil.convertTimestampToTwoYearStringDate( result.getAnalysis().getSampleItem().getCollectionDate() ) );

        List<String> values = new ArrayList<String>();
        values.add(patientService.getLastName() == null ? "" : patientService.getLastName().toUpperCase());
        values.add(patientService.getNationalId());
        
        SampleOrderService orderService = new SampleOrderService( sampleService.getSample());
        values.add( orderService.getSampleOrderItem() == null ? "" : orderService.getSampleOrderItem().getReferringPatientNumber());

        String name = StringUtil.buildDelimitedStringFromList(values, " / ", true);
        
        if( useTestName ){
            item.setPatientOrTestName( resultService.getTestName() );
            item.setNonPrintingPatient( name );
        }else{
            item.setPatientOrTestName( name );
        }

        return item;
    }

    @Override
    protected String reportFileName(){
        return  "ActivityReport";
    }

    @Override
    protected String errorReportFileName(){
        return "HaitiNoticeOfReportError";
    }

    protected ActivityReportBean createIdentityActivityBean( ActivityReportBean item, boolean blankCollectionDate ){
        ActivityReportBean filler = new ActivityReportBean();

        filler.setAccessionNumber( item.getAccessionNumber() );
        filler.setReceivedDate( item.getReceivedDate() );
        filler.setCollectionDate( blankCollectionDate ? " " : item.getCollectionDate() );
        filler.setPatientOrTestName( item.getNonPrintingPatient() );

        return filler;
    }

    protected String getNameForId( ReportSpecificationList list ){

        String selection = list.getSelection();

        for( IdValuePair pair : list.getList()){
            if( selection.equals( pair.getId() )){
                return pair.getValue();
            }
        }

        return "";
    }
}
