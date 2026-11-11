package com.code_share.finalapp;

import android.content.Context;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.List;

public final class PythonExecutor {
    private PythonExecutor() {
    }

    public static String execute(Context context, String code) {
        initialize(context);
        Python python = Python.getInstance();
        PyObject module = python.getModule("myhelper");
        if (module == null) {
            return "Error: Python helper module not available.";
        }
        return module.callAttr("main", code).toString();
    }

    public static String executeMethod(Context context, String code, String methodName, List<String> inputs) {
        initialize(context);
        Python python = Python.getInstance();
        PyObject module = python.getModule("myhelper");
        if (module == null) {
            return "Error: Python helper module not available.";
        }

        if (inputs == null) {
            return module.callAttr("execute_method", code, methodName, new java.util.ArrayList<String>()).toString();
        }

        return module.callAttr("execute_method", code, methodName, inputs).toString();
    }

    private static synchronized void initialize(Context context) {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(context.getApplicationContext()));
        }
    }
}
