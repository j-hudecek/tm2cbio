tm2cbio
=======

![GSE4922 to cbio](https://github.com/j-hudecek/tm2cbio/blob/master/tm2cbio2.PNG)



Utility to translate tranSMART export files to a directory ready for import to cBioPortal

1. Use the tranSMART data export functionality to obtain the data file with clinical data.
2. Fill in the mapping file (see example for a simplistic mapping)
3. Run the utility with a single argument - path to the mapping file
4. Run the cbioportal importer on the created directory

Importer uses leaf concept nodes as names for clinical attributes where possible. If two concepts have the same leaf name, distinguishing parts of the concept path are added. This can be further customized by `mapping concept to column name replace`.
If tranSMART ontology contains "special attributes" that cBioPortal recognizes (e.g. age, sex, for a list see mapping file), you can specify where in the ontology tree are they stored (don't use quotes and use slashes (/) instead of backslashes (\)for specifying the path) and what conversion and/or text replacements need to be applied.
All attributes are imported as STRING by default. INT and BOOLEAN can be specified using the `mapping type ` configuration followed by a concept path. for example
`mapping type \Crizotinib PF2341066\IC50=INT`

This converter supports clinical, expression, copy number and segmented (region level) copy number data.

To run the utility you need a log4j and groovy installed. You can run it with

	wget https://archive.apache.org/dist/logging/log4j/1.2.17/log4j-1.2.17.jar
	groovyc -cp .:log4j-1.2.17.jar src/org/transmart/tm2cbio/*.groovy src/org/transmart/tm2cbio/utils/*.groovy src/org/transmart/tm2cbio/datatypes/*.groovy src/org/transmart/tm2cbio/datatypes/expression/*.groovy src/org/transmart/tm2cbio/datatypes/copynumber/*.groovy src/org/transmart/tm2cbio/datatypes/clinical/*.groovy src/org/transmart/tm2cbio/datatypes/segmented/*.groovy
	java -cp $GROOVY_HOME/embeddable/groovy-all-2.3.8.jar:.  org.transmart.tm2cbio.Main tm2cbio.mapping.GSE4922.txt

You will need to replace 2.3.8 with the version of Groovy you have installed
