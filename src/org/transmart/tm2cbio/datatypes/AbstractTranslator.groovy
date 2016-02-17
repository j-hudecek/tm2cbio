package org.transmart.tm2cbio.datatypes

import org.transmart.tm2cbio.Config
import org.transmart.tm2cbio.utils.SetList

/**
 * Created by j.hudecek on 23-3-2015.
 */
abstract class AbstractTranslator {
    def sampleMap = [:]

    public abstract void createMetaFile(Config c)

    public abstract SetList<String> writeDataFile(Config c, SetList<String> patients)

    public List<String> patientsForThisDataType = []

    public abstract String getCaseListName()

    protected static void checkSampleID(String sampleid) {
        if (sampleid.indexOf(' ') != -1)
            throw new IllegalArgumentException("Patient or sample IDs can't contain spaces ('$sampleid')!")
        if (sampleid.indexOf(',') != -1)
            throw new IllegalArgumentException("Patient or sample IDs can't contain commas ('$sampleid')!")
    }
    
    protected void initSampleMapping(AbstractTypeConfig typeConfig) {
        if (typeConfig.samplesfile_path != null)
            new File(typeConfig.samplesfile_path).eachLine { line, lineNumber ->
                if (lineNumber == 0)
                    return
                String[] rawFields = line.replace("\"","").split('\t')
                sampleMap.put(rawFields[0], rawFields[1])
            }

    }
    
    protected String mapSampleid(String sampleid) {
        checkSampleID(sampleid)
        if (sampleMap.size() != 0 && sampleMap.containsKey(sampleid))
            return sampleMap[sampleid]
        return sampleid
    }
}
