package org.transmart.tm2cbio.datatypes.copynumber

import org.transmart.tm2cbio.Config
import org.transmart.tm2cbio.datatypes.AbstractTranslator
import org.transmart.tm2cbio.datatypes.AbstractTypeConfig

/**
 * Created by j.hudecek on 11-9-2015.
 */
class CopyNumberConfig extends AbstractTypeConfig {
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
