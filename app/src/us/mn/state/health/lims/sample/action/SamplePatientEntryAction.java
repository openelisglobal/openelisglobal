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
package us.mn.state.health.lims.sample.action;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import us.mn.state.health.lims.common.action.BaseActionForm;
import us.mn.state.health.lims.common.action.IActionConstants;
import us.mn.state.health.lims.common.formfields.FormFields;
import us.mn.state.health.lims.common.services.DisplayListService;
import us.mn.state.health.lims.common.services.DisplayListService.ListType;
import us.mn.state.health.lims.common.services.SampleOrderService;
import us.mn.state.health.lims.common.util.DateUtil;
import us.mn.state.health.lims.patient.action.bean.PatientManagementInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The SampleEntryAction class represents the initial Action for the SampleEntry
 * form of the application
 *
 */
public class SamplePatientEntryAction extends BaseSampleEntryAction {

	protected ActionForward performAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		String forward = "success";

		request.getSession().setAttribute(IActionConstants.SAVE_DISABLED, IActionConstants.TRUE);

		BaseActionForm dynaForm = (BaseActionForm) form;

		dynaForm.initialize(mapping);

		boolean needSampleInitialConditionList = FormFields.getInstance().useField(FormFields.Field.InitialSampleCondition);

        SampleOrderService sampleOrderService = new SampleOrderService();
        PropertyUtils.setProperty( dynaForm, "sampleOrderItems", sampleOrderService.getSampleOrderItem() );
		PropertyUtils.setProperty(dynaForm, "patientProperties", new PatientManagementInfo());
		PropertyUtils.setProperty(dynaForm, "sampleTypes", DisplayListService.getList(ListType.SAMPLE_TYPE));
		PropertyUtils.setProperty(dynaForm, "testSectionList", DisplayListService.getList(ListType.TEST_SECTION));
        PropertyUtils.setProperty( dynaForm, "currentDate", DateUtil.getCurrentDateAsText());

		addProjectList( dynaForm );

		if (needSampleInitialConditionList) {
			PropertyUtils.setProperty(dynaForm, "initialSampleConditionList", DisplayListService.getList(ListType.INITIAL_SAMPLE_CONDITION));
		}

		return mapping.findForward(forward);
	}

}
