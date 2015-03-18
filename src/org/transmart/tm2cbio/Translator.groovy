package org.transmart.tm2cbio

/**
 * Created by j.hudecek on 10-11-2014.
 */
class Translator {
    private static final String termSeparator = '/'

    private static List<String> exportColumns

    private static List<String> patients = [];

    public static void process(Config c) {
        new File(c.target_path).mkdirs()
        createMetaStudyFile(c)
        createMetaClinicalFile(c)
        Map concept2col = createConceptToColumnMapping(c)
        println("Created meta files")

        // figure out on which column index are the special columns that need replacing/converting
        def toReplace = [:]
        def toConvert = [:]
        c.special_attributes.each({
            def replaceName = "mapping_"+it+"_replace"
            def convertName = "mapping_"+it+"_convert"
            def pathName = "mapping_"+it+"_path"
            if (c.hasProperty(replaceName) && c.@"$replaceName" != null)
                toReplace.put(exportColumns.findIndexOf {it==Config.translateConcept(c.@"$pathName")}, replaceName)
            if (c.hasProperty(convertName) && c.@"$convertName" != null)
                toConvert.put(exportColumns.findIndexOf {it==Config.translateConcept(c.@"$pathName")}, convertName)
        })

        new File(c.target_path + "/data_clinical.txt").withWriter {out ->
            writeMeta(out, concept2col, c)
            writeData(out, toReplace, toConvert, c)
        }
        println("Created data file '"+c.target_path + "/data_clinical.txt'")
        new File(c.target_path+"/case_lists").mkdirs()
        new File(c.target_path+"/case_lists/all.txt").write("""cancer_study_identifier: ${c.study_id}
stable_id: ${c.study_id}_all
case_list_name: All
case_list_description: All tumor samples (${c.patient_count} samples)
case_list_ids: ${patients.join('\t')}
""")
        println("Created case list with "+patients.size()+" cases")
    }

    private static String applyRegexes(String input, String regexesInConfig, Config c) {
        String with_name = regexesInConfig+"_with"
        c.@"$regexesInConfig".eachWithIndex({ regex,i -> input = input.replace(regex, c.@"$with_name"[i])})
        return input;
    }

    private static String applyConversion(String input, String converter, Config c) {
        def methodName = c.@"$converter"
        Converter."$methodName"(input)
    }

    private static void writeData(out, Map toReplace, Map toConvert, Config c) {
        new File(c.clinical_file_path).eachLine {line, lineNumber ->
            if (lineNumber == 1)
                return;
            String[] rawFields = line.split('\t')
            //patient id will be sample id as well, we need it there twice
            def fields = [rawFields[0].trim()]
            fields.addAll(rawFields)
            //store patient's ID (first column) for case list
            patients.push(fields[0].trim())
            toReplace.each {
                fields[it.key] = applyRegexes(fields[it.key], it.value, c)
            }
            toConvert.each {
                try {
                    fields[it.key] = applyConversion(fields[it.key], it.value, c)
                }
                catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid format for conversion '" + it.value + "': " + fields[it.key] + " on line " + lineNumber + " on column number " + it.key, ex)
                }
            }
            out.println(fields.join('\t'))
        }
    }

    private static void writeMeta(Writer out, Map concept2col, Config c) {
        //write meta information
        out.println("#" + exportColumns.collect({
            concept2col[it]
        }).join('\t'));
        out.println("#" + exportColumns.join('\t'));
        out.println("#" + exportColumns.collect({
            getTypeForConcept(it, c)
        }).join('\t'));
        out.println(exportColumns.collect({
            "SAMPLE" //for SAMPLE/PATIENT type
        }).join('\t'))
        out.println(exportColumns.collect({
            "5" //for priority
        }).join('\t'))
        out.println(exportColumns.collect({
            concept2col[it]
        }).join('\t'))
    }

    private static String getTypeForConcept(String it, Config c) {
        if (c.types.containsKey(it))
            return c.types[it]
        else
            "STRING"
    }

    private static String getLeaf(String concept) {
        String pattern = '.*'+termSeparator;
        concept.replaceAll(~pattern, '')
    }


    private static Map createConceptToColumnMapping(Config c) {
        String firstRow;
        println("Reading data file '"+c.clinical_file_path+"'")
        new File(c.clinical_file_path).withReader { firstRow = it.readLine() } ;
        firstRow = applyRegexes(firstRow, "mapping_concept_to_column_name_replace", c)
        firstRow = Config.translateConcept(firstRow)

        //prepend PATIENT_ID and SAMPLE ID 
        exportColumns = ['PATIENT_ID']
        exportColumns.addAll(firstRow.split('\t'))
        exportColumns[1] = 'SAMPLE_ID'
        //get a clean list of concepts (must have a \)
        String[] concepts = exportColumns.dropWhile {it.indexOf(termSeparator) == -1}
        //map of concept from transmart and its column in cbioportal, initiated to the leaf name
        def concept2col = concepts.collectEntries {[it, getLeaf(it)]}
        HashMap<String, List<String>> leaf2concept = new HashMap<String, List<String>>();
        //get a list of collisions - different concepts with the same leaf name
        concepts.each {
            String leaf = getLeaf(it);
            if (!leaf2concept.containsKey(leaf))
                leaf2concept[leaf] = [];
            leaf2concept[leaf].push(it)
        }

        //prepend unique parts of the path to the leaf name to avoid the collision
        leaf2concept.each {
            List<String> conceptsInCollision = it.value;
            if (conceptsInCollision.size() > 1) {
                //get a list of terms per concept
                def termsPerConcept = conceptsInCollision*.split(termSeparator).toList()
                //concepts can have a different number of terms, iterate over the longest one
                int longestPathLength = termsPerConcept.max({it.size()}).size()
                def longestPath = termsPerConcept.find { it.size() == longestPathLength }
                List<Integer> toskip = [];

                for (int i = 0; i < longestPathLength; i++) {
                    //remove all terms which are the same in all the concepts
                    def term = longestPath[i];
                    if (termsPerConcept.every({ it.size() <= i || it[i] == term})) {
                        //all concepts have the same term on this position (or they are shorter), skip this term
                        toskip.push(i)
                    }
                }
                //set the concatenated terms as the new column label for the concept
                termsPerConcept.eachWithIndex({terms, i ->
                    def uniqueTerms = []
                    for (int j=0;j<terms.size()-1;j++)
                        if (!toskip.contains(j))
                            uniqueTerms.push(terms[j])
                    //always keep the leaf
                    uniqueTerms.push(terms.last())
                    concept2col[conceptsInCollision[i]] = uniqueTerms.join('_')
                })
            }
        }
        //add non concept columns without any mapping
        exportColumns.takeWhile {it.indexOf(termSeparator) == -1}.each {
            concept2col.put(it, it)
        }

        //rename standard (AGE, SEX, ...) columns
        c.special_attributes.each({
            def pathName = "mapping_"+it+"_path"
            if (c.@"$pathName" != null)
                concept2col[Config.translateConcept(c.@"$pathName")] = it
        })

        //check uniqueness of columns just in case...
        def uniqueCols = concept2col.values().unique(false)
        if (uniqueCols.size() != concept2col.values().size()) {
            //restore original ordering of columns for the error message
            throw new IllegalArgumentException("Columns are not unique: "
                    + exportColumns.collect({ concept2col[it] }).join(' '))
        }
        concept2col
    }

    private static void createMetaStudyFile(Config c) {
        def metastudy = new File(c.target_path + "/meta_study.txt");
        metastudy.write("""type_of_cancer: ${c.study_type}
cancer_study_identifier: ${c.study_id}
name: ${c.study_name}
description: ${c.study_description}
short_name: ${c.study_short_name}
citation: ${c.study_citation}
pmid: ${c.study_pmid}""")
    }

    private static void createMetaClinicalFile(Config c) {
        def metaclinical = new File(c.target_path + "/meta_clinical.txt");
        metaclinical.write("""cancer_study_identifier: ${c.study_id}
genetic_alteration_type: CLINICAL
datatype: FREE-FORM
stable_id: ${c.study_id}_clinical
profile_description: Clinical data for ${c.patient_count} patients from tranSMART.
show_profile_in_analysis_tab: true
profile_name: Clinical
""")
    }

}
