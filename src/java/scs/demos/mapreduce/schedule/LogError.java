package scs.demos.mapreduce.schedule;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LogError {
    public static String getStackTrace(Throwable t)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }
}
