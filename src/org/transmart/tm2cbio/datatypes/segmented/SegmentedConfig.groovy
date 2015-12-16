package org.transmart.tm2cbio.datatypes.segmented

import org.transmart.tm2cbio.Config
import org.transmart.tm2cbio.datatypes.AbstractTranslator
import org.transmart.tm2cbio.datatypes.AbstractTypeConfig
import org.transmart.tm2cbio.datatypes.segmented.SegmentedTranslator

/**
 * Created by j.hudecek on 16-12-2015.
 */
class SegmentedConfig extends AbstractTypeConfig {
    public String data_column

    public SegmentedConfig() {
        typeName = "segmented"
    }


    public void check(Config c) {
        super.check()
        if (profile_name == null || profile_name.trim() == "")
            profile_name = c.study_name + " segmented copy number data"
        if (profile_description == null || profile_description.trim() == "")
            profile_description = c.study_name + " segmented copy number data"
        if (data_column == "" || data_column == null)
            data_column = "FLAG"
    }

    @Override
    public AbstractTranslator getTranslator(Config c, int config_number) {
        new SegmentedTranslator(c, config_number)
    }
}
