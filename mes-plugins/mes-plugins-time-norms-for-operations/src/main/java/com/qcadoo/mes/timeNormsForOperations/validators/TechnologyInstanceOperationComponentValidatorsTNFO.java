package com.qcadoo.mes.timeNormsForOperations.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;

@Service
public class TechnologyInstanceOperationComponentValidatorsTNFO {

    @Autowired
    private TechnologyValidatorsServiceTNFO technologyValidatorsServiceTNFO;

    public boolean validate(final DataDefinition tocDD, final Entity technologyInstanceOperationComponent) {
        boolean isValid = true;
        isValid = isValid && technologyValidatorsServiceTNFO.checkIfUnitMatch(tocDD, technologyInstanceOperationComponent);
        isValid = isValid
                && technologyValidatorsServiceTNFO.checkIfUnitsInTechnologyMatch(tocDD, technologyInstanceOperationComponent);
        isValid = isValid
                && technologyValidatorsServiceTNFO.checkIfUnitsInInstanceTechnologyMatch(tocDD,
                        technologyInstanceOperationComponent);
        return isValid;
    }

}
