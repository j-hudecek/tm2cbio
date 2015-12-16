package org.transmart.tm2cbio.utils

import org.transmart.tm2cbio.Config
import org.transmart.tm2cbio.datatypes.AbstractTypeConfig

/**
 * Created by j.hudecek on 15-9-2015.
 */
class ImportScriptGenerator {
    private final String filename = "torun"

    private Config config
    private File script

    public ImportScriptGenerator(Config c) {
        config = c
        script = new File("$c.target_path/$filename");
    }

    private void addCommand(String label, String cmd) {
        script.append("$config.importer_command $cmd || { echo '$label failed' ; exit 1; } \n")
    }

    public void Generate() {
        script.write("""#import script created by tm2cbio
if [ -z \$PORTAL_HOME ]; then echo "PORTAL_HOME is not defined, it has to point to home directory of cbioportal (where portal.properties are)" ; exit 1; fi
echo "Importing study $config.study_id"\n""")
        if (config.importer_remove_old_study == "TRUE") {
            addCommand("Removing old study", "remove-study --meta-filename \"$config.metaStudyFilename\"")
        }
        addCommand("Importing new study metafile", "import-study --meta-filename \"$config.metaStudyFilename\"")
        config.typeConfigs.each { type ->
            script.append("echo 'Importing $type.key data'\n")
            type.value.eachWithIndex { AbstractTypeConfig typeConfig, i ->
                if (typeConfig != null) {
                    addCommand("Importing $type.key datafile $i", "import-study-data --meta-filename \"${typeConfig.getMetaFilename(config)}\" --data-filename \"${typeConfig.getDataFilename(config)}\"")
                    script.append("echo 'Imported $type.key data ${i > 0 ? i : ''}'\n")
                }
            }
        }
        addCommand("Importing patient lists", "import-case-list --meta-filename \"$config.target_path/patient_lists/all.txt\"")
        script.append("echo 'Import done!'\n")

        println "Created script $config.target_path/$filename, run it to import the study to cBioPortal"
    }
}
