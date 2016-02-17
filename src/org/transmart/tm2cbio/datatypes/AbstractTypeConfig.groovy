package org.transmart.tm2cbio.datatypes

import org.transmart.tm2cbio.Config

/**
 * Created by j.hudecek on 11-9-2015.
 */
abstract class AbstractTypeConfig {
    public String file_path;
    public String profile_name;
    public String profile_description;
    public String samplesfile_path

    public String typeName
    public int configNumber

    //empty string for config 0, config number if specified (i.e. expression 1)
    protected String getConfigNumberAsString() {
        if (configNumber > 0)
            return configNumber.toString()
        else
            return ""
    }

    public String getMetaFilename(Config c) {
        c.target_path + "/meta_$typeName${configNumberAsString}.txt"
    }

    public String getDataFilename(Config c) {
        c.target_path + "/" + getDataFilenameOnly(c)
    }
    
    public String getDataFilenameOnly(Config c) {
        "data_$typeName${configNumberAsString}.txt"        
    } 

    public void check(Config c) {
        if (file_path != null)
            file_path = Config.expandPath(file_path)
        else
            throw new IllegalArgumentException("$typeName $configNumberAsString is missing the file path")

    }

    public abstract AbstractTranslator getTranslator(Config c, int config_number);

    public void setVariable(String name, String value) {
        name = name.replace(/[^ ]+ /, "").replace(" ", "_")
        this.@"$name" = value;
    }

}
