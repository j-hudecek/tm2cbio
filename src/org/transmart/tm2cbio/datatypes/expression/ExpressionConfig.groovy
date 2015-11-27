package org.transmart.tm2cbio.datatypes.expression

import org.transmart.tm2cbio.Config
import org.transmart.tm2cbio.datatypes.AbstractTranslator
import org.transmart.tm2cbio.datatypes.AbstractTypeConfig

/**
 * Created by j.hudecek on 11-9-2015.
 */
class ExpressionConfig extends AbstractTypeConfig {

    public enum AgregateGeneExpressions{
        FIRST,
        LAST,
        AVERAGE,
        GEOMETRICAVERAGE,
        ERROR
    }

    public String data_column

    public String aggregate

    public ExpressionConfig() {
        typeName = "expression"
    }


    public void check(Config c) {
        super.check()
        if (profile_name == null || profile_name.trim() == "")
            profile_name = c.study_name + " expression data"
        if (profile_description == null || profile_description.trim() == "")
            profile_description = c.study_name + " expression data"
        if (aggregate == null) {
            aggregate = "ERROR"
        } else {
            aggregate = aggregate.toUpperCase()
        }
        try {
            AgregateGeneExpressions.valueOf(aggregate)
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'$aggregate' is not a valid value for expression aggregate. Valid values are FIRST, LAST, AVERAGE, GEOMETRICAVERAGE or ERROR (default)")
        }
    }

    @Override
    public AbstractTranslator getTranslator(Config c, int config_number) {
        new ExpressionTranslator(c, config_number)
    }
}
