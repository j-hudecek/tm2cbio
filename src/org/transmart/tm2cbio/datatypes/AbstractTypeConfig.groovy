package org.transmart.tm2cbio.datatypes

import org.transmart.tm2cbio.Config
import org.transmart.tm2cbio.datatypes.AbstractTranslator

/**
 * Created by j.hudecek on 11-9-2015.
 */
abstract class AbstractTypeConfig {
    public String file_path;
    public String profile_name;
    public String profile_description;

    public void check(Config c) {
        if (file_path != null)
            file_path = Config.expandPath(file_path)
    }

    public abstract AbstractTranslator getTranslator(Config c, int config_number);

    public void setVariable(String name, String value) {
        name = name.replace(/[^ ]+ /, "").replace(" ", "_")
        this.@"$name" = value;
    }
}
