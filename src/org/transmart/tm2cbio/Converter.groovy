package org.transmart.tm2cbio

/**
 * Created by j.hudecek on 10-11-2014.
 */
class Converter {
    public static String days2months(String s) throws NumberFormatException {
        double days = parseEuropeanDouble(s)
        return (days/30).intValue().toString()
    }

    public static String days2years(String s) throws NumberFormatException {
        double days = parseEuropeanDouble(s)
        return (days/365).intValue().toString()
    }

    public static String month2years(String s) throws NumberFormatException {
        double months = parseEuropeanDouble(s)
        return (months/12).intValue().toString()
    }

    public static String month2days(String s) throws NumberFormatException {
        double months = parseEuropeanDouble(s)
        return (months*30).intValue().toString()
    }

    public static String year2days(String s) throws NumberFormatException {
        double years = parseEuropeanDouble(s)
        return (years*365).intValue().toString()
    }

    public static String years2months(String s) throws NumberFormatException {
        double years = parseEuropeanDouble(s)
        return (years*12).intValue().toString()
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
