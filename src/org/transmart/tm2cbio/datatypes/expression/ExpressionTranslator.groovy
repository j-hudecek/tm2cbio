package org.transmart.tm2cbio.datatypes.expression

import org.transmart.tm2cbio.Config
import org.transmart.tm2cbio.datatypes.AbstractTranslator
import org.transmart.tm2cbio.utils.SetList

/**
 * Created by j.hudecek on 23-3-2015.
 * cancer_study_identifier: brca_tcga_pub

 */
class ExpressionTranslator extends AbstractTranslator {

    ExpressionConfig typeConfig
    //magic stable ids for data type
    def datatype2StableId = ['CONTINUOUS':'mrna', 'Z-SCORE':'mrna_median_Zscores', 'DISCRETE':'mrna_outliers']

    def samplesPerGene = [:]
    def entrezIdsPerHugo = [:]
    def hugoIdsPerEntrez = [:]

    public void createMetaFile(Config c) {
        if (typeConfig.file_path == "") {
            return
        }
        if (typeConfig.profile_name == null)
            typeConfig.profile_name = "$c.study_name Expression data"
        def meta = new File(typeConfig.getMetaFilename(c));
        meta.write("""cancer_study_identifier: ${c.study_id}
genetic_alteration_type: MRNA_EXPRESSION
datatype: ${typeConfig.data_column}
stable_id: ${datatype2StableId[typeConfig.data_column]}
profile_name: ${typeConfig.profile_name}
profile_description: ${typeConfig.profile_description} for ${c.patient_count} patients.
data_filename: ${typeConfig.getDataFilenameOnly(c)}
show_profile_in_analysis_tab: true
""")
    }

    public SetList<String> writeDataFile(Config c, SetList<String> patients) {
        new File(typeConfig.getDataFilename(c)).withWriter { out ->
            patients = readData(c, patients)
            writeData(out)
        }
        println("Created data file '" + typeConfig.getDataFilename(c)+"'")
        return patients
    }

    public ExpressionTranslator(Config c, int config_number) {
        typeConfig = c.typeConfigs["expression"][config_number]
    }

    private SetList<String> readData(Config c, SetList<String> patients) {
        int valueindex = 0;
        int geneindex = -1;
        int hugoindex = -1;
        int entrezindex = -1;
        boolean useHugo = false;
        initSampleMapping(typeConfig)
        println("Reading data file '" + typeConfig.file_path + "'")
        new File(typeConfig.file_path).eachLine { line, lineNumber ->
            String[] rawFields = line.split('\t')
            if (lineNumber == 1) {
                rawFields.eachWithIndex { String entry, int i ->
                    if (entry.trim() == typeConfig.entrez_column) {
                        geneindex = entrezindex = i
                    };
                    if (entry.trim() ==  typeConfig.hugo_column) {
                        geneindex = hugoindex = i
                        useHugo = true
                    };
                    if (entry.trim() == typeConfig.data_column) {
                        valueindex = i
                    };
                }
                if (geneindex == -1) {
                    throw new IllegalArgumentException("'$typeConfig.hugo_column' or '$typeConfig.entrez_column' column not found! At least one has to be specified")
                }
                if (valueindex == 0) {
                    throw new IllegalArgumentException("'${typeConfig.data_column}' column with expression values not found!")
                }
                return
            }
            //patient id will be sample id as well, we need it there twice, trim trailing spaces
            def sampleid = rawFields[0].trim()
            sampleid = mapSampleid(sampleid)
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
            if (!samplesPerGene[geneid].containsKey(sampleid)) {
                samplesPerGene[geneid][sampleid] = []
            }
            //key can be either hugo or entrez whichever is present, we figure it out based on its presence in the mapping tables
            samplesPerGene[geneid][sampleid].push(value)
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

        samplesPerGene.each { samplesForGene ->
            def geneid = samplesForGene.key
            String hugoid, entrezid
            //figure out if we used hugo or entrez as key
            if (entrezIdsPerHugo[geneid] != null) {
                hugoid = geneid
                entrezid = entrezIdsPerHugo[hugoid]
            } else {
                entrezid = geneid
                hugoid = hugoIdsPerEntrez[entrezid]
            }

            def fields = [hugoid, entrezid]
            patientsForThisDataType.each {
                def values = samplesForGene.value[it]
                if (values == null)
                    fields.push(null)
                else {
                    if (ExpressionConfig.AgregateGeneExpressions.AVERAGE.toString() == typeConfig.aggregate) {
                        fields.push((values*.toDouble().sum() / values.size()).toString())
                    } else if (ExpressionConfig.AgregateGeneExpressions.GEOMETRICAVERAGE.toString() == typeConfig.aggregate) {
                        fields.push(geometricMean(values*.toDouble()).toString())
                    } else if (ExpressionConfig.AgregateGeneExpressions.FIRST.toString() == typeConfig.aggregate) {
                        fields.push(values[0])
                    } else if (ExpressionConfig.AgregateGeneExpressions.LAST.toString() == typeConfig.aggregate) {
                        fields.push(values.last())
                    } else if (ExpressionConfig.AgregateGeneExpressions.ERROR.toString() == typeConfig.aggregate) {
                        if (values.size() > 1)
                            throw new IllegalArgumentException("Gene '$geneid' has multiple values, please specify a method of aggregation in 'expression aggregate', one of FIRST, LAST, AVERAGE, GEOMETRICAVERAGE or ERROR ")
                        fields.push(values.first())
                    } else {
                        throw new IllegalArgumentException("Aggregation method '$typeConfig.aggregate' is not yet implemented, please specify one of FIRST, LAST, AVERAGE, GEOMETRICAVERAGE or ERROR in 'expression aggregate' ")
                    }
                }
            }
            out.println(fields.join('\t'))
        }
    }

    public static double geometricMean(List<Double> x) {
        int n = x.size();
        double GM_log = 0.0d;
        for (int i = 0; i < n; ++i) {
            if (x[i] == 0L) {
                return 0.0d;
            }
            GM_log += Math.log(x[i]);
        }
        return Math.exp(GM_log / n);
    }

    private void writeHeader(out) {
        def fields = ["Hugo_Symbol", "Entrez_Gene_Id"]
        fields.addAll(patientsForThisDataType)
        //header
        out.println(fields.join('\t'))
    }

    @Override
    public String getCaseListName() {
        'mrna'+typeConfig.configNumberAsString
    }


}
