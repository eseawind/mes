package com.qcadoo.mes.productionPerShift.hooks;

import static com.qcadoo.mes.orders.constants.OrderFields.TECHNOLOGY;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.orders.constants.OrdersConstants;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.components.WindowComponent;
import com.qcadoo.view.api.ribbon.RibbonActionItem;
import com.qcadoo.view.api.ribbon.RibbonGroup;

@Service
public class OrderDetailsHooksPPS {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    public void updateViewPPSButtonState(final ViewDefinitionState view) {
        FormComponent orderForm = (FormComponent) view.getComponentByReference("form");

        WindowComponent window = (WindowComponent) view.getComponentByReference("window");
        RibbonGroup orderProgressPlans = (RibbonGroup) window.getRibbon().getGroupByName("orderProgressPlans");
        RibbonActionItem productionPerShift = (RibbonActionItem) orderProgressPlans.getItemByName("productionPerShift");

        Long orderId = orderForm.getEntityId();

        if (orderId != null) {
            Entity order = dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(orderId);

            if (order != null) {
                Entity technology = order.getBelongsToField(TECHNOLOGY);

                if ((technology == null)) {
                    productionPerShift.setEnabled(false);
                    productionPerShift.requestUpdate(true);

                    return;
                } else {
                    productionPerShift.setEnabled(true);
                    productionPerShift.requestUpdate(true);
                    return;
                }
            }
        }

        productionPerShift.setEnabled(false);
        productionPerShift.requestUpdate(true);
    }
}
