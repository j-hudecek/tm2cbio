package org.transmart.tm2cbio

/**
 * Created by j.hudecek on 23-3-2015.
 */
abstract class AbstractTranslator {

        public abstract void createMetaFile(Config c)

        public abstract List<String> writeDataFile(Config c, List<String> patients)

        public abstract void init(Config c)

        public List<String> patientsForThisDataType = []
        public abstract String getCaseListName()

        protected static void checkSampleID(String sampleid) {
            if (sampleid.indexOf(' ') != -1)
                throw new IllegalArgumentException("Patient or sample IDs can't contain spaces ('$sampleid')!")
            if (sampleid.indexOf(',') != -1)
                throw new IllegalArgumentException("Patient or sample IDs can't contain commas ('$sampleid')!")
        }
}
