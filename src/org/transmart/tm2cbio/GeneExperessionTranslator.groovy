package org.transmart.tm2cbio

/**
 * Created by j.hudecek on 23-3-2015.
 * cancer_study_identifier: brca_tcga_pub

 */
class GeneExperessionTranslator extends AbstractTranslator {

    def samplesPerGene = [:]
    def geneIdsPerHugo = [:]
    def allSamples = []

    public void createMetaFile(Config c) {
        if (c.expression_file_path == "") {
            return
        }
        def meta = new File(c.target_path + "/meta_expression.txt");
        meta.write("""cancer_study_identifier: ${c.study_id}
genetic_alteration_type: MRNA_EXPRESSION
datatype: ${c.expression_data_column}
stable_id: ${c.study_id}_mrna
profile_name: ${c.expression_profile_name}
profile_description: ${c.expression_profile_description} for ${c.patient_count} patients.
show_profile_in_analysis_tab: true
""")
    }

    public List<String> writeDataFile(Config c, List<String> patients) {
        new File(c.target_path + "/data_expression.txt").withWriter {out ->
            patients = readData(c, patients)
            writeData(out)
        }
        println("Created data file '" + c.target_path + "/data_expression.txt'")
        return patients
    }

    public void init(Config c) {

    }

    private List<String> readData(Config c, List<String> patients) {
        int valueindex = 0;
        int geneindex = 0;
        int hugoindex = 0;
        println("Reading data file '"+c.expression_file_path+"'")
        new File(c.expression_file_path).eachLine {line, lineNumber ->
            String[] rawFields = line.split('\t')
            if (lineNumber == 1) {
                rawFields.eachWithIndex {String entry, int i ->
                    if (entry.trim() == "GENE ID") {
                        geneindex = i
                    };
                    if (entry.trim() == "GENE SYMBOL") {
                        hugoindex = i
                    };
                    if (entry.trim() == c.expression_data_column) {
                        valueindex = i
                    };
                }
                if (geneindex == 0) {
                    throw new IllegalArgumentException("GENE ID column not found!")
                }
                if (geneindex == 0) {
                    throw new IllegalArgumentException("GENE SYMBOL column not found!")
                }
                if (geneindex == 0) {
                    throw new IllegalArgumentException("'${c.expression_data_column}' column with expression values not found!")
                }
                return
            }
            //patient id will be sample id as well, we need it there twice, trim trailing spaces
            def sampleid = rawFields[0].trim()
            checkSampleID(sampleid)
            if (!allSamples.contains(sampleid)) {
                allSamples.push(sampleid)
            }
            if (!patients.contains(sampleid)) {
                patients.push(sampleid)
            }
            def hugoid = rawFields[hugoindex].trim()
            def geneid = rawFields[geneindex].trim()
            def value = rawFields[valueindex].trim()
            if (!samplesPerGene.containsKey(hugoid)) {
                samplesPerGene[hugoid] = [:]
            }
            samplesPerGene[hugoid][sampleid] = value
            geneIdsPerHugo[hugoid] = geneid
        }
        patients
    }

    private void writeData(out) {

        // Hugo_Symbol<TAB>Entrez_Gene_Id<TAB>SAMPLE_ID_1<TAB>SAMPLE_ID_2<TAB>...
        // ACAP3<TAB>116983<TAB>-0.005<TAB>-0.550<TAB>...
        // AGRN<TAB>375790<TAB>0.142<TAB>0.091<TAB>...
        writeHeader(out)

        samplesPerGene.each {samplesForGene ->
            def fields = [samplesForGene.key, geneIdsPerHugo[samplesForGene.key]]
            allSamples.each {fields.push(samplesForGene.value[it])}
            out.println(fields.join('\t'))
        }
    }

    private void writeHeader(out) {
        def fields = ["Hugo_Symbol", "Entrez_Gene_Id"]
        fields.addAll(allSamples)
        //header
        out.println(fields.join('\t'))
    }
}
