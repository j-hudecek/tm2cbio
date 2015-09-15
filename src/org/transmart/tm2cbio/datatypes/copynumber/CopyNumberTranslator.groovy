package org.transmart.tm2cbio.datatypes.copynumber

import org.transmart.tm2cbio.Config
import org.transmart.tm2cbio.utils.SetList
import org.transmart.tm2cbio.datatypes.AbstractTranslator

/**
 * Created by j.hudecek on 23-3-2015.

 */
class CopyNumberTranslator extends AbstractTranslator {

    def samplesPerGene = [:]
    def geneIdsPerHugo = [:]

    CopyNumberConfig specific_config

    public void createMetaFile(Config c) {
        if (specific_config.file_path == "") {
            return
        }
        if (specific_config.profile_name == null)
            specific_config.profile_name = "$c.study_name CNA data"
        if (specific_config.profile_description == null)
            specific_config.profile_description = specific_config.profile_name
        def meta = new File(c.target_path + "/meta_copynumber.txt");
        meta.write("""cancer_study_identifier: ${c.study_id}
genetic_alteration_type: COPY_NUMBER_ALTERATION
datatype: DISCRETE
stable_id: ${c.study_id}_gistic
profile_name: ${specific_config.profile_name}
profile_description: ${specific_config.profile_description} for ${c.patient_count} patients.
show_profile_in_analysis_tab: true
""")
    }

    public SetList<String> writeDataFile(Config c, SetList<String> patients) {
        new File(c.target_path + "/data_copynumber.txt").withWriter {out ->
            patients = readData(c, patients)
            writeData(out)
        }
        println("Created data file '" + c.target_path + "/data_copynumber.txt'")
        patients
    }


    public CopyNumberTranslator(Config c, int config_number) {
        specific_config = c.specific_configs["copynumber"][config_number]
        configNumber = config_number
    }


    private SetList<String> readData(Config c, SetList<String> patients) {
        int valueindex = 0;
        int geneindex = 0;
        int hugoindex = 0;
        if (specific_config.data_column == "" || specific_config.data_column == null)
            specific_config.data_column = "FLAG"
        println("Reading data file '"+specific_config.file_path+"'")
        new File(specific_config.file_path).eachLine {line, lineNumber ->
            String[] rawFields = line.split('\t')
            if (lineNumber == 1) {
                rawFields.eachWithIndex {String entry, int i ->
                    if (entry.trim() == "BIOMARKER") {
                        hugoindex = i
                    };
                    if (entry.trim() == specific_config.data_column) {
                        valueindex = i
                    };
                }
                if (hugoindex == 0) {
                    throw new IllegalArgumentException("BIOMARKER column not found!")
                }
                if (valueindex == 0) {
                    throw new IllegalArgumentException("'${specific_config.data_column}' column with copynumber values not found!")
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
        'cna'
    }


}
