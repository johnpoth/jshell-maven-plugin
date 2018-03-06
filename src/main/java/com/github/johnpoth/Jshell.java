package com.github.johnpoth;

import javax.tools.Tool;

import java.util.Iterator;
import java.util.ServiceLoader;

public class Jshell {

    public static void main(String[]args) {
        ServiceLoader<Tool> sl = ServiceLoader.load(javax.tools.Tool.class);
        Iterator<Tool> iter = sl.iterator();
        Tool jshell = getJshell(iter);
        jshell.run(System.in, System.out, System.err);
    }

    private static Tool getJshell(Iterator<Tool> iter) {
        while (iter.hasNext()) {
            Tool next = iter.next();
            if(next.name().equals("jshell")){
                return next;
            }
        }
        throw new RuntimeException("No JShell service providers found!");
    }
}
