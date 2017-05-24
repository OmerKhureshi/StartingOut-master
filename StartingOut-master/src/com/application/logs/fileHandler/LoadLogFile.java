package com.application.logs.fileHandler;

import com.application.db.DatabaseUtil;
import com.application.logs.parsers.FileParser;
import com.application.logs.parsers.ParseCallTrace;
import com.application.logs.parsers.ParseMethodDefinition;
import org.apache.derby.iapi.db.Database;

import java.io.File;
import java.sql.Date;
import java.sql.SQLException;

public class LoadLogFile {
    FileParser fileParser = null;
    File logFile = null;

    public enum LogType {
        MethodDefn, CallTrace
    }

    // public void load(LogType logType) {
    //     if (logType == LogType.MethodDefn) {
    //         new ParseMethodDefinition().readFile(MethodDefinitionLogFile.getFile());
    //     } else if (logType == LogType.CallTrace) {
    //         new ParseCallTrace().readFile(CallTraceLogFile.getFile());
    //     }
    // }

    public static void main(String[] args) {

    }
}
