/*
 * LibreClinica is distributed under the
 * GNU Lesser General Public License (GNU LGPL).

 * For details see: https://libreclinica.org/license
 * LibreClinica, copyright (C) 2020
 */
/*
 * GNU Lesser General Public License (GNU LGPL).
 * For details see: https://libreclinica.org/license
 *
 * LibreClinica is distributed under the
 * Copyright 2003-2008 Akaza Research
 */
package org.akaza.openclinica.service.rule.expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.akaza.openclinica.bean.admin.CRFBean;
import org.akaza.openclinica.bean.core.EntityBean;
import org.akaza.openclinica.bean.core.ItemDataType;
import org.akaza.openclinica.bean.core.Status;
import org.akaza.openclinica.bean.core.SubjectEventStatus;
import org.akaza.openclinica.bean.core.Utils;
import org.akaza.openclinica.bean.managestudy.EventDefinitionCRFBean;
import org.akaza.openclinica.bean.managestudy.StudyBean;
import org.akaza.openclinica.bean.managestudy.StudyEventBean;
import org.akaza.openclinica.bean.managestudy.StudyEventDefinitionBean;
import org.akaza.openclinica.bean.managestudy.StudySubjectBean;
import org.akaza.openclinica.bean.submit.CRFVersionBean;
import org.akaza.openclinica.bean.submit.EventCRFBean;
import org.akaza.openclinica.bean.submit.ItemBean;
import org.akaza.openclinica.bean.submit.ItemDataBean;
import org.akaza.openclinica.bean.submit.ItemFormMetadataBean;
import org.akaza.openclinica.bean.submit.ItemGroupBean;
import org.akaza.openclinica.bean.submit.ItemGroupMetadataBean;
import org.akaza.openclinica.dao.admin.CRFDAO;
import org.akaza.openclinica.dao.managestudy.EventDefinitionCRFDAO;
import org.akaza.openclinica.dao.managestudy.StudyEventDAO;
import org.akaza.openclinica.dao.managestudy.StudyEventDefinitionDAO;
import org.akaza.openclinica.dao.managestudy.StudySubjectDAO;
import org.akaza.openclinica.dao.submit.CRFVersionDAO;
import org.akaza.openclinica.dao.submit.EventCRFDAO;
import org.akaza.openclinica.dao.submit.ItemDAO;
import org.akaza.openclinica.dao.submit.ItemDataDAO;
import org.akaza.openclinica.dao.submit.ItemFormMetadataDAO;
import org.akaza.openclinica.dao.submit.ItemGroupDAO;
import org.akaza.openclinica.dao.submit.ItemGroupMetadataDAO;
import org.akaza.openclinica.domain.datamap.StudyEvent;
import org.akaza.openclinica.domain.rule.RuleSetBean;
import org.akaza.openclinica.domain.rule.expression.ExpressionObjectWrapper;
import org.akaza.openclinica.exception.OpenClinicaSystemException;
import org.akaza.openclinica.i18n.util.ResourceBundleProvider;
import org.akaza.openclinica.logic.expressionTree.ExpressionTreeHelper;
import org.joda.time.DateMidnight;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpressionService {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());
    
    private final String SEPARATOR = ".";
    private final String ESCAPED_SEPARATOR = "\\.";
    private final String STUDY_EVENT_DEFINITION_OR_ITEM_GROUP_PATTERN = "[A-Z_0-9]+|[A-Z_0-9]+\\[(ALL|[1-9]\\d*)\\]$";
    private final String STUDY_EVENT_DEFINITION_OR_ITEM_GROUP_PATTERN_NO_ALL = "[A-Z_0-9]+|[A-Z_0-9]+\\[[1-9]\\d*\\]$";
    private final String STUDY_EVENT_DEFINITION_OR_ITEM_GROUP_PATTERN_WITH_ORDINAL = "[A-Z_0-9]+\\[(END|ALL|[1-9]\\d*)\\]$";
    private final String STUDY_EVENT_DEFINITION_OR_ITEM_GROUP_PATTERN_WITH_END = "[A-Z_0-9]+|[A-Z_0-9]+\\[(END|ALL|[1-9]\\d*)\\]$";
    private final String PRE = "[A-Z_0-9]+\\[";
    private final String POST = "\\]";
    private final String CRF_OID_OR_ITEM_DATA_PATTERN = "[A-Z_0-9]+";
    private final String BRACKETS_AND_CONTENTS = "\\[(END|ALL|[1-9]\\d*)\\]";
    private final String ALL_IN_BRACKETS = "ALL";
    private final String OPENING_BRACKET = "[";
    private final String CLOSING_BRACKET = "]";

    DataSource ds;
    Pattern[] pattern;
    Pattern[] rulePattern;
    Pattern[] ruleActionPattern;
    ExpressionObjectWrapper expressionWrapper;

    public final static String STARTDATE = ".STARTDATE";
    public final static String STATUS = ".STATUS";
    public static final String STUDY_EVENT_OID_START_KEY = "SE_";
    /*
     * The variables below are used as a small Cache so that we don't go to the
     * database every time we want to get an Object by it's OID. This is a very
     * stripped down cache which will help performance in a single
     * request/response cycle.
     */
    private HashMap<String, StudyEventDefinitionBean> studyEventDefinitions;
    private HashMap<String, ItemGroupBean> itemGroups;
    private HashMap<String, ItemBean> items;

    public ExpressionService(DataSource ds) {
        init(ds, null);
    }

    public ExpressionService(ExpressionObjectWrapper expressionWrapper) {
        init(expressionWrapper.getDs(), expressionWrapper);
    }

    private void init(DataSource ds, ExpressionObjectWrapper expressionWrapper) {
        pattern = new Pattern[4];
        pattern[3] = Pattern.compile(STUDY_EVENT_DEFINITION_OR_ITEM_GROUP_PATTERN); // STUDY_EVENT_DEFINITION_OID
                                                                                    // +
                                                                                    // ordinal
        pattern[2] = Pattern.compile(CRF_OID_OR_ITEM_DATA_PATTERN); // CRF_OID
                                                                    // or
                                                                    // CRF_VERSION_OID
        pattern[1] = Pattern.compile(STUDY_EVENT_DEFINITION_OR_ITEM_GROUP_PATTERN); // ITEM_GROUP_DATA_OID
                                                                                    // +
                                                                                    // ordinal
        pattern[0] = Pattern.compile(CRF_OID_OR_ITEM_DATA_PATTERN); // ITEM_DATA_OID

        // [ALL] ordinals are not accepted in Rule Expressions
        rulePattern = new Pattern[4];
        rulePattern[3] = Pattern.compile(STUDY_EVENT_DEFINITION_OR_ITEM_GROUP_PATTERN_NO_ALL); // STUDY_EVENT_DEFINITION_OID
                                                                                               // +
                                                                                               // ordinal
        rulePattern[2] = Pattern.compile(CRF_OID_OR_ITEM_DATA_PATTERN); // CRF_OID
                                                                        // or
                                                                        // CRF_VERSION_OID
        rulePattern[1] = Pattern.compile(STUDY_EVENT_DEFINITION_OR_ITEM_GROUP_PATTERN_NO_ALL); // ITEM_GROUP_DATA_OID
                                                                                               // +
                                                                                               // ordinal
        rulePattern[0] = Pattern.compile(CRF_OID_OR_ITEM_DATA_PATTERN); // ITEM_DATA_OID

        // [END] support added
        ruleActionPattern = new Pattern[4];
        ruleActionPattern[3] = Pattern.compile(STUDY_EVENT_DEFINITION_OR_ITEM_GROUP_PATTERN); // STUDY_EVENT_DEFINITION_OID
                                                                                              // +
                                                                                              // ordinal
        ruleActionPattern[2] = Pattern.compile(CRF_OID_OR_ITEM_DATA_PATTERN); // CRF_OID
                                                                              // or
                                                                              // CRF_VERSION_OID
        ruleActionPattern[1] = Pattern.compile(STUDY_EVENT_DEFINITION_OR_ITEM_GROUP_PATTERN_WITH_END); // ITEM_GROUP_DATA_OID
                                                                                                       // +
                                                                                                       // ordinal
        ruleActionPattern[0] = Pattern.compile(CRF_OID_OR_ITEM_DATA_PATTERN); // ITEM_DATA_OID

        this.studyEventDefinitions = new HashMap<>();
        this.itemGroups = new HashMap<>();
        this.items = new HashMap<>();

        this.ds = ds;
        this.expressionWrapper = expressionWrapper;
    }

    public boolean ruleSetExpressionChecker(String expression) {
        if (checkSyntax(expression)) {
            isExpressionValid(expression);
        } else {
            throw new OpenClinicaSystemException("OCRERR_0032");
        }
        return true;
    }

    public String ifValueIsDate(ItemBean itemBean, String value) {
        String theFinalValue = value;

        if (value != null && itemBean.getDataType() == ItemDataType.DATE) {
            value = Utils.convertedItemDateValue(
                value,
                ResourceBundleProvider.getFormatBundle().getString("date_format_string"), "MM/dd/yyyy"
            );
            theFinalValue = ExpressionTreeHelper.isValidDateMMddyyyy(value);
        }
        return theFinalValue;
    }

    private String getValueFromDb(String expression, List<ItemDataBean> itemData, Map<Integer, ItemBean> itemBeans)
            throws OpenClinicaSystemException {
        if (isExpressionPartial(expression)) {
            throw new OpenClinicaSystemException(
                "getValueFromDb:We cannot get the Value of a PARTIAL expression : " + expression
            );
        }
        String value = null;
        try {
            int index = getItemGroupOidOrdinalFromExpression(expression).equals("") ? 0 : Integer
                    .parseInt(getItemGroupOidOrdinalFromExpression(expression)) - 1;
            ItemDataBean itemDataBean = itemData.get(index);
            value = itemData.get(index).getValue();
            if (itemBeans.containsKey(itemDataBean.getItemId())) {
                value = ifValueIsDate(itemBeans.get(itemDataBean.getItemId()), value);
            }
        } catch (NullPointerException npe) {
            logger.error("NullPointerException was thrown ");
        } catch (IndexOutOfBoundsException ioobe) {
            logger.error("IndexOutOfBoundsException was thrown ");
        }

        return value;
    }

    public String getSSZoneId() {
        Integer subjectId = expressionWrapper.getStudySubjectId();
        logger.debug("subjectId {} : ", subjectId);
        if (subjectId == null) {
            return null;
        }
        StudySubjectBean ssBean = getStudySubjectDao().findByPK(subjectId);
        return ssBean.getTime_zone().trim();
    }

    public HashMap<String, String> getSSDate(String ssZoneId, String serverZoneId) {
        HashMap<String, String> map = new HashMap<>();
        if (ssZoneId == null || ssZoneId.equals("")) {
            ssZoneId = TimeZone.getDefault().getID();
        }

        DateTimeZone ssZone = DateTimeZone.forID(ssZoneId);
        DateMidnight dm = new DateMidnight(ssZone);
        DateTimeFormatter fmt = ISODateTimeFormat.date();
        map.put("ssDate", fmt.print(dm));

        map.put("serverZoneId", serverZoneId);
        DateTimeZone serverZone = DateTimeZone.forID(serverZoneId);
        DateMidnight serverDate = new DateMidnight(serverZone);
        map.put("serverDate", fmt.print(serverDate));
        return map;
    }

    public HashMap<String, String> getValueFromDbb(String expression) throws OpenClinicaSystemException {
        HashMap<String, String> map = new HashMap<>();
        if (isExpressionPartial(expression)) {
            throw new OpenClinicaSystemException(
                "getValueFromDb:We cannot get the Value of a PARTIAL expression : " + expression
            );
        }

        try {
            // Get the studyEventId from RuleSet Target so we can know which
            // StudySubject we are dealing with.
            String ruleSetExpression = expressionWrapper.getRuleSet().getTarget().getValue();
            String ruleSetExpressionStudyEventId = getStudyEventDefinitionOidOrdinalFromExpression(ruleSetExpression);
            StudyEventBean studyEvent = getStudyEventDao().findByPK(Integer.parseInt(ruleSetExpressionStudyEventId));

            // Prepare Method arguments
            String studyEventDefinitionOid = getStudyEventDefinitionOidFromExpression(expression);
            String crfOrCrfVersionOid = getCrfOidFromExpression(expression);
            String studyEventDefinitionOrdinal = getStudyEventDefinitionOidOrdinalFromExpression(expression);
            studyEventDefinitionOrdinal = studyEventDefinitionOrdinal.equals("") ? "1" : studyEventDefinitionOrdinal;
            String studySubjectId = String.valueOf(studyEvent.getStudySubjectId());
            logger.debug("studySubjectId: " + studySubjectId);
            logger.debug(
                "ruleSet studyEventId  {} , studyEventDefinitionOid {} , crfOrCrfVersionOid {} , studyEventDefinitionOrdinal {} ,studySubjectId {}",
                studyEvent.getId(), studyEventDefinitionOid, crfOrCrfVersionOid, studyEventDefinitionOrdinal, studySubjectId
            );

            StudyEventBean studyEventOfThisExpression = getStudyEventDao()
                .findAllByStudyEventDefinitionAndCrfOidsAndOrdinal(
                    studyEventDefinitionOid, crfOrCrfVersionOid, studyEventDefinitionOrdinal, studySubjectId
                );

            if (studyEvent.getId() == studyEventOfThisExpression.getId()) {
                map.put("match", "true");
            }

            logger.debug(
                "studyEvent : {} , itemOid {} , itemGroupOid {}",
                studyEventOfThisExpression.getId(), getItemOidFromExpression(expression), getItemGroupOidFromExpression(expression)
            );

            List<ItemDataBean> itemData = getItemDataDao().findByStudyEventAndOids(
                studyEventOfThisExpression.getId(),
                getItemOidFromExpression(expression),
                getItemGroupOidFromExpression(expression)
            );

            expression = fixGroupOrdinal(expression, ruleSetExpression, itemData, expressionWrapper.getEventCrf());

            int index = getItemGroupOidOrdinalFromExpression(expression).equals("") ? 0 : Integer
                    .parseInt(getItemGroupOidOrdinalFromExpression(expression)) - 1;

            ItemDataBean itemDataBean = itemData.get(index);
            ItemBean itemBean = getItemDao().findByPK(itemDataBean.getItemId());
            String value = itemData.get(index).getValue();
            value = ifValueIsDate(itemBean, value);
            map.put("value", value);
        } catch (Exception e) {
            logger.debug("Exception was thrown", e);
        }

        return map;
    }

    public ItemDataBean getItemDataBeanFromDb(String expression) throws OpenClinicaSystemException {
        if (isExpressionPartial(expression)) {
            throw new OpenClinicaSystemException(
                "getItemDataBeanFromDb:We cannot get the ItemData of a PARTIAL expression : " + expression
            );
        }
        String studyEventId = getStudyEventDefinitionOidOrdinalFromExpression(expression);
        int index = getItemGroupOidOrdinalFromExpression(expression).equals("") ? 0 : Integer
                .parseInt(getItemGroupOidOrdinalFromExpression(expression)) - 1;
        List<ItemDataBean> itemData = getItemDataDao().findByStudyEventAndOids(Integer.valueOf(studyEventId),
                getItemOidFromExpression(expression), getItemGroupOidFromExpression(expression));

        return itemData.size() > index ? itemData.get(index) : null;
    }

    public String getValueFromForm(String expression) {
        String result = null;
        HashMap<String, String> formValues = expressionWrapper.getItemsAndTheirValues();
        if (formValues != null && !formValues.isEmpty()) {
            String withGroup = getItemGroupPLusItem(expression);
            String withoutGroup = getItemOidFromExpression(expression);
            result = formValues.containsKey(withGroup) ? formValues.get(withGroup) : formValues
                    .containsKey(withoutGroup) ? formValues.get(withoutGroup) : null;
        } else {
            logger.warn("The HashMap that stores form values was null, Better this be a Bulk operation");
        }
        return result;
    }

    public String getValueFromForm(String expression, Map<String, ItemBean> itemBeans) {
        if (itemBeans == null) {
            logger.debug("The Map that stores ItemBeans is null. Item Date value cannot be processed.");
        }
        String result = null;
        HashMap<String, String> formValues = expressionWrapper.getItemsAndTheirValues();
        if (formValues != null && !formValues.isEmpty()) {
            String withGroup = getItemGroupPLusItem(expression);
            String withoutGroup = getItemOidFromExpression(expression);
            result = formValues.containsKey(withGroup) ? formValues.get(withGroup) : formValues
                    .containsKey(withoutGroup) ? formValues.get(withoutGroup) : null;
            if (itemBeans != null) {
                ItemBean itemBean = itemBeans.containsKey(withGroup) ? itemBeans.get(withGroup) : itemBeans
                        .containsKey(withoutGroup) ? itemBeans.get(withoutGroup) : null;
                result = ifValueIsDate(itemBean, result);
            }
        } else {
            logger.warn("The HashMap that stores form values was null, Better this be a Bulk operation");
        }
        return result;
    }

    public String evaluateExpression(String expression) throws OpenClinicaSystemException {
        String value = null;
        Map<Integer, ItemBean> itemBeansI = new HashMap<>();

        if (items != null) {
            Iterator<ItemBean> iter = items.values().iterator();
            while (iter.hasNext()) {
                ItemBean item = iter.next();
                itemBeansI.put(item.getId(), item);
            }
        }
        if (expressionWrapper.getRuleSet() != null) {
            if (checkIfExpressionIsForScheduling(expression)) {
                StudyEvent studyEvent;

                if (expression.endsWith(ExpressionService.STARTDATE)) {
                    String oid = expression.substring(0, expression.indexOf(ExpressionService.STARTDATE));
                    studyEvent = getStudyEventFromOID(oid);
                    if (studyEvent != null) {
                        logger.debug(
                            "Study Event Start Date: " + studyEvent.getDateStart().toString().substring(0, 10).trim()
                        );
                        return studyEvent.getDateStart().toString().substring(0, 10).trim();
                    } else {
                        return "";
                    }
                } else {
                    String oid = expression.substring(0, expression.indexOf(ExpressionService.STATUS));
                    studyEvent = getStudyEventFromOID(oid);
                    if (studyEvent != null) {
                        logger.debug(
                            "Status: " + SubjectEventStatus.getSubjectEventStatusName(studyEvent.getSubjectEventStatusId())
                        );
                        return SubjectEventStatus.getSubjectEventStatusName(studyEvent.getSubjectEventStatusId());
                    } else {
                        return "";
                    }
                }
            }

            if (isExpressionPartial(expression)) {
                String fullExpression = constructFullExpressionIfPartialProvided(expression, expressionWrapper
                        .getRuleSet().getTarget().getValue());
                List<ItemDataBean> itemDatas = getItemDatas(fullExpression);

                fullExpression = fixGroupOrdinal(fullExpression, expressionWrapper.getRuleSet().getTarget().getValue(),
                        itemDatas, expressionWrapper.getEventCrf());
                if (checkSyntax(fullExpression)) {
                    String valueFromForm;
                    if (items == null) {
                        valueFromForm = getValueFromForm(fullExpression);
                    } else {
                        valueFromForm = getValueFromForm(fullExpression, items);
                    }
                    String valueFromDb = getValueFromDb(fullExpression, itemDatas, itemBeansI);
                    logger.debug("valueFromForm : {} , valueFromDb : {}", valueFromForm, valueFromDb);
                    if (valueFromForm == null && valueFromDb == null) {
                        throw new OpenClinicaSystemException(
                            "OCRERR_0017", new Object[] { fullExpression, expressionWrapper.getRuleSet().getTarget().getValue() }
                        );
                    }
                    /*
                     * if (valueFromForm != null) { // TODO: Do this if type a
                     * date String dateFormat =
                     * ResourceBundleProvider.getFormatBundle
                     * ().getString("date_format_string"); String dateRegexp =
                     * ResourceBundleProvider
                     * .getFormatBundle().getString("date_regexp");
                     * valueFromForm =
                     * ExpressionTreeHelper.isValidDate(valueFromForm,
                     * dateFormat, dateRegexp); }
                     */
                    value = valueFromForm == null ? valueFromDb : valueFromForm;
                }
            } else {
                // So Expression is not Partial
                HashMap<String, String> map = getValueFromDbb(expression);
                String valueFromForm = null;
                if (checkSyntax(expression)) {
                    String valueFromDb = map.get("value");
                    String matchEvents = map.get("match");

                    // if se_id are a match go in , otherwise nothing
                    if (matchEvents != null && matchEvents.equals("true")) {
                        if (items == null) {
                            valueFromForm = getValueFromForm(expression);
                        } else {
                            valueFromForm = getValueFromForm(expression, items);
                        }
                    }
                    logger.debug("valueFromDb : {}", valueFromDb);
                    value = valueFromForm == null ? valueFromDb : valueFromForm;
                    if (value == null) {
                        logger.info("The value is null for expression" + expression);
                        throw new OpenClinicaSystemException("OCRERR_0018", new Object[] { expression });
                    }
                }
            }
        }
        return value;
    }

    public boolean checkIfExpressionIsForScheduling(String expression) {
        return expression.toUpperCase().startsWith("SE_") &&
               (expression.toUpperCase().endsWith(ExpressionService.STARTDATE) ||
                expression.toUpperCase().endsWith(ExpressionService.STATUS));
    }

    public StudyEvent getStudyEventFromOID(String oid) {
        Integer subjectId = expressionWrapper.getStudySubjectId();
        StudyEvent studyEvent;
        if (oid.contains("[")) {
            int leftBracketIndex = oid.indexOf("[");
            int rightBracketIndex = oid.indexOf("]");
            int ordinal = Integer.parseInt(oid.substring(leftBracketIndex + 1, rightBracketIndex));
            studyEvent = getStudyEventFromDb(oid.substring(0, leftBracketIndex), ordinal, subjectId);
        } else {
            studyEvent = getStudyEventFromDb(oid, 1, subjectId);
        }
        return studyEvent;
    }

    private StudyEvent getStudyEventFromDb(String oid, Integer ordinal, Integer studySubjectId) {
        StudyEvent studyEvent;
        try {
            studyEvent = expressionWrapper.getStudyEventDaoHib()
                .fetchByStudyEventDefOIDAndOrdinalTransactional(oid, ordinal, studySubjectId);
        } catch (org.springframework.transaction.IllegalTransactionStateException itse) {
            studyEvent = expressionWrapper.getStudyEventDaoHib()
                .fetchByStudyEventDefOIDAndOrdinal(oid, ordinal, studySubjectId);
        }
        return studyEvent;
    }

    private List<ItemDataBean> getItemDatas(String expression) {
        String studyEventId = getStudyEventDefinitionOidOrdinalFromExpression(expression);
        return getItemDataDao().findByStudyEventAndOids(
            Integer.valueOf(studyEventId),
            getItemOidFromExpression(expression),
            getItemGroupOidFromExpression(expression)
        );
    }

    private String fixGroupOrdinal(String ruleExpression, String targetExpression, List<ItemDataBean> itemData, EventCRFBean eventCrf) {

        String returnedRuleExpression = ruleExpression;

        if (getItemGroupOid(ruleExpression).equals(getItemGroupOid(targetExpression))) {
            if (getGroupOrdninalCurated(ruleExpression).equals("") &&
                !getGroupOrdninalCurated(targetExpression).equals("")) {

                returnedRuleExpression = replaceGroupOidOrdinalInExpression(
                    ruleExpression,
                    Integer.valueOf(getGroupOrdninalCurated(targetExpression))
                );
            }
        } else {
            EventCRFBean theEventCrfBean;
            // TODO: because of DAO refactoring we may need to check also eventCrf.isActive()
            if (eventCrf != null) {
                theEventCrfBean = eventCrf;
            } else if (!itemData.isEmpty()) {
                theEventCrfBean = getEventCRFDao().findByPK(itemData.get(0).getEventCRFId());
            } else {
                return returnedRuleExpression;
            }

            Integer itemId = itemData.isEmpty() ? (getItemDao().findByOid(getItemOid(ruleExpression)).get(0)).getId() : itemData.get(0).getItemId();

            ItemGroupMetadataBean itemGroupMetadataBean = getItemGroupMetadataDao().findByItemAndCrfVersion(
                itemId,
                theEventCrfBean.getCRFVersionId()
            );
            if (isGroupRepeating(itemGroupMetadataBean) && getGroupOrdninalCurated(ruleExpression).equals("")) {
                returnedRuleExpression = replaceGroupOidOrdinalInExpression(
                    ruleExpression,
                    Integer.valueOf(getGroupOrdninalCurated(targetExpression))
                );
            }

        }
        return returnedRuleExpression;
    }

    private Boolean isGroupRepeating(ItemGroupMetadataBean itemGroupMetadataBean) {
        return itemGroupMetadataBean.getRepeatNum() > 1 || itemGroupMetadataBean.getRepeatMax() > 1;
    }

    public boolean isInsertActionExpressionValid(String expression, RuleSetBean ruleSet, Integer allowedLength) {
        boolean result = false;

        Integer k = getExpressionSize(expression);
        if (k > allowedLength) {
            return false;
        }

        if (ruleSet != null) {
            String fullExpression = constructFullExpressionIfPartialProvided(expression, ruleSet.getTarget().getValue());
            boolean isRuleExpressionValid = checkInsertActionExpressionSyntax(fullExpression);

            if (isRuleExpressionValid) {
                isExpressionValid(fullExpression);
                result = true;
            }
        }
        return result;
    }

    public boolean isRandomizeActionExpressionValid(String expression, RuleSetBean ruleSet, Integer allowedLength) {
        boolean result = false;

        Integer k = getExpressionSize(expression);
        if (k > allowedLength) {
            return false;
        }

        if (ruleSet != null) {
            String fullExpression = constructFullExpressionIfPartialProvided(expression, ruleSet.getTarget().getValue());
            boolean isRuleExpressionValid = checkInsertActionExpressionSyntax(fullExpression);

            if (isRuleExpressionValid) {
                isExpressionValid(fullExpression);
                result = true;
            }
        }
        return result;
    }

    public boolean isExpressionValid(String expression, RuleSetBean ruleSet, Integer allowedLength) {
        boolean result = false;

        Integer k = getExpressionSize(expression);
        if (k > allowedLength) {
            return false;
        }

        if (ruleSet != null) {
            String fullExpression = constructFullExpressionIfPartialProvided(expression, ruleSet.getTarget().getValue());
            boolean isRuleExpressionValid = checkSyntax(fullExpression);

            if (isRuleExpressionValid) {
                isExpressionValid(fullExpression);
                result = true;
            }
        }
        return result;
    }

    public boolean ruleExpressionChecker(String expression) {
        boolean result = false;

        isExpressionValid(expression);

        if (checkIfExpressionIsForScheduling(expression)) {
            return checkSyntax(expression);
        }

        if (expressionWrapper.getRuleSet() != null) {
            if (isExpressionPartial(expressionWrapper.getRuleSet().getTarget().getValue())) {
                return true;
            }
            String fullExpression = constructFullExpressionIfPartialProvided(expression, expressionWrapper.getRuleSet()
                    .getTarget().getValue());

            boolean isRuleExpressionValid;
            if (isExpressionPartial(expression)) {
                isRuleExpressionValid = checkSyntax(fullExpression);
            } else {
                isRuleExpressionValid = checkRuleExpressionSyntax(fullExpression);
            }

            if (isRuleExpressionValid) {
                isExpressionValid(fullExpression);
                result = true;
            }

            String targetGroupOid = getItemGroupOid(expressionWrapper.getRuleSet().getTarget().getValue());
            String ruleGroupOid = getItemGroupOid(fullExpression);
            CRFVersionBean targetCrfVersion = getCRFVersionFromExpression(expressionWrapper.getRuleSet().getTarget()
                    .getValue());
            CRFVersionBean ruleCrfVersion = getCRFVersionFromExpression(fullExpression);
            Boolean isTargetGroupRepeating = targetCrfVersion == null ? getItemGroupDao()
                    .isItemGroupRepeatingBasedOnAllCrfVersions(targetGroupOid) : getItemGroupDao()
                    .isItemGroupRepeatingBasedOnCrfVersion(targetGroupOid, targetCrfVersion.getId());
            Boolean isRuleGroupRepeating = ruleCrfVersion == null ? getItemGroupDao()
                    .isItemGroupRepeatingBasedOnAllCrfVersions(ruleGroupOid) : getItemGroupDao()
                    .isItemGroupRepeatingBasedOnCrfVersion(ruleGroupOid, ruleCrfVersion.getId());
            if (!isTargetGroupRepeating && isRuleGroupRepeating) {
                String ordinal = getItemGroupOidOrdinalFromExpression(fullExpression);
                if (ordinal.equals("") || ordinal.equals("ALL")) {
                    result = false;
                }
            }
        } else {
            if (checkSyntax(expression) && getItemBeanFromExpression(expression) != null) {
                result = true;
            }
        }
        return result;
    }

    public Integer getExpressionSize(String expression) {
        String[] splitExpression = expression.split(ESCAPED_SEPARATOR);
        return splitExpression.length;
    }

    public Boolean isExpressionPartial(String expression) {
        String[] splitExpression = expression.split(ESCAPED_SEPARATOR);
        return splitExpression.length != 4;
    }

    public String constructFullExpressionIfPartialProvided(String expression, CRFVersionBean crfVersion, StudyEventDefinitionBean studyEventDefinition) {
        String[] splitExpression = expression.split(ESCAPED_SEPARATOR);
        String resultingExpression = null;
        if (splitExpression.length == 1) {
            ItemGroupMetadataBean itemGroupMetadata = getItemGroupMetadataDao()
                    .findByItemAndCrfVersion(getItemBeanFromExpression(expression).getId(), crfVersion.getId());
            ItemGroupBean itemGroup = (ItemGroupBean) getItemGroupDao().findByPK(itemGroupMetadata.getItemGroupId());
            resultingExpression = studyEventDefinition.getOid() + SEPARATOR + crfVersion.getOid() + SEPARATOR
                    + itemGroup.getOid() + SEPARATOR + expression;
        }
        if (splitExpression.length == 2) {
            resultingExpression = studyEventDefinition.getOid() + SEPARATOR + crfVersion.getOid() + SEPARATOR
                    + expression;
        }
        if (splitExpression.length == 3) {
            resultingExpression = studyEventDefinition.getOid() + SEPARATOR + expression;
        }
        return resultingExpression;
    }

    public String constructFullExpressionIfPartialProvided(String expression, String ruleSetTargetExpression) {
        if (expression == null || expression.isEmpty()) {
            logger.debug("expression is null.");
            return expression;
        } else {
            String[] splitExpression = expression.split(ESCAPED_SEPARATOR);
            switch (splitExpression.length) {
            case 1:
                return deContextualizeExpression(3, expression, ruleSetTargetExpression);
            case 2:
                return deContextualizeExpression(2, expression, ruleSetTargetExpression);
            case 3:
                return deContextualizeExpression(1, expression, ruleSetTargetExpression);
            case 4:
                return expression;
            default:
                throw new OpenClinicaSystemException(
                    "Full Expression cannot be constructed from provided expression : " + expression
                );
            }
        }
    }

    private String deContextualizeExpression(int j, String ruleExpression, String ruleSetTargetExpression) {
        String[] splitRuleSetExpression = ruleSetTargetExpression.split(ESCAPED_SEPARATOR);
        String buildExpression = "";
        ItemBean item = null;
        String groupOidWithoutOrdinal = "";
        String groupOidFromItem = "";
        String repeatOrdinal = "";
        if (j == 3) {
            item = getItemFromExpression(ruleExpression);
			int indexOfOpenBrack = splitRuleSetExpression[2].indexOf("[");
			if (indexOfOpenBrack != -1) {
			groupOidWithoutOrdinal = splitRuleSetExpression[2].substring(0, indexOfOpenBrack);
			} else {
				groupOidWithoutOrdinal = splitRuleSetExpression[2];
			}
            ArrayList<ItemGroupBean> igBean = getItemGroupDao().findGroupsByItemID(item.getId());
            groupOidFromItem = igBean.get(0).getOid();
        }

        for (int i = 0; i < j; i++) {
            if (j == 3 && i == 2 && !groupOidFromItem.equalsIgnoreCase(groupOidWithoutOrdinal)) {
                ArrayList<ItemFormMetadataBean> itemFormMetadataBeans = getItemFormMetadataDao().findAllByItemId(
                        item.getId());
                List<ItemGroupMetadataBean> itemGroupMetadataBeans = getItemGroupMetadataDao().findByCrfVersion(
                        itemFormMetadataBeans.get(0).getCrfVersionId());
                if (!itemGroupMetadataBeans.get(0).isRepeatingGroup()) {
                    repeatOrdinal = "[1]";
                }
                buildExpression = buildExpression + groupOidFromItem + repeatOrdinal + SEPARATOR;
            } else {
                buildExpression = buildExpression + splitRuleSetExpression[i] + SEPARATOR;
            }

        }
        return buildExpression + ruleExpression;
    }

    private String getItemOidFromExpression(String expression) throws OpenClinicaSystemException {
        return getOidFromExpression(expression, 0, 0);
    }

    private String getItemGroupOidFromExpression(String expression) throws OpenClinicaSystemException {
        return getOidFromExpression(expression, 1, 1).replaceAll(BRACKETS_AND_CONTENTS, "");
    }

    private String getItemGroupOidWithOrdinalFromExpression(String expression) throws OpenClinicaSystemException {
        return getOidFromExpression(expression, 1, 1);
    }

    private String getItemGroupOidOrdinalFromExpression(String expression) throws OpenClinicaSystemException {
        String itemGroupOid = getOidFromExpression(expression, 1, 1);
        String itemGroupOidOrdinal = "";
        if (itemGroupOid.matches(STUDY_EVENT_DEFINITION_OR_ITEM_GROUP_PATTERN_WITH_ORDINAL)) {
            itemGroupOidOrdinal = itemGroupOid.trim().replaceAll(PRE, "").trim().replaceAll(POST, "");
        }
        return itemGroupOidOrdinal;
    }

    private String getItemGroupPLusItem(String expression) throws OpenClinicaSystemException {
        return getItemGroupOidWithOrdinalFromExpression(expression) + SEPARATOR + getItemOidFromExpression(expression);
    }

    private String getCrfOidFromExpression(String expression) throws OpenClinicaSystemException {
        return getOidFromExpression(expression, 2, 2);
    }

    private String getStudyEventDefinitionOidFromExpression(String expression) throws OpenClinicaSystemException {
        return getOidFromExpression(expression, 3, 3).replaceAll(BRACKETS_AND_CONTENTS, "");
    }

    private String getStudyEventDefinitionOidWithOrdinalFromExpression(String expression) throws OpenClinicaSystemException {
        return getOidFromExpression(expression, 3, 3);
    }

    public String getItemGroupNameAndOrdinal(String expression) {
        return getItemGroupExpression(expression).getName() + " " + OPENING_BRACKET
                + getItemGroupOidOrdinalFromExpression(expression) + CLOSING_BRACKET;
    }

    public String getStudyEventDefinitionOidOrdinalFromExpression(String expression) throws OpenClinicaSystemException {
        String studyEventDefinitionOid = getOidFromExpression(expression, 3, 3);
        String studyEventDefinitionOidOrdinal = "";
        if (studyEventDefinitionOid.matches(STUDY_EVENT_DEFINITION_OR_ITEM_GROUP_PATTERN_WITH_ORDINAL)) {
            studyEventDefinitionOidOrdinal = studyEventDefinitionOid.trim().replaceAll(PRE, "").trim()
                    .replaceAll(POST, "");
        }
        return studyEventDefinitionOidOrdinal;
    }

    /**
     * Use this method to create 1ItemOID or ItemOID Used in Data Entry Rule Execution
     *
     * @param expression expression
     * @return GroupOrdinal + ItemOID
     */
    public String getGroupOrdninalConcatWithItemOid(String expression) {
        String ordinal = getGroupOrdninalCurated(expression);
        logger.debug(
            " original expression {} , post getGroupOrdninalConcatWithItemOid : {} ",
            expression, ordinal + getItemOidFromExpression(expression)
        );
        return ordinal + getItemOidFromExpression(expression);
    }

    public String getGroupOidWithItemOid(String expression) {
        return getItemGroupOidWithOrdinalFromExpression(expression) + SEPARATOR + getItemOidFromExpression(expression);
    }

    public String getItemOid(String expression) {
        return getItemOidFromExpression(expression);
    }

    public String getItemGroupOid(String expression) {
        if (expression.split(ESCAPED_SEPARATOR).length < 2) {
            return null;
        }
        return getItemGroupOidFromExpression(expression);
    }

    public String getCrfOid(String expression) {
        if (expression.split(ESCAPED_SEPARATOR).length < 3) {
            return null;
        }
        return getCrfOidFromExpression(expression);
    }

    public String getStudyEventDefenitionOid(String expression) {
        if (expression.split(ESCAPED_SEPARATOR).length < 4) {
            return null;
        }
        return getStudyEventDefinitionOidFromExpression(expression);
    }

    public String getGroupOrdninalCurated(String expression) {
        String originalOrdinal = getItemGroupOidOrdinalFromExpression(expression);
        return originalOrdinal.equals(ALL_IN_BRACKETS) ? "" : originalOrdinal;
    }

    public String getStudyEventDefinitionOrdninalCurated(String expression) {
        if (expression.split(ESCAPED_SEPARATOR).length < 4) {
            return "";
        }
        String originalOrdinal = getStudyEventDefinitionOidOrdinalFromExpression(expression);
        return originalOrdinal.equals(ALL_IN_BRACKETS) ? "" : originalOrdinal;
    }

    public String getStudyEventDefenitionOrdninalCurated(String expression) {
        String originalOrdinal = getStudyEventDefinitionOidOrdinalFromExpression(expression);
        String ordinal;
        if (originalOrdinal.equals(ALL_IN_BRACKETS)) {
            throw new OpenClinicaSystemException("ALL not supported in the following instance");
        } else if (originalOrdinal.equals("")) {
            ordinal = "1";
        } else {
            ordinal = originalOrdinal;
        }
        return ordinal;
    }

    public String getGroupOidConcatWithItemOid(String expression) {
        String result = getItemGroupOidFromExpression(expression) + SEPARATOR + getItemOidFromExpression(expression);
        logger.debug("getGroupOidConcatWithItemOid returns : {} ", result);
        return result;
    }

    public String getGroupOidOrdinal(String expression) {
        String result = this.getItemGroupOidWithOrdinalFromExpression(expression);
        logger.debug("getGroupOidOrdinal returns : {} ", result);
        return result;
    }

    public String replaceGroupOidOrdinalInExpression(String expression, Integer ordinal) {
        String replacement = getStudyEventDefinitionOidWithOrdinalFromExpression(expression) + SEPARATOR
                + getCrfOidFromExpression(expression) + SEPARATOR;
        if (ordinal == null) {
            replacement += getItemGroupOidWithOrdinalFromExpression(expression) + SEPARATOR
                    + getItemOidFromExpression(expression);
        } else {
            replacement += getItemGroupOidFromExpression(expression) + OPENING_BRACKET + ordinal + CLOSING_BRACKET
                    + SEPARATOR + getItemOidFromExpression(expression);
        }
        logger.debug("Original Expression : {} , Rewritten as {} .", expression, replacement);
        return replacement;
    }

    public String replaceCRFOidInExpression(String expression, String replacementCrfOid) {
        if (expression.split(ESCAPED_SEPARATOR).length < 4) {
            if (expression.split(ESCAPED_SEPARATOR).length == 3) {
                return replacementCrfOid + SEPARATOR + getItemGroupOidWithOrdinalFromExpression(expression) + SEPARATOR
                        + getItemOidFromExpression(expression);
            }
            return expression;
        }
        return getStudyEventDefinitionOidWithOrdinalFromExpression(expression) + SEPARATOR + replacementCrfOid
                + SEPARATOR + getItemGroupOidWithOrdinalFromExpression(expression) + SEPARATOR
                + getItemOidFromExpression(expression);
    }

    public String getCustomExpressionUsedToCreateView(String expression, int sampleOrdinal) {
        return getStudyEventDefenitionOid(expression) +
            OPENING_BRACKET + sampleOrdinal + CLOSING_BRACKET +
            SEPARATOR + "XXX" + SEPARATOR + getGroupOidWithItemOid(expression);
    }

    public String replaceStudyEventDefinitionOIDWith(String expression, String replacement) {
        replacement = getStudyEventDefinitionOidFromExpression(expression) + OPENING_BRACKET + replacement + CLOSING_BRACKET;
        String studyEventDefinitionOID = getStudyEventDefinitionOidWithOrdinalFromExpression(expression);
        return expression.replace(studyEventDefinitionOID, replacement);
    }

    /*
     * public String replaceFirstIntegerBy(String expression, String
     * replacement) { return expression.trim().replaceFirst("\\[\\d+\\]", "[" +
     * replacement + "]"); }
     */

    private String getOidFromExpression(String expression, int patternIndex, int expressionIndex)  throws OpenClinicaSystemException {
        String[] splitExpression = expression.split(ESCAPED_SEPARATOR);
        // int patternIndex = ?;
        if (!match(splitExpression[splitExpression.length - 1 - expressionIndex], pattern[patternIndex])) {
            if (!match(splitExpression[splitExpression.length - 1 - expressionIndex], ruleActionPattern[patternIndex])) {
                throw new OpenClinicaSystemException("OCRERR_0019", new String[] { expression });
            }
        }
        return splitExpression[splitExpression.length - 1 - expressionIndex];
    }

    public ItemBean getItemBeanFromExpression(String expression) {
        List<ItemBean> items = getItemDao().findByOid(getItemOidFromExpression(expression));
        return items.size() > 0 ? items.get(0) : null;
    }

    public StudyEventDefinitionBean getStudyEventDefinitionFromExpression(String expression) {
        if (expression.split(ESCAPED_SEPARATOR).length == 4) {
            return getStudyEventDefinitionFromExpression(expression, expressionWrapper.getStudyBean());
        } else if (expression.split(ESCAPED_SEPARATOR).length == 2 &&
                  (expression.endsWith(STARTDATE) || expression.endsWith(STATUS))) {
            return getStudyEventDefinitionFromExpressionForEvents(expression, expressionWrapper.getStudyBean());
        } else {
            return null;
        }
    }

    public StudyEventDefinitionBean getStudyEventDefinitionFromExpressionForEvents(String expression, StudyBean study) {
        String studyEventDefinitionKey = getStudyEventDefinitionOidFromExpressionForEvents(expression);
        logger.debug(
            "Expression : {} , Study Event Definition OID {} , Study Bean {} ",
            expression, studyEventDefinitionKey, study.getId()
        );
        if (studyEventDefinitions.get(studyEventDefinitionKey) != null) {
            return studyEventDefinitions.get(studyEventDefinitionKey);
        } else {
            // temp fix
            int studyId = study.getParentStudyId() != 0 ? study.getParentStudyId() : study.getId();
            StudyEventDefinitionBean studyEventDefinition = getStudyEventDefinitionDao().findByOidAndStudy(
                    studyEventDefinitionKey, studyId, studyId);
            // another way to get at the problem which I fix in the findByOidAndStudy method, tbh
            // TODO: because of DAO refactoring we may need to check also studyEventDefinition.isActive()
            if (studyEventDefinition != null) {
                studyEventDefinitions.put(studyEventDefinitionKey, studyEventDefinition);
                return studyEventDefinition;
            } else {
                return null;
            }
        }
    }

    public String getStudyEventDefinitionOidFromExpressionForEvents(String expression) {
        return getOidFromExpression(expression, 1, 1).replaceAll(BRACKETS_AND_CONTENTS, "");
    }

    public StudyEventDefinitionBean getStudyEventDefinitionFromExpression(String expression, StudyBean study) {
        String studyEventDefinitionKey = getStudyEventDefinitionOidFromExpression(expression);
        logger.debug(
            "Expression : {} , Study Event Definition OID {} , Study Bean {} ",
            expression, studyEventDefinitionKey, study.getId()
        );
        if (studyEventDefinitions.get(studyEventDefinitionKey) != null) {
            return studyEventDefinitions.get(studyEventDefinitionKey);
        } else {
            // temp fix
            int studyId = study.getParentStudyId() != 0 ? study.getParentStudyId() : study.getId();
            StudyEventDefinitionBean studyEventDefinition = getStudyEventDefinitionDao().findByOidAndStudy(
                    studyEventDefinitionKey, studyId, studyId);
            // another way to get at the problem which I fix in the findByOidAndStudy method, tbh
            if (studyEventDefinition != null && studyEventDefinition.isActive()) {
                studyEventDefinitions.put(studyEventDefinitionKey, studyEventDefinition);
                return studyEventDefinition;
            } else {
                return null;
            }
        }
    }

    public StudyEventDefinitionBean getStudyEventDefinitionFromExpressionForEventScheduling(String expression) {
        return expression.split(ESCAPED_SEPARATOR).length == 2 ? getStudyEventDefinitionFromExpressionForEventScheduling(expression, false) : null;
    }

    public StudyEventDefinitionBean getStudyEventDefinitionFromExpressionForEventScheduling(String expression, boolean onlyOID) {
        StudyBean study = expressionWrapper.getStudyBean();
        String studyEventDefinitionKey;
        if (onlyOID) {
            studyEventDefinitionKey = expression.replaceAll(BRACKETS_AND_CONTENTS, "");
        } else {
            studyEventDefinitionKey = getOidFromExpression(expression, 1, 1).replaceAll(BRACKETS_AND_CONTENTS, "");
        }

        logger.debug(
            "Expression : {} , Study Event Definition OID {} , Study Bean {} ",
            expression, studyEventDefinitionKey, study != null ? study.getId() : null
        );
        if (studyEventDefinitions.get(studyEventDefinitionKey) != null) {
            return studyEventDefinitions.get(studyEventDefinitionKey);
        } else {
            StudyEventDefinitionBean studyEventDefinition = getStudyEventDefinitionDao().findByOid(
                    studyEventDefinitionKey);
            // another way to get at the problem which I fix in the findByOidAndStudy method, tbh
            // TODO: because of DAO refactoring we may need to also check studyEventDefinition.isActive()
            if (studyEventDefinition != null && studyEventDefinitionKey.equals(studyEventDefinition.getOid())) {
                studyEventDefinitions.put(studyEventDefinitionKey, studyEventDefinition);
                return studyEventDefinition;
            } else {
                return null;
            }
        }
    }

    public ItemGroupBean getItemGroupExpression(String expression) {
        if (expression.split(ESCAPED_SEPARATOR).length < 2) {
            return null;
        }
        String itemGroupKey = getItemGroupOidFromExpression(expression);
        logger.debug("Expression : {} , ItemGroup OID : {} " + expression, itemGroupKey);
        if (itemGroups.get(itemGroupKey) != null) {
            return itemGroups.get(itemGroupKey);
        } else {
            ItemGroupBean itemGroup = getItemGroupDao().findByOid(itemGroupKey);
            if (itemGroup != null && itemGroup.isActive()) {
                itemGroups.put(itemGroupKey, itemGroup);
                return itemGroup;
            } else {
                return null;
            }
        }
    }

    public ItemGroupBean getItemGroupExpression(String expression, CRFBean crf) {
        logger.debug("Expression : " + expression);
        logger.debug("Expression : " + getItemGroupOidFromExpression(expression));
        return getItemGroupDao().findByOidAndCrf(getItemGroupOidFromExpression(expression), crf.getId());
    }

    /*
     * public ItemGroupBean getItemGroupFromExpression(String expression) {
     * logger.debug("Expression : " + expression); logger.debug("Expression : "
     * + getItemGroupOidFromExpression(expression)); return
     * getItemGroupDao().findByOid(getItemGroupOidFromExpression(expression)); }
     */

    public ItemBean getItemExpression(String expression, ItemGroupBean itemGroup) {
        String itemKey = getItemOidFromExpression(expression);
        logger.debug("Expression : {} , Item OID : {}", expression, itemKey);
        if (items.containsKey(itemKey)) {
            return items.get(itemKey);
        } else {
            ItemBean item = getItemDao().findItemByGroupIdandItemOid(itemGroup.getId(), itemKey);
            if (item != null && item.isActive()) {
                items.put(itemKey, item);
                return item;
            } else {
                return null;
            }
        }
    }

    public ItemBean getItemFromExpression(String expression) {
        String itemKey = getItemOidFromExpression(expression);
        logger.debug("Expression : {} , Item OID : {}", expression, itemKey);
        if (items.containsKey(itemKey)) {
            return items.get(itemKey);
        } else {
            List<ItemBean> persistentItems = getItemDao().findByOid(itemKey);
            ItemBean item = persistentItems.size() > 0 ? persistentItems.get(0) : null;
            if (item != null && item.isActive()) {
                items.put(itemKey, item);
                return item;
            } else {
                return null;
            }
        }
    }

    public CRFBean getCRFFromExpression(String expression) {
        if (expression.split(ESCAPED_SEPARATOR).length < 3) {
            return null;
        }
        CRFBean crf;
        logger.debug("Expression : " + expression);
        logger.debug("Expression : " + getCrfOidFromExpression(expression));
        CRFVersionBean crfVersion = getCrfVersionDao().findByOid(getCrfOidFromExpression(expression));
        if (crfVersion != null && crfVersion.isActive()) {
            int crfId = getCrfVersionDao().getCRFIdFromCRFVersionId(crfVersion.getId());
            crf = getCrfDao().findByPK(crfId);
        } else {
            crf = getCrfDao().findByOid(getCrfOidFromExpression(expression));
        }
        return crf;
    }

    public CRFVersionBean getCRFVersionFromExpression(String expression) {
        logger.debug("Expression : " + expression);
        return expression.split(ESCAPED_SEPARATOR).length < 3 ? null : getCrfVersionDao().findByOid(getCrfOidFromExpression(expression));
    }

    /**
     * Given a Complete Expression check business logic validity of each
     * component. Will throw OpenClinicaSystemException with correct
     * explanation. This might allow immediate communication of message to user.
     *
     * @param expression expression
     */
    @Deprecated
    public void isExpressionValidOLD(String expression) {
        StudyEventDefinitionBean studyEventDefinition = getStudyEventDefinitionFromExpression(expression);
        CRFBean crf = getCRFFromExpression(expression);
        if (studyEventDefinition == null || crf == null) {
            throw new OpenClinicaSystemException("OCRERR_0020");
        }

        EventDefinitionCRFBean eventDefinitionCrf = getEventDefinitionCRFDao().findByStudyEventDefinitionIdAndCRFId(
            this.expressionWrapper.getStudyBean(),
            studyEventDefinition.getId(),
            crf.getId()
        );
        if (eventDefinitionCrf == null ||
            eventDefinitionCrf.getId() == 0 ||
            eventDefinitionCrf.getStatus() != Status.AVAILABLE) {
            throw new OpenClinicaSystemException("OCRERR_0021");
        }

        ItemGroupBean itemGroup = getItemGroupExpression(expression, crf);
        if (itemGroup == null) {
            throw new OpenClinicaSystemException("OCRERR_0022");
        }

        ItemBean item = getItemExpression(expression, itemGroup);
        if (item == null) {
            throw new OpenClinicaSystemException("OCRERR_0023");
        }

        logger.debug("Study Event Definition ID : " + studyEventDefinition.getId());
        logger.debug("Crf ID : " + crf.getId());
        logger.debug("Event Definition CRF ID : " + eventDefinitionCrf.getId());
        logger.debug("Item ID : " + item.getId());
    }

    /**
     * Given a Complete Expression check business logic validity of each
     * component. Will throw OpenClinicaSystemException with correct
     * explanation. This might allow immediate communication of message to user.
     *
     * @param expression expression
     */
    public void isExpressionValid(String expression) {

        int length = expression.split(ESCAPED_SEPARATOR).length;
        ItemBean item = null;
        ItemGroupBean itemGroup = null;
        CRFBean crf = null;
        boolean isEventStartDateAndStatusParamExist = (expression.endsWith(STARTDATE) || expression.endsWith(STATUS));

        if (length > 0 && !isEventStartDateAndStatusParamExist) {
            item = getItemFromExpression(expression);
            if (item == null) {
                throw new OpenClinicaSystemException("OCRERR_0023");
            }
            // throw new OpenClinicaSystemException("item is Invalid");
        }

        if (length > 1 && !isEventStartDateAndStatusParamExist) {
            String itemGroupOid = getItemGroupOidFromExpression(expression);
            itemGroup = getItemGroupDao().findByOid(itemGroupOid);
            ArrayList<ItemGroupBean> igBean = getItemGroupDao().findGroupsByItemID(item.getId());

            if (itemGroup == null || itemGroup.getId() != igBean.get(0).getId()) {
                throw new OpenClinicaSystemException("OCRERR_0022");
            }
            // throw new OpenClinicaSystemException("itemGroup is Invalid");
        }

        if (length > 2 && !isEventStartDateAndStatusParamExist) {
            crf = getCRFFromExpression(expression);
            if (crf == null || crf.getId() != itemGroup.getCrfId()) {
                throw new OpenClinicaSystemException("OCRERR_0033");
            }
            // throw new OpenClinicaSystemException("CRF is Invalid");
        }

        if (length > 3 && !isEventStartDateAndStatusParamExist) {
            StudyEventDefinitionBean studyEventDefinition = getStudyEventDefinitionFromExpression(expression);
            crf = getCRFFromExpression(expression);
            if (studyEventDefinition == null || crf == null) {
                throw new OpenClinicaSystemException("OCRERR_0034", new String[]{expression});
            }
            // throw new OpenClinicaSystemException("StudyEventDefinition is Invalid");

            EventDefinitionCRFBean eventDefinitionCrf = getEventDefinitionCRFDao()
                .findByStudyEventDefinitionIdAndCRFId(
                    this.expressionWrapper.getStudyBean(), studyEventDefinition.getId(), crf.getId()
                );
            if (eventDefinitionCrf == null || eventDefinitionCrf.getId() == 0) {
                throw new OpenClinicaSystemException("OCRERR_0034", new String[]{expression});
            }
            // throw new OpenClinicaSystemException("StudyEventDefinition is Invalid");
        }

        if (length == 2 && isEventStartDateAndStatusParamExist) {
            StudyEventDefinitionBean studyEventDefinition = getStudyEventDefinitionFromExpressionForEventScheduling(expression);
            // System.out.println("StudyEventDefinition:  " + studyEventDefinition.getOid());
            if (studyEventDefinition == null) {
                throw new OpenClinicaSystemException("OCRERR_0034", new String[]{expression});
            }
        }

        if (length != 2 && isEventStartDateAndStatusParamExist) {
            throw new OpenClinicaSystemException("OCRERR_0034", new String[] { expression });
        }
    }

    public EventDefinitionCRFBean getEventDefinitionCRF(String expression) {
        if (expression.split(ESCAPED_SEPARATOR).length < 4) {
            return null;
        }
        StudyEventDefinitionBean studyEventDefinition = getStudyEventDefinitionFromExpression(expression);
        CRFBean crf = getCRFFromExpression(expression);

        if (studyEventDefinition == null || crf == null) {
            throw new OpenClinicaSystemException("OCRERR_0020");
        }

        return getEventDefinitionCRFDao().findByStudyEventDefinitionIdAndCRFId(
            this.expressionWrapper.getStudyBean(),
            studyEventDefinition.getId(),
            crf.getId()
        );
    }

    public String checkValidityOfItemOrItemGroupOidInCrf(String oid, RuleSetBean ruleSet) {
        oid = oid.trim();
        String[] theOid = oid.split(ESCAPED_SEPARATOR);
        if (theOid.length == 2) {
            ItemGroupBean itemGroup = getItemGroupDao().findByOid(theOid[0]);
            boolean isItemGroupBePartOfCrfOrNull = ruleSet.getCrfId() == null || itemGroup.getCrfId().equals(ruleSet.getCrfId());
            // TODO: because of DAO refactoring we may need to also check itemGroup.isActive()
            if (itemGroup != null && isItemGroupBePartOfCrfOrNull) {
                if (ruleSet.getCrfId() != null && itemGroup.getCrfId().equals(ruleSet.getCrfId())) {
                    return "OK";
                }
                if (ruleSet.getCrfId() != null && !itemGroup.getCrfId().equals(ruleSet.getCrfId())) {
                    return oid;
                }
                ItemBean item = getItemDao().findItemByGroupIdandItemOid(itemGroup.getId(), theOid[1]);
                // TODO: because of DAO refactoring we may need to also check item.isActive()
                if (item != null) {
                    return "OK";
                }
            }

        }
        if (theOid.length == 1) {
            ItemGroupBean itemGroup = getItemGroupDao().findByOid(oid);
            // TODO: because of DAO refactoring we may need to also check itemGroup.isActive()
            if (itemGroup != null) {
                if (ruleSet.getCrfId() != null && itemGroup.getCrfId().equals(ruleSet.getCrfId())) {
                    return "OK";
                }
                if (ruleSet.getCrfId() != null && !itemGroup.getCrfId().equals(ruleSet.getCrfId())) {
                    return oid;
                }
                return "OK";
            }

            // ItemBean item =
            // getItemDao().findItemByGroupIdandItemOid(getItemGroupExpression(ruleSet.getTarget().getValue()).getId(),
            // oid);
            ArrayList<ItemBean> items = getItemDao().findByOid(oid);
            if (items == null || items.size() != 0) {
                return "OK";
            }
        }

        return oid;
    }

    public boolean checkSyntax(String expression) {
        if (expression.startsWith(SEPARATOR) || expression.endsWith(SEPARATOR)) {
            return false;
        }
        String[] splitExpression = expression.split(ESCAPED_SEPARATOR);
        int patternIndex = 0;
        for (int i = splitExpression.length - 1; i >= 0; i--) {
            if (!match(splitExpression[i], pattern[patternIndex++])) {
                return false;
            }
        }
        return true;
    }

    public boolean checkInsertActionExpressionSyntax(String expression) {
        if (expression.startsWith(SEPARATOR) || expression.endsWith(SEPARATOR)) {
            return false;
        }
        String[] splitExpression = expression.split(ESCAPED_SEPARATOR);
        int patternIndex = 0;
        for (int i = splitExpression.length - 1; i >= 0; i--) {
            if (!match(splitExpression[i], ruleActionPattern[patternIndex++])) {
                return false;
            }
        }
        return true;
    }

    public boolean checkRuleExpressionSyntax(String expression) {
        if (expression.startsWith(SEPARATOR) || expression.endsWith(SEPARATOR)) {
            return false;
        }
        String[] splitExpression = expression.split(ESCAPED_SEPARATOR);
        int patternIndex = 0;
        for (int i = splitExpression.length - 1; i >= 0; i--) {
            if (!match(splitExpression[i], rulePattern[patternIndex++])) {
                return false;
            }
        }
        return true;
    }

    private boolean match(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }

    private ItemDAO getItemDao() {
        return new ItemDAO(ds);
    }

    private ItemDataDAO getItemDataDao() {
        return new ItemDataDAO(ds);
    }

    private CRFVersionDAO getCrfVersionDao() {
        return new CRFVersionDAO(ds);
    }

    private CRFDAO getCrfDao() {
        return new CRFDAO(ds);
    }

    private ItemGroupDAO getItemGroupDao() {
        return new ItemGroupDAO(ds);
    }

    private ItemGroupMetadataDAO getItemGroupMetadataDao() {
        return new ItemGroupMetadataDAO(ds);
    }

    private EventDefinitionCRFDAO getEventDefinitionCRFDao() {
        return new EventDefinitionCRFDAO(ds);
    }

    private StudyEventDefinitionDAO getStudyEventDefinitionDao() {
        return new StudyEventDefinitionDAO(ds);
    }

    private StudyEventDAO getStudyEventDao() {
        return new StudyEventDAO(ds);
    }

    private StudySubjectDAO getStudySubjectDao() {
        return new StudySubjectDAO(ds);
    }

    private EventCRFDAO getEventCRFDao() {
        return new EventCRFDAO(ds);
    }

    public void setExpressionWrapper(ExpressionObjectWrapper expressionWrapper) {
        this.expressionWrapper = expressionWrapper;
    }

    public ItemFormMetadataDAO getItemFormMetadataDao() {
        return new ItemFormMetadataDAO(ds);
    }

}
