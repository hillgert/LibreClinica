/*
 * LibreClinica is distributed under the
 * GNU Lesser General Public License (GNU LGPL).

 * For details see: https://libreclinica.org/license
 * LibreClinica, copyright (C) 2020
 */
package org.akaza.openclinica.service.crfdata;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.akaza.openclinica.bean.admin.CRFBean;
import org.akaza.openclinica.bean.core.DataEntryStage;
import org.akaza.openclinica.bean.core.Status;
import org.akaza.openclinica.bean.login.UserAccountBean;
import org.akaza.openclinica.bean.managestudy.EventDefinitionCRFBean;
import org.akaza.openclinica.bean.managestudy.StudyEventBean;
import org.akaza.openclinica.bean.submit.CRFVersionBean;
import org.akaza.openclinica.bean.submit.DisplayItemBean;
import org.akaza.openclinica.bean.submit.DisplayItemGroupBean;
import org.akaza.openclinica.bean.submit.DisplayItemWithGroupBean;
import org.akaza.openclinica.bean.submit.EventCRFBean;
import org.akaza.openclinica.bean.submit.ItemBean;
import org.akaza.openclinica.bean.submit.ItemDataBean;
import org.akaza.openclinica.bean.submit.ItemFormMetadataBean;
import org.akaza.openclinica.bean.submit.ItemGroupBean;
import org.akaza.openclinica.bean.submit.ItemGroupMetadataBean;
import org.akaza.openclinica.bean.submit.SectionBean;
import org.akaza.openclinica.dao.hibernate.DynamicsItemFormMetadataDao;
import org.akaza.openclinica.dao.hibernate.DynamicsItemGroupMetadataDao;
import org.akaza.openclinica.dao.login.UserAccountDAO;
import org.akaza.openclinica.dao.managestudy.EventDefinitionCRFDAO;
import org.akaza.openclinica.dao.managestudy.StudyEventDAO;
import org.akaza.openclinica.dao.submit.EventCRFDAO;
import org.akaza.openclinica.dao.submit.ItemDAO;
import org.akaza.openclinica.dao.submit.ItemDataDAO;
import org.akaza.openclinica.dao.submit.ItemFormMetadataDAO;
import org.akaza.openclinica.dao.submit.ItemGroupDAO;
import org.akaza.openclinica.dao.submit.ItemGroupMetadataDAO;
import org.akaza.openclinica.dao.submit.SectionDAO;
import org.akaza.openclinica.domain.crfdata.DynamicsItemFormMetadataBean;
import org.akaza.openclinica.domain.crfdata.DynamicsItemGroupMetadataBean;
import org.akaza.openclinica.domain.rule.RuleSetBean;
import org.akaza.openclinica.domain.rule.action.PropertyBean;
import org.akaza.openclinica.domain.rule.action.StratificationFactorBean;
import org.akaza.openclinica.exception.OpenClinicaException;
import org.akaza.openclinica.service.rule.expression.ExpressionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicsMetadataService implements MetadataServiceInterface {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final String ESCAPED_SEPARATOR = "\\.";
    private DynamicsItemFormMetadataDao dynamicsItemFormMetadataDao;
    private DynamicsItemGroupMetadataDao dynamicsItemGroupMetadataDao;
    private DataSource ds;
    private EventDefinitionCRFDAO eventDefinitionCRFDAO;
    private ExpressionService expressionService;
    private UserAccountDAO uadao;
    
    public DynamicsMetadataService(DataSource ds) {
        this.ds = ds;
        this.uadao = new UserAccountDAO(this.ds);
    }

    public boolean hide(Object metadataBean, EventCRFBean eventCrfBean) {
        // TODO -- interesting problem, where is the SpringServletAccess object going to live now? tbh 03/2010
        ItemFormMetadataBean itemFormMetadataBean = (ItemFormMetadataBean) metadataBean;
        itemFormMetadataBean.setShowItem(false);
        // DynamicsItemFormMetadataDao dynamicsMetadataDao = (DynamicsItemFormMetadataDao) metadataDao;
        // DynamicsItemFormMetadataDao metadataDao = (DynamicsItemFormMetadataDao) SpringServletAccess.getApplicationContext(context).getBean("dynamicsItemFormMetadataDao");
        DynamicsItemFormMetadataBean dynamicsMetadataBean = new DynamicsItemFormMetadataBean(itemFormMetadataBean, eventCrfBean);
        dynamicsMetadataBean.setShowItem(false);
        getDynamicsItemFormMetadataDao().saveOrUpdate(dynamicsMetadataBean);
        return true;
    }

    // TODO: deprecated? probably not because it is still in use
    public boolean isShown(Object metadataBean, EventCRFBean eventCrfBean) {
        ItemFormMetadataBean itemFormMetadataBean = (ItemFormMetadataBean) metadataBean;
        DynamicsItemFormMetadataBean dynamicsMetadataBean = getDynamicsItemFormMetadataBean(itemFormMetadataBean, eventCrfBean, null);
        if (dynamicsMetadataBean != null) {
            return dynamicsMetadataBean.isShowItem();
        } else {
            logger.debug("did not find a row in the db for " + itemFormMetadataBean.getId());
            return false;
        }
    }

    public boolean hasPassedDDE(ItemFormMetadataBean itemFormMetadataBean, EventCRFBean eventCrfBean, ItemDataBean itemDataBean) {
        DynamicsItemFormMetadataBean dynamicsMetadataBean =
            getDynamicsItemFormMetadataDao().findByMetadataBean(itemFormMetadataBean, eventCrfBean, itemDataBean);//findByItemDataBean(itemDataBean);
        if (dynamicsMetadataBean == null) {
            return false;
        }
        return dynamicsMetadataBean.getPassedDde() > 0;
    }

    // TODO: deprecated? is not used
    public boolean isShown(Integer itemId, EventCRFBean eventCrfBean) {
        // do we check against the database, or just against the object? prob against the db
        // ItemFormMetadataBean itemFormMetadataBean = (ItemFormMetadataBean) metadataBean;
        // return itemFormMetadataBean.isShowItem();
        ItemFormMetadataBean itemFormMetadataBean = getItemFormMetadataDAO().findByItemIdAndCRFVersionId(itemId, eventCrfBean.getCRFVersionId());
        DynamicsItemFormMetadataBean dynamicsMetadataBean = getDynamicsItemFormMetadataBean(itemFormMetadataBean, eventCrfBean, null);
        // DynamicsItemFormMetadataBean dynamicsMetadataBean = getDynamicsItemFormMetadataDao().findByMetadataBean(itemFormMetadataBean, eventCrfBean);
        if (dynamicsMetadataBean != null) {
            return dynamicsMetadataBean.isShowItem();
        } else {
            logger.debug("did not find a row in the db for " + itemFormMetadataBean.getId());
            return false;
        }
        // return false;
    }

    public boolean isShown(Integer itemId, EventCRFBean eventCrfBean, ItemDataBean itemDataBean) {
        // do we check against the database, or just against the object? against the db
        // ItemFormMetadataBean itemFormMetadataBean = (ItemFormMetadataBean) metadataBean;
        // return itemFormMetadataBean.isShowItem();
        ItemFormMetadataBean itemFormMetadataBean = getItemFormMetadataDAO().findByItemIdAndCRFVersionId(itemId, eventCrfBean.getCRFVersionId());
        DynamicsItemFormMetadataBean dynamicsMetadataBean = getDynamicsItemFormMetadataBean(itemFormMetadataBean, eventCrfBean, itemDataBean);
        // DynamicsItemFormMetadataBean dynamicsMetadataBean = getDynamicsItemFormMetadataDao().findByMetadataBean(itemFormMetadataBean, eventCrfBean);
        if (dynamicsMetadataBean != null) {
            // System.out.println("DID find a row in the db for (with IDB) " + itemFormMetadataBean.getId() + " idb id " + itemDataBean.getId());
            return dynamicsMetadataBean.isShowItem();
        } else {
            // System.out.println("did not find a row in the db for (with IDB) " + itemFormMetadataBean.getId() + " idb id " + itemDataBean.getId());
            return false;
        }
        // return false;
    }

    public boolean isGroupShown(int metadataId, EventCRFBean eventCrfBean) throws OpenClinicaException {
        ItemGroupMetadataBean itemGroupMetadataBean = getItemGroupMetadataDAO().findByPK(metadataId);
        DynamicsItemGroupMetadataBean dynamicsMetadataBean = getDynamicsItemGroupMetadataBean(itemGroupMetadataBean, eventCrfBean);
        if (dynamicsMetadataBean != null) {
            return dynamicsMetadataBean.isShowGroup();
        } else {
            // System.out.println("didnt find a group row in the db ");
            return false;
        }
    }

    public boolean isGroupShown(int metadataId, int eventCrfBeanId) throws OpenClinicaException {
        ItemGroupMetadataBean itemGroupMetadataBean = getItemGroupMetadataDAO().findByPK(metadataId);
        DynamicsItemGroupMetadataBean dynamicsMetadataBean = getDynamicsItemGroupMetadataBean(itemGroupMetadataBean, eventCrfBeanId);
        if (dynamicsMetadataBean != null) {
            return dynamicsMetadataBean.isShowGroup();
        } else {
            // System.out.println("didnt find a group row in the db ");
            return false;
        }
    }

    public boolean hasGroupPassedDDE(int metadataId, int eventCrfBeanId) throws OpenClinicaException {
        ItemGroupMetadataBean itemGroupMetadataBean = getItemGroupMetadataDAO().findByPK(metadataId);
        DynamicsItemGroupMetadataBean dynamicsMetadataBean = getDynamicsItemGroupMetadataBean(itemGroupMetadataBean, eventCrfBeanId);
        if (dynamicsMetadataBean == null) {
            return false;
        }
        return dynamicsMetadataBean.getPassedDde() > 0;
    }

    /**
     * TODO: remove the @deprecated call. The reason it is there now is to accommodate the call being made from the DataEntryServlet
     *
     * @param itemFormMetadataBean item form metadata
     * @param eventCrfBean event crf
     * @param itemDataBean item data
     * @return DynamicsItemFormMetadataBean
     */
    private DynamicsItemFormMetadataBean getDynamicsItemFormMetadataBean(ItemFormMetadataBean itemFormMetadataBean, EventCRFBean eventCrfBean, ItemDataBean itemDataBean) {
        return getDynamicsItemFormMetadataDao().findByMetadataBean(itemFormMetadataBean, eventCrfBean, itemDataBean);
    }

    private DynamicsItemGroupMetadataBean getDynamicsItemGroupMetadataBean(ItemGroupMetadataBean metadataBean, EventCRFBean eventCrfBean) {
        DynamicsItemGroupMetadataBean dynamicsMetadataBean = getDynamicsItemGroupMetadataDao().findByMetadataBean(metadataBean, eventCrfBean);
        logger.debug(" returning " + metadataBean.getId() + " " + metadataBean.getItemGroupId() + " " + eventCrfBean.getId());
        return dynamicsMetadataBean;
    }

    private DynamicsItemGroupMetadataBean getDynamicsItemGroupMetadataBean(ItemGroupMetadataBean metadataBean, int eventCrfBeanId) {
        return getDynamicsItemGroupMetadataDao().findByMetadataBean(metadataBean, eventCrfBeanId);
    }

    public boolean showItem(ItemFormMetadataBean metadataBean, EventCRFBean eventCrfBean, ItemDataBean itemDataBean) {
        ItemFormMetadataBean itemFormMetadataBean = metadataBean;
        itemFormMetadataBean.setShowItem(true);
        DynamicsItemFormMetadataBean dynamicsMetadataBean = new DynamicsItemFormMetadataBean(itemFormMetadataBean, eventCrfBean);
        dynamicsMetadataBean.setItemDataId(itemDataBean.getId());
        dynamicsMetadataBean.setShowItem(true);
        dynamicsMetadataBean.setPassedDde(0);
        getDynamicsItemFormMetadataDao().saveOrUpdate(dynamicsMetadataBean);
        logger.debug("just touched ifmb id " + metadataBean.getId() + " ecb id " + eventCrfBean.getId() + " item id " + metadataBean.getItemId()
            + " itemdata id " + itemDataBean.getId());
        return true;
    }

    public boolean hideNewItem(ItemFormMetadataBean metadataBean, EventCRFBean eventCrfBean, ItemDataBean itemDataBean) {
        ItemFormMetadataBean itemFormMetadataBean = metadataBean;
        DynamicsItemFormMetadataBean dynamicsMetadataBean = new DynamicsItemFormMetadataBean(itemFormMetadataBean, eventCrfBean);
        dynamicsMetadataBean.setItemDataId(itemDataBean.getId());
        dynamicsMetadataBean.setShowItem(false);
        getDynamicsItemFormMetadataDao().saveOrUpdate(dynamicsMetadataBean);
        return true;
    }

    public boolean hideItem(ItemFormMetadataBean metadataBean, EventCRFBean eventCrfBean, ItemDataBean itemDataBean) {
        ItemFormMetadataBean itemFormMetadataBean = metadataBean;
        DynamicsItemFormMetadataBean dynamicsMetadataBean = this.getDynamicsItemFormMetadataDao().findByItemDataBean(itemDataBean);
        dynamicsMetadataBean = dynamicsMetadataBean != null && dynamicsMetadataBean.getId()>0 ?
            dynamicsMetadataBean : new DynamicsItemFormMetadataBean(itemFormMetadataBean, eventCrfBean);
        dynamicsMetadataBean.setShowItem(false);
        getDynamicsItemFormMetadataDao().saveOrUpdate(dynamicsMetadataBean);
        return true;
    }

    public boolean showGroup(ItemGroupMetadataBean metadataBean, EventCRFBean eventCrfBean) {
        ItemGroupMetadataBean itemGroupMetadataBean = metadataBean;
        itemGroupMetadataBean.setShowGroup(true);
        DynamicsItemGroupMetadataBean dynamicsMetadataBean = new DynamicsItemGroupMetadataBean(itemGroupMetadataBean, eventCrfBean);
        dynamicsMetadataBean.setPassedDde(0);
        getDynamicsItemGroupMetadataDao().saveOrUpdate(dynamicsMetadataBean);
        return true;
    }

    public boolean hideGroup(ItemGroupMetadataBean metadataBean, EventCRFBean eventCrfBean) {
        ItemGroupMetadataBean itemGroupMetadataBean = metadataBean;
        itemGroupMetadataBean.setShowGroup(false);
        DynamicsItemGroupMetadataBean dynamicsMetadataBean = new DynamicsItemGroupMetadataBean(itemGroupMetadataBean, eventCrfBean);
        dynamicsMetadataBean.setPassedDde(0);
        getDynamicsItemGroupMetadataDao().saveOrUpdate(dynamicsMetadataBean);
        return true;
    }

    public void show(Integer itemDataId, List<PropertyBean> properties, RuleSetBean ruleSet) {
        ItemDataBean itemDataBeanA = (ItemDataBean) getItemDataDAO().findByPK(itemDataId);
        EventCRFBean eventCrfBeanA = (EventCRFBean) getEventCRFDAO().findByPK(itemDataBeanA.getEventCRFId());

        for (PropertyBean propertyBean : properties) {
            String oid = propertyBean.getOid();
            ItemOrItemGroupHolder itemOrItemGroup = getItemOrItemGroup(oid);
            // OID is an item
            if (itemOrItemGroup.getItemBean() != null) {
                ItemDataBean oidBasedItemData = getItemData(itemOrItemGroup.getItemBean(), eventCrfBeanA, itemDataBeanA.getOrdinal());
                ItemFormMetadataBean itemFormMetadataBean =
                    getItemFormMetadataDAO().findByItemIdAndCRFVersionId(itemOrItemGroup.getItemBean().getId(), eventCrfBeanA.getCRFVersionId());
                DynamicsItemFormMetadataBean dynamicsMetadataBean = getDynamicsItemFormMetadataBean(itemFormMetadataBean, eventCrfBeanA, oidBasedItemData);
                if (dynamicsMetadataBean == null) {
                    showItem(itemFormMetadataBean, eventCrfBeanA, oidBasedItemData);
                } else if (dynamicsMetadataBean != null && !dynamicsMetadataBean.isShowItem()) {
                    dynamicsMetadataBean.setShowItem(true);
                    getDynamicsItemFormMetadataDao().saveOrUpdate(dynamicsMetadataBean);
                }
            }
            // OID is a group
            else {
                logger.debug("found item group id 1 " + oid);
                ItemGroupBean itemGroupBean = itemOrItemGroup.getItemGroupBean();
                ArrayList<SectionBean> sectionBeans = getSectionDAO().findAllByCRFVersionId(eventCrfBeanA.getCRFVersionId());
                for (int i = 0; i < sectionBeans.size(); i++) {
                    SectionBean sectionBean = (SectionBean) sectionBeans.get(i);
                    // System.out.println("found section " + sectionBean.getId());
                    List<ItemGroupMetadataBean> itemGroupMetadataBeans =
                        getItemGroupMetadataDAO().findMetaByGroupAndSection(itemGroupBean.getId(), eventCrfBeanA.getCRFVersionId(), sectionBean.getId());
                    for (ItemGroupMetadataBean itemGroupMetadataBean : itemGroupMetadataBeans) {
                        if (itemGroupMetadataBean.getItemGroupId() == itemGroupBean.getId()) {
                            // System.out.println("found item group id 2 " + oid);
                            DynamicsItemGroupMetadataBean dynamicsGroupBean = getDynamicsItemGroupMetadataBean(itemGroupMetadataBean, eventCrfBeanA);
                            if (dynamicsGroupBean == null) {
                                showGroup(itemGroupMetadataBean, eventCrfBeanA);
                            } else if (dynamicsGroupBean != null && !dynamicsGroupBean.isShowGroup()) {
                                dynamicsGroupBean.setShowGroup(true);
                                getDynamicsItemGroupMetadataDao().saveOrUpdate(dynamicsGroupBean);
                            }
                        }
                    }
                }
            }
        }
    }

    public void hide(Integer itemDataId, List<PropertyBean> properties) {
        ItemDataBean itemDataBean = (ItemDataBean) getItemDataDAO().findByPK(itemDataId);
        EventCRFBean eventCrfBean = (EventCRFBean) getEventCRFDAO().findByPK(itemDataBean.getEventCRFId());
        for (PropertyBean propertyBean : properties) {
            String oid = propertyBean.getOid();
            ItemOrItemGroupHolder itemOrItemGroup = getItemOrItemGroup(oid);
            // OID is an item
            if (itemOrItemGroup.getItemBean() != null) {
                ItemDataBean oidBasedItemData = getItemData(itemOrItemGroup.getItemBean(), eventCrfBean, itemDataBean.getOrdinal());
                ItemFormMetadataBean itemFormMetadataBean =
                    getItemFormMetadataDAO().findByItemIdAndCRFVersionId(itemOrItemGroup.getItemBean().getId(), eventCrfBean.getCRFVersionId());
                DynamicsItemFormMetadataBean dynamicsMetadataBean = getDynamicsItemFormMetadataBean(itemFormMetadataBean, eventCrfBean, oidBasedItemData);
                if (dynamicsMetadataBean == null && oidBasedItemData.getValue().equals("")) {
                    showItem(itemFormMetadataBean, eventCrfBean, oidBasedItemData);
                } else if (dynamicsMetadataBean != null && dynamicsMetadataBean.isShowItem() && oidBasedItemData.getValue().equals("")) {
                    dynamicsMetadataBean.setShowItem(false);
                    getDynamicsItemFormMetadataDao().saveOrUpdate(dynamicsMetadataBean);
                }
            }
            // OID is a group
            else {
                // ItemGroupBean itemGroupBean = itemOrItemGroup.getItemGroupBean();
            }
        }
    }

    private Boolean isGroupRepeating(ItemGroupMetadataBean itemGroupMetadataBean) {
        return itemGroupMetadataBean.getRepeatNum() > 1 || itemGroupMetadataBean.getRepeatMax() > 1;
    }

    private ItemDataBean oneToIndexedMany(ItemDataBean itemDataBeanA, EventCRFBean eventCrfBeanA, ItemGroupMetadataBean itemGroupMetadataBeanA,
            ItemBean itemBeanB, ItemGroupBean itemGroupBeanB, ItemGroupMetadataBean itemGroupMetadataBeanB, EventCRFBean eventCrfBeanB, UserAccountBean ub,
            int index) {

        ItemDataBean theOidBasedItemData = null;
        int size = getItemDataDAO().getGroupSize(itemBeanB.getId(), eventCrfBeanB.getId());
        int maxOrdinal = getItemDataDAO().getMaxOrdinalForGroupByItemAndEventCrf(itemBeanB.getId(), eventCrfBeanB);
        if (size > 0 && size >= index) {
            List<ItemDataBean> theItemDataBeans = getItemDataDAO().findAllByEventCRFIdAndItemId(eventCrfBeanB.getId(), itemBeanB.getId());
            theOidBasedItemData = theItemDataBeans.get(index - 1);
        } else {
            List<ItemBean> items = getItemDAO().findAllItemsByGroupId(itemGroupBeanB.getId(), eventCrfBeanB.getCRFVersionId());
            int number =
                itemGroupMetadataBeanB.getRepeatNum() > index ? itemGroupMetadataBeanB.getRepeatNum() : index <= itemGroupMetadataBeanB.getRepeatMax() ? index
                    : 0;
            for (int ordinal = 1 + maxOrdinal; ordinal <= number + maxOrdinal - size; ordinal++) {
                for (ItemBean itemBeanX : items) {
                    ItemDataBean oidBasedItemData = getItemData(itemBeanX, eventCrfBeanB, ordinal);
                    if (oidBasedItemData.getId() == 0) {
                        oidBasedItemData = createItemData(oidBasedItemData, itemBeanX, ordinal, eventCrfBeanB, ub);
                    }
                }
            }
            List<ItemDataBean> theItemDataBeans = getItemDataDAO().findAllByEventCRFIdAndItemId(eventCrfBeanB.getId(), itemBeanB.getId());
            theOidBasedItemData = theItemDataBeans.get(index - 1);
        }
        return theOidBasedItemData;
    }

    private ItemDataBean oneToEndMany(ItemDataBean itemDataBeanA, EventCRFBean eventCrfBeanA, ItemGroupMetadataBean itemGroupMetadataBeanA, ItemBean itemBeanB,
            ItemGroupBean itemGroupBeanB, ItemGroupMetadataBean itemGroupMetadataBeanB, EventCRFBean eventCrfBeanB, UserAccountBean ub) {

        ItemDataBean theOidBasedItemData = null;
        int maxOrdinal = getItemDataDAO().getMaxOrdinalForGroupByItemAndEventCrf(itemBeanB.getId(), eventCrfBeanB);
        List<ItemBean> items = getItemDAO().findAllItemsByGroupId(itemGroupBeanB.getId(), eventCrfBeanB.getCRFVersionId());
        if (1 + maxOrdinal > itemGroupMetadataBeanB.getRepeatMax()) {
            logger.debug("Cannot add new repeat of this group because it has reached MaxRepeat.");
        } else {
            for (ItemBean itemBeanX : items) {
                ItemDataBean oidBasedItemData = getItemData(itemBeanX, eventCrfBeanB, 1 + maxOrdinal);
                if (oidBasedItemData.getId() == 0) {
                    oidBasedItemData = createItemData(oidBasedItemData, itemBeanX, 1 + maxOrdinal, eventCrfBeanB, ub);
                }
            }
        }
        List<ItemDataBean> theItemDataBeans = getItemDataDAO().findAllByEventCRFIdAndItemId(eventCrfBeanB.getId(), itemBeanB.getId());
        theOidBasedItemData = theItemDataBeans.get(theItemDataBeans.size() - 1);
        return theOidBasedItemData;
    }

    private List<ItemDataBean> oneToMany(ItemDataBean itemDataBeanA, EventCRFBean eventCrfBeanA, ItemGroupMetadataBean itemGroupMetadataBeanA,
            ItemBean itemBeanB, ItemGroupBean itemGroupBeanB, ItemGroupMetadataBean itemGroupMetadataBeanB, EventCRFBean eventCrfBeanB, UserAccountBean ub) {

        List<ItemDataBean> itemDataBeans = new ArrayList<>();
        Integer size = getItemDataDAO().getGroupSize(itemBeanB.getId(), eventCrfBeanB.getId());
        int maxOrdinal = getItemDataDAO().getMaxOrdinalForGroupByItemAndEventCrf(itemBeanB.getId(), eventCrfBeanB);
        if (size > 0 || maxOrdinal > 0) {
            itemDataBeans.addAll(getItemDataDAO().findAllByEventCRFIdAndItemId(eventCrfBeanB.getId(), itemBeanB.getId()));
        } else {
            List<ItemBean> items = getItemDAO().findAllItemsByGroupId(itemGroupBeanB.getId(), eventCrfBeanB.getCRFVersionId());
            for (int ordinal = 1 + maxOrdinal; ordinal <= itemGroupMetadataBeanB.getRepeatNum() + maxOrdinal; ordinal++) {
                for (ItemBean itemBeanX : items) {
                    ItemDataBean oidBasedItemData = getItemData(itemBeanX, eventCrfBeanB, ordinal);
                    if (oidBasedItemData.getId() == 0) {
                        oidBasedItemData = createItemData(oidBasedItemData, itemBeanX, ordinal, eventCrfBeanB, ub);
                    }
                    if (itemBeanX.getId() == itemBeanB.getId()) {
                        itemDataBeans.add(oidBasedItemData);
                    }
                }
            }
        }
        return itemDataBeans;
    }

    private ItemDataBean oneToOne(ItemDataBean itemDataBeanA, EventCRFBean eventCrfBeanA, ItemGroupMetadataBean itemGroupMetadataBeanA, ItemBean itemBeanB,
            ItemGroupMetadataBean itemGroupMetadataBeanB, EventCRFBean eventCrfBeanB, UserAccountBean ub, Integer ordinal) {
        ordinal = ordinal == null ? 1 : ordinal;
        ItemDataBean oidBasedItemData = getItemData(itemBeanB, eventCrfBeanB, ordinal);

        int ownerId = oidBasedItemData.getOwnerId();
        UserAccountBean updater;
        if (oidBasedItemData.getId() == 0) {
            oidBasedItemData = createItemData(oidBasedItemData, itemBeanB, ordinal, eventCrfBeanB, ub);
            updater = uadao.findByPK(ownerId);
            oidBasedItemData.setUpdater(updater);
        } else {
            if (oidBasedItemData.getUpdaterId() == 0) {
                updater = uadao.findByPK(ownerId);
                oidBasedItemData.setUpdater(updater);
            }
        }
            
        return oidBasedItemData;
    }

    private ItemDataBean createItemData(ItemDataBean oidBasedItemData, ItemBean itemBeanB, int ordinal, EventCRFBean eventCrfBeanA, UserAccountBean ub) {
        oidBasedItemData.setItemId(itemBeanB.getId());
        oidBasedItemData.setEventCRFId(eventCrfBeanA.getId());
        oidBasedItemData.setStatus(Status.AVAILABLE);
        oidBasedItemData.setOwner(ub);
        oidBasedItemData.setOrdinal(ordinal);
        oidBasedItemData = getItemDataDAO().create(oidBasedItemData);
        return oidBasedItemData;
    }

    private String getValue(PropertyBean property, RuleSetBean ruleSet, EventCRFBean eventCrfBean,List<StratificationFactorBean> stratificationFactorBeans) {
        String value = null;
        if (property.getValue() != null && property.getValue().length() > 0) {
            value = property.getValue();
            logger.info("Value from property value is : {}", value);
        }
        if (property.getValueExpression() == null) {
            logger.info("There is no ValueExpression for property = " + property.getOid());
        } else {
            String expression = getExpressionService()
                .constructFullExpressionIfPartialProvided(
                    property.getValueExpression().getValue(),
                    ruleSet.getTarget().getValue()
                );
            if(expression != null && !expression.isEmpty()) {
                ItemBean itemBean = getExpressionService().getItemBeanFromExpression(expression);
                String itemGroupBOrdinal = getExpressionService().getGroupOrdninalCurated(expression);
                ItemDataBean itemData =
                    getItemDataDAO().findByItemIdAndEventCRFIdAndOrdinal(itemBean.getId(), eventCrfBean.getId(),
                            itemGroupBOrdinal.equals("") ? 1 : Integer.parseInt(itemGroupBOrdinal));
                if (itemData.getId() == 0) {
                    logger.info("Cannot get Value for ExpressionValue {}", expression);
                } else {
                    value = itemData.getValue();
                    logger.info("Value from ExpressionValue '{}'  is : {}", expression, value);
                }
            }
        }
        return value;
    }

    private String getDateFormat(PropertyBean property) {
        String format = "yyyy-MM-dd";

        if (property.getValueExpression() != null) {
            logger.info("The Value is ValueExpression in the property so the date format will be : {}", "dd-MMM-yyyy");
            format = "dd-MMM-yyyy";
        }

        logger.info("The format of the date will be : {}", format);
        return format;
    }

    public void insert(ItemDataBean itemDataBean, List<PropertyBean> properties, UserAccountBean ub, RuleSetBean ruleSet,List<StratificationFactorBean> stratificationFactorBeans) {
        insert(itemDataBean.getId(), properties, ub, ruleSet, itemDataBean.getStatus(), stratificationFactorBeans);
    }

    public void insert(Integer itemDataId, List<PropertyBean> properties, UserAccountBean ub, RuleSetBean ruleSet,List<StratificationFactorBean> stratificationFactorBeans) {
        insert(itemDataId, properties, ub, ruleSet, null, stratificationFactorBeans);
    }

    private void insert(Integer itemDataId, List<PropertyBean> properties, UserAccountBean ub, RuleSetBean ruleSet, Status itemDataStatus , List<StratificationFactorBean> stratificationFactorBeans) {
        ItemDataBean itemDataBeanA = (ItemDataBean) getItemDataDAO().findByPK(itemDataId);
        EventCRFBean eventCrfBeanA = getEventCRFDAO().findByPK(itemDataBeanA.getEventCRFId());
        StudyEventBean studyEventBeanA = getStudyEventDAO().findByPK(eventCrfBeanA.getStudyEventId());
        ItemGroupMetadataBean itemGroupMetadataBeanA = getItemGroupMetadataDAO().findByItemAndCrfVersion(itemDataBeanA.getItemId(), eventCrfBeanA.getCRFVersionId());
        Boolean isGroupARepeating = isGroupRepeating(itemGroupMetadataBeanA);
        String itemGroupAOrdinal = getExpressionService().getGroupOrdninalCurated(ruleSet.getTarget().getValue());

        for (PropertyBean propertyBean : properties) {
            String expression = getExpressionService().constructFullExpressionIfPartialProvided(propertyBean.getOid(), ruleSet.getTarget().getValue());
            ItemBean itemBeanB = getExpressionService().getItemBeanFromExpression(expression);
            ItemGroupBean itemGroupBeanB = getExpressionService().getItemGroupExpression(expression);
            ItemGroupMetadataBean itemGroupMetadataBeanB = null;
            Boolean isGroupBRepeating = null;
            String itemGroupBOrdinal = null;
            EventCRFBean eventCrfBeanB = null;
            boolean isItemInSameForm = getItemFormMetadataDAO().findByItemIdAndCRFVersionId(itemBeanB.getId(), eventCrfBeanA.getCRFVersionId()).getId() != 0;
            // Item Does not below to same form
            if (!isItemInSameForm) {
                List<EventCRFBean> eventCrfs =
                    getEventCRFDAO().findAllByStudyEventAndCrfOrCrfVersionOid(studyEventBeanA, getExpressionService().getCrfOid(expression));
                if (eventCrfs.size() == 0) {
                    CRFVersionBean crfVersion = getExpressionService().getCRFVersionFromExpression(expression);
                    CRFBean crf = getExpressionService().getCRFFromExpression(expression);
                    int crfVersionId = 0;
                    EventDefinitionCRFBean eventDefinitionCRFBean =
                        getEventDefinitionCRfDAO().findByStudyEventDefinitionIdAndCRFId(studyEventBeanA.getStudyEventDefinitionId(), crf.getId());
                    if (eventDefinitionCRFBean.getId() != 0) {
                        crfVersionId = crfVersion != null ? crfVersion.getId() : eventDefinitionCRFBean.getDefaultVersionId();

                    }
                    // Create new event crf
                    eventCrfBeanB = eventCrfBeanA.copy();
                    eventCrfBeanB.setStatus(Status.AVAILABLE);
                    eventCrfBeanB.setId(0);
                    eventCrfBeanB.setCRFVersionId(crfVersionId);
                    eventCrfBeanB = getEventCRFDAO().create(eventCrfBeanB);

                } else {
                    eventCrfBeanB = eventCrfs.get(0);
                }
            } else {
                eventCrfBeanB = eventCrfBeanA;
            }

            itemGroupMetadataBeanB = getItemGroupMetadataDAO().findByItemAndCrfVersion(itemBeanB.getId(), eventCrfBeanB.getCRFVersionId());
            isGroupBRepeating = isGroupRepeating(itemGroupMetadataBeanB);
            itemGroupBOrdinal = getExpressionService().getGroupOrdninalCurated(expression);

            // If A and B are both non repeating groups
            if (!isGroupARepeating && !isGroupBRepeating) {
                ItemDataBean oidBasedItemData =
                    oneToOne(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupMetadataBeanB, eventCrfBeanB, ub, 1);

                oidBasedItemData.setValue(getValue(propertyBean, ruleSet, eventCrfBeanA,stratificationFactorBeans));

                if(itemDataStatus != null) oidBasedItemData.setStatus(itemDataStatus);
                getItemDataDAO().updateValue(oidBasedItemData, getDateFormat(propertyBean));
            }
            // If A is not repeating group & B is a repeating group with no index selected
            if (!isGroupARepeating && isGroupBRepeating && itemGroupBOrdinal.equals("")) {
                List<ItemDataBean> oidBasedItemDatas =
                    oneToMany(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupBeanB, itemGroupMetadataBeanB, eventCrfBeanB, ub);
                for (ItemDataBean oidBasedItemData : oidBasedItemDatas) {
                    oidBasedItemData.setValue(getValue(propertyBean, ruleSet, eventCrfBeanA,stratificationFactorBeans));
                    if(itemDataStatus != null) oidBasedItemData.setStatus(itemDataStatus);
                    getItemDataDAO().updateValue(oidBasedItemData, getDateFormat(propertyBean));
                }
            }
            // If A is not repeating group & B is a repeating group with index selected
            if (!isGroupARepeating && isGroupBRepeating && !itemGroupBOrdinal.equals("")) {
                ItemDataBean oidBasedItemData =
                    oneToIndexedMany(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupBeanB, itemGroupMetadataBeanB, eventCrfBeanB,
                            ub, Integer.parseInt(itemGroupBOrdinal));
                oidBasedItemData.setValue(getValue(propertyBean, ruleSet, eventCrfBeanA,stratificationFactorBeans));
                if(itemDataStatus != null) oidBasedItemData.setStatus(itemDataStatus);
                getItemDataDAO().updateValue(oidBasedItemData, getDateFormat(propertyBean));
            }
            // If A is repeating/ non repeating group & B is a repeating group with index selected as END
            if (isGroupBRepeating && itemGroupBOrdinal.equals("END")) {
                ItemDataBean oidBasedItemData =
                    oneToEndMany(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupBeanB, itemGroupMetadataBeanB, eventCrfBeanB, ub);
                oidBasedItemData.setValue(getValue(propertyBean, ruleSet, eventCrfBeanA,stratificationFactorBeans));
                if(itemDataStatus != null) oidBasedItemData.setStatus(itemDataStatus);
                getItemDataDAO().updateValue(oidBasedItemData, getDateFormat(propertyBean));
            }
            // If A is repeating group with index & B is a repeating group with index selected
            if (isGroupARepeating && isGroupBRepeating && !itemGroupBOrdinal.equals("") && !itemGroupBOrdinal.equals("END")) {
                ItemDataBean oidBasedItemData =
                    oneToIndexedMany(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupBeanB, itemGroupMetadataBeanB, eventCrfBeanB,
                            ub, Integer.parseInt(itemGroupBOrdinal));
                oidBasedItemData.setValue(getValue(propertyBean, ruleSet, eventCrfBeanA,stratificationFactorBeans));
                if(itemDataStatus != null) oidBasedItemData.setStatus(itemDataStatus);
                getItemDataDAO().updateValue(oidBasedItemData, getDateFormat(propertyBean));
            }
            // If A is repeating group with index & B is a repeating group with no index selected
            if (isGroupARepeating && isGroupBRepeating && itemGroupBOrdinal.equals("")) {
                ItemDataBean oidBasedItemData =
                    oneToIndexedMany(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupBeanB, itemGroupMetadataBeanB, eventCrfBeanB,
                            ub, Integer.parseInt(itemGroupAOrdinal));
                oidBasedItemData.setValue(getValue(propertyBean, ruleSet, eventCrfBeanA,stratificationFactorBeans));
                if(itemDataStatus != null) oidBasedItemData.setStatus(itemDataStatus);
                getItemDataDAO().updateValue(oidBasedItemData, getDateFormat(propertyBean));
            }
//            // If A is repeating group with index & B is none-repeating group
//            if (isGroupARepeating && !isGroupBRepeating ) {
//                ItemDataBean oidBasedItemData =
//                        oneToOne(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupMetadataBeanB, eventCrfBeanB, ub, 1);
//
//                oidBasedItemData.setValue(getValue(propertyBean, ruleSet, eventCrfBeanA));
//                getItemDataDAO().updateValue(oidBasedItemData, "yyyy-MM-dd");
//            }

        }
    }

    private ItemDataBean getItemData(ItemBean itemBean, EventCRFBean eventCrfBean, Integer ordinal) {
        return getItemDataDAO().findByItemIdAndEventCRFIdAndOrdinal(itemBean.getId(), eventCrfBean.getId(), ordinal);
    }

    public void hideNew(Integer itemDataId, List<PropertyBean> properties, UserAccountBean ub, RuleSetBean ruleSet) {
        ItemDataBean itemDataBeanA = (ItemDataBean) getItemDataDAO().findByPK(itemDataId);
        EventCRFBean eventCrfBeanA = getEventCRFDAO().findByPK(itemDataBeanA.getEventCRFId());
        ItemGroupMetadataBean itemGroupMetadataBeanA = getItemGroupMetadataDAO().findByItemAndCrfVersion(itemDataBeanA.getItemId(), eventCrfBeanA.getCRFVersionId());
        Boolean isGroupARepeating = isGroupRepeating(itemGroupMetadataBeanA);
        String itemGroupAOrdinal = getExpressionService().getGroupOrdninalCurated(ruleSet.getTarget().getValue());

        for (PropertyBean propertyBean : properties) {
            String oid = propertyBean.getOid();
            ItemOrItemGroupHolder itemOrItemGroup = getItemOrItemGroup(oid);
            // OID is an item
            if (itemOrItemGroup.getItemBean() != null) {
                String expression = getExpressionService().constructFullExpressionIfPartialProvided(propertyBean.getOid(), ruleSet.getTarget().getValue());
                ItemBean itemBeanB = getExpressionService().getItemBeanFromExpression(expression);
                ItemGroupBean itemGroupBeanB = getExpressionService().getItemGroupExpression(expression);
                EventCRFBean eventCrfBeanB = eventCrfBeanA;
                ItemGroupMetadataBean itemGroupMetadataBeanB = getItemGroupMetadataDAO().findByItemAndCrfVersion(itemBeanB.getId(), eventCrfBeanB.getCRFVersionId());
                Boolean isGroupBRepeating = isGroupRepeating(itemGroupMetadataBeanB);
                String itemGroupBOrdinal = getExpressionService().getGroupOrdninalCurated(expression);

                List<ItemDataBean> itemDataBeans = new ArrayList<>();
                // If A and B are both non repeating groups
                if (!isGroupARepeating && !isGroupBRepeating) {
                    ItemDataBean oidBasedItemData =
                        oneToOne(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupMetadataBeanB, eventCrfBeanB, ub, 1);
                    itemDataBeans.add(oidBasedItemData);

                }
                // If A is not repeating group & B is a repeating group with no index selected
                if (!isGroupARepeating && isGroupBRepeating && itemGroupBOrdinal.equals("")) {
                    List<ItemDataBean> oidBasedItemDatas =
                        oneToMany(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupBeanB, itemGroupMetadataBeanB, eventCrfBeanB, ub);
                    itemDataBeans.addAll(oidBasedItemDatas);
                }
                // If A is not repeating group & B is a repeating group with index selected
                if (!isGroupARepeating && isGroupBRepeating && !itemGroupBOrdinal.equals("")) {
                    ItemDataBean oidBasedItemData =
                        oneToIndexedMany(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupBeanB, itemGroupMetadataBeanB,
                                eventCrfBeanB, ub, Integer.parseInt(itemGroupBOrdinal));
                    itemDataBeans.add(oidBasedItemData);
                }
                // If A is repeating group with index & B is a repeating group with index selected
                if (isGroupARepeating && isGroupBRepeating && !itemGroupBOrdinal.equals("")) {
                    ItemDataBean oidBasedItemData =
                        oneToIndexedMany(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupBeanB, itemGroupMetadataBeanB,
                                eventCrfBeanB, ub, Integer.parseInt(itemGroupBOrdinal));
                    itemDataBeans.add(oidBasedItemData);
                }
                // If A is repeating group with index & B is a repeating group with no index selected
                if (isGroupARepeating && isGroupBRepeating && itemGroupBOrdinal.equals("")) {
                    ItemDataBean oidBasedItemData =
                        oneToIndexedMany(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupBeanB, itemGroupMetadataBeanB,
                                eventCrfBeanB, ub, Integer.parseInt(itemGroupAOrdinal));
                    itemDataBeans.add(oidBasedItemData);
                }

                for (ItemDataBean oidBasedItemData : itemDataBeans) {
                    ItemFormMetadataBean itemFormMetadataBean =
                        getItemFormMetadataDAO().findByItemIdAndCRFVersionId(itemOrItemGroup.getItemBean().getId(), eventCrfBeanA.getCRFVersionId());
                    DynamicsItemFormMetadataBean dynamicsMetadataBean = getDynamicsItemFormMetadataBean(itemFormMetadataBean, eventCrfBeanA, oidBasedItemData);
                    if (dynamicsMetadataBean == null && oidBasedItemData.getValue().equals("")) {
                        hideNewItem(itemFormMetadataBean, eventCrfBeanA, oidBasedItemData);
                    } else if (dynamicsMetadataBean != null && dynamicsMetadataBean.isShowItem() && oidBasedItemData.getValue().equals("")) {
                        // tbh #5287: add an additional check here to see if it should be hidden
                        dynamicsMetadataBean.setShowItem(false);
                        getDynamicsItemFormMetadataDao().saveOrUpdate(dynamicsMetadataBean);
                    }
                }
            }
            // OID is a group
            else {
                // ItemGroupBean itemGroupBean = itemOrItemGroup.getItemGroupBean();
                // below taken from showNew and reversed, tbh 07/2010
                logger.debug("found item group id 1 " + oid);
                ItemGroupBean itemGroupBean = itemOrItemGroup.getItemGroupBean();
                ArrayList<SectionBean> sectionBeans = getSectionDAO().findAllByCRFVersionId(eventCrfBeanA.getCRFVersionId());
                for (SectionBean sectionBean : sectionBeans) {
                    // System.out.println("found section " + sectionBean.getId());
                    List<ItemGroupMetadataBean> itemGroupMetadataBeans =
                            getItemGroupMetadataDAO().findMetaByGroupAndSection(itemGroupBean.getId(), eventCrfBeanA.getCRFVersionId(), sectionBean.getId());
                    for (ItemGroupMetadataBean itemGroupMetadataBean : itemGroupMetadataBeans) {
                        if (itemGroupMetadataBean.getItemGroupId() == itemGroupBean.getId()) {
                            // System.out.println("found item group id 2 " + oid);
                            DynamicsItemGroupMetadataBean dynamicsGroupBean = getDynamicsItemGroupMetadataBean(itemGroupMetadataBean, eventCrfBeanA);
                            if (dynamicsGroupBean == null) {
                                hideGroup(itemGroupMetadataBean, eventCrfBeanA);
                            } else if (!dynamicsGroupBean.isShowGroup()) {
                                dynamicsGroupBean.setShowGroup(false);
                                getDynamicsItemGroupMetadataDao().saveOrUpdate(dynamicsGroupBean);
                                // TODO is below required in hide?
                            } else if (eventCrfBeanA.getStage().equals(DataEntryStage.DOUBLE_DATA_ENTRY)) {
                                dynamicsGroupBean.setPassedDde(1);//setVersion(1); // version 1 = passed DDE
                                getDynamicsItemGroupMetadataDao().saveOrUpdate(dynamicsGroupBean);
                            }
                        }
                    }
                }
            }
        }
        // tbh #5287: reset the check to make sure items that have been shown are not re-hidden
        // resetItemCounter();
    }

    public void showNew(Integer itemDataId, List<PropertyBean> properties, UserAccountBean ub, RuleSetBean ruleSet) {
        ItemDataBean itemDataBeanA = (ItemDataBean) getItemDataDAO().findByPK(itemDataId);
        EventCRFBean eventCrfBeanA = getEventCRFDAO().findByPK(itemDataBeanA.getEventCRFId());
        ItemGroupMetadataBean itemGroupMetadataBeanA = getItemGroupMetadataDAO().findByItemAndCrfVersion(itemDataBeanA.getItemId(), eventCrfBeanA.getCRFVersionId());
        Boolean isGroupARepeating = isGroupRepeating(itemGroupMetadataBeanA);
        String itemGroupAOrdinal = getExpressionService().getGroupOrdninalCurated(ruleSet.getTarget().getValue());

        for (PropertyBean propertyBean : properties) {
            String oid = propertyBean.getOid();
            ItemOrItemGroupHolder itemOrItemGroup = getItemOrItemGroup(oid);
            // OID is an item
            if (itemOrItemGroup.getItemBean() != null) {
                String expression = getExpressionService().constructFullExpressionIfPartialProvided(propertyBean.getOid(), ruleSet.getTarget().getValue());
                ItemBean itemBeanB = getExpressionService().getItemBeanFromExpression(expression);
                ItemGroupBean itemGroupBeanB = getExpressionService().getItemGroupExpression(expression);
                EventCRFBean eventCrfBeanB = eventCrfBeanA;
                ItemGroupMetadataBean itemGroupMetadataBeanB = getItemGroupMetadataDAO().findByItemAndCrfVersion(itemBeanB.getId(), eventCrfBeanB.getCRFVersionId());
                Boolean isGroupBRepeating = isGroupRepeating(itemGroupMetadataBeanB);
                String itemGroupBOrdinal = getExpressionService().getGroupOrdninalCurated(expression);

                List<ItemDataBean> itemDataBeans = new ArrayList<>();
                // If A and B are both non repeating groups
                if (!isGroupARepeating && !isGroupBRepeating) {
                    ItemDataBean oidBasedItemData =
                        oneToOne(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupMetadataBeanB, eventCrfBeanB, ub, 1);
                    itemDataBeans.add(oidBasedItemData);

                }
                // If A is not repeating group & B is a repeating group with no index selected
                if (!isGroupARepeating && isGroupBRepeating && itemGroupBOrdinal.equals("")) {
                    List<ItemDataBean> oidBasedItemDatas =
                        oneToMany(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupBeanB, itemGroupMetadataBeanB, eventCrfBeanB, ub);
                    itemDataBeans.addAll(oidBasedItemDatas);
                }
                // If A is not repeating group & B is a repeating group with index selected
                if (!isGroupARepeating && isGroupBRepeating && !itemGroupBOrdinal.equals("")) {
                    ItemDataBean oidBasedItemData =
                        oneToIndexedMany(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupBeanB, itemGroupMetadataBeanB,
                                eventCrfBeanB, ub, Integer.parseInt(itemGroupBOrdinal));
                    itemDataBeans.add(oidBasedItemData);
                }
                // If A is repeating group with index & B is a repeating group with index selected
                if (isGroupARepeating && isGroupBRepeating && !itemGroupBOrdinal.equals("")) {
                    ItemDataBean oidBasedItemData =
                        oneToIndexedMany(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupBeanB, itemGroupMetadataBeanB,
                                eventCrfBeanB, ub, Integer.parseInt(itemGroupBOrdinal));
                    itemDataBeans.add(oidBasedItemData);
                }
                // If A is repeating group with index & B is a repeating group with no index selected
                if (isGroupARepeating && isGroupBRepeating && itemGroupBOrdinal.equals("")) {
                    ItemDataBean oidBasedItemData =
                        oneToIndexedMany(itemDataBeanA, eventCrfBeanA, itemGroupMetadataBeanA, itemBeanB, itemGroupBeanB, itemGroupMetadataBeanB,
                                eventCrfBeanB, ub, Integer.parseInt(itemGroupAOrdinal));
                    itemDataBeans.add(oidBasedItemData);
                }
                logger.debug("** found item data beans: " + itemDataBeans.toString());
                for (ItemDataBean oidBasedItemData : itemDataBeans) {
                    ItemFormMetadataBean itemFormMetadataBean =
                        getItemFormMetadataDAO().findByItemIdAndCRFVersionId(itemBeanB.getId(), eventCrfBeanB.getCRFVersionId());
                    DynamicsItemFormMetadataBean dynamicsMetadataBean = getDynamicsItemFormMetadataBean(itemFormMetadataBean, eventCrfBeanA, oidBasedItemData);
                    if (dynamicsMetadataBean == null) {
                        showItem(itemFormMetadataBean, eventCrfBeanA, oidBasedItemData);
                        // itemsAlreadyShown.add(new Integer(oidBasedItemData.getId()));
                    } else if (!dynamicsMetadataBean.isShowItem()) {
                        dynamicsMetadataBean.setShowItem(true);
                        getDynamicsItemFormMetadataDao().saveOrUpdate(dynamicsMetadataBean);
                        // itemsAlreadyShown.add(new Integer(oidBasedItemData.getId()));
                    } else if (eventCrfBeanA.getStage().equals(DataEntryStage.DOUBLE_DATA_ENTRY)) {
                        logger.debug("hit DDE here: idb " + oidBasedItemData.getId());
                        // need a guard clause to guarantee DDE
                        // if we get there, it means that we've hit DDE and the bean exists
                        dynamicsMetadataBean.setPassedDde(1);//setVersion(1);// version 1 = passed DDE
                        getDynamicsItemFormMetadataDao().saveOrUpdate(dynamicsMetadataBean);
                    }
                }

            }
            // OID is a group
            else {
                logger.debug("found item group id 1 " + oid);
                ItemGroupBean itemGroupBean = itemOrItemGroup.getItemGroupBean();
                ArrayList<SectionBean> sectionBeans = getSectionDAO().findAllByCRFVersionId(eventCrfBeanA.getCRFVersionId());
                for (SectionBean sectionBean : sectionBeans) {
                    // System.out.println("found section " + sectionBean.getId());
                    List<ItemGroupMetadataBean> itemGroupMetadataBeans =
                            getItemGroupMetadataDAO().findMetaByGroupAndSection(itemGroupBean.getId(), eventCrfBeanA.getCRFVersionId(), sectionBean.getId());
                    for (ItemGroupMetadataBean itemGroupMetadataBean : itemGroupMetadataBeans) {
                        if (itemGroupMetadataBean.getItemGroupId() == itemGroupBean.getId()) {
                            // System.out.println("found item group id 2 " + oid);
                            DynamicsItemGroupMetadataBean dynamicsGroupBean = getDynamicsItemGroupMetadataBean(itemGroupMetadataBean, eventCrfBeanA);
                            if (dynamicsGroupBean == null) {
                                showGroup(itemGroupMetadataBean, eventCrfBeanA);
                            } else if (!dynamicsGroupBean.isShowGroup()) {
                                dynamicsGroupBean.setShowGroup(true);
                                getDynamicsItemGroupMetadataDao().saveOrUpdate(dynamicsGroupBean);
                            } else if (eventCrfBeanA.getStage().equals(DataEntryStage.DOUBLE_DATA_ENTRY)) {
                                dynamicsGroupBean.setPassedDde(1);//setVersion(1); // version 1 = passed DDE
                                getDynamicsItemGroupMetadataDao().saveOrUpdate(dynamicsGroupBean);
                            }
                        }
                    }
                }
            }
        }
    }

    private ItemOrItemGroupHolder getItemOrItemGroup(String oid) {

        String[] theOid = oid.split(ESCAPED_SEPARATOR);
        if (theOid.length == 2) {
            ItemGroupBean itemGroup = getItemGroupDAO().findByOid(theOid[0].trim());
            if (itemGroup != null) {
                ItemBean item = getItemDAO().findItemByGroupIdandItemOid(itemGroup.getId(), theOid[1].trim());
                if (item != null) {
                    // System.out.println("returning two non nulls");
                    return new ItemOrItemGroupHolder(item, itemGroup);
                }
            }
        }
        if (theOid.length == 1) {
            ItemGroupBean itemGroup = getItemGroupDAO().findByOid(oid.trim());
            if (itemGroup != null) {
                // System.out.println("returning item group not null");
                return new ItemOrItemGroupHolder(null, itemGroup);
            }

            List<ItemBean> items = getItemDAO().findByOid(oid.trim());
            ItemBean item = items.size() > 0 ? items.get(0) : null;
            if (item != null) {
                // System.out.println("returning item not null");
                return new ItemOrItemGroupHolder(item, null);
            }
        }

        return new ItemOrItemGroupHolder(null, null);
    }

    public void updateGroupDynamicsInSection(List<DisplayItemWithGroupBean> displayItemWithGroups, int sectionId, EventCRFBean eventCrfBean) {
        for (DisplayItemWithGroupBean itemWithGroup : displayItemWithGroups) {
            if (itemWithGroup.isInGroup()) {
                updateDynShowGroupInSection(itemWithGroup.getItemGroup(),eventCrfBean);
                updateGroupDynItemsInSection(itemWithGroup, sectionId, eventCrfBean.getCRFVersionId(), eventCrfBean.getId());
            }
        }
    }

    public void updateDynShowGroupInSection(DisplayItemGroupBean itemGroup, EventCRFBean eventCrfBean) {
        DynamicsItemGroupMetadataBean dgm = dynamicsItemGroupMetadataDao.findByMetadataBean(itemGroup.getGroupMetaBean(), eventCrfBean);
        if (dgm != null && dgm.getId() > 0) {
            itemGroup.getGroupMetaBean().setShowGroup(dgm.isShowGroup());
        }
    }

    public void updateGroupDynItemsInSection(DisplayItemWithGroupBean itemWithGroup, int sectionId, int crfVersionId, int eventCrfId) {
        DisplayItemGroupBean digb = itemWithGroup.getItemGroup();
        int groupId = digb.getItemGroupBean().getId();
        List<Integer> itemIds = this.dynamicsItemFormMetadataDao.findItemIdsForAGroupInSection(groupId, sectionId, crfVersionId, eventCrfId);
        if (itemIds != null && itemIds.size() > 0) {
            List<Integer> showItemIds = this.dynamicsItemFormMetadataDao.findShowItemIdsForAGroupInSection(groupId, sectionId, crfVersionId, eventCrfId);
            this.updateItemGroupInASection(digb, itemIds, showItemIds);
            this.updateGroupDynItemsInASection(itemWithGroup, showItemIds, groupId, sectionId, crfVersionId, eventCrfId);
        }
    }

    public void updateRepeatingGroupDynItemsInASection(List<DisplayItemWithGroupBean> displayItemWithGroups, int sectionId, int crfVersionId, int eventCrfId) {
        for (DisplayItemWithGroupBean itemWithGroup : displayItemWithGroups) {
            if (itemWithGroup.isInGroup()) {
                DisplayItemGroupBean digb = itemWithGroup.getItemGroup();
                if (isGroupRepeating(digb.getItemGroupBean().getMeta())) {
                    int groupId = digb.getItemGroupBean().getId();
                    List<Integer> itemIds = this.dynamicsItemFormMetadataDao.findItemIdsForAGroupInSection(groupId, sectionId, crfVersionId, eventCrfId);
                    if (itemIds != null && itemIds.size() > 0) {
                        List<Integer> showItemIds = this.dynamicsItemFormMetadataDao.findShowItemIdsForAGroupInSection(groupId, sectionId, crfVersionId, eventCrfId);
                        this.updateItemGroupInASection(digb, itemIds, showItemIds);
                        this.updateGroupDynItemsInASection(itemWithGroup, showItemIds, groupId, sectionId, crfVersionId, eventCrfId);
                    }
                }
            }
        }
    }

    private void updateItemGroupInASection(DisplayItemGroupBean itemGroup, List<Integer> itemIds, List<Integer> showItemIds) {
        ArrayList<DisplayItemBean> dibs = (ArrayList<DisplayItemBean>) itemGroup.getItems();
        for (DisplayItemBean dib : dibs) {
            ItemFormMetadataBean meta = dib.getMetadata();
            if (showItemIds != null && showItemIds.contains(dib.getItem().getId())) {
                meta.setShowItem(true);
            } else if (itemIds.contains(dib.getItem().getId())){
                dib.getMetadata().setShowItem(false);
            }
        }
    }

    private void updateGroupDynItemsInASection(DisplayItemWithGroupBean itemWithGroup, List<Integer> showItemIds,
            int groupId, int sectionId, int crfVersionId, int eventCrfId) {
        List<DisplayItemGroupBean> digbs = itemWithGroup.getItemGroups();
        List<Integer> showDataIds = this.dynamicsItemFormMetadataDao.findShowItemDataIdsForAGroupInSection(groupId, sectionId, crfVersionId, eventCrfId);
        List<Integer> hideDataIds = this.dynamicsItemFormMetadataDao.findHideItemDataIdsForAGroupInSection(groupId, sectionId, crfVersionId, eventCrfId);
        for (int n = 0; n < digbs.size(); ++n) {
            DisplayItemGroupBean dg = digbs.get(n);
            ArrayList<DisplayItemBean> items = (ArrayList<DisplayItemBean>)dg.getItems();
            for (int m = 0; m < items.size(); ++m) {
                DisplayItemBean dib = items.get(m);
                ItemFormMetadataBean meta = dib.getMetadata();
                dib.setBlankDwelt(false);
                if (hideDataIds!=null && hideDataIds.contains(dib.getData().getId())) {
                    meta.setShowItem(false);
                }
                if (showDataIds!=null && showDataIds.contains(dib.getData().getId())) {
                    meta.setShowItem(true);
                }
                if (!meta.isShowItem() && showItemIds!=null && showItemIds.contains(dib.getItem().getId())) {
                    dib.setBlankDwelt(true);
                }
            }
        }
    }

    public Boolean hasShowingDynGroupInSection(int sectionId, int crfVersionId, int eventCrfId) {
        return dynamicsItemGroupMetadataDao.hasShowingInSection(sectionId, crfVersionId, eventCrfId);
    }

    public Boolean hasShowingDynItemInSection(int sectionId, int crfVersionId, int eventCrfId) {
        return dynamicsItemFormMetadataDao.hasShowingInSection(sectionId, crfVersionId, eventCrfId);
    }

    public DynamicsItemFormMetadataDao getDynamicsItemFormMetadataDao() {
        return dynamicsItemFormMetadataDao;
    }

    public DynamicsItemGroupMetadataDao getDynamicsItemGroupMetadataDao() {
        return dynamicsItemGroupMetadataDao;
    }

    public void setDynamicsItemGroupMetadataDao(DynamicsItemGroupMetadataDao dynamicsItemGroupMetadataDao) {
        this.dynamicsItemGroupMetadataDao = dynamicsItemGroupMetadataDao;
    }

    public void setDynamicsItemFormMetadataDao(DynamicsItemFormMetadataDao dynamicsItemFormMetadataDao) {
        this.dynamicsItemFormMetadataDao = dynamicsItemFormMetadataDao;
    }

    // JN: The following methods were all returning global variables causing an issue in the case of concurrent users.
    // Modified to return new DAO Object. The thread pooling will take care of any heap issues and I do not expect any.

    private EventCRFDAO getEventCRFDAO() {
        return  new EventCRFDAO(ds);
    }

    private ItemDataDAO getItemDataDAO() {
        return new ItemDataDAO(ds);
    }

    private ItemDAO getItemDAO() {
        return new ItemDAO(ds);
    }

    private ItemGroupDAO getItemGroupDAO() {
        return new ItemGroupDAO(ds);
    }

    private SectionDAO getSectionDAO() {
        return new SectionDAO(ds);
    }

    private ItemFormMetadataDAO getItemFormMetadataDAO() {
        return new ItemFormMetadataDAO(ds);
    }

    private ItemGroupMetadataDAO getItemGroupMetadataDAO() {
        return new ItemGroupMetadataDAO(ds);
    }

    public StudyEventDAO getStudyEventDAO() {
        return new StudyEventDAO(ds);
    }

    public EventDefinitionCRFDAO getEventDefinitionCRfDAO() {
        eventDefinitionCRFDAO = this.eventDefinitionCRFDAO != null ? eventDefinitionCRFDAO : new EventDefinitionCRFDAO(ds);
        return eventDefinitionCRFDAO;
    }

    public ExpressionService getExpressionService() {
        return expressionService;
    }

    public void setExpressionService(ExpressionService expressionService) {
        this.expressionService = expressionService;
    }
    
    class ItemOrItemGroupHolder {

        ItemBean itemBean;
        ItemGroupBean itemGroupBean;

        public ItemOrItemGroupHolder(ItemBean itemBean, ItemGroupBean itemGroupBean) {
            this.itemBean = itemBean;
            this.itemGroupBean = itemGroupBean;
        }

        public ItemBean getItemBean() {
            return itemBean;
        }

        public void setItemBean(ItemBean itemBean) {
            this.itemBean = itemBean;
        }

        public ItemGroupBean getItemGroupBean() {
            return itemGroupBean;
        }

        public void setItemGroupBean(ItemGroupBean itemGroupBean) {
            this.itemGroupBean = itemGroupBean;
        }

    }

}
