package com.home.clicker;

import com.home.clicker.events.EventRouter;
import com.home.clicker.events.custom.ChangeFrameVisibleEvent;
import com.home.clicker.events.custom.StateChangeEvent;
import com.home.clicker.events.custom.ChatCommandEvent;
import com.home.clicker.misc.WhisperNotifierStatus;
import com.home.clicker.ui.FrameStates;
import com.home.clicker.misc.WhisperNotifier;
import com.home.clicker.utils.FileMonitor;
import com.home.clicker.utils.LoggedMessagesUtils;
import com.home.clicker.utils.PoeShortCastSettings;
import com.home.clicker.utils.User32;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import lc.kra.system.keyboard.GlobalKeyboardHook;
import lc.kra.system.keyboard.event.GlobalKeyAdapter;
import lc.kra.system.keyboard.event.GlobalKeyEvent;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Exslims
 * 07.12.2016
 */
public class PrivateMessageManager {
    private GlobalKeyboardHook keyboardHook = new GlobalKeyboardHook();
    private GlobalKeyAdapter adapter;
    private User32 user32 = User32.INSTANCE;
    private Robot robot;
    private boolean altPressed = false;
    private WinDef.HWND cachedWindow;

    public PrivateMessageManager() {
        GlobalKeyAdapter adapter = getAdapter();
        this.adapter = adapter;

        EventRouter.registerHandler(ChatCommandEvent.class, event
                -> execute(((ChatCommandEvent)event).getMessage()));
        keyboardHook.addKeyListener(adapter);
        new WhisperNotifier();
        new LoggedMessagesUtils();
        new FileMonitor();

        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        //TODO UBERI NAHUI ETO
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                byte[] windowText = new byte[512];
                PointerType hwnd = user32.GetForegroundWindow();
                User32.INSTANCE.GetWindowTextA(hwnd, windowText, 512);
                if(!Native.toString(windowText).equals("Path of Exile") &&
                        !Native.toString(windowText).equals("PoeShortCast")){
                    EventRouter.fireEvent(new ChangeFrameVisibleEvent(FrameStates.HIDE));
                    PoeShortCastSettings.APP_STATUS = FrameStates.HIDE;
                }else{
                    EventRouter.fireEvent(new ChangeFrameVisibleEvent(FrameStates.SHOW));
                    PoeShortCastSettings.APP_STATUS = FrameStates.SHOW;
                }
            }
        },0,1000);
    }

    private void execute(String message) {
        keyboardHook.removeKeyListener(adapter);
        EventRouter.fireEvent(new StateChangeEvent(FrameStates.HIDE));
        StringSelection selection = new StringSelection(message);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);

        setForegroundWindow("Path of Exile");

        robot.keyRelease(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_F2);

        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);

        keyboardHook.addKeyListener(adapter);
    }


    private GlobalKeyAdapter getAdapter(){
        return new GlobalKeyAdapter() {
            @Override
            public void keyPressed(GlobalKeyEvent event) {
                switch (event.getVirtualKeyCode()){
                    case GlobalKeyEvent.VK_F2: {
                        EventRouter.fireEvent(new StateChangeEvent(FrameStates.SHOW));
                    }
                    break;
                    case GlobalKeyEvent.VK_F3: {
                        EventRouter.fireEvent(new StateChangeEvent(FrameStates.UNDEFINED));
                    }
                    break;
                    case GlobalKeyEvent.VK_TAB: {
//                        if(!altPressed) {
//                            nextTab();
//                        }
                    }
                    break;
                    case GlobalKeyEvent.VK_LMENU: {
                        altPressed = true;
                    }
                    break;

                }
            }
            @Override
            public void keyReleased(GlobalKeyEvent event) {
                switch (event.getVirtualKeyCode()){
                    case GlobalKeyEvent.VK_LMENU: {
                        altPressed = false;
                    }
                    break;
                    case GlobalKeyEvent.VK_F2: {
                        EventRouter.fireEvent(new StateChangeEvent(FrameStates.HIDE));
                    }

                }
            }
        };
    }

    public void setForegroundWindow(final String titleName){
//        if(cachedWindow == null) {
            user32.EnumWindows(new WinUser.WNDENUMPROC() {
                int count = 0;

                public boolean callback(WinDef.HWND hWnd, Pointer arg1) {
                    byte[] windowText = new byte[512];
                    user32.GetWindowTextA(hWnd, windowText, 512);
                    String wText = Native.toString(windowText);

                    if (wText.isEmpty()) {
                        return true;
                    }
                    if (wText.equals(titleName)) {
                        cachedWindow = hWnd;
                        user32.SetForegroundWindow(hWnd);
                        return false;
                    }
                    return true;
                }
            }, null);
//        }else {
//            user32.SetForegroundWindow(cachedWindow);
//        }
    }

    private void nextTab(){
        robot.keyPress(KeyEvent.VK_RIGHT);
        robot.keyRelease(KeyEvent.VK_RIGHT);
    }

}