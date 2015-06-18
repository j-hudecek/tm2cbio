package org.transmart.tm2cbio

/**
 * Created by j.hudecek on 10-11-2014.
 */
class Translator {

    private static List<String> patients = [];

    public static void process(Config c) {
        new File(c.target_path).mkdirs()
        createMetaStudyFile(c)
        def translators = [ new ClinicalTranslator(), new GeneExperessionTranslator(), new CopyNumberTranslator()]
        translators.each {it.init(c)}
        translators.each {it.createMetaFile(c)}
        println("Created meta files")

        translators.each { patients = it.writeDataFile(c, patients)}
        println("Created data files")

        translators.each {
            writeCaseList(c, it.caseListName, it.patientsForThisDataType)
        }
        writeCaseList(c, 'all', patients)
    }

    private static void writeCaseList(Config c, String name, List<String> cases) {
        new File(c.target_path + "/case_lists").mkdirs()
        new File(c.target_path + "/case_lists/${name}.txt").write("""cancer_study_identifier: ${c.study_id}
stable_id: ${c.study_id}_$name
case_list_name: $name
case_list_description: $name tumor samples (${cases.size()} samples)
case_list_ids: ${cases.join('\t')}
""")
        println("Created '$name' case list with ${cases.size()} cases")
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
