package com.oddlabs.tt.net;

import com.oddlabs.tt.gui.InfoPrinter;
import com.oddlabs.tt.util.Utils;

import java.util.*;
import java.util.ResourceBundle;

public final strictfp class ChatCommand {
    private static final Map commands = new HashMap();

    private static final Set ignored_nicks = new HashSet();

    private static final ResourceBundle bundle =
            ResourceBundle.getBundle(ChatCommand.class.getName());

    static {
        try {
            ChatMethod send_message =
                    new ChatMethod() {
                        public final void execute(InfoPrinter info_printer, String text) {
                            sendMessage(info_printer, text);
                        }
                    };
            commands.put("message", send_message);
            commands.put("msg", send_message);
            commands.put("tell", send_message);
            commands.put("whisper", send_message);
            ChatMethod get_info =
                    new ChatMethod() {
                        public final void execute(InfoPrinter info_printer, String text) {
                            getInfo(info_printer, text);
                        }
                    };
            commands.put("info", get_info);
            commands.put("finger", get_info);
            ChatMethod ignore_nick =
                    new ChatMethod() {
                        public final void execute(InfoPrinter info_printer, String text) {
                            ignore(info_printer, text);
                        }
                    };
            commands.put("ignore", ignore_nick);
            ChatMethod unignore_nick =
                    new ChatMethod() {
                        public final void execute(InfoPrinter info_printer, String text) {
                            unignore(info_printer, text);
                        }
                    };
            commands.put("unignore", unignore_nick);
            ChatMethod ignore_list =
                    new ChatMethod() {
                        public final void execute(InfoPrinter info_printer, String text) {
                            ignoreList(info_printer, text);
                        }
                    };
            commands.put("ignorelist", ignore_list);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final boolean filterCommand(InfoPrinter info_printer, String text) {
        return filterCommand(info_printer, null, text);
    }

    public static final boolean filterCommand(
            InfoPrinter info_printer, Map custom_commands, String text) {
        if (!text.startsWith("/")) return false;
        int fist_space = firstSpace(text);
        String cmd = text.substring(1, fist_space);
        String args = text.substring(fist_space, text.length()).trim();
        ChatMethod method = custom_commands != null ? (ChatMethod) custom_commands.get(cmd) : null;
        if (method == null) method = (ChatMethod) commands.get(cmd);
        if (method != null) {
            method.execute(info_printer, args);
        } else {
            String unknown_cmd_message =
                    Utils.getBundleString(bundle, "unknown_command", new Object[] {cmd});
            info_printer.print(unknown_cmd_message);
        }
        return true;
    }

    private static final int firstSpace(String text) {
        int fist_space = text.indexOf(" ");
        if (fist_space == -1) return text.length();
        else return fist_space;
    }

    private static final void sendMessage(InfoPrinter info_printer, String text) {
        int first_space = firstSpace(text);
        String nick = text.substring(0, first_space);
        String message = text.substring(first_space, text.length()).trim();
        if (!Network.getMatchmakingClient().isConnected())
            info_printer.print(Utils.getBundleString(bundle, "not_connected"));
        else
            Network.getMatchmakingClient()
                    .sendPrivateMessage(info_printer.getGUIRoot(), nick, message);
    }

    private static final void getInfo(InfoPrinter info_printer, String text) {
        int first_space = firstSpace(text);
        String nick = text.substring(0, first_space);
        if (!Network.getMatchmakingClient().isConnected())
            info_printer.print(Utils.getBundleString(bundle, "not_connected"));
        else Network.getMatchmakingClient().requestInfo(info_printer.getGUIRoot(), nick);
    }

    public static final void ignore(InfoPrinter info_printer, String text) {
        int first_space = firstSpace(text);
        String nick = text.substring(0, first_space);
        boolean result = ignored_nicks.add(nick.toLowerCase());
        if (result) {
            String msg = Utils.getBundleString(bundle, "ignoring", new Object[] {nick});
            info_printer.print(msg);
        }
    }

    public static final void unignore(InfoPrinter info_printer, String text) {
        int first_space = firstSpace(text);
        String nick = text.substring(0, first_space);
        boolean result = ignored_nicks.remove(nick.toLowerCase());
        if (result) {
            String msg = Utils.getBundleString(bundle, "unignoring", new Object[] {nick});
            info_printer.print(msg);
        }
    }

    public static boolean isIgnoring(String nick) {
        return ignored_nicks.contains(nick.toLowerCase());
    }

    private static final void ignoreList(InfoPrinter info_printer, String text) {
        String[] nicks = new String[ignored_nicks.size()];
        ignored_nicks.toArray(nicks);
        String result;
        if (nicks.length == 0) {
            result = Utils.getBundleString(bundle, "ignore_list_empty");
        } else {
            result = Utils.getBundleString(bundle, "ignore_list");
            for (int i = 0; i < nicks.length; i++) result += " " + nicks[i];
        }
        info_printer.print(result);
    }
}
