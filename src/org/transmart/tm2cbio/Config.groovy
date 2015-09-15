package org.transmart.tm2cbio

import org.transmart.tm2cbio.datatypes.AbstractTypeConfig
import org.transmart.tm2cbio.datatypes.clinical.ClinicalConfig
import org.transmart.tm2cbio.datatypes.copynumber.CopyNumberConfig
import org.transmart.tm2cbio.datatypes.expression.ExpressionConfig

/**
 * Created by j.hudecek on 10-11-2014.
 */
class Config extends Expando {
    //these specify configs for a data type. In front of 1st space
    public final Map prefixes = [clinical: { -> new ClinicalConfig() },
                                 expression: { -> new ExpressionConfig()},
                                 copynumber: { -> new CopyNumberConfig() }]

    public final Map typeConfigs = [:]

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

    public String expression_file_path;
    public String expression_data_column;
    public String expression_profile_name;
    public String expression_profile_description;

    public String copynumber_file_path;
    public String copynumber_data_column;
    public String copynumber_profile_name;
    public String copynumber_profile_description;

    public int patient_count

    public Config(String filename) {
        List<String> lines = new File(expandPath(filename)).readLines();
        if (lines.size() == 0)
            throw new IllegalArgumentException("Empty mapping file '" + expandPath(filename) + "'")
        if (lines[0] != "#tranSMART to cBioPortal mapping file")
            throw new IllegalArgumentException("mapping file must have '#tranSMART to cBioPortal mapping file' as a header on the first line '" + expandPath(filename) + "'")
        lines.each {
            if (it == "" || it.startsWith("#") || it.endsWith('='))
                return; //empty line, comment or empty assignment
            String[] variable_def = it.split("=")
            if (variable_def.size() != 2)
                throw new IllegalArgumentException("wrong format at $it at file '" + expandPath(filename) + "'")
            String variable_name = variable_def[0]
            String variable_value = variable_def[1]
            String variable_name_prefix = variable_name.split(' ')[0]
            if (prefixes[variable_name_prefix] != null) {
                //it is a type specific config clause
                AbstractTypeConfig specificConfig = fetchSpecificTypeConfig(variable_name_prefix, variable_name)
                variable_name = variable_name.replaceFirst(/^$variable_name_prefix (([0-9]+) )?/, "")
                specificConfig.setVariable(variable_name, variable_value);
            } else {
                //general config
                variable_name = variable_name.replace(" ", "_")
                this.@"$variable_name" = variable_value;
            }
        }
        target_path = expandPath(target_path)
        typeConfigs.each { config ->
            config.value.each { it.check(this) }
        }
        patient_count = new File(typeConfigs["clinical"][0].file_path).readLines().size() - 1
        print("Loaded config: ");
        typeConfigs.each {
            print "${it.key}: ${it.value.size()}, "
        }
        println()
    }

    private AbstractTypeConfig fetchSpecificTypeConfig(String variable_name_prefix, String variable_name) {
        if (typeConfigs[variable_name_prefix] == null)
            typeConfigs[variable_name_prefix] = []
        //get the right specific config
        int config_number = extractConfigNumber(variable_name, variable_name_prefix)
        AbstractTypeConfig specificConfig = typeConfigs[variable_name_prefix][config_number];
        if (specificConfig == null)
            specificConfig = typeConfigs[variable_name_prefix][config_number] = prefixes[variable_name_prefix]() as AbstractTypeConfig
        specificConfig
    }

    private int extractConfigNumber(String variable_name, String variable_name_prefix) {
        //expects config term to be: <prefix> [<order>] <term>, for example "expression 1 file path"
        def config_number = 0
        def matcher = variable_name =~ /^$variable_name_prefix ([0-9]+) /
        if (matcher.find())
            config_number = Integer.parseInt(matcher[0][1])
        config_number
    }


    public static String expandPath(String path) {
        if (path.startsWith("~" + File.separator)) {
            path = System.getProperty("user.home") + path.substring(1);
        }

        return path.trim();
    }

}
