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
package com.qcadoo.mes.timeNormsForOperations.listeners;

import org.springframework.stereotype.Service;

import com.qcadoo.mes.timeNormsForOperations.constants.TechnologyOperCompFieldsTNFO;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;

@Service
public class OperationDetailsListenersTNFO {

    public void changeNextOperationAfterProducedTypeOperation(final ViewDefinitionState viewDefinitionState,
            final ComponentState state, final String[] args) {
        FieldComponent nextOperationAfterProducedType = (FieldComponent) viewDefinitionState
                .getComponentByReference(TechnologyOperCompFieldsTNFO.NEXT_OPERATION_AFTER_PRODUCED_TYPE);
        FieldComponent nextOperationAfterProducedQuantity = (FieldComponent) viewDefinitionState
                .getComponentByReference(TechnologyOperCompFieldsTNFO.NEXT_OPERATION_AFTER_PRODUCED_QUANTITY);
        FieldComponent nextOperationAfterProducedQuantityUNIT = (FieldComponent) viewDefinitionState
                .getComponentByReference(TechnologyOperCompFieldsTNFO.NEXT_OPERATION_AFTER_PRODUCED_QUANTITY_UNIT);

        if (nextOperationAfterProducedType.getFieldValue().equals("02specified")) {
            nextOperationAfterProducedQuantity.setVisible(true);
            nextOperationAfterProducedQuantityUNIT.setVisible(true);

        } else {
            nextOperationAfterProducedQuantity.setVisible(false);
            nextOperationAfterProducedQuantityUNIT.setVisible(false);
        }
        nextOperationAfterProducedQuantity.requestComponentUpdateState();
    }

}
