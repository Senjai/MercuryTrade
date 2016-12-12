package com.home.clicker.ui;

import com.home.clicker.events.*;
import com.home.clicker.events.custom.*;
import com.home.clicker.pojo.Message;
import com.home.clicker.ui.chat.ChatTab;
import com.home.clicker.ui.misc.SettingsPanel;
import com.pagosoft.plaf.PlafOptions;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.home.clicker.ui.FrameStates.SHOW;

/**
 * Exslims
 * 07.12.2016
 */
public class WindowFrame extends JFrame {
    private static final int CHAT_WIDTH = 400;
    private static final int CHAT_HEIGHT = 200;
    private static final int CHAT_X = 300;
    private static final int CHAT_Y = 300;

    private Dimension screenSize;
    private JTabbedPane chatPanel;
    private JPopupMenu settingsMenu;
    private int x;
    private int y;

    private Map<String,JPanel> whisperChatTabs = new HashMap<>();
    private JPanel settingsPanel;

    public WindowFrame() {
        super("PoeShortCast");

        PlafOptions.setAsLookAndFeel();
        PlafOptions.updateAllUIs();
        UIManager.getLookAndFeelDefaults().put("Menu.arrowIcon", null);

        setLayout(null);
        getRootPane().setOpaque(false);
        setUndecorated(true);

        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screenSize.width, screenSize.height);

        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        setBackground(new Color(0, 0, 0, 0));
        setOpacity(0.9f);
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        setFocusable(false);

        try {
            initSettingsContextMenu();
            add(getChatButton());
            this.chatPanel = createChatPanel();
            add(chatPanel);
            this.settingsPanel = getSettingsPanel();
            add(settingsPanel);

        } catch (IOException e) {
            e.printStackTrace();
        }
        registerUIHandlers();
    }

    private JTabbedPane createChatPanel(){
        JTabbedPane chat = new JTabbedPane();
        chat.setPreferredSize(new Dimension(CHAT_WIDTH,CHAT_HEIGHT));
        chat.setSize(new Dimension(CHAT_WIDTH,CHAT_HEIGHT));
        chat.setLocation(CHAT_X,CHAT_Y);
        chat.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                x = e.getX();
                y = e.getY();
            }
        });
        chat.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                e.translatePoint(e.getComponent().getLocation().x - x,e.getComponent().getLocation().y - y);
                chat.setLocation(e.getX(),e.getY());
            }
        });
        chat.setVisible(false);
        return chat;
    }

    private JButton getChatButton() throws IOException {
        BufferedImage buttonIcon = ImageIO.read(getClass().getClassLoader().getResource("chatImage.png"));
        BufferedImage icon = Scalr.resize(buttonIcon, 40);
        JButton button = new JButton(new ImageIcon(icon));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(50,50));
        button.setSize(new Dimension(50,50));
        button.setLocation(30,screenSize.height - 50);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e)) {
                    if (chatPanel.isVisible()) {
                        changeState(FrameStates.HIDE);
                    } else {
                        changeState(SHOW);
                    }
                }
            }
        });
        button.setComponentPopupMenu(settingsMenu);
        button.setFocusable(false);
        return button;
    }

    private void registerUIHandlers(){
        EventRouter.registerHandler(ActualWritersChangeEvent.class, event -> {
            List<Message> messages = ((ActualWritersChangeEvent)event).getMessages();
            for (Message message : messages) {
                ChatTab existChat = (ChatTab)whisperChatTabs.get(message.getWhisperNickname());
                if(existChat == null){
                    JPanel chatTab = new ChatTab(message.getWhisperNickname(),message.getMessage());
                    chatTab.setMinimumSize(chatPanel.getPreferredSize());
                    chatPanel.addTab(message.getWhisperNickname(),chatTab);
                    chatPanel.setSelectedComponent(chatTab);

                    whisperChatTabs.put(message.getWhisperNickname(),chatTab);
                }else {
                    chatPanel.add(existChat.getWhisper(),existChat);
                    chatPanel.setSelectedComponent(existChat);
                    existChat.addNewMessage(message.getMessage());
                }
            }
        });
        EventRouter.registerHandler(RemoveChatEvent.class, event -> {
            String whisperNickname = ((RemoveChatEvent) event).getWhisperNickname();
            int tabCount = chatPanel.getTabCount();
            if(tabCount > 1) {
                for (int i = 0; i < chatPanel.getTabCount(); i++) {
                    String tabTitle = chatPanel.getTitleAt(i);
                    if (whisperNickname.equals(tabTitle)) {
                        chatPanel.remove(i);
                        whisperChatTabs.remove(whisperNickname);
                        break;
                    }
                }
            }else {
                chatPanel.remove(0);
                changeState(FrameStates.HIDE);
            }
        });

        EventRouter.registerHandler(StateChangeEvent.class, event -> {
            changeState(((StateChangeEvent)event).getState());
        });
        //todo
        EventRouter.registerHandler(NewPatchSCEvent.class, event -> {
            JFrame frame = new JFrame("New patch");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            JLabel label = new JLabel(((NewPatchSCEvent)event).getPatchTitle());
            frame.getContentPane().add(label);
            frame.pack();
            frame.setVisible(true);
        });

        EventRouter.registerHandler(ChangeFrameVisibleEvent.class, new SCEventHandler<ChangeFrameVisibleEvent>() {
            @Override
            public void handle(ChangeFrameVisibleEvent event) {
                switch (event.getStates()){
                    case SHOW:{
                        if(!WindowFrame.this.isShowing()) {
                            WindowFrame.this.setVisible(true);
                        }
                    }
                    break;
                    case HIDE:{
                        if(WindowFrame.this.isShowing()) {
                            WindowFrame.this.setVisible(false);
                        }
                    }
                    break;
                }
            }
        });
    }

    private void initSettingsContextMenu(){
        settingsMenu = new JPopupMenu("Popup");
        JMenuItem item = new JMenu("settings");
        item.setHorizontalTextPosition(JMenuItem.CENTER);
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e)){
                    System.out.println("SettingsPanel visible: " + settingsPanel.isShowing());
                    settingsPanel.setVisible(true);
                }
            }
        });

        JMenuItem exit = new JMenu("Exit program");
        exit.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.exit(0);
            }
        });
        exit.setHorizontalTextPosition(JMenuItem.CENTER);
        exit.setArmed(false);
        settingsMenu.add(item);
        settingsMenu.add(exit);
    }

    private JPanel getSettingsPanel() {
        SettingsPanel sPanel = new SettingsPanel();
        sPanel.setVisible(false);
        sPanel.setPreferredSize(new Dimension(200,50));
        sPanel.setSize(new Dimension(200,50));
        sPanel.setLocation(500,500);
        return sPanel;
    }
    private void changeState(FrameStates states){
        switch (states){
            case SHOW: {
                chatPanel.setVisible(true);
                break;
            }
            case HIDE:{
                chatPanel.setVisible(false);
            }
            break;
            case UNDEFINED:{
                if(chatPanel.isVisible()){
                    chatPanel.setVisible(false);
                }else
                    chatPanel.setVisible(true);
            }
        }
    }
}