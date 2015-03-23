package org.transmart.tm2cbio

/**
 * Created by j.hudecek on 10-11-2014.
 */
class Config  extends Expando {
    public final List special_attributes = ["AGE", "SEX", "RACE", "ETHNICITY", "AGE_AT_DIAGNOSIS", "TUMOR_TYPE", "DAYS_TO_DEATH", "OS_STATUS", "OS_MONTHS", "DFS_STATUS", "DFS_MONTHS", "CANCER_TYPE", "TUMOR_SITE"]

    public String clinical_file_path

    public String target_path

    public String study_id
    public String study_name
    public String study_short_name
    public String study_description
    public String study_type
    public String study_citation
    public String study_pmid

    public String type_name
    public String type_short
    public String type_keywords
    public String type_color
    public String type_short_name

    public String clinical_attributes_names
    public String clinical_attributes_descriptions
    public String clinical_attributes_types

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

    public String expression_file_path;
    public String expression_data_column;
    public String expression_profile_name;
    public String expression_profile_description;

    public int patient_count

    public Map types = [:]

    public Config(String filename) {
        List<String> lines = new File(expandPath(filename)).readLines();
        if (lines.size() == 0)
            throw new IllegalArgumentException("Empty mapping file '"+expandPath(filename)+"'")
        if (lines[0] != "#tranSMART to cBioPortal mapping file")
            throw new IllegalArgumentException("mapping file must have '#tranSMART to cBioPortal mapping file' as a header on the first line '"+expandPath(filename)+"'")
        lines.each {
            if (it == "" || it.startsWith("#") || it.endsWith('='))
                return; //empty assignment
            String[] variable_def = it.split("=")
            if (variable_def.size()!=2)
                throw new IllegalArgumentException("wrong format at $it at file '"+expandPath(filename)+"'")
            String variable_name = variable_def[0]
            String variable_value = variable_def[1]
            if (variable_name.endsWith(" replace")) {
                //handle replace regexes
                String[] replace_def = variable_value.split("\t")
                if (replace_def.size() != 2)
                    throw new IllegalArgumentException("invalid replacement at $it at file '"+expandPath(filename)+"'")
                checkPathBeforeReplace(variable_name)
                variable_name = variable_name.replace(" ", "_")
                String with_name = variable_name+"_with"
                if (this.@"$variable_name" == null) {
                    this.@"$variable_name" = []
                    this.@"$with_name" = []
                }
                this.@"$variable_name".push(replace_def[0])
                this.@"$with_name".push(replace_def[1])
            } else if (variable_name.startsWith("mapping type ")) {
                //handle type annotation
                def concept = translateConcept(variable_name.substring("mapping type ".length()))
                if (variable_value != "STRING" && variable_value != "INT" && variable_value != "BOOLEAN")
                    throw new IllegalArgumentException("$variable_name has invalid type $variable_value only STRING, INT or BOOLEAN are allowed")
                types.put(concept, variable_value)
            } else {
                //general config
                checkPathBeforeConvert(variable_name)
                variable_name = variable_name.replace(" ", "_")
                this.@"$variable_name" = variable_value;
            }
        }
        target_path = expandPath(target_path)
        clinical_file_path = expandPath(clinical_file_path)
        patient_count = new File(clinical_file_path).readLines().size()-1
        if (expression_profile_name == null || expression_profile_name.trim()  == "")
            expression_profile_name = study_name+" expression data"
        if (expression_profile_description == null || expression_profile_description.trim()  == "")
            expression_profile_description = study_name+" expression data"
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

    public static String expandPath(String path) {
        if (path.startsWith("~" + File.separator)) {
            path = System.getProperty("user.home") + path.substring(1);
        }

        return path;
    }

}
