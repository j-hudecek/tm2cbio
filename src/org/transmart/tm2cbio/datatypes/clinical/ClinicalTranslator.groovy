package org.transmart.tm2cbio.datatypes.clinical

import org.transmart.tm2cbio.Config
import org.transmart.tm2cbio.datatypes.AbstractTranslator
import org.transmart.tm2cbio.utils.Converter
import org.transmart.tm2cbio.utils.SetList

import java.util.stream.Collectors

/**
 * Created by j.hudecek on 20-3-2015.
 */
class ClinicalTranslator extends AbstractTranslator {

    private Map concept2col
    //column names to replace
    private Map toReplace
    //columns to run conversion on
    private Map toConvert

    private static final String termSeparator = '/'

    private List<String> exportColumns
    private List<String> patientExportColumns = []
    private List<String> sampleExportColumns = []

    private ClinicalConfig typeConfig

    private String forConfigNumber

    public void createMetaFile(Config c) {
        createMetaFilePatient(c)
        createMetaFileSample(c)
    }

    private void createMetaFilePatient(Config c){
        def metaclinical = new File(getPatientFileName(typeConfig.getMetaFilename(c)))
        metaclinical.write("""cancer_study_identifier: ${c.study_id}
genetic_alteration_type: CLINICAL
datatype: PATIENT_ATTRIBUTES
data_filename: ${getPatientFileName(typeConfig.getDataFilenameOnly(c))}
""")
    }

    private void createMetaFileSample(Config c){
        def metaclinical = new File(getSampleFileName(typeConfig.getMetaFilename(c)))
        metaclinical.write("""cancer_study_identifier: ${c.study_id}
genetic_alteration_type: CLINICAL
datatype: SAMPLE_ATTRIBUTES
data_filename: ${getSampleFileName(typeConfig.getDataFilenameOnly(c))}
""")
    }

    private String getPatientFileName(String filename){
        return filename.substring(0, filename.lastIndexOf("."))+"_patient.txt"
    }

    private String getSampleFileName(String filename){
        return filename.substring(0, filename.lastIndexOf("."))+"_sample.txt"
    }

    public SetList<String> writeDataFile(Config c, SetList<String> patients) {
        // create filewriters for the patient and sample files
        Writer patientFileWriter = new File(getPatientFileName(typeConfig.getDataFilename(c))).newWriter();
        Writer sampleFileWriter = new File(getSampleFileName(typeConfig.getDataFilename(c))).newWriter();

        // write the top 5 meta lines
        writeMeta(patientFileWriter, concept2col, patientExportColumns)
        writeMeta(sampleFileWriter, concept2col, sampleExportColumns)

        // write the actual data
        patients = writeData(patientFileWriter, sampleFileWriter, toReplace, toConvert, patients)

        // close the writers
        patientFileWriter.close()
        sampleFileWriter.close()

        println("Created data file $forConfigNumber'" + getPatientFileName(typeConfig.getDataFilename(c)) + "'")
        println("Created data file $forConfigNumber'" + getSampleFileName(typeConfig.getDataFilename(c)) + "'")
        return patients
    }

    public ClinicalTranslator(Config c, int config_number) {
        typeConfig = c.typeConfigs["clinical"][config_number]
        concept2col = createConceptToColumnMapping(c)
        // figure out on which column index are the special columns that need replacing/converting
        toReplace = [:]
        toConvert = [:]
        typeConfig.toReplace.each({
            def replaceName = "mapping_" + it + "_replace"
            def pathName = "mapping_" + it + "_path"
            if (typeConfig.get(pathName) == null)
                throw new IllegalArgumentException("Cannot replace $it, you have to define custom concept to column mapping for it (clinical mapping <name> path=<name>")
            toReplace.put(exportColumns.findIndexOf {
                it == typeConfig.translateConcept(typeConfig.get(pathName))
            }, replaceName)
        })
        typeConfig.toConvert.each({
            def convertName = "mapping_" + it + "_convert"
            def pathName = "mapping_" + it + "_path"
            if (typeConfig.get(pathName) == null)
                throw new IllegalArgumentException("Cannot convert $it, you have to define custom concept to column mapping for it")
            toConvert.put(exportColumns.findIndexOf {
                it == typeConfig.translateConcept(typeConfig.get(pathName))
            }, convertName)
        })
        if (config_number > 0)
            forConfigNumber = " for config number $config_number"
        else
            forConfigNumber = ""
    }

    @Override
    String getCaseListName() {
        'clinical'+typeConfig.configNumberAsString
    }

    private String applyRegexes(String input, String regexesInConfig) {
        String with_name = regexesInConfig + "_with"
        typeConfig.get(regexesInConfig).eachWithIndex({ regex, i -> input = input.replace(regex, typeConfig.get(with_name)[i]) })
        return input;
    }

    private String applyConversion(String input, String converter) {
        def methodName = typeConfig.get(converter)
        Converter."$methodName"(input)
    }

    private SetList<String> writeData(patientWriter, sampleWriter, Map toReplace, Map toConvert, SetList<String> patients) {
        new File(typeConfig.file_path).eachLine { line, lineNumber ->
            if (lineNumber == 1)
                return;
            String[] rawFields = line.replace("\"","").split('\t', -1) //-1 is a limit of length to include also empty fields
            //patient id will be sample id as well, we need it there twice, trim trailing spaces
            rawFields[0] = rawFields[0].trim()
            if (rawFields[0].indexOf(' ') != -1)
                throw new IllegalArgumentException("Patient or sample IDs$forConfigNumber can't contain spaces!")
            if (rawFields[0].indexOf(',') != -1)
                throw new IllegalArgumentException("Patient or sample IDs$forConfigNumber can't contain commas!")
            def fields = [rawFields[0]]
            fields.addAll(rawFields)
            if (patients.contains(fields[0])) {
                println("WARNING: duplicate ID in clinical data$forConfigNumber '${fields[0]}', ignoring data on line $lineNumber")
                return
            }
            //store patient's ID (first column) for case list
            patients.push(fields[0])
            patientsForThisDataType.push(fields[0])
            toReplace.each {
                fields[it.key] = applyRegexes(fields[it.key], it.value)
            }
            toConvert.each {
                try {
                    fields[it.key] = applyConversion(fields[it.key], it.value)
                }
                catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid format for conversion$forConfigNumber '" + it.value + "': " + fields[it.key] + " on line " + lineNumber + " on column number " + it.key, ex)
                }
            }
            fields = fields.collect({
                if (it == "")
                    return "NA"
                return it
            })

            // split the result fields into patient and sample fields and write the to the files
            def patientValues = [], sampleValues = []
            splitValues(fields, patientValues, sampleValues)
            patientWriter.println(patientValues.stream().collect(Collectors.joining("\t")))
            sampleWriter.println(sampleValues.stream().collect(Collectors.joining("\t")));
        }
        return patients
    }

    private void splitValues(List<String> fields, List<String> patientValues, List<String> sampleValues){
        // for each of the fields
        for(int i=0; i<fields.size(); i++){
            // fetch the headername and the value
            def header = exportColumns[i]
            def value = fields.get(i)
            // check whether the header belongs to patient or sample and add to the respective list
            if(patientExportColumns.contains(header)){
                patientValues.add(value)
            }
            // since an attribute can be in both another if
            if(sampleExportColumns.contains(header)){
                sampleValues.add(value)
            }
        }
    }

    private void writeMeta(Writer out, Map concept2col, List<String> columns) {
        //write meta information
        out.println("#" + columns.collect({
            concept2col[it].replace("_", " ")
        }).join('\t'));
        out.println("#" + columns.collect({
            it.replace("_", " ")
        }).join('\t'))
        out.println("#" + columns.collect({
            getTypeForConcept(it)
        }).join('\t'))
        out.println("#" + columns.collect({
            "5" //for priority
        }).join('\t'))
        out.println(columns.collect({
            concept2col[it]
        }).join('\t'))
    }


    private String getTypeForConcept(String it) {
        if (typeConfig.types.containsKey(it))
            return typeConfig.types[it]
        else
            "STRING"
    }

    private String getLeaf(String concept) {
        String pattern = '.*' + termSeparator;
        concept.replaceAll(~pattern, '')
    }


    private Map createConceptToColumnMapping(Config c) {
        String firstRow;
        println("Reading data file '" + typeConfig.file_path + "'")
        new File(typeConfig.file_path).withReader { firstRow = it.readLine() };
        firstRow = applyRegexes(firstRow, "mapping_concept_to_column_name_replace")
        firstRow = typeConfig.translateConcept(firstRow)

        //prepend PATIENT_ID and SAMPLE ID
        exportColumns = ['PATIENT_ID']
        exportColumns.addAll(firstRow.split('\t'))
        exportColumns[1] = 'SAMPLE_ID'
        //get a clean list of concepts (must have a \)
        String[] concepts = exportColumns.dropWhile { it.indexOf(termSeparator) == -1 }
        //map of concept from transmart and its column in cbioportal, initiated to the leaf name
        def concept2col = concepts.collectEntries { [it, getLeaf(it)] }
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
                int longestPathLength = termsPerConcept.max({ it.size() }).size()
                def longestPath = termsPerConcept.find { it.size() == longestPathLength }
                List<Integer> toskip = [];

                for (int i = 0; i < longestPathLength; i++) {
                    //remove all terms which are the same in all the concepts
                    def term = longestPath[i];
                    if (termsPerConcept.every({ it.size() <= i || it[i] == term })) {
                        //all concepts have the same term on this position (or they are shorter), skip this term
                        toskip.push(i)
                    }
                }
                //set the concatenated terms as the new column label for the concept
                termsPerConcept.eachWithIndex({ terms, i ->
                    def uniqueTerms = []
                    for (int j = 0; j < terms.size() - 1; j++)
                        if (!toskip.contains(j))
                            uniqueTerms.push(terms[j])
                    //always keep the leaf
                    uniqueTerms.push(terms.last())
                    concept2col[conceptsInCollision[i]] = uniqueTerms.join(' ')
                })
            }
        }
        //add non concept columns without any mapping
        exportColumns.takeWhile { it.indexOf(termSeparator) == -1 }.each {
            concept2col.put(it, it)
        }

        //rename standard (AGE, SEX, ...) columns
        typeConfig.special_attributes.each({
            def pathName = "mapping_" + it + "_path"
            if (typeConfig.get(pathName) != null)
                concept2col[typeConfig.translateConcept(typeConfig.get(pathName))] = it
        })

        //check uniqueness of columns just in case...
        def uniqueCols = concept2col.values().unique(false)
        if (uniqueCols.size() != concept2col.values().size()) {
            //restore original ordering of columns for the error message
            throw new IllegalArgumentException("Columns are not unique$forConfigNumber: "
                    + exportColumns.collect({ concept2col[it] }).join(' '))
        }

        // split the original column names into sample and patient list
        generateSamplePatientColumns()
        concept2col
    }

    private void generateSamplePatientColumns(){
        exportColumns.each {
            if(!typeConfig.attributeTypes.containsKey(it.toUpperCase())){
                println("\tMissing and assumed sample attribute: ${it}")
                sampleExportColumns.add(it)
            }
            else{
                String type = typeConfig.attributeTypes.get(it.toUpperCase())
                if(type.equalsIgnoreCase("S")){
                    sampleExportColumns.add(it)
                }
                else if(type.equalsIgnoreCase("P")){
                    patientExportColumns.add(it)
                }
                else{
                    patientExportColumns.add(it)
                    sampleExportColumns.add(it)
                }
            }
        }
    }
}
