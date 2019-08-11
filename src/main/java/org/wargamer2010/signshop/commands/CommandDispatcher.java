
package org.wargamer2010.signshop.commands;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.wargamer2010.signshop.player.SignShopPlayer;

public class CommandDispatcher {
    private Map<String, ICommandHandler> handlers = new HashMap<>();

    public synchronized void registerHandler(String commandName, ICommandHandler handler) {
        handlers.put(commandName, handler);
    }

    public boolean handle(String command, String[] args, SignShopPlayer player) {
        String lower = command.toLowerCase();
        ICommandHandler handler = handlers.get(lower);
        if (handler == null) {
            handler = handlers.get("");
            command = "";
        } else {
            command = lower;
        }

        return handler.handle(command, args, player);
    }
}
