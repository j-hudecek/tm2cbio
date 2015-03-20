package org.transmart.tm2cbio

/**
 * Created by j.hudecek on 10-11-2014.
 */
class Translator {

    private static List<String> patients = [];

    public static void process(Config c) {
        new File(c.target_path).mkdirs()
        createMetaStudyFile(c)
        ClinicalTranslator.createMetaClinicalFile(c)
        println("Created meta files")

        patients = ClinicalTranslator.writeDataFile(c)
        writeCaseList(c)
    }

    private static void writeCaseList(Config c) {
        new File(c.target_path + "/case_lists").mkdirs()
        new File(c.target_path + "/case_lists/all.txt").write("""cancer_study_identifier: ${
            c.study_id
        }
stable_id: ${c.study_id}_all
case_list_name: All
case_list_description: All tumor samples (${c.patient_count} samples)
case_list_ids: ${patients.join('\t')}
""")
        println("Created case list with " + patients.size() + " cases")
    }

    private static void createMetaStudyFile(Config c) {
        def metastudy = new File(c.target_path + "/meta_study.txt");
        if (c.study_type.toUpperCase() != c.study_type )
            throw new IllegalArgumentException("Cancer type must be ALL CAPS")
        metastudy.write("""type_of_cancer: ${c.study_type}
cancer_study_identifier: ${c.study_id}
name: ${c.study_name}
description: ${c.study_description}
short_name: ${c.study_short_name}
citation: ${c.study_citation}
pmid: ${c.study_pmid}""")
    }


}
