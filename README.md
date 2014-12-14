tm2cbio
=======

Utility to translate tranSMART export files to a directory ready for import to cBioPortal

1. Use the tranSMART data export functionality to obtain the data file with clinical data.
2. Fill in the mapping file (see example for a simplistic mapping)
3. Run the utility with a single argument - path to the mapping file
4. Run the cbioportal importer on the created directory

Importer uses leaf concept nodes as names for clinical attributes where possible. If two concepts have the same leaf name, distinguishing parts of the concept path are added. This can be further customized by `mapping concept to column name replace`.
If tranSMART ontology contains "special attributes" that cBioPortal recognizes (e.g. age, sex, for a list see mapping file), you can specify where in the ontology tree are they stored and what conversion and/or text replacements need to be applied.
All attributes are imported as STRING by default. INT and BOOLEAN can be specified using the `mapping type ` configuration followed by a concept path. for example
`mapping type \Crizotinib PF2341066\IC50=INT`

To run the utility you need a log4j and groovy installed. You can run it with

	wget https://archive.apache.org/dist/logging/log4j/1.2.17/log4j-1.2.17.jar
	groovyc -cp .:log4j-1.2.17.jar src/org/transmart/tm2cbio/*groovy
	java -cp $GROOVY_HOME/embeddable/groovy-all-*.jar:.  org.transmart.tm2cbio.Main
