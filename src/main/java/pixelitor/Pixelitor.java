/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor;

import static java.lang.String.*;
import static pixelitor.tools.pen.PenToolMode.*;

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.UIManager;

import com.bric.util.JVM;

import net.jafama.FastMath;
import pixelitor.colors.FgBgColors;
import pixelitor.colors.FillType;
import pixelitor.filters.Filter;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.View;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.io.IOThread;
import pixelitor.io.OpenSave;
import pixelitor.layers.AddLayerMaskAction;
import pixelitor.layers.AddTextLayerAction;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMaskAddType;
import pixelitor.layers.MaskViewMode;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;
import pixelitor.tools.util.DragDisplay;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Messages;
import pixelitor.utils.Shapes;
import pixelitor.utils.Utils;

/**
 * The main class
 */
public class Pixelitor {
    public static Locale SYS_LOCALE;

    private Pixelitor() {
        // should not be instantiated
    }
    
    private static final int PIXELITOR_PORT = 58779;
	private static void startRPCServer()
	{
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				while(true)
				{
					try(ServerSocket serverSocket = new ServerSocket(PIXELITOR_PORT))
					{
						while(true)
						{
							try
							{
								Socket clientSocket = serverSocket.accept();
								InputStream is = clientSocket.getInputStream();
								byte[] data = is.readAllBytes();
								
								ArrayList<String> args = new ArrayList<>();
								int startArg = -1;
								for(int i = 0; i < data.length; ++i)
								{
									if(startArg == -1 && data[i] != 0)
										startArg = i;
									if(data[i] == 0)
									{
										if(startArg != -1 && (i - startArg) > 0)
											args.add(new String(data, startArg, i - startArg, StandardCharsets.UTF_8));
										startArg = -1;
									}
								}
						
								for(String fileName : args)
								{
									File f = new File(fileName);
						            if (f.exists()) {
						                OpenSave.openFileAsync(f);
						            } else {
						                Messages.showError("File not found",
						                        format("The file \"%s\" does not exist", f.getAbsolutePath()));
						            }
								}
						
								if(!args.isEmpty())
								{
									java.awt.EventQueue.invokeLater(new Runnable()
									{
										@Override
										public void run()
										{
											var pw = PixelitorWindow.getInstance();
											pw.deiconify();
											pw.toFront();
											pw.repaint();
										}
									});
								}
							}
							catch(IOException e)
							{
								e.printStackTrace();
							}
						}
					}
					catch(IOException e1)
					{
						System.out.println(e1.getMessage());
					}
				}
			}
		};
		
		new Thread(r, "Pixelitor RPC").start();
	}

    public static void main(String[] args) {
    	
    	startRPCServer();
    	
        // the app can be put into development mode by
        // adding -Dpixelitor.development=true to the command line
        if ("true".equals(System.getProperty("pixelitor.development"))) {
            Utils.makeSureAssertionsAreEnabled();
            Build.CURRENT = Build.DEVELOPMENT;
        }

        // Force using English locale, because using the default system
        // settings leads to mixed-language problems (see issue #35),
        // but keep the system locale for number formatting
        SYS_LOCALE = Locale.getDefault();
        Locale.setDefault(Locale.US);

        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Pixelitor");

        if (JVM.isLinux) {
            // doesn't seem to pick up good defaults
            System.setProperty("awt.useSystemAAFontSettings", "lcd");
            System.setProperty("swing.aatext", "true");

            if (GraphicsEnvironment.isHeadless()) {
                System.err.println("Pixelitor can't be used in headless mode");
                System.exit(1);
            }
        }

        ExceptionHandler.INSTANCE.addLastHandler((t, e) -> Messages.showException(e, t));

        EventQueue.invokeLater(() -> {
            try {
                createAndShowGUI(args);
            } catch (Exception e) {
                Dialogs.showExceptionDialog(e);
            }
        });

        DragDisplay.initializeFont();

        // Force the initialization of FastMath look-up tables now
        // on the main thread, so that later no unexpected delays happen.
        // This is OK because static initializers are thread safe.
        FastMath.cos(0.1);
    }

    private static void createAndShowGUI(String[] args) {
        assert EventQueue.isDispatchThread() : "not EDT thread";

//        GlobalKeyboardWatch.showEventsSlowerThan(100, TimeUnit.MILLISECONDS);

        setLookAndFeel();

        var pw = PixelitorWindow.getInstance();
        Dialogs.setMainWindowInitialized(true);

        // Just to make 100% sure that at the end of GUI
        // initialization the focus is not grabbed by
        // a textfield and the keyboard shortcuts work properly
        FgBgColors.getGUI().requestFocus();

        TipsOfTheDay.showTips(pw, false);

        // The IO-intensive pre-loading of fonts is scheduled
        // to run after all the files have been opened,
        // and on the same IO thread
        openCLFilesAsync(args)
                .thenAcceptAsync(v -> afterStartTestActions(pw), EventQueue::invokeLater)
                .thenRunAsync(Utils::preloadFontNames,
                        IOThread.getExecutor())
                .exceptionally(Messages::showExceptionOnEDT);
    }

    private static void setLookAndFeel() {
        try {
//            // https://docs.oracle.com/javase/tutorial/uiswing/lookandfeel/color.html
//            UIManager.put("nimbusBase", new ColorUIResource(19, 111, 13));
//            UIManager.put("nimbusBlueGrey", new ColorUIResource(5, 27, 111));
//            UIManager.put("control", new ColorUIResource(111, 0, 18));

            UIManager.setLookAndFeel(
                    "javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            Dialogs.showExceptionDialog(e);
        }
    }

    /**
     * Schedules the opening of the files given as command-line arguments
     */
    private static CompletableFuture<Void> openCLFilesAsync(String[] args) {
        if (args.length == 0) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<?>> openedFiles = new ArrayList<>();

        for (String fileName : args) {
            File f = new File(fileName);
            if (f.exists()) {
                openedFiles.add(OpenSave.openFileAsync(f));
            } else {
                Messages.showError("File not found",
                        format("The file \"%s\" does not exist", f.getAbsolutePath()));
            }
        }

        return Utils.allOfList(openedFiles);
    }

    public static void exitApp(PixelitorWindow pw) {
        assert EventQueue.isDispatchThread();
        var paths = IOThread.getCurrentWritePaths();
        if (!paths.isEmpty()) {
            String msg = "<html>The writing of the following files is not finished yet. Exit anyway?<br><ul>";
            for (String path : paths) {
                msg += "<li>" + path;
            }
            boolean wait = Dialogs.showOKCancelWarningDialog(
                    msg,
                    "Warning", new String[]{"Wait 10 seconds", "Exit now"},
                    0);
            if (wait && IOThread.isBusyWriting()) {
                // wait on another thread so that the status bar
                // can be updated while waiting
                new Thread(() -> {
                    Utils.sleep(10, TimeUnit.SECONDS);
                    EventQueue.invokeLater(() -> exitApp(pw));
                }).start();

                return;
            }
        }

        var unsavedComps = OpenImages.getUnsavedComps();
        if (!unsavedComps.isEmpty()) {
            String msg;
            if (unsavedComps.size() == 1) {
                msg = format("<html>There are unsaved changes in <b>%s</b>." +
                                "<br>Are you sure you want to exit?",
                        unsavedComps.get(0).getName());
            } else {
                msg = "<html>There are unsaved changes. Are you sure you want to exit?" +
                        "<br>Unsaved images:<ul>";
                for (Composition comp : unsavedComps) {
                    msg += "<li>" + comp.getName();
                }
            }

            if (Dialogs.showYesNoWarningDialog(pw, "Unsaved changes", msg)) {
                pw.setVisible(false);
                AppPreferences.savePrefsAndExit();
            }
        } else {
            pw.setVisible(false);
            AppPreferences.savePrefsAndExit();
        }
    }

    /**
     * A possibility for automatic debugging or testing
     */
    private static void afterStartTestActions(PixelitorWindow pw) {
        if (Build.CURRENT == Build.FINAL) {
            // in the final builds nothing should run
            return;
        }

//        SplashImageCreator.saveManySplashImages();

//        addTestPath();

//        keepSwitchingToolsRandomly();
//        startFilter(new Marble());

//        Navigator.showInDialog(pw);

//        Tools.PEN.activate();
        //        addMaskAndShowIt();

//        showAddTextLayerDialog();

//        AutoPaint.showDialog();

//        Tests3x3.addStandardImage(false);

//        ImageComponents.getActiveCV().setZoom(ZoomLevel.Z6400, true);

//        GlobalKeyboardWatch.registerDebugMouseWatching(false);

//        new TweenWizard().start(pw);

//        dispatchKeyPress(pw, true, KeyEvent.VK_T, 'T');
    }

    private static void dispatchKeyPress(PixelitorWindow pw, boolean ctrl, int keyCode, char keyChar) {
        int modifiers;
        if (ctrl) {
            modifiers = InputEvent.CTRL_DOWN_MASK;
        } else {
            modifiers = 0;
        }
        pw.dispatchEvent(new KeyEvent(pw, KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), modifiers, keyCode, keyChar));
    }

    private static void addTestPath() {
        var shape = new Rectangle2D.Double(100, 100, 300, 100);

        Path path = Shapes.shapeToPath(shape, OpenImages.getActiveView());

        Tools.PEN.setPath(path);
        Tools.PEN.startRestrictedMode(EDIT, false);
        Tools.PEN.activate();
    }

    private static void showAddTextLayerDialog() {
        AddTextLayerAction.INSTANCE.actionPerformed(null);
    }

    private static void addMaskAndShowIt() {
        AddLayerMaskAction.INSTANCE.actionPerformed(null);
        View view = OpenImages.getActiveView();
        Layer layer = view.getComp()
                .getActiveLayer();
        MaskViewMode.SHOW_MASK.activate(view, layer, "after-start test");
    }

    private static void startFilter(Filter filter) {
        filter.startOn(OpenImages.getActiveDrawableOrThrow());
    }

    private static void addNewImage() {
        NewImage.addNewImage(FillType.WHITE, 600, 400, "Test");
        OpenImages.getActiveLayer()
                .addMask(LayerMaskAddType.PATTERN);
    }

    private static void keepSwitchingToolsRandomly() {
        Runnable backgroundTask = () -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                Utils.sleep(1, TimeUnit.SECONDS);

                Runnable changeToolOnEDTTask = () -> Tools.getRandomTool().activate();
                GUIUtils.invokeAndWait(changeToolOnEDTTask);
            }
        };
        new Thread(backgroundTask).start();
    }
}
