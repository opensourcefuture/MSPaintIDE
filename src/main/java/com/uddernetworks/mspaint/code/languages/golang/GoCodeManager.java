package com.uddernetworks.mspaint.code.languages.golang;

import com.uddernetworks.mspaint.cmd.Commandline;
import com.uddernetworks.mspaint.code.execution.CompilationResult;
import com.uddernetworks.mspaint.code.execution.DefaultCompilationResult;
import com.uddernetworks.mspaint.code.execution.DefaultRunningCode;
import com.uddernetworks.mspaint.imagestreams.ConsoleManager;
import com.uddernetworks.mspaint.imagestreams.ImageOutputStream;
import com.uddernetworks.mspaint.main.MainGUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;

public class GoCodeManager {

    private static Logger LOGGER = LoggerFactory.getLogger(GoCodeManager.class);
    private GoLanguage language;


    public GoCodeManager(GoLanguage language) {
        this.language = language;
    }

    public DefaultCompilationResult executeCode(MainGUI mainGUI, ImageOutputStream imageOutputStream, ImageOutputStream compilerStream) {
        mainGUI.setIndeterminate(true);

        compilerStream.changeColor(Color.RED);
        PrintStream compilerOut = new PrintStream(compilerStream);
        PrintStream programOut = new PrintStream(imageOutputStream);

        ConsoleManager.setAll(new PrintStream(compilerOut));

        var input = this.language.getLanguageSettings().<File>getSetting(GoOptions.INPUT_DIRECTORY);

        if (this.language.getLanguageSettings().getSetting(GoOptions.COMPILE)) {
            LOGGER.info("Compiling...");
            mainGUI.setStatusText("Compiling...");
            var start = System.currentTimeMillis();
            Commandline.runLiveCommand(Arrays.asList("go", "install", "-v", "-x", "./..."), input, "Golang");
            LOGGER.info("Compilation completed in {}ms", System.currentTimeMillis() - start);
        }

        if (this.language.getLanguageSettings().<Boolean>getSetting(GoOptions.EXECUTE)) new DefaultCompilationResult(CompilationResult.Status.COMPILE_COMPLETE);

        var runningFileOptional = this.language.getLanguageSettings().<File>getSettingOptional(GoOptions.RUNNING_FILE);

        // Puts .java files into this directory, which are then compiled from here

        LOGGER.info("Executing...");
        mainGUI.setStatusText("Executing...");
        final var programStart = System.currentTimeMillis();

        var runningCodeManager = mainGUI.getStartupLogic().getRunningCodeManager();
        runningCodeManager.runCode(new DefaultRunningCode(() -> {
            ConsoleManager.setAll(programOut);
            var runningString = "./...";
            if (runningFileOptional.isPresent()) runningString = input.toPath().relativize(runningFileOptional.get().toPath()).toString();
            return Commandline.runLiveCommand(Arrays.asList("go", "run", runningString), input, "Golang");
        }).afterSuccess(exitCode -> {
            if (exitCode < 0) {
                LOGGER.info("Forcibly terminated after " + (System.currentTimeMillis() - programStart) + "ms");
            } else {
                LOGGER.info("Executed " + (exitCode > 0 ? "with errors " : "") + "in " + (System.currentTimeMillis() - programStart) + "ms");
            }
        }).afterError(message -> {
            LOGGER.info("Program stopped for the reason: " + message);
        }).afterAll((exitCode, ignored) -> {
            mainGUI.setStatusText("");
        }));

        return new DefaultCompilationResult(CompilationResult.Status.RUNNING);
    }

}