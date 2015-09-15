package org.transmart.tm2cbio

/**
 * Created by j.hudecek on 11-9-2015.
 */
class CopyNumberConfig extends SpecificConfig {
    public String data_column

    public void check(Config c) {
        super.check()
        if (profile_name == null || profile_name.trim()  == "")
            profile_name = c.study_name+" copy number data"
        if (profile_description == null || profile_description.trim()  == "")
            profile_description = c.study_name+" copy number data"
    }

    @Override
    public AbstractTranslator getTranslator(Config c, int config_number) {
        new CopyNumberTranslator(c, config_number)
    }

}
