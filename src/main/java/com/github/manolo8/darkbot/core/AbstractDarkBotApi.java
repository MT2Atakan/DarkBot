package com.github.manolo8.darkbot.core;

import com.sun.jna.platform.win32.User32;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public abstract class AbstractDarkBotApi implements IDarkBotAPI {

    protected static final User32 USER_32 = User32.INSTANCE;

    @Override
    public void keyboardClick(Character ch) {
        if (ch != null) keyboardClick((char) ch);
    }

    public String readMemoryString(long address) {
        int flags = readMemoryInt(address + 36);
        int width = (flags & 0x00000001);
        int size = readMemoryInt(address + 32) << width;
        int type = (flags & 0x00000006) >> 1;

        if (size > 1024 || size < 0) return "ERROR";

        byte[] bytes;

        if (type == 2)
            bytes = readMemory(readMemoryLong(readMemoryLong(address + 24) + 16) + readMemoryInt(address + 16), size);
        else
            bytes = readMemory(readMemoryLong(address + 16), size);

        return width == 0 ? new String(bytes, StandardCharsets.ISO_8859_1) : new String(bytes, StandardCharsets.UTF_16LE);
    }

    public void handleRefresh() {
        refresh();
    }
    public abstract void refresh();

}

class LoggingAPIHandler implements InvocationHandler {

    private DarkBotAPI API;

    LoggingAPIHandler(DarkBotAPI API) {
        this.API = API;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        boolean log = method.getName().startsWith("write") && !method.getName().equals("writeMemoryDouble");
        if (log) {
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            for (int i = 3; i < trace.length - 3; i++) System.out.println(trace[i]);
            System.out.println("API CALL: " + method.getName() + (args != null ? Arrays.toString(args) : ""));
        }
        Object res = method.invoke(API, args);
        if (res != null && log) System.out.println("  -> " + res);
        return res;
    }

}