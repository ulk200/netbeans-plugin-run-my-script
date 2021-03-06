package org.czjvic.runmyscript;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.IOProvider;
import org.openide.windows.TopComponent;
import org.openide.loaders.DataObject;
import org.openide.filesystems.FileUtil;
import org.openide.windows.InputOutput;
import org.openide.util.NbPreferences;

@ActionID(
        category = "Debug",
        id = "org.czjvic.runmyscript.RunMyScriptAction"
)
@ActionRegistration(
        iconBase = "org/czjvic/runmyscript/Actions-system-run-icon.png",
        displayName = "Run My Script"
)
@ActionReferences({
    @ActionReference(path = "Menu/Source", position = 850, separatorBefore = 845),
    @ActionReference(path = "Shortcuts", name = "DS-F12")
})
@Messages("CTL_RunMyScriptAction=Run My Script")
public final class RunMyScriptAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        TopComponent activeTC = TopComponent.getRegistry().getActivated();
        DataObject dataLookup = activeTC.getLookup().lookup(DataObject.class);
        String currentFilePath = FileUtil.toFile(dataLookup.getPrimaryFile()).getAbsolutePath();

        String scriptPath = NbPreferences.forModule(RunMyScriptPanel.class).get("path", "");
        String cmd = scriptPath.replace("$CURRENT_FILE$", currentFilePath);

        InputOutput outputWindow = IOProvider.getDefault().getIO("Run My Script", false);
        outputWindow.closeInputOutput();        
        outputWindow = IOProvider.getDefault().getIO("Run My Script", true);
        
        outputWindow.getOut().println("Starting command: " + cmd);
        outputWindow.select();
               
        String enviromentVariable = NbPreferences.forModule(RunMyScriptPanel.class).get("enviroment", "");

        String[] env = {enviromentVariable};
        try {
            Process process = Runtime.getRuntime().exec(cmd, env);

            BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            String s = null;
            while ((s = stdOut.readLine()) != null) {
                outputWindow.getOut().println(s);
            }

            while ((s = stdError.readLine()) != null) {
                outputWindow.getOut().println(s);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            outputWindow.getOut().println("Failed running command. Exception message: " + ex.getMessage());
        }

    }
    //https://platform.netbeans.org/tutorials/nbm-options.html
}
