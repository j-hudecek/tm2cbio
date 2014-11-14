package org.transmart.tm2cbio

import org.apache.log4j.Logger

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
            println("File "+args[0]+" doesn't exist");
            return;
        }
        Config c = new Config(args[0]);
        Translator.process(c);
    }
}
