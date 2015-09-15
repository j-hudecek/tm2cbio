package org.transmart.tm2cbio

/**
 * Created by j.hudecek on 23-3-2015.
 * cancer_study_identifier: brca_tcga_pub

 */
class GeneExpressionTranslator extends AbstractTranslator {

    ExpressionConfig specific_config

    def samplesPerGene = [:]
    def entrezIdsPerHugo = [:]
    def hugoIdsPerEntrez = [:]

    public void createMetaFile(Config c) {
        if (specific_config.file_path == "") {
            return
        }
        if (specific_config.profile_name == null)
            specific_config.profile_name = "$c.study_name Expression data"
        def meta = new File(c.target_path + "/meta_expression${configNumberAsString}.txt");
        meta.write("""cancer_study_identifier: ${c.study_id}
genetic_alteration_type: MRNA_EXPRESSION
datatype: ${specific_config.data_column}
stable_id: ${c.study_id}_mrna
profile_name: ${specific_config.profile_name}
profile_description: ${specific_config.profile_description} for ${c.patient_count} patients.
show_profile_in_analysis_tab: true
""")
    }

    public SetList<String> writeDataFile(Config c, SetList<String> patients) {
        new File(c.target_path + "/data_expression${configNumberAsString}.txt").withWriter {out ->
            patients = readData(c, patients)
            writeData(out)
        }
        println("Created data file '" + c.target_path + "/data_expression${configNumberAsString}.txt'")
        return patients
    }

    public GeneExpressionTranslator(Config c, int config_number) {
        specific_config = c.specific_configs["expression"][config_number]
        configNumber = config_number
    }

    private SetList<String> readData(Config c, SetList<String> patients) {
        int valueindex = 0;
        int geneindex = -1;
        int hugoindex = -1;
        int entrezindex = -1;
        boolean useHugo = false;
        println("Reading data file '"+specific_config.file_path+"'")
        new File(specific_config.file_path).eachLine {line, lineNumber ->
            String[] rawFields = line.split('\t')
            if (lineNumber == 1) {
                rawFields.eachWithIndex {String entry, int i ->
                    if (entry.trim() == "GENE ID") {
                        geneindex = entrezindex = i
                    };
                    if (entry.trim() == "GENE SYMBOL") {
                        geneindex = hugoindex = i
                        useHugo = true
                    };
                    if (entry.trim() == specific_config.data_column) {
                        valueindex = i
                    };
                }
                if (geneindex == -1) {
                    throw new IllegalArgumentException("GENE ID or GENE SYMBOL column not found! At least one has to be specified")
                }
                if (valueindex == 0) {
                    throw new IllegalArgumentException("'${specific_config.data_column}' column with expression values not found!")
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
            def hugoid = hugoindex != -1 ? rawFields[hugoindex].trim() : ''
            def entrezid = entrezindex != -1 ? rawFields[entrezindex].trim() : ''
            String geneid
            if (useHugo)
                geneid = hugoid
            else
                geneid = entrezid
            def value = rawFields[valueindex].trim()
            if (!samplesPerGene.containsKey(geneid)) {
                samplesPerGene[geneid] = [:]
            }
            //key can be either hugo or entrez whichever is present, we figure it out based on its presence in the mapping tables
            samplesPerGene[geneid][sampleid] = value
            if (hugoid != '')
                entrezIdsPerHugo[hugoid] = entrezid
            if (entrezid != '')
                hugoIdsPerEntrez[entrezid] = hugoid
        }
        patients
    }

    private void writeData(out) {

        // Hugo_Symbol<TAB>Entrez_Gene_Id<TAB>SAMPLE_ID_1<TAB>SAMPLE_ID_2<TAB>...
        // ACAP3<TAB>116983<TAB>-0.005<TAB>-0.550<TAB>...
        // AGRN<TAB>375790<TAB>0.142<TAB>0.091<TAB>...
        writeHeader(out)

        samplesPerGene.each {samplesForGene ->
            def geneid = samplesForGene.key
            String hugoid, entrezid
            //figure out if we used hugo or entrez as key
            if (entrezIdsPerHugo[geneid] != null) {
                hugoid = geneid
                entrezid = entrezIdsPerHugo[hugoid]
            } else {
                entrezid = geneid
                hugoid =  hugoIdsPerEntrez[entrezid]
            }

            def fields = [hugoid, entrezid]
            patientsForThisDataType.each {fields.push(samplesForGene.value[it])}
            out.println(fields.join('\t'))
        }
    }

    private void writeHeader(out) {
        def fields = ["Hugo_Symbol", "Entrez_Gene_Id"]
        fields.addAll(patientsForThisDataType)
        //header
        out.println(fields.join('\t'))
    }

    @Override
    public String getCaseListName() {
        'mrna'
    }


}