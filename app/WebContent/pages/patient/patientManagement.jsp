<%@ page language="java" contentType="text/html; charset=utf-8" %>
<%@ page import="us.mn.state.health.lims.common.action.IActionConstants,
                 us.mn.state.health.lims.common.util.SystemConfiguration,
                 us.mn.state.health.lims.common.formfields.FormFields,
                 us.mn.state.health.lims.common.formfields.FormFields.Field,
                 us.mn.state.health.lims.common.util.StringUtil,
                 us.mn.state.health.lims.common.util.Versioning,
                 us.mn.state.health.lims.patient.action.bean.PatientManagementInfo" %>


<%@ taglib uri="/tags/struts-bean"		prefix="bean" %>
<%@ taglib uri="/tags/struts-html"		prefix="html" %>
<%@ taglib uri="/tags/struts-logic"		prefix="logic" %>
<%@ taglib uri="/tags/struts-tiles"		prefix="tiles" %>
<%@ taglib uri="/tags/struts-nested"	prefix="nested" %>
<%@ taglib uri="/tags/labdev-view"		prefix="app" %>
<%@ taglib uri="/tags/sourceforge-ajax" prefix="ajax"%>

<script type="text/javascript" src="scripts/ajaxCalls.js?ver=<%= Versioning.getBuildNumber() %>"></script>
<script type="text/javascript" src="<%=basePath%>scripts/utilities.js?ver=<%= Versioning.getBuildNumber() %>" ></script>

<bean:define id="formName"		value='<%=(String) request.getAttribute(IActionConstants.FORM_NAME)%>' />
<bean:define id="patientProperties" name='<%=formName%>' property='patientProperties' type="PatientManagementInfo" />


<%!
	String basePath = "";
	boolean supportSTNumber = true;
	boolean supportAKA = true;
	boolean supportMothersName = true;
	boolean supportPatientType = true;
	boolean supportInsurance = true;
	boolean supportSubjectNumber = true;
	boolean supportNationalID = true;
	boolean supportOccupation = true;
	boolean supportCommune = true;
	boolean supportAddressDepartment = false;
	boolean supportMothersInitial = true;
	boolean patientRequired = true;
	boolean patientIDRequired = true;
	boolean patientNamesRequired = true;
	boolean patientAgeRequired = true;
	boolean patientGenderRequired = true;
 %>
<%
	String path = request.getContextPath();
	basePath = request.getScheme() + "://" + request.getServerName() + ":"
			+ request.getServerPort() + path + "/";
	supportSTNumber = FormFields.getInstance().useField(Field.StNumber);
	supportAKA = FormFields.getInstance().useField(Field.AKA);
	supportMothersName = FormFields.getInstance().useField(Field.MothersName);
	supportPatientType = FormFields.getInstance().useField(Field.PatientType);
	supportInsurance = FormFields.getInstance().useField(Field.InsuranceNumber);
	supportSubjectNumber = FormFields.getInstance().useField(Field.SubjectNumber);
	supportNationalID = FormFields.getInstance().useField(Field.NationalID);
	supportOccupation = FormFields.getInstance().useField(Field.Occupation);
	supportCommune = FormFields.getInstance().useField(Field.Commune);
	supportMothersInitial = FormFields.getInstance().useField(Field.MotherInitial);
	supportAddressDepartment = FormFields.getInstance().useField(Field.AddressDepartment );
	
	if("SampleConfirmationEntryForm".equals( formName )){
		patientIDRequired = FormFields.getInstance().useField(Field.PatientIDRequired_SampleConfirmation);
		patientRequired = FormFields.getInstance().useField(Field.PatientRequired_SampleConfirmation );
		patientAgeRequired = FormFields.getInstance().useField(Field.PatientAgeRequired_SampleConfirmation );
		patientGenderRequired = FormFields.getInstance().useField(Field.PatientGenderRequired_SampleConfirmation );
	}else{
		patientIDRequired = FormFields.getInstance().useField(Field.PatientIDRequired);
	    patientRequired = FormFields.getInstance().useField(Field.PatientRequired );
	    patientAgeRequired = FormFields.getInstance().useField(Field.PatientAgeRequired_SampleEntry );
		patientGenderRequired = FormFields.getInstance().useField(Field.PatientGenderRequired_SampleEntry);
	}
	
	patientNamesRequired = FormFields.getInstance().useField(Field.PatientNameRequired);
%>

<script type="text/javascript" >

var $jq = jQuery.noConflict();

/*the prefix pt_ is being used for scoping.  Since this is being used as a tile there may be collisions with other
  tiles with simular names.  Only those elements that may cause confusion are being tagged, and we know which ones will collide
  because we can predicte the future */

var supportSTNumber = <%= supportSTNumber %>;
var supportAKA = <%= supportAKA %>;
var supportMothersName = <%= supportMothersName %>;
var supportPatientType = <%= supportPatientType %>;
var supportInsurance = <%= supportInsurance %>;
var supportSubjectNumber = <%= supportSubjectNumber %>;
var supportNationalID = <%= supportNationalID %>;
var supportMothersInitial = <%= supportMothersInitial %>;
var supportCommune = <%= supportCommune %>;
var supportCity = <%= FormFields.getInstance().useField(Field.AddressVillage) %>;
var supportOccupation = <%= supportOccupation %>;
var supportAddressDepartment = <%= supportAddressDepartment %>;
var patientRequired = <%= patientRequired %>;
var patientIDRequired = <%= patientIDRequired %>;
var patientNamesRequired = <%= patientNamesRequired %>;
var patientAgeRequired = <%= patientAgeRequired %>;
var patientGenderRequired = <%= patientGenderRequired %>;
var supportEducation = <%= FormFields.getInstance().useField(Field.PatientEducation) %>;
var supportPatientNationality = <%= FormFields.getInstance().useField(Field.PatientNationality) %>;
var supportMaritialStatus = <%= FormFields.getInstance().useField(Field.PatientMarriageStatus) %>;
var supportHealthRegion = <%= FormFields.getInstance().useField(Field.PatientHealthRegion) %>;
var supportHealthDistrict = <%= FormFields.getInstance().useField(Field.PatientHealthDistrict) %>;

var pt_invalidElements = [];
var pt_requiredFields = [];
if( patientAgeRequired){
	pt_requiredFields.push("dateOfBirthID");
}
if( patientGenderRequired){
	pt_requiredFields.push("genderID");
}
if( patientNamesRequired){
	pt_requiredFields.push("firstNameID"); 
	pt_requiredFields.push("lastNameID"); 
}

var pt_requiredOneOfFields = [];

if( patientIDRequired){
	pt_requiredOneOfFields.push("nationalID") ;
	pt_requiredOneOfFields.push("patientGUID_ID") ;
	if (supportSTNumber) {
		pt_requiredOneOfFields.push("ST_ID");
	} else if (supportSubjectNumber){
		pt_requiredOneOfFields = new Array("subjectNumberID");
	}
}

var updateStatus = "add";
var patientInfoChangeListeners = [];
var dirty = false;

function  /*bool*/ pt_isFieldValid(fieldname)
{
	return pt_invalidElements.indexOf(fieldname) == -1;
}


function  /*void*/ pt_setFieldInvalid(field)
{
	if( pt_invalidElements.indexOf(field) == -1 )
	{
		pt_invalidElements.push(field);
	}
}

function  /*void*/ pt_setFieldValid(field)
{
	var removeIndex = pt_invalidElements.indexOf( field );
	if( removeIndex != -1 )
	{
		for( var i = removeIndex + 1; i < pt_invalidElements.length; i++ )
		{
			pt_invalidElements[i - 1] = pt_invalidElements[i];
		}

		pt_invalidElements.length--;
	}
}

function  /*void*/ pt_setFieldValidity( valid, fieldName ){
	if( valid ){
		pt_setFieldValid( fieldName );
	}else{
		pt_setFieldInvalid( fieldName );
	}
}

function /*boolean*/ patientFormValid(){
	if ( patientRequired || !pt_patientRequiredFieldsAllEmpty()) {
		return pt_invalidElements.length == 0 && pt_requiredFieldsValid();
	} else {
		return true;
	}
}

function pt_patientRequiredFieldsAllEmpty() {
	var i;

	for(i = 0; i < pt_requiredFields.length; ++i ){
		if( !$(pt_requiredFields[i]).value.blank() ){
			return false;
		}
	}
	
	for(i = 0; i < pt_requiredOneOfFields.length; ++i ){
		if( !($(pt_requiredOneOfFields[i]).value.blank()) ){
			return false;
		}
	}
	return true;
}

function /*void*/ pt_setSave()
{
	if( window.setSave ){
		setSave();
	}else{
		$("saveButtonId").disabled = !patientFormValid();
	}
}

function /*boolean*/ pt_isSaveEnabled()
{
	return !$("saveButtonId").disabled;
}

function  /*void*/ setMyCancelAction(form, action, validate, parameters)
{

	//first turn off any further validation
	setAction(window.document.forms[0], 'Cancel', 'no', '');
}

function  /*void*/ pt_requiredFieldsValid(){
    var i;
	for( i = 0; i < pt_requiredFields.length; ++i ){
		if( $(pt_requiredFields[i]).value.blank() ){
			return false;
		}
	}

	if( pt_requiredOneOfFields.length == 0){
		return true;
	}

	for( i = 0; i < pt_requiredOneOfFields.length; ++i ){
		if( !($(pt_requiredOneOfFields[i]).value.blank()) ){
			return true;
		}
	}

	return false;
}

function  /*string*/ pt_requiredFieldsValidMessage()
{
	var hasError = false;
	var returnMessage = "";
	var oneOfMembers = "";
	var requiredField = "";
    var i;

	for( i = 0; i < pt_requiredFields.length; ++i ){
		if( $(pt_requiredFields[i]).value.blank() ){
			hasError = true;
			requiredField += " : " + pt_requiredFields[i];
		}
	}

	for( i = 0; i < pt_requiredOneOfFields.length; ++i ){
		if( !pt_requiredOneOfFields[i].value.blank() ){
			oneOfFound = true;
			break;
		}

		oneOfMemebers += " : " + pt_requiredOneOfFields[i];
	}

	if( !oneOFound ){
		hasError = true;
	}

	if( hasError )
	{
		if( !requiredField.blank() ){
			returnMessage = "Please enter the following patient values  " + requiredField;
		}
		if( !oneOfMembers.blank() ){
			returnMessage = "One of the following must have a value " + onOfMemebers;
		}
	}else{
		returnMessage = "valid";
	}

	return returnMessage;
}


function  /*void*/ handleBirthDateChange( dateElement, checkForValidDate )
{

	if(!checkForValidDate ){
		updatePatientAge( dateElement );
	}else{
		 checkValidAgeDate( dateElement );
	}
}

function  /*void*/ processValidateDateSuccess(xhr){

    //alert(xhr.responseText);
	var message = xhr.responseXML.getElementsByTagName("message").item(0).firstChild.nodeValue;
	var formField = xhr.responseXML.getElementsByTagName("formfield").item(0).firstChild.nodeValue;

	var isValid = message == "<%=IActionConstants.VALID%>";

	setValidIndicaterOnField(isValid, formField);
	pt_setFieldValidity( isValid, formField );


	if( isValid ){
		updatePatientAge( $("dateOfBirthID") );
	}else if( message == "<%=IActionConstants.INVALID_TO_LARGE%>" ){
		alert( '<bean:message key="error.date.birthInPast" />' );
	}
	
	pt_setSave();
}

function  /*void*/ checkValidAgeDate(dateElement)
{
	if( dateElement && !dateElement.value.blank() ){
		isValidDate( dateElement.value, processValidateDateSuccess, dateElement.name, "past" );
	}else{
		setValidIndicaterOnField(dateElement.value.blank(), dateElement.name);
	    pt_setFieldValidity( dateElement.value.blank(),  dateElement.name);
		pt_setSave();
		$("age").value = null;
	}
}


function  /*void*/ updatePatientAge( DOB )
{
	var date = String( DOB.value );

	var datePattern = '<%=SystemConfiguration.getInstance().getPatternForDateLocale() %>';
	var splitPattern = datePattern.split("/");
	var dayIndex = 0;
	var monthIndex = 1;
	var yearIndex = 2;

	for( var i = 0; i < 3; i++ ){
		if(splitPattern[i] == "DD"){
			dayIndex = i;
		}else if(splitPattern[i] == "MM" ){
			monthIndex = i;
		}else if(splitPattern[i] == "YYYY" ){
			yearIndex = i;
		}
	}


	var splitDOB = date.split("/");
	var monthDOB = splitDOB[monthIndex];
	var dayDOB = splitDOB[dayIndex];
	var yearDOB = splitDOB[yearIndex];

	var today = new Date();

	var adjustment = 0;

	if( !monthDOB.match( /^\d+$/ ) ){
		monthDOB = "01";
	}

	if( !dayDOB.match( /^\d+$/ ) ){
		dayDOB = "01";
	}

	//months start at 0, January is month 0
	var monthToday = today.getMonth() + 1;

	if( monthToday < monthDOB ||
	    (monthToday == monthDOB && today.getDate() < dayDOB  ))
	    {
	    	adjustment = -1;
	    }

	var calculatedAge = today.getFullYear() - yearDOB + adjustment;

	var age = document.getElementById("age");
	age.value = calculatedAge;
}

function /*void*/ handleAgeChange( age )
{
	if( pt_checkValidAge( age ) )
	{
		pt_updateDOB( age );
		setValidIndicaterOnField( true, $("dateOfBirthID").name);
		pt_setFieldValid( $("dateOfBirthID").name );
	}

	pt_setSave();
}

function  /*bool*/ pt_checkValidAge( age )
{
	var valid = age.value.blank();

	if( !valid ){
		var regEx = new RegExp("^\\s*\\d{1,2}\\s*$");
	 	valid =  regEx.test(age.value);
	}

	setValidIndicaterOnField(  valid , age.name );
	pt_setFieldValidity( valid, age.name );

	return valid;
}

function  /*void*/ pt_updateDOB( age )
{
	if( age.value.blank() ){
		$("dateOfBirthID").value = null;
	}else{
		var today = new Date();

		var day = "xx";
		var month = "xx";
		var year = today.getFullYear() - age.value;

		var datePattern = '<%=SystemConfiguration.getInstance().getPatternForDateLocale() %>';
		var splitPattern = datePattern.split("/");

		var DOB = "";

		for( var i = 0; i < 3; i++ ){
			if(splitPattern[i] == "DD"){
				DOB = DOB + day + "/";
			}else if(splitPattern[i] == "MM" ){
				DOB = DOB + month + "/";
			}else if(splitPattern[i] == "YYYY" ){
				DOB = DOB + year + "/";
			}
		}

		$("dateOfBirthID").value = DOB.substring(0, DOB.length - 1 );
	}
}

function  /*void*/ getDetailedPatientInfo()
{
	$("patientPK_ID").value = patientSelectID;

	new Ajax.Request (
                       'ajaxQueryXML',  //url
                        {//options
                          method: 'get', //http method
                          parameters: "provider=PatientSearchPopulateProvider&personKey=" + patientSelectID,
                          onSuccess:  processSearchPopulateSuccess,
                          onFailure:  processSearchPopulateFailure
                         }
                          );
}

function  /*void*/ setUpdateStatus( newStatus )
{
	if( updateStatus != newStatus )
	{
		updateStatus = newStatus;
		document.getElementById("processingStatus").value = newStatus;
	}
}

function  /*void*/ processSearchPopulateSuccess(xhr)
{

	setUpdateStatus("noAction");
    //alert(xhr.responseText);
	var response = xhr.responseXML.getElementsByTagName("formfield").item(0);

	var nationalIDValue = getXMLValue(response, "nationalID");
	var STValue = getXMLValue(response, "ST_ID");
	var subjectNumberValue = getXMLValue(response, "subjectNumber");
	var lastNameValue = getXMLValue(response, "lastName");
	var firstNameValue = getXMLValue(response, "firstName");
	var akaValue = getXMLValue(response, "aka");
	var motherValue = getXMLValue(response, "mother");
	var motherInitialValue = getXMLValue(response, "motherInitial");
	var streetValue = getXMLValue(response, "street");
	var cityValue = getXMLValue(response, "city");
	var communeValue = getXMLValue(response, "commune");
	var dobValue = getXMLValue(response, "dob");
	var genderValue = getSelectIndexFor( "genderID", getXMLValue(response, "gender"));
	var patientTypeValue = getSelectIndexFor( "patientTypeID", getXMLValue(response, "patientType"));
	var insuranceValue = getXMLValue(response, "insurance");
	var occupationValue = getXMLValue(response, "occupation");
	var patientUpdatedValue = getXMLValue(response, "patientUpdated");
	var personUpdatedValue = getXMLValue(response, "personUpdated");
	var addressDepartment = getXMLValue( response, "addressDept" );
	var education = getSelectIndexFor( "educationID", getXMLValue(response, "education"));
	var nationality = getSelectIndexFor( "nationalityID", getXMLValue(response, "nationality"));
	var otherNationality = getXMLValue( response, "otherNationality");
	var maritialStatus = getSelectIndexFor( "maritialStatusID", getXMLValue(response, "maritialStatus"));
	var healthRegion = getSelectIndexByTextFor( "healthRegionID", getXMLValue(response, "healthRegion"));
	var healthDistrict = getXMLValue(response, "healthDistrict");
	var guid = getXMLValue( response, "guid");

	setPatientInfo( nationalIDValue,
					STValue,
					subjectNumberValue,
					lastNameValue,
					firstNameValue,
					akaValue,
					motherValue,
					streetValue,
					cityValue,
					dobValue,
					genderValue,
					patientTypeValue,
					insuranceValue,
					occupationValue,
					patientUpdatedValue,
					personUpdatedValue,
					motherInitialValue,
					communeValue,
					addressDepartment,
					education,
					nationality,
					otherNationality,
					maritialStatus,
					healthRegion,
					healthDistrict,
					guid );

}

function /*string*/ getXMLValue( response, key )
{
	var field = response.getElementsByTagName(key).item(0);

	if( field != null )
	{
		 return field.firstChild.nodeValue;
	}
	else
	{
		return undefined;
	}
}

function  /*void*/ processSearchPopulateFailure(xhr) {
		//alert(xhr.responseText); // do something nice for the user
}

function  /*void*/ clearPatientInfo(){
	setPatientInfo();
}

function /*void*/ clearErrors(){

	for( var i = 0; i < pt_invalidElements.length; ++i ){
		setValidIndicaterOnField( true, $(pt_invalidElements[i]).name );
	}

	pt_invalidElements = [];

}

function  /*void*/ setPatientInfo(nationalID, ST_ID, subjectNumber, lastName, firstName, aka, mother, street, city, dob, gender,
		patientType, insurance, occupation, patientUpdated, personUpdated, motherInitial, commune, addressDept, educationId, nationalId, nationalOther,
		maritialStatusId, healthRegionId, healthDistrictId, guid ) {

	clearErrors();

	if ( supportNationalID) { $("nationalID").value = nationalID == undefined ? "" : nationalID; }
	if(supportSTNumber){ $("ST_ID").value = ST_ID == undefined ? "" : ST_ID; }
	if(supportSubjectNumber){ $("subjectNumberID").value = subjectNumber == undefined ? "" : subjectNumber; }
	$("lastNameID").value = lastName == undefined ? "" : lastName;
	$("firstNameID").value = firstName == undefined ? "" : firstName;
	if(supportAKA){$("akaID").value = aka == undefined ? "" : aka; }
	if(supportMothersName){$("motherID").value = mother == undefined ? "" : mother; }
	if(supportMothersInitial){$("motherInitialID").value = (motherInitial == undefined ? "" : motherInitial); }
	$("streetID").value = street == undefined ? "" : street;
	if(supportCity){$("cityID").value = city == undefined ? "" : city; }
	if(supportCommune){$("communeID").value = commune == undefined ? "" : commune; }
	if(supportInsurance){$("insuranceID").value = insurance == undefined ? "" : insurance; }
	if(supportOccupation){$("occupationID").value = occupation == undefined ? "" : occupation; }
	$("patientLastUpdated").value = patientUpdated == undefined ? "" : patientUpdated;
	$("personLastUpdated").value = personUpdated == undefined ? "" : personUpdated;
	$("patientGUID_ID").value = guid == undefined ? "" : guid;
	if(supportPatientNationality){
		$("nationalityID").selectedIndex = nationalId == undefined ? 0 : nationalId; 
		$("nationalityOtherId").value = nationalOther == undefined ? "" : nationalOther;}
	if( supportEducation){ $("educationID").selectedIndex =  educationId == undefined ? 0 : educationId;}
	if( supportMaritialStatus){ $("maritialStatusID").selectedIndex = maritialStatusId == undefined ? 0 : maritialStatusId;}
	if( supportHealthRegion){ 
		var healthRegion = $("healthRegionID"); 
		healthRegion.selectedIndex = healthRegionId == undefined ? 0 : healthRegionId;
		$("shadowHealthRegion").value = healthRegion.options[healthRegion.selectedIndex].label;}
	if( supportHealthDistrict){
		if($("healthRegionID").selectedIndex != 0){
			getDistrictsForRegion( $("healthRegionID").value, healthDistrictId, healthDistrictSuccess, null);
		} 
	}

	if(supportAddressDepartment){
		var deptMessage = $("deptMessage");
		deptMessage.innerText = deptMessage.textContent = "";
		//for historic reasons, we switched from text to dropdown
		if( addressDept == undefined ){
			$("departmentID").value = 0;
		}else if( isNaN( addressDept) ){
			$("departmentID").value = 0;
			deptMessage.textContent = "<%= StringUtil.getMessageForKey("patient.address.dept.entry.msg") %>" + " " + addressDept;
		}else{
			$("departmentID").value = addressDept;
		}

	}

	if (dob == undefined) {
		$("dateOfBirthID").value = "";
		$("age").value = "";
	} else {
		var dobElement = $("dateOfBirthID");
		dobElement.value = dob;
		handleBirthDateChange(dobElement, false);
	}

	document.getElementById("genderID").selectedIndex = gender == undefined ? 0 : gender;
	if(supportPatientType){document.getElementById("patientTypeID").selectedIndex = patientType == undefined ? 0 : patientType; }

	// run this b/c dynamically populating the fields does not constitute an onchange event to populate the patmgmt tile
	// this is the fx called by the onchange event if manually changing the fields
	updatePatientEditStatus();

}

function  /*void*/ updatePatientEditStatus() {
	if (updateStatus == "noAction") {
		setUpdateStatus("update");
	}

	for(var i = 0; i < patientInfoChangeListeners.length; i++){
			patientInfoChangeListeners[i]($("firstNameID").value,
										  $("lastNameID").value,
										  $("genderID").value,
										  $("dateOfBirthID").value,
										  supportSTNumber ? $("ST_ID").value : "",
										  supportSubjectNumber ? $("subjectNumberID").value : "",
										  supportNationalID ? $("nationalID").value : "",
										  supportMothersName ? $("motherID").value : null,
										  $("patientPK_ID").value);

		}

	makeDirty();

	pt_setSave();
}

function /*void*/ makeDirty(){
	dirty=true;
	if( typeof(showSuccessMessage) != 'undefinded' ){
		showSuccessMessage(false); //refers to last save
	}
	// Adds warning when leaving page if content has been entered into makeDirty form fields
	function formWarning(){ 
    return "<bean:message key="banner.menu.dataLossWarning"/>";
	}
	window.onbeforeunload = formWarning;
}

function  /*void*/  addPatient(){
	clearPatientInfo();
	clearErrors();
	if(supportSTNumber){$("ST_ID").disabled = false;}
	if(supportSubjectNumber){$("subjectNumberID").disabled = false;}
	if(supportNationalID){$("nationalID").disabled = false;}
	setUpdateStatus( "add" );
	
	for(var i = 0; i < patientInfoChangeListeners.length; i++){
			patientInfoChangeListeners[i]("", "", "", "", "", "", "", "", "");
		}
}

function  /*void*/ savePage()
{
	window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
	var form = window.document.forms[0];
	form.action = "PatientManagementUpdate.do";
	form.submit();
}

function /*void*/ addPatientInfoChangedListener( listener ){
	patientInfoChangeListeners.push( listener );
}

function clearDeptMessage(){
	$("deptMessage").innerText = deptMessage.textContent = "";
}

function updateHealthDistrict( regionElement){
	$("shadowHealthRegion").value = regionElement.options[regionElement.selectedIndex].text;
	getDistrictsForRegion( regionElement.value, "", healthDistrictSuccess, null);
}

function healthDistrictSuccess( xhr ){
  	//alert(xhr.responseText);

	var message = xhr.responseXML.getElementsByTagName("message").item(0).firstChild.nodeValue;
	var districts = xhr.responseXML.getElementsByTagName("formfield").item(0).childNodes[0].childNodes;
	var selected = xhr.responseXML.getElementsByTagName("formfield").item(0).childNodes[1];
	var isValid = message == "<%=IActionConstants.VALID%>";
	var healthDistrict = $("healthDistrictID");
	var i = 0;

	healthDistrict.disabled = "";
	if( isValid ){
		healthDistrict.options.length = 0;
		healthDistrict.options[0] = new Option('', '');
		for( ;i < districts.length; ++i){
			healthDistrict.options[i + 1] = new Option(districts[i].attributes.getNamedItem("value").value, districts[i].attributes.getNamedItem("value").value);
		}
	}
	
	if( selected){
		healthDistrict.selectedIndex = getSelectIndexFor( "healthDistrictID", selected.childNodes[0].nodeValue);
	}
}

function validatePhoneNumber( phoneElement){
    validatePhoneNumberOnServer( phoneElement, processPhoneSuccess);
}

function  processPhoneSuccess(xhr){
    //alert(xhr.responseText);

    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
    var message = xhr.responseXML.getElementsByTagName("message").item(0);
    var success = false;

    if (message.firstChild.nodeValue == "valid"){
        success = true;
    }
    var labElement = formField.firstChild.nodeValue;

    setValidIndicaterOnField(success, labElement);
    pt_setFieldValidity( success, labElement );

    if( !success ){
        alert( message.firstChild.nodeValue );
    }

    pt_setSave();
}

function validateSubjectNumber( el, numberType ){

    validateSubjectNumberOnServer( el.value, numberType, el.id, processSubjectNumberSuccess );
}

function  processSubjectNumberSuccess(xhr){
    //alert(xhr.responseText);
    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
    var message = xhr.responseXML.getElementsByTagName("message").item(0);
    var messageParts = message.firstChild.nodeValue.split("#");
    var valid = messageParts[0] == "valid";
    var warning = messageParts[0] == "warning";
    var fail = messageParts[0] == "fail";
    var success = valid || warning;
    var labElement = formField.firstChild.nodeValue;

    setValidIndicaterOnField(success, labElement);
    pt_setFieldValidity( success, labElement );

    if( warning || fail ){
        alert( messageParts[1] );
    }

    pt_setSave();
}
</script>
<nested:hidden name='<%=formName%>' property="patientProperties.currentDate" styleId="currentDate"/>

<div id="PatientPage" style="display:inline"  >
	<nested:hidden property="patientProperties.patientLastUpdated" name='<%=formName%>' styleId="patientLastUpdated" />
	<nested:hidden property="patientProperties.personLastUpdated" name='<%=formName%>'  styleId="personLastUpdated"/>

	<tiles:insert attribute="patientSearch" />

	<nested:hidden name='<%=formName%>' property="patientProperties.patientProcessingStatus" styleId="processingStatus" value="add" />
	<nested:hidden name='<%=formName%>' property="patientProperties.patientPK" styleId="patientPK_ID" />
	<nested:hidden name='<%=formName%>' property="patientProperties.guid" styleId="patientGUID_ID" />
    <logic:equal value="false" name="<%=formName%>" property="patientProperties.readOnly" >
	<br/>
	<div class="patientSearch">
		<hr style="width:100%" />
        <input type="button" value="Nouveau Patient" onclick="addPatient()">
	</div>
    </logic:equal>
	<div id="PatientDetail"   >
	<h2><bean:message key="patient.information"/></h2>
	<table style="width:80%" border="0">
    <tr>
        <% if( !supportSubjectNumber){ %>
        <td>
            <bean:message key="patient.externalId"/>
            <% if( patientIDRequired){ %>
            <span class="requiredlabel">*</span>
            <% } %>
        </td>
        <%} %>
        <% if( supportSTNumber){ %>
        <td style="text-align:right;">
            <bean:message key="patient.ST.number"/>:
        </td>
        <td>
            <nested:text name='<%=formName%>'
                         property="patientProperties.STnumber"
                         onchange="validateSubjectNumber(this, 'STnumber');updatePatientEditStatus();"
                         styleId="ST_ID"
                         styleClass="text"
                         size="60" />
        </td>
    </tr>
    <tr>
        <td style="width:5%">&nbsp;

        </td>
        <%} %>
        <% if( supportSubjectNumber){ %>
        <td>&nbsp;

        </td>
        <td style="text-align:right;">
            <bean:message key="patient.subject.number"/>:
            <span class="requiredlabel">*</span>
        </td>
        <td>
            <nested:text name='<%=formName%>'
                         property="patientProperties.subjectNumber"
                         onchange="validateSubjectNumber(this, 'subjectNumber');updatePatientEditStatus();"
                         styleId="subjectNumberID"
                         styleClass="text"
                         size="60" />
        </td>
    </tr>
    <tr>
        <td style="width:5%">&nbsp;

        </td>
        <% } %>
        <% if( supportNationalID ){ %>
        <td style="width:25%;text-align:right;">
            <%=StringUtil.getContextualMessageForKey("patient.NationalID") %>:

        </td>
        <td style="width:25%">
            <nested:text name='<%=formName%>'
                         property="patientProperties.nationalId"
                         onchange="validateSubjectNumber(this, 'nationalId');updatePatientEditStatus();"
                         styleId="nationalID"
                         styleClass="text"
                         size="60"/>
        </td>
        <td style="width:10%">&nbsp;

        </td>
        <td style="width:15%">&nbsp;

        </td>
    </tr>
    <%} %>
    <tr class="spacerRow" ><td colspan="2">&nbsp;</td></tr>
	<tr>
		<td style="width:15%">
			<bean:message key="patient.name" />
		</td>
		<td style="width:10%;text-align:right;">
			<bean:message key="patient.epiLastName" />
			:
			<% if( patientNamesRequired){ %>
				<span class="requiredlabel">*</span>
			<% } %>
		</td>
		<td style="width:20%">
			<nested:text name='<%=formName%>'
					  property="patientProperties.lastName"
					  styleClass="text"
				      size="60"
				      onchange="updatePatientEditStatus();"
				      styleId="lastNameID"/>
		</td>
		<td style="width:10%;text-align:right;">
			<bean:message key="patient.epiFirstName" />
			:
			<% if( patientNamesRequired){ %>
				<span class="requiredlabel">*</span>
			<% } %>	
		</td>
		<td style="width:20%">
			<nested:text name='<%=formName%>'
					  property="patientProperties.firstName"
					  styleClass="text"
					  size="40"
					  onchange="updatePatientEditStatus();"
					  styleId="firstNameID"/>
		</td>
	</tr>
	<% if(supportAKA){ %>
	<tr>
	<td></td>
	<td style="text-align:right;">
		<bean:message key="patient.aka"/>
	</td>
	<td>
		<nested:text name='<%=formName%>'
				  property="patientProperties.aka"
				  onchange="updatePatientEditStatus();"
				  styleId="akaID"
				  styleClass="text"
				  size="60" />
	</td>
	</tr>
	<% } %>
	<% if( supportMothersName ){ %>
	<tr>
		<td></td>
		<td style="text-align:right;">
			<bean:message key="patient.mother.name"/>
		</td>
		<td>
			<nested:text name='<%=formName%>'
					  property="patientProperties.mothersName"
					  onchange="updatePatientEditStatus();"
				  	  styleId="motherID"
				  	  styleClass="text"
				      size="60" />
		</td>
	</tr>
	<% } if(supportMothersInitial){ %>
	<tr>
		<td></td>
		<td style="text-align:right;">
			<bean:message key="patient.mother.initial"/>
		</td>
		<td>
			<nested:text name='<%=formName%>'
					  property="patientProperties.mothersInitial"
					  onchange="updatePatientEditStatus();"
				  	  styleId="motherInitialID"
				  	  styleClass="text"
				      size="1"
				      maxlength="1" />
		</td>
	</tr>
	<%} %>
	<tr class="spacerRow" ><td colspan="2">&nbsp;</td></tr>
	<tr>
		<td >
			<bean:message  key="person.streetAddress" />
		</td>
		<td style="text-align:right;">
			<bean:message  key="person.streetAddress.street" />:
		</td>
		<td>
			<nested:text name='<%=formName%>'
					  property="patientProperties.streetAddress"
					  onchange="updatePatientEditStatus();"
					  styleId="streetID"
					  styleClass="text"
					  size="70" />
		</td>
	</tr>
	<% if( FormFields.getInstance().useField(Field.AddressVillage)) { %>
	<tr>
		<td></td>
		<td style="text-align:right;">
		    <%= StringUtil.getContextualMessageForKey("person.town") %>:
		</td>
		<td>
			<nested:text name='<%=formName%>'
					  property="patientProperties.city"
					  onchange="updatePatientEditStatus();"
					  styleId="cityID"
					  styleClass="text"
					  size="30" />
		</td>
	</tr>
	<% } %>
	<% if( supportCommune){ %>
	<tr>
		<td></td>
		<td style="text-align:right;">
			<bean:message  key="person.commune" />:
		</td>
		<td>
			<nested:text name='<%=formName%>'
					  property="patientProperties.commune"
					  onchange="updatePatientEditStatus();"
					  styleId="communeID"
					  styleClass="text"
					  size="30" />
		</td>
	</tr>
	<% } %>
	<% if( supportAddressDepartment){ %>
	<tr>
		<td></td>
		<td style="text-align:right;">
			<bean:message  key="person.department" />:
		</td>
		<td>
            <logic:equal value="false" name="<%=formName%>" property="patientProperties.readOnly" >
			<html:select name='<%=formName%>'
						 property="patientProperties.addressDepartment"
						 onchange="updatePatientEditStatus();clearDeptMessage();"
					     styleId="departmentID" >
			<option value="0" ></option>
			<html:optionsCollection name="<%=formName %>" property="patientProperties.addressDepartments" label="dictEntry" value="id" />
			</html:select><br>
			<span id="deptMessage"></span>
            </logic:equal>
            <logic:equal value="true" name="<%=formName%>" property="patientProperties.readOnly" >
                <html:text property="patientProperties.addressDepartment" name="<%=formName%>" />
            </logic:equal>
		</td>
	</tr>
	<% } %>
	<% if( FormFields.getInstance().useField(Field.PatientPhone)){ %>
		<tr>
			<td>&nbsp;</td>
			<td style="text-align:right;"><%= StringUtil.getContextualMessageForKey("person.phone") %>:</td>
			<td>
				<html:text styleId="patientPhone" name='<%= formName %>' property="patientProperties.phone" maxlength="35" onchange="validatePhoneNumber( this );" />
			</td>
		</tr>
	<% } %>
	<tr class="spacerRow"><td >&nbsp;</td></tr>
	<% if( FormFields.getInstance().useField(Field.PatientHealthRegion)){ %>
	<tr>
	<td>&nbsp;</td>
	<td style="text-align:right;"><bean:message key="person.health.region"/>: </td>
		<td>
			<nested:hidden name='<%=formName%>' property="patientProperties.healthRegion" styleId="shadowHealthRegion" />
			<html:select name='<%=formName%>'
						 property="patientProperties.healthRegion"
						 onchange="updateHealthDistrict( this );"
					     styleId="healthRegionID" >
			<option value="0" ></option>
			<html:optionsCollection name="<%=formName %>" property="patientProperties.healthRegions" label="value" value="id" />
			</html:select>
		</td>	
	</tr>		
	<% } %>
	<% if( FormFields.getInstance().useField(Field.PatientHealthDistrict)){ %>
	<tr>
	<td>&nbsp;</td>
	<td style="text-align:right;"><bean:message key="person.health.district"/>: </td>
		<td>
			<html:select name='<%=formName%>'
						 property="patientProperties.healthDistrict"
					     styleId="healthDistrictID"
					     disabled="true" >
			<option value="0" ></option>

			</html:select>
		</td>	
	</tr>		
	<% } %>
	</table>

	<table>
	<tr>
		<td style="text-align:right;">
			<bean:message key="patient.birthDate" />&nbsp;<bean:message key="sample.date.format"/>:
			<% if(patientAgeRequired){ %>
				<span class="requiredlabel">*</span>
			<% } %>
		</td>
		<td>
			<nested:text name='<%=formName%>'
					  property="patientProperties.birthDateForDisplay"
					  styleClass="text"
					  size="20"
					  onchange="handleBirthDateChange( this, true ); updatePatientEditStatus();"
					  styleId="dateOfBirthID" />
			<div id="patientProperties.birthDateForDisplayMessage" class="blank" ></div>
		</td>
		<td style="text-align:right;">
			<bean:message  key="patient.age" />:
		</td>
		<td >
            <html:text property="patientProperties.age"
                       name="<%=formName%>"
                       size="3"
                       maxlength="3"
                       onchange="handleAgeChange( this ); updatePatientEditStatus();"
                       styleClass="text"
                    styleId="age"/>
			<div id="patientProperties.ageMessage" class="blank" ></div>
		</td>
		<td style="text-align:right;">
			<bean:message  key="patient.gender" />:
			<% if(patientGenderRequired){ %>
				<span class="requiredlabel">*</span>
			<% } %>
		</td>
		<td>
            <logic:equal value="false" name="<%=formName%>" property="patientProperties.readOnly" >
			<nested:select name='<%=formName%>'
						 property="patientProperties.gender"
						 onchange="updatePatientEditStatus();"
						 styleId="genderID">
				<option value=" " ></option>
				<nested:optionsCollection name='<%=formName%>' property="patientProperties.genders"   label="value" value="id" />
			</nested:select>
            </logic:equal>
            <logic:equal value="true" name="<%=formName%>" property="patientProperties.readOnly" >
                <html:text property="patientProperties.gender" name="<%=formName%>" />
            </logic:equal>
		</td>
	</tr>
	<% if( supportInsurance || supportPatientType){ %>
	<tr>
		<% if( supportPatientType ){ %>
		<td style="text-align:right;">
			<bean:message  key="patienttype.type" />:
		</td>
		<td>
			<nested:select name='<%=formName%>'
						 property="patientProperties.patientType"
						 onchange="updatePatientEditStatus();"
						 styleId="patientTypeID"  >
				<option value="0" ></option>
				<nested:optionsCollection name='<%=formName%>' property="patientProperties.patientTypes" label="description" value="type" />
			</nested:select>
		</td>
		<% } if( supportInsurance ){ %>
		<td style="text-align:right;">
			<bean:message  key="patient.insuranceNumber" />:
		</td>
		<td>
			<nested:text name='<%=formName%>'
					  property="patientProperties.insuranceNumber"
					  onchange="updatePatientEditStatus();"
					  styleId="insuranceID"
					  styleClass="text"
					  size="20" />
		</td>
		<td style="text-align:right;">
		</td>
		<% } %>

	</tr>
	<% } if( supportOccupation ){ %>
	<tr>
	<td style="text-align:right;">
		<bean:message  key="patient.occupation" />:
	</td>
	<td>
		<nested:text name='<%=formName%>'
				  property="patientProperties.occupation"
				  onchange="updatePatientEditStatus();"
				  styleId="occupationID"
				  styleClass="text"
				  size="20" />
	</td>
	</tr>
	<% } %>
	<% if( FormFields.getInstance().useField(Field.PatientEducation)){ %>
		<tr>
			<td style="text-align:right;"><bean:message key="patient.education"/>: </td>
				<td>
					<html:select name='<%=formName%>'
								 property="patientProperties.education"
							     styleId="educationID" >
					<option value="0" ></option>
					<html:optionsCollection name="<%=formName %>" property="patientProperties.educationList" label="value" value="value" />
					</html:select>
				</td>	
			</tr>	
	<% } %>
	<% if( FormFields.getInstance().useField(Field.PatientMarriageStatus)){ %>
		<tr>
			<td style="text-align:right;"><bean:message key="patient.maritialStatus"/>: </td>
				<td>
					<html:select name='<%=formName%>'
								 property="patientProperties.maritialStatus"
							     styleId="maritialStatusID" >
					<option value="0" ></option>
					<html:optionsCollection name="<%=formName %>" property="patientProperties.maritialList" label="value" value="value" />
					</html:select>
				</td>	
			</tr>	
	<% } %>
	<% if( FormFields.getInstance().useField(Field.PatientNationality)){ %>
		<tr>
			<td style="text-align:right;"><bean:message key="patient.nationality"/>: </td>
				<td>
					<html:select name='<%=formName%>'
								 property="patientProperties.nationality"
							     styleId="nationalityID" >
					<option value="0" ></option>
					<html:optionsCollection name="<%=formName %>" property="patientProperties.nationalityList" label="value" value="value" />
					</html:select>
				</td>
				<td><bean:message key="specify"/>:</td>
				<td>
					<html:text name='<%=formName %>'  property="patientProperties.otherNationality" styleId="nationalityOtherId" />
				</td>	
			</tr>	
	<% } %>
	</table>
	</div>
</div>

<script type="text/javascript" >

//overrides method of same name in patientSearch
function selectedPatientChangedForManagement(firstName, lastName, gender, DOB, stNumber, subjectNumber, nationalID, mother, pk ){
	if( pk ){
		getDetailedPatientInfo();
		$("patientPK_ID").value = pk;
	}else{
		clearPatientInfo();
		setUpdateStatus("add");
	}
}

var registered = false;

function registerPatientChangedForManagement(){
	if( !registered ){
		addPatientChangedListener( selectedPatientChangedForManagement );
		registered = true;
	}
}

registerPatientChangedForManagement();
</script>
