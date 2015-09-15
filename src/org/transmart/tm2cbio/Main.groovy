package org.transmart.tm2cbio
/**
 * Created by j.hudecek on 10-11-2014.
 */
class Main {
    private static void usage() {
        println("Converter of tranSMART data export files to cBioPortal import files")
        println("usage tm2cbio <mapping file>")
    }

    static main(args) {
        if (args.size() == 0) {
            usage();
            return;
        }
        if (!new File(args[0]).exists()) {
            println("File '" + args[0] + "' doesn't exist");
            return;
        }
        println("Converting file using mapping file '" + args[0] + "'")
        Config c = new Config(args[0]);
        Translator.process(c);
        println("Done")
    }
}
