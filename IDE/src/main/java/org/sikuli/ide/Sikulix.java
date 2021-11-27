/*
 * Copyright (c) 2010-2021, sikuli.org, sikulix.com - MIT license
 */

package org.sikuli.ide;

import org.sikuli.basics.FileManager;
import org.sikuli.basics.HotkeyEvent;
import org.sikuli.basics.HotkeyListener;
import org.sikuli.basics.HotkeyManager;
import org.sikuli.script.SikuliXception;
import org.sikuli.script.runnerSupport.IScriptRunner;
import org.sikuli.script.runnerSupport.Runner;
import org.sikuli.script.support.Commons;
import org.sikuli.script.support.PreferencesUser;
import org.sikuli.script.support.gui.SXDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.sikuli.util.CommandArgsEnum.*;

public class Sikulix {

  static SXDialog ideSplash;
  static int waitStart = 0;

  public static void stopSplash() {
    if (waitStart > 0) {
      try {
        Thread.sleep(waitStart * 1000);
      } catch (InterruptedException e) {
      }
    }

    if (ideSplash != null) {
      ideSplash.setVisible(false);
      ideSplash.dispose();
      ideSplash = null;
    }
  }

  public static void main(String[] args) {
    //region startup
    Commons.setStartClass(Sikulix.class);

    Commons.setStartArgs(args);

    if (Commons.hasStartArg(QUIET)) {
      Commons.setQuiet();
    } else if (Commons.hasStartArg(VERBOSE)) {
      Commons.setVerbose();
    } else if (Commons.hasStartArg(DEBUG)) {
      Commons.setDebug();
    }

    if (Commons.hasStartArg(APPDATA)) {
      File path = Commons.setAppDataPath(Commons.getStartArg(APPDATA));
      Commons.setTempFolder(new File(path, "Temp"));
    } else {
      Commons.setTempFolder();
    }

    Commons.initGlobalOptions();

    if (Commons.hasExtendedArg("reset")) {
      System.out.println("[INFO] IDE: resetting global options and terminating --- see docs");

      if (PreferencesUser.get().getStore().get("USER_TYPE", "0").equals("2")) {
        PreferencesUser.get().getStore().remove("IDE_LOCATION");
        PreferencesUser.get().getStore().remove("IDE_SIZE");
        PreferencesUser.get().getStore().remove("IDE_SESSION");
      } else {
        PreferencesUser.get().setDefaults();
      }

      System.exit(0);
    }

    if (Commons.hasStartArg(HELP)) {
      Commons.printHelp();
      Commons.setDebug();
      Commons.show();
      Commons.showOptions("SX_PREFS_IDE");
      System.exit(0);
    }

    if (Commons.isDebug()) {
      Commons.show();
    }

    if (Commons.hasStartArg(RUN)) {
      HotkeyManager.getInstance().addHotkey("Abort", new HotkeyListener() {
        @Override
        public void hotkeyPressed(HotkeyEvent e) {
          if (Commons.hasStartArg(RUN)) {
            Runner.abortAll();
            Commons.terminate(254, "AbortKey was pressed: aborting all running scripts");
          }
        }
      });
      String[] scripts = Runner.resolveRelativeFiles(Commons.asArray(Commons.getOption("SX_ARG_RUN")));
      int exitCode = Runner.runScripts(scripts, Commons.getUserArgs(), new IScriptRunner.Options());
      if (exitCode > 255) {
        exitCode = 254;
      }
      Commons.terminate(exitCode, "");
    }

    if (Commons.hasStartArg(RUNSERVER)) {
      Class cServer = null;
      try {
        cServer = Class.forName("org.sikuli.script.runners.ServerRunner");
        cServer.getMethod("run").invoke(null);
        Commons.terminate();
      } catch (ClassNotFoundException e) {
      } catch (NoSuchMethodException e) {
      } catch (IllegalAccessException e) {
      } catch (InvocationTargetException e) {
      }
      try {
        cServer = Class.forName("org.sikuli.script.support.SikulixServer");
        if (!(Boolean) cServer.getMethod("run").invoke(null)) {
          Commons.terminate(1, "SikulixServer: terminated with errors");
        }
      } catch (ClassNotFoundException e) {
      } catch (IllegalAccessException e) {
      } catch (InvocationTargetException e) {
      } catch (NoSuchMethodException e) {
      }
      Commons.terminate();
    }

    Commons.debug("IDE starting (%4.1f)", Commons.getSinceStart());
    //endregion


    ideSplash = null;
    if (Commons.isRunningFromJar() || (!Commons.isRunningFromJar() && !Commons.hasStartArg(VERBOSE))) {
      if (!Commons.isQuiet()) {
        ideSplash = new SXDialog("sxidestartup", SikulixIDE.getWindowTop(), SXDialog.POSITION.TOP);
        ideSplash.run();
      }
    }

    if (!Commons.hasStartArg(APPDATA)) {
      File isRunning;
      FileOutputStream isRunningFile;
      String isRunningFilename = "s_i_k_u_l_i-ide-isrunning";
      isRunning = new File(Commons.getTempFolder(), isRunningFilename);
      boolean shouldTerminate = false;
      try {
        isRunning.createNewFile();
        isRunningFile = new FileOutputStream(isRunning);
        if (null == isRunningFile.getChannel().tryLock()) {
          Class<?> classIDE = Class.forName("org.sikuli.ide.SikulixIDE");
          Method stopSplash = classIDE.getMethod("stopSplash", new Class[0]);
          stopSplash.invoke(null, new Object[0]);
          org.sikuli.script.Sikulix.popError("Terminating: IDE already running");
          shouldTerminate = true;
        } else {
          Commons.setIsRunning(isRunning, isRunningFile);
        }
      } catch (Exception ex) {
        org.sikuli.script.Sikulix.popError("Terminating on FatalError: cannot access IDE lock for/n" + isRunning);
        shouldTerminate = true;
      }
      if (shouldTerminate) {
        System.exit(1);
      }
      for (String aFile : Commons.getTempFolder().list()) {
        if ((aFile.startsWith("Sikulix"))
            || (aFile.startsWith("jffi") && aFile.endsWith(".tmp"))) {
          FileManager.deleteFileOrFolder(new File(Commons.getTempFolder(), aFile));
        }
      }
    }

    //region IDE temp folder
    File ideTemp = new File(Commons.getTempFolder(), String.format("Sikulix_%d", FileManager.getRandomInt()));
    ideTemp.mkdirs();
    try {
      File tempTest = new File(ideTemp, "tempTest.txt");
      FileManager.writeStringToFile("temp test", tempTest);
      boolean success = true;
      if (tempTest.exists()) {
        tempTest.delete();
        if (tempTest.exists()) {
          success = false;
        }
      } else {
        success = false;
      }
      if (!success) {
        throw new SikuliXception(String.format("init: temp folder not useable: %s", Commons.getTempFolder()));
      }
    } catch (Exception e) {
      throw new SikuliXception(String.format("init: temp folder not useable: %s", Commons.getTempFolder()));
    }
    Commons.setIDETemp(ideTemp);
    //endregion

    if (Commons.runningMac()) {
      try {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
      } catch (Exception e) {
      }
    }

//    System.setProperty("python.home", "");
//    System.setProperty("python.import.site", "false");

    SikulixIDE.start(args);

    //TODO start IDE in subprocess?
    //region IDE subprocess
    if (false) {
      /*
      if (false) {
        RunTime.terminate(999, "//TODO start IDE in subprocess?");
        List<String> cmd = new ArrayList<>();
        System.getProperty("java.home");
        if (Commons.runningWindows()) {
          cmd.add(System.getProperty("java.home") + "\\bin\\java.exe");
        } else {
          cmd.add(System.getProperty("java.home") + "/bin/java");
        }
        if (!Commons.isJava8()) {
      */
//      Suppress Java 9+ warnings
//      --add-opens
//      java.desktop/javax.swing.plaf.basic=ALL-UNNAMED
//      --add-opens
//      java.base/sun.nio.ch=ALL-UNNAMED
//      --add-opens
//      java.base/java.io=ALL-UNNAMED
/*

//TODO IDE start: --add-opens supress warnings
          cmd.add("--add-opens");
          cmd.add("java.desktop/javax.swing.plaf.basic=ALL-UNNAMED");
          cmd.add("--add-opens");
          cmd.add("java.base/sun.nio.ch=ALL-UNNAMED");
          cmd.add("--add-opens");
          cmd.add("java.base/java.io=ALL-UNNAMED");
        }

        cmd.add("-Dfile.encoding=UTF-8");
        cmd.add("-Dsikuli.IDE_should_run");

        if (!classPath.isEmpty()) {
          cmd.add("-cp");
          cmd.add(classPath);
        }

        cmd.add("org.sikuli.ide.SikulixIDE");
//      cmd.addAll(finalArgs);

        RunTime.startLog(3, "*********************** leaving start");
        //TODO detach IDE: for what does it make sense?
*/
/*
    if (shouldDetach()) {
      ProcessRunner.detach(cmd);
      System.exit(0);
    } else {
      int exitCode = ProcessRunner.runBlocking(cmd);
      System.exit(exitCode);
    }
*/
/*

        int exitCode = ProcessRunner.runBlocking(cmd);
        System.exit(exitCode);
      }
      //endregion
*/
    }
    //endregion
  }
}
