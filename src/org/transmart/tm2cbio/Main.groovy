package org.transmart.tm2cbio
/**
 * Created by j.hudecek on 10-11-2014.
 */
class Main {
    private static void usage() {
        println("usage tm2cbio <mapping file>")
    }

    static main(args) {
        if (args.size() == 0) {
            usage();
            return;
        }
        println("tm2cbio - Converter of tranSMART data export files to cBioPortal import files")
        if (!new File(args[0]).exists()) {
            println("File '" + args[0] + "' doesn't exist");
            return;
        }
        println("Converting files using mapping file '" + args[0] + "'")
        Config c = new Config(args[0]);
        Translator.process(c);
        println("Done")
    }
}
