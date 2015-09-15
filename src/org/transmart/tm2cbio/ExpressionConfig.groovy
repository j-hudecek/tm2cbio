package org.transmart.tm2cbio

/**
 * Created by j.hudecek on 11-9-2015.
 */
class ExpressionConfig extends SpecificConfig {
    public String data_column

    public void check(Config c) {
        super.check()
        if (profile_name == null || profile_name.trim()  == "")
            profile_name = c.study_name+" expression data"
        if (profile_description == null || profile_description.trim()  == "")
            profile_description = c.study_name+" expression data"
    }

    @Override
    public AbstractTranslator getTranslator(Config c, int config_number) {
        new GeneExpressionTranslator(c, config_number)
    }
}
