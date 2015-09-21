/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.czjvic.runmyscript;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.util.NbPreferences;
import org.openide.windows.IOColorPrint;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;
import org.openide.windows.OutputWriter;

/**
 *
 * @author josefvrba
 */
public class Command implements Runnable {

    String cmd;

    public Command(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public void run() {

        //close previous window
        InputOutput outputWindow = IOProvider.getDefault().getIO("Run My Script", false);
        outputWindow.closeInputOutput();

        //create new output window
        outputWindow = IOProvider.getDefault().getIO("Run My Script", true);
        outputWindow.getOut().println("Starting command: " + this.cmd);

        //show window if enabled in preferences
        if (NbPreferences.forModule(RunMyScriptPanel.class).getBoolean("showWindow", true)) {
            outputWindow.select();
        }

        //prepare enviroment variables
        String enviromentVariable = NbPreferences.forModule(RunMyScriptPanel.class).get("enviroment", "");
        String[] env = {enviromentVariable};

        //run process
        try {
            Process process = Runtime.getRuntime().exec(this.cmd, env);
            inheritIO(process.getInputStream(), process.getErrorStream(), outputWindow);
        } catch (Exception ex) {
            ex.printStackTrace();
            outputWindow.getOut().println("Failed running command. Exception message: " + ex.getMessage());
        }
    }

    /**
     * Process output streams.
     *
     * @param outputStream
     * @param errorStream
     * @param dest
     *
     * @return void
     */
    private static void inheritIO(final InputStream outputStream, final InputStream errorStream, final InputOutput dest) {
        new Thread(new Runnable() {
            public void run() {

                StringBuilder sb = new StringBuilder();
                
                // match path followed with line and column
                Pattern pathPattern = Pattern.compile("\\s*((?:[a-zA-Z]\\:){0,1}(?:[\\\\/].+){1,}):(\\d+):(\\d+)\\s*");
                Matcher matcher = null;

                Scanner sc = new Scanner(outputStream);
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    sb.append(line);
                    matcher = pathPattern.matcher(line);
                    
                    if (matcher.matches()) {
                        final String path = matcher.group(1);
                        final Integer lineNumber = Integer.parseInt(matcher.group(2));
                        final Integer columnNumber = Integer.parseInt(matcher.group(3));
                        try {
                            IOColorPrint.print(dest, line, new OutputListener() {

                                @Override
                                public void outputLineSelected(OutputEvent oe) {
                                    // nothing to do
                                }

                                @Override
                                public void outputLineAction(OutputEvent oe) {
                                    File file = new File(path);
                                    FileObject fobj = FileUtil.toFileObject(file);
                                    DataObject dobj = null;
                                    try {
                                        dobj = DataObject.find(fobj);
                                    } catch (DataObjectNotFoundException ex) {
                                        ex.printStackTrace();
                                    }
                                    if (dobj != null) {
                                        LineCookie lc = dobj.getLookup().lookup(LineCookie.class);
                                        if (lc == null) {
                                            return;
                                        }
                                        Line l = lc.getLineSet().getOriginal(lineNumber);
                                        l.show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.NONE, columnNumber);
                                    }
                                }

                                @Override
                                public void outputLineCleared(OutputEvent oe) {
                                    // nothing to do
                                }
                            }, false, Color.BLUE);
                        } catch (IOException ex) {
                            dest.getOut().println(ex.getMessage());
                        }
                    } else {
                        dest.getOut().println(line);
                    }
                }

                sc = new Scanner(errorStream);
                while (sc.hasNextLine()) {
                    dest.getOut().println(sc.nextLine());
                }
                dest.getOut().println("Command successful finished.");

                try {
                    //if xml parsing enabled, try to parse
                    if (NbPreferences.forModule(RunMyScriptPanel.class).getBoolean("parseXml", false)) {
                        OutputProcessor op = new OutputProcessor(sb.toString().trim());
                    }
                } catch (Exception ex) {
                    dest.getOut().println("XML parse failed. Exception message: " + ex.getMessage());
                }

            }
        }).start();
    }

}
