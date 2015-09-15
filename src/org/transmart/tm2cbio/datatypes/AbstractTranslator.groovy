package org.transmart.tm2cbio

/**
 * Created by j.hudecek on 23-3-2015.
 */
abstract class AbstractTranslator {

    public int configNumber

    //empty string for config 0, config number if specified (i.e. expression 1)
    protected String getConfigNumberAsString()  {
        if (configNumber >0)
            return configNumber.toString()
        else
            return ""
    }

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
}
