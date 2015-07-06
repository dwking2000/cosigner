package io.emax.heimdal.common;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Utility {
    public static String slurpStream(InputStream input) {
        java.util.Scanner scanner = new java.util.Scanner(input).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    public static String exceptionToString(Exception e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        return stringWriter.toString();
    }
}
