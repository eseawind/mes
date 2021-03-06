/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.2.0
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */
package com.qcadoo.mes.productionCounting.internal;

import static com.qcadoo.mes.orders.constants.OrderFields.STATE;
import static com.qcadoo.mes.orders.constants.OrderFields.TECHNOLOGY;
import static com.qcadoo.mes.productionCounting.internal.constants.OrderFieldsPC.*;
import static com.qcadoo.mes.productionCounting.internal.constants.ProductionRecordFields.*;
import static com.qcadoo.mes.productionCounting.internal.constants.TypeOfProductionRecording.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.qcadoo.mes.basic.ParameterService;
import com.qcadoo.mes.basic.constants.ProductFields;
import com.qcadoo.mes.basicProductionCounting.constants.BasicProductionCountingConstants;
import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.mes.orders.constants.OrdersConstants;
import com.qcadoo.mes.orders.states.constants.OrderState;
import com.qcadoo.mes.productionCounting.internal.constants.ProductionRecordFields;
import com.qcadoo.mes.productionCounting.internal.constants.TypeOfProductionRecording;
import com.qcadoo.mes.productionCounting.states.constants.ProductionRecordState;
import com.qcadoo.mes.states.service.client.util.StateChangeHistoryService;
import com.qcadoo.mes.technologies.TechnologyService;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.components.LookupComponent;
import com.qcadoo.view.api.components.WindowComponent;
import com.qcadoo.view.api.ribbon.RibbonActionItem;

@Service
public class ProductionRecordViewService {

    private static final String L_AMOUNT_OF_PRODUCT_PRODUCED = "amountOfProductProduced";

    private static final String L_FORM = "form";

    private static final String L_PRODUCT = "product";

    private static final String L_DONE_QUANTITY = "doneQuantity";

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private TechnologyService technologyService;

    @Autowired
    private ParameterService parameterService;

    public void fillProductionLineLookup(final ViewDefinitionState view) {
        LookupComponent orderLookup = (LookupComponent) view.getComponentByReference("order");
        Entity order = orderLookup.getEntity();
        Long productionLineId = null;
        if (order != null) {
            productionLineId = order.getBelongsToField(OrderFields.PRODUCTION_LINE).getId();
        }
        LookupComponent productionLineLookup = (LookupComponent) view.getComponentByReference("productionLine");
        productionLineLookup.setFieldValue(productionLineId);
    }

    public void setTimeAndPiecworkComponentsVisible(final String recordingType, final Entity order, final ViewDefinitionState view) {
        boolean recordingTypeEqualsForEach = FOR_EACH.getStringValue().equals(recordingType);
        boolean recordingTypeEqualsBasic = BASIC.getStringValue().equals(recordingType);

        view.getComponentByReference(TECHNOLOGY_INSTANCE_OPERATION_COMPONENT).setVisible(recordingTypeEqualsForEach);

        boolean registerProductionTime = order.getBooleanField(REGISTER_PRODUCTION_TIME);
        view.getComponentByReference("timeTab").setVisible(registerProductionTime && !recordingTypeEqualsBasic);

        ProductionRecordState recordState = getRecordState(view);
        WindowComponent window = (WindowComponent) view.getComponentByReference("window");
        RibbonActionItem calcTotalLaborTimeBtn = window.getRibbon().getGroupByName("workTime")
                .getItemByName("calcTotalLaborTime");
        calcTotalLaborTimeBtn.setEnabled(registerProductionTime && !recordingTypeEqualsBasic
                && ProductionRecordState.DRAFT.equals(recordState));
        calcTotalLaborTimeBtn.requestUpdate(true);

        boolean registerPiecework = order.getBooleanField(REGISTER_PIECEWORK);
        view.getComponentByReference("pieceworkTab").setVisible(registerPiecework && recordingTypeEqualsForEach);
    }

    public ProductionRecordState getRecordState(final ViewDefinitionState view) {
        FormComponent form = (FormComponent) view.getComponentByReference("form");
        Entity productionRecordFormEntity = form.getEntity();
        String stateStringValue = productionRecordFormEntity.getStringField(ProductionRecordFields.STATE);
        if (StringUtils.isEmpty(stateStringValue)) {
            return ProductionRecordState.DRAFT;
        }
        return ProductionRecordState.parseString(stateStringValue);
    }

    public void changeProducedQuantityFieldState(final ViewDefinitionState viewDefinitionState) {
        final FormComponent form = (FormComponent) viewDefinitionState.getComponentByReference("form");
        Entity order = null;
        if (form.getEntityId() != null) {
            order = dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(
                    form.getEntityId());
        }

        FieldComponent typeOfProductionRecording = (FieldComponent) viewDefinitionState
                .getComponentByReference(TYPE_OF_PRODUCTION_RECORDING);
        ComponentState doneQuantity = viewDefinitionState.getComponentByReference(L_DONE_QUANTITY);
        ComponentState amountOfPP = viewDefinitionState.getComponentByReference(L_AMOUNT_OF_PRODUCT_PRODUCED);

        if (order == null || order.getStringField(STATE).equals(OrderState.PENDING.getStringValue())
                || order.getStringField(STATE).equals(OrderState.ACCEPTED.getStringValue())) {
            doneQuantity.setEnabled(false);
            amountOfPP.setEnabled(false);
        } else if ("".equals(typeOfProductionRecording.getFieldValue())
                || TypeOfProductionRecording.BASIC.getStringValue().equals(typeOfProductionRecording.getFieldValue())) {
            doneQuantity.setEnabled(true);
            amountOfPP.setEnabled(true);
        } else {
            doneQuantity.setEnabled(false);
            amountOfPP.setEnabled(false);
        }
    }

    public void setOrderDefaultValue(final ViewDefinitionState view) {

        FormComponent form = (FormComponent) view.getComponentByReference(L_FORM);
        if (form.getEntityId() != null) {
            return;
        }

        for (String componentReference : Arrays.asList(REGISTER_QUANTITY_IN_PRODUCT, REGISTER_QUANTITY_OUT_PRODUCT,
                REGISTER_PRODUCTION_TIME, JUST_ONE, ALLOW_TO_CLOSE, AUTO_CLOSE_ORDER, REGISTER_PIECEWORK)) {
            FieldComponent component = (FieldComponent) view.getComponentByReference(componentReference);
            if (component.getFieldValue() == null) {
                component.setFieldValue(getDefaultValueForProductionRecordFromParameter(componentReference));
                component.requestComponentUpdateState();
            }
            component.setEnabled(false);
        }
        FieldComponent typeOfProductionRecording = (FieldComponent) view.getComponentByReference(TYPE_OF_PRODUCTION_RECORDING);
        if (typeOfProductionRecording.getFieldValue() == null) {
            typeOfProductionRecording
                    .setFieldValue(getDefaultValueForTypeOfProductionRecordingParameter(TYPE_OF_PRODUCTION_RECORDING));

        }

    }

    private boolean getDefaultValueForProductionRecordFromParameter(final String reference) {
        Entity parameter = parameterService.getParameter();
        return parameter.getBooleanField(reference);
    }

    private String getDefaultValueForTypeOfProductionRecordingParameter(final String reference) {
        Entity parameter = parameterService.getParameter();
        return parameter.getStringField(reference);
    }

    public void checkTypeOfProductionRecording(final ViewDefinitionState viewDefinitionState) {
        FieldComponent typeOfProductionRecording = (FieldComponent) viewDefinitionState
                .getComponentByReference(TYPE_OF_PRODUCTION_RECORDING);
        if ("".equals(typeOfProductionRecording.getFieldValue())
                || BASIC.getStringValue().equals(typeOfProductionRecording.getFieldValue())) {
            for (String componentName : Arrays.asList(REGISTER_QUANTITY_IN_PRODUCT, REGISTER_QUANTITY_OUT_PRODUCT,
                    REGISTER_PRODUCTION_TIME, REGISTER_PIECEWORK, JUST_ONE, ALLOW_TO_CLOSE, AUTO_CLOSE_ORDER)) {
                FieldComponent component = (FieldComponent) viewDefinitionState.getComponentByReference(componentName);
                component.setEnabled(false);
            }
        } else if (CUMULATED.getStringValue().equals(typeOfProductionRecording.getFieldValue())) {
            FieldComponent component = (FieldComponent) viewDefinitionState.getComponentByReference(REGISTER_PIECEWORK);
            component.setFieldValue(false);
            component.setEnabled(false);
        }
    }

    public void setProducedQuantity(final ViewDefinitionState view) {
        FieldComponent typeOfProductionRecording = (FieldComponent) view.getComponentByReference(TYPE_OF_PRODUCTION_RECORDING);
        FieldComponent doneQuantity = (FieldComponent) view.getComponentByReference(L_DONE_QUANTITY);
        String orderNumber = (String) view.getComponentByReference(NUMBER).getFieldValue();
        Entity order;
        List<Entity> productionCountings;

        if ("".equals(typeOfProductionRecording.getFieldValue())) {
            return;
        }

        if (orderNumber == null) {
            return;
        }
        order = dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).find()
                .add(SearchRestrictions.eq(NUMBER, orderNumber)).uniqueResult();
        if (order == null) {
            return;
        }
        productionCountings = dataDefinitionService
                .get(BasicProductionCountingConstants.PLUGIN_IDENTIFIER,
                        BasicProductionCountingConstants.MODEL_BASIC_PRODUCTION_COUNTING).find()
                .add(SearchRestrictions.eq(ORDER, order)).list().getEntities();

        Entity technology = order.getBelongsToField(TECHNOLOGY);

        if (productionCountings.isEmpty()) {
            return;
        }
        for (Entity counting : productionCountings) {
            Entity aProduct = (Entity) counting.getField(L_PRODUCT);
            if (technologyService.getProductType(aProduct, technology).equals(TechnologyService.L_03_FINAL_PRODUCT)) {
                doneQuantity.setFieldValue(counting.getField("producedQuantity"));
                break;
            }
        }
    }
}
