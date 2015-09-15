package org.transmart.tm2cbio.utils

/**
 * Created by j.hudecek on 10-11-2014.
 */
class Converter {
    private static String conversionImpl(String s, groovy.lang.Closure<Double> c )
    {
        if (s.startsWith("NA"))
            return "NA"
        double days = parseEuropeanDouble(s)
        return c.call(days).intValue().toString()
    }

    public static String days2months(String s) throws NumberFormatException {
        conversionImpl(s, {it/30})
    }

    public static String days2years(String s) throws NumberFormatException {
        conversionImpl(s, {it/365})
    }

    public static String month2years(String s) throws NumberFormatException {
        conversionImpl(s, {it/12})
    }

    public static String month2days(String s) throws NumberFormatException {
        conversionImpl(s, {it*30})
    }

    public static String year2days(String s) throws NumberFormatException {
        conversionImpl(s, {it*365})
    }

    public static String years2months(String s) throws NumberFormatException {
        conversionImpl(s, {it*12})
    }

    private static double parseEuropeanDouble(String s) {
        double d;
        try {
            d = Double.parseDouble(s);
        }
        catch (NumberFormatException) {
            d = Double.parseDouble(s.replace('.', ','));
        }
        d
    }
}
