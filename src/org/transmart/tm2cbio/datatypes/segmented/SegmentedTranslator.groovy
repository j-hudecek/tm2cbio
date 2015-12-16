package org.transmart.tm2cbio.datatypes.segmented

import org.transmart.tm2cbio.Config
import org.transmart.tm2cbio.datatypes.AbstractTranslator
import org.transmart.tm2cbio.utils.SetList

/**
 * Created by j.hudecek on 16-12-2015.
 */
class SegmentedTranslator extends AbstractTranslator {

    def samplesPerGene = [:]
    def geneIdsPerHugo = [:]

    SegmentedConfig typeConfig

    public void createMetaFile(Config c) {
        if (typeConfig.file_path == "") {
            return
        }
        if (typeConfig.profile_name == null)
            typeConfig.profile_name = "$c.study_name segmented copy number data"
        if (typeConfig.profile_description == null)
            typeConfig.profile_description = typeConfig.profile_name
        def meta = new File(typeConfig.getMetaFilename(c));
        meta.write("""cancer_study_identifier: ${c.study_id}
genetic_alteration_type: SEGMENT
datatype: SEGMENT
stable_id: ${c.study_id}_segment
profile_name: ${typeConfig.profile_name}
description: ${typeConfig.profile_description} for ${c.patient_count} patients.
profile_description: ${typeConfig.profile_description} for ${c.patient_count} patients.
show_profile_in_analysis_tab: true
data_filename: data_segmented.txt
reference_genome_id: hg19
""")
    }

    public SetList<String> writeDataFile(Config c, SetList<String> patients) {
        new File(typeConfig.getDataFilename(c)).withWriter { out ->
            patients = processData(c, out, patients)
        }
        println("Created data file '" + typeConfig.getDataFilename(c) + "'")
        patients
    }


    public SegmentedTranslator(Config c, int config_number) {
        typeConfig = c.typeConfigs["segmented"][config_number]
    }


    private SetList<String> processData(Config c, out, SetList<String> patients) {
        int valueindex = -1;
        int hugoindex = -1;
        int chrindex = -1;
        int startindex = -1;
        int endindex = -1;

        println("Reading data file '" + typeConfig.file_path + "'")

        writeHeader(out)
//            'ID<TAB>chrom<TAB>loc.start<TAB>loc.end<TAB>num.mark<TAB>seg.mean
//            SAMPLE_ID_1<TAB>1<TAB>3208470<TAB>245880329<TAB>128923<TAB>0.0025
//            SAMPLE_ID_2<TAB>2<TAB>474222<TAB>5505492<TAB>2639<TAB>-0.0112
//            SAMPLE_ID_2<TAB>2<TAB>5506070<TAB>5506204<TAB>2<TAB>-1.5012

        new File(typeConfig.file_path).eachLine { line, lineNumber ->
            String[] rawFields = line.split('\t')
            if (lineNumber == 1) {
                rawFields.eachWithIndex { String entry, int i ->
                    if (entry.trim() == "CHROMOSOME") {
                        chrindex = i
                    };
                    if (entry.trim() == "START") {
                        startindex = i
                    };
                    if (entry.trim() == "END") {
                        endindex = i
                    };
                    if (entry.trim() == typeConfig.data_column) {
                        valueindex = i
                    };
                }
                if (chrindex == -1 || startindex == -1 || endindex == -1) {
                    throw new IllegalArgumentException("CHROMOSOME, START or END is not there!")
                }
                if (valueindex == -1) {
                    throw new IllegalArgumentException("'${typeConfig.data_column}' column with copynumber values not found!")
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
            def value = rawFields[valueindex].trim()
            def chr = rawFields[chrindex].trim()
            if (chr.startsWith('chr'))
                chr = chr.substring(3) //skip "chr"
            def start = rawFields[startindex].trim()
            def end = rawFields[endindex].trim()
            out.println([sampleid, chr, start, end,"1", value].join('\t')) //use 1 for number of probes
        }
        patients
    }

    private void writeHeader(out) {
        //header
        out.println("ID\tchrom\tloc.start\tloc.end\tnum.mark\tseg.mean")
    }

    @Override
    public String getCaseListName() {
        'cnasegment' + typeConfig.configNumberAsString
    }
}
