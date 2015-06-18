package org.transmart.tm2cbio

/**
 * Created by j.hudecek on 23-3-2015.

 */
class CopyNumberTranslator extends AbstractTranslator {

    def samplesPerGene = [:]
    def geneIdsPerHugo = [:]

    public void createMetaFile(Config c) {
        if (c.copynumber_file_path == "") {
            return
        }
        def meta = new File(c.target_path + "/meta_copynumber.txt");
        meta.write("""cancer_study_identifier: ${c.study_id}
genetic_alteration_type: COPY_NUMBER_ALTERATION
datatype: DISCRETE
stable_id: ${c.study_id}_gistic
profile_name: ${c.copynumber_profile_name}
profile_description: ${c.copynumber_profile_description} for ${c.patient_count} patients.
show_profile_in_analysis_tab: true
""")
    }

    public List<String> writeDataFile(Config c, List<String> patients) {
        new File(c.target_path + "/data_copynumber.txt").withWriter {out ->
            patients = readData(c, patients)
            writeData(out)
        }
        println("Created data file '" + c.target_path + "/data_copynumber.txt'")
        patients
    }

    public void init(Config c) {

    }

    private List<String> readData(Config c, List<String> patients) {
        int valueindex = 0;
        int geneindex = 0;
        int hugoindex = 0;
        if (c.copynumber_data_column == "" || c.copynumber_data_column == null)
            c.copynumber_data_column = "FLAG"
        println("Reading data file '"+c.copynumber_file_path+"'")
        new File(c.copynumber_file_path).eachLine {line, lineNumber ->
            String[] rawFields = line.split('\t')
            if (lineNumber == 1) {
                rawFields.eachWithIndex {String entry, int i ->
                    if (entry.trim() == "BIOMARKER") {
                        hugoindex = i
                    };
                    if (entry.trim() == c.copynumber_data_column) {
                        valueindex = i
                    };
                }
                if (hugoindex == 0) {
                    throw new IllegalArgumentException("BIOMARKER column not found!")
                }
                if (valueindex == 0) {
                    throw new IllegalArgumentException("'${c.copynumber_data_column}' column with copynumber values not found!")
                }
                return
            }
            //patient id will be sample id as well, we need it there twice, trim trailing spaces
            def sampleid = rawFields[0].trim()
            checkSampleID(sampleid)
            if (!patientsForThisDataType.contains(sampleid)) {
                patientsForThisDataType.push(sampleid)
            }
            if (!patients.contains(sampleid)) {
                patients.push(sampleid)
            }
            def hugoid = rawFields[hugoindex].trim()
            def value = rawFields[valueindex].trim()
            if (hugoid == "null")
                return //ignore the value for unknown genes
            if (!samplesPerGene.containsKey(hugoid)) {
                samplesPerGene[hugoid] = [:]
            }
            samplesPerGene[hugoid][sampleid] = value
        }
        patients
    }

    private void writeData(out) {

        // Hugo_Symbol<TAB>Entrez_Gene_Id<TAB>SAMPLE_ID_1<TAB>SAMPLE_ID_2<TAB>...
        // ACAP3<TAB>116983<TAB>-0.005<TAB>-0.550<TAB>...
        // AGRN<TAB>375790<TAB>0.142<TAB>0.091<TAB>...
        writeHeader(out)

        samplesPerGene.each {samplesForGene ->
            def fields = [samplesForGene.key]
            patientsForThisDataType.each {fields.push(samplesForGene.value[it])}
            out.println(fields.join('\t'))
        }
    }

    private void writeHeader(out) {
        def fields = ["Hugo_Symbol"]
        fields.addAll(patientsForThisDataType)
        //header
        out.println(fields.join('\t'))
    }

    @Override
    public String getCaseListName() {
        'gistic'
    }


}
