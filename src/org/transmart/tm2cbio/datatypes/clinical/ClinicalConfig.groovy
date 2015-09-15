package org.transmart.tm2cbio.datatypes.clinical

import org.transmart.tm2cbio.Config
import org.transmart.tm2cbio.datatypes.AbstractTranslator
import org.transmart.tm2cbio.datatypes.AbstractTypeConfig

/**
 * Created by j.hudecek on 11-9-2015.
 */
class ClinicalConfig extends AbstractTypeConfig {
    public String attributes_names
    public String attributes_descriptions
    public String attributes_types

    public final List special_attributes = ["AGE", "SEX", "RACE", "ETHNICITY", "AGE_AT_DIAGNOSIS", "TUMOR_TYPE", "DAYS_TO_DEATH", "OS_STATUS", "OS_MONTHS", "DFS_STATUS", "DFS_MONTHS", "CANCER_TYPE", "TUMOR_SITE"]

    public String mapping_AGE_path
    public String mapping_AGE_convert
    public String mapping_SEX_path
    public List<String> mapping_SEX_replace
    public List<String> mapping_SEX_replace_with
    public String mapping_RACE_path
    public List<String> mapping_RACE_replace
    public List<String> mapping_RACE_replace_with
    public String mapping_ETHNICITY_path
    public List<String> mapping_ETHNICITY_replace
    public List<String> mapping_ETHNICITY_replace_with
    public String mapping_AGE_AT_DIAGNOSIS_path
    public String mapping_AGE_AT_DIAGNOSIS_convert
    public String mapping_TUMOR_TYPE_path
    public List<String> mapping_TUMOR_TYPE_replace
    public List<String> mapping_TUMOR_TYPE_replace_with
    public String mapping_DAYS_TO_DEATH_path
    public String mapping_DAYS_TO_DEATH_convert
    public String mapping_OS_STATUS_path
    public List<String> mapping_OS_STATUS_replace
    public List<String> mapping_OS_STATUS_replace_with
    public String mapping_OS_MONTHS_path
    public String mapping_OS_MONTHS_convert
    public String mapping_DFS_STATUS_path
    public List<String> mapping_DFS_STATUS_replace
    public List<String> mapping_DFS_STATUS_replace_with
    public String mapping_DFS_MONTHS_path
    public String mapping_DFS_MONTHS_convert
    public String mapping_CANCER_TYPE_path
    public List<String> mapping_CANCER_TYPE_replace
    public List<String> mapping_CANCER_TYPE_replace_with
    public String mapping_TUMOR_SITE_path
    public List<String> mapping_TUMOR_SITE_replace
    public List<String> mapping_TUMOR_SITE_replace_with

    public List<String> mapping_concept_to_column_name_replace
    public List<String> mapping_concept_to_column_name_replace_with

    public Map types = [:]


    @Override
    public AbstractTranslator getTranslator(Config c, int config_number) {
        new ClinicalTranslator(c, config_number)
    }

    @Override
    public void setVariable(String name, String value) {
        if (name.endsWith(" replace")) {
            //handle replace regexes
            String[] replace_def = value.split("\t")
            if (replace_def.size() != 2)
                throw new IllegalArgumentException("invalid replacement for '$name'")
            checkPathBeforeReplace(name)
            name = name.replace(" ", "_")
            String with_name = name + "_with"
            if (this.@"$name" == null) {
                this.@"$name" = []
                this.@"$with_name" = []
            }
            this.@"$name".push(replace_def[0])
            this.@"$with_name".push(replace_def[1])
        } else if (name.startsWith("mapping type ")) {
            //handle type annotation
            def concept = translateConcept(name.substring("mapping type ".length()))
            if (value != "STRING" && value != "INT" && value != "BOOLEAN")
                throw new IllegalArgumentException("$name has invalid type $value only STRING, INT or BOOLEAN are allowed")
            types.put(concept, value)
        }  else {
            //general config
            checkPathBeforeConvert(name)
            name = name.replace(" ", "_")
            this.@"$name" = value;
        }
    }


        private void checkPathBeforeConvert(String variable_name) {
            //if it's a convert directive, check that we already know the path to the special attribute
            def attrName = variable_name.replace(' convert', '').replace('mapping ', '')
            if (this.special_attributes.contains(attrName) && variable_name.endsWith(' convert')) {
                def pathName = variable_name.replace(' convert', ' path').replace(' ', '_')
                if (!this.hasProperty(pathName) || this.@"$pathName" == null)
                    throw new IllegalArgumentException("Cannot add conversion to " + attrName + " without specifying the concept path")
            }
        }

        private void checkPathBeforeReplace(String variable_name) {
            //check that the path to special attribute is specified before the regex
            def attrName = variable_name.replace(' replace', '').replace('mapping ', '')
            if (this.special_attributes.contains(attrName)) {
                def pathName = variable_name.replace(' replace', ' path').replace(' ', '_')
                if (!this.hasProperty(pathName) || this.@"$pathName" == null)
                    throw new IllegalArgumentException("Cannot add replace regexes to " + attrName + " without specifying the concept path")
            }
        }


        public static  String translateConcept(String concept) {
            //bug workaround: groovy's regexes fall apart if term separator is \
            return concept.replace(' ','_').replace('\\','/')
        }
}
