package com.masscustsoft.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class LogStream  extends ByteArrayOutputStream {
    
    private String lineSeparator, level;
    
    /**
     * Constructor
     * @param level Level at which to write the log message
     */
    public LogStream(String level) {
        super();
        this.level=level;
        lineSeparator = System.getProperty("line.separator");
    }
    
    /**
     * upon flush() write the existing contents of the OutputStream to the logger as 
     * a log record.
     * @throws java.io.IOException in case of error
     */
    @Override
    public void flush() throws IOException {

        String record;
        synchronized(this) {
            super.flush();
            record = this.toString();
            super.reset();
        }
        
        if (record.length() == 0 || record.equals(lineSeparator)) {
            // avoid empty records
            return;
        }

        LogUtil.log(level,record);
    }
}
