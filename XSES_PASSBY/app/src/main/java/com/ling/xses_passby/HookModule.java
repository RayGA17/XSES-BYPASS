package com.ling.xses_passby;

import android.content.Context;

import java.lang.reflect.Method;
import java.util.HashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookModule implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private static final String TAG = "BypassWorkflow ==> ";

    private static final String BUILD_CLASS = "android.os.Build";
    private static final String SYSTEM_SERVICE_MANAGER = "com.android.server.SystemServiceManager";
    private static final String AMS_LIFECYCLE_CLASS = "com.android.server.am.ActivityManagerService$Lifecycle";
    private static final String AMS_CLASS = "com.android.server.am.ActivityManagerService";
    private static final String PROCESS_RECORD_CLASS = "com.android.server.am.ProcessRecord";
    private static final String INSTALLER_CLASS = "com.android.server.pm.Installer";
    private static final String PMS_CLASS = "com.android.server.power.PowerManagerService";
    private static final String XSES_SERVICE_CLASS = "com.android.server.xgseserver.xss.xsesService";
    private static final String XTC_BEHAVIOR_BUILDER_CLASS = "com.xtc.behavior.XtcBehavior$Builder";

    // --- Flags ---
    private static volatile boolean isInstallerHooked = false;
    private static volatile boolean isAmsHooked = false;
    private static volatile boolean isPmsHooked = false;
    private static volatile boolean isXsesHooked = false;
    private static volatile boolean isBehaviorHooked = false;

    private static void logClassMethods(Class<?> clazz, String className) {
        if (clazz == null) return;
        XposedBridge.log(TAG + "Debug: Scanning methods for " + className);
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            StringBuilder paramTypes = new StringBuilder();
            for (Class<?> paramType : method.getParameterTypes()) {
                paramTypes.append(paramType.getName()).append(",");
            }
            if (paramTypes.length() > 0) paramTypes.setLength(paramTypes.length() - 1);
            XposedBridge.log(TAG + "  Method: " + method.getName() + "(" + paramTypes + ") -> " + method.getReturnType().getName());
        }
    }

    private static void hookInstaller(ClassLoader classLoader) {
        try {
            final Class<?> installerClass = XposedHelpers.findClassIfExists(INSTALLER_CLASS, classLoader);
            if (installerClass == null) {
                XposedBridge.log(TAG + "   Oi    ### P 4: INSTALLER CLS NOT FOUND SKIP");
                return;
            }
            XposedHelpers.findAndHookMethod(installerClass, "xseCheckDeStatus", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    XposedBridge.log(TAG + "--- INTERCEPT: INSTALLER.XSECHECKDESTATUS BYPASS TRUE");
                    param.setResult(true);
                }
            });
            XposedBridge.log(TAG + "P 4: INSTALLER HOOK SUCCESS");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "   Oi    ### P 4: INSTALLER HOOK FAIL " + t);
            Class<?> installerClass = XposedHelpers.findClassIfExists(INSTALLER_CLASS, classLoader);
            logClassMethods(installerClass, INSTALLER_CLASS);
        }
    }

    private static void hookActivityManagerService(ClassLoader classLoader) {
        try {
            final Class<?> amsClass = XposedHelpers.findClassIfExists(AMS_CLASS, classLoader);
            if (amsClass == null) {
                XposedBridge.log(TAG + "   Oi    ### P 4: AMS CLS NOT FOUND SKIP");
                return;
            }
            XposedBridge.log(TAG + "P 4: AMS MULTI HOOK START");


            XposedHelpers.findAndHookMethod(amsClass, "xseException", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + "HOOKED xseException method");
                    param.setResult(null);
                }
            });
            XposedBridge.log(TAG + "AMS XSE HOOK SUCCESS");


            final Class<?> processRecordClass = XposedHelpers.findClassIfExists(PROCESS_RECORD_CLASS, classLoader);
            if (processRecordClass == null) {
                XposedBridge.log(TAG + "   Oi    ### P 4: PROCESSRECORD CLS NOT FOUND SKIP UNCERTIFIED");
            } else {
                XposedHelpers.findAndHookMethod(amsClass, "isUnCertifiedAppProc", processRecordClass, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(TAG + "--- INTERCEPT: AMS.ISUNCERTIFIEDAPPPROC BYPASS FALSE");
                        param.setResult(false);
                    }
                });
                XposedBridge.log(TAG + "P 4: AMS.ISUNCERTIFIEDAPPPROC HOOK SUCCESS");
            }

            XposedBridge.log(TAG + "P 4: AMS MULTI HOOK SUCCESS");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "   Oi    ### P 4: AMS HOOK FAIL " + t);
            Class<?> amsClass = XposedHelpers.findClassIfExists(AMS_CLASS, classLoader);
            logClassMethods(amsClass, AMS_CLASS);
        }
    }

    private static void hookPowerManagerService(ClassLoader classLoader) {
        try {
            final Class<?> pmsClass = XposedHelpers.findClassIfExists(PMS_CLASS, classLoader);
            if (pmsClass == null) {
                XposedBridge.log(TAG + "   Oi    ### P 4: PMS CLS NOT FOUND SKIP");
                return;
            }
            // 改为 hook shutdownOrRebootInternal
            XposedHelpers.findAndHookMethod(pmsClass, "shutdownOrRebootInternal", int.class, boolean.class, String.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String reason = (String) param.args[2];  // reason 是第三个参数
                    if ("xsebootloader".equals(reason)) {
                        XposedBridge.log(TAG + "--- INTERCEPT: PMS.SHUTDOWNORREBOOT XSEBOOTLOADER BLOCKED");
                        param.setResult(null);
                    }
                }
            });
            XposedBridge.log(TAG + "P 4: PMS HOOK SUCCESS (using shutdownOrRebootInternal)");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "   Oi    ### P 4: PMS HOOK FAIL " + t);
            Class<?> pmsClass = XposedHelpers.findClassIfExists(PMS_CLASS, classLoader);
            logClassMethods(pmsClass, PMS_CLASS);
        }
    }

    private static void hookXsesService(ClassLoader classLoader) {
        try {
            final Class<?> xsesClass = XposedHelpers.findClassIfExists(XSES_SERVICE_CLASS, classLoader);
            if (xsesClass == null) {
                XposedBridge.log(TAG + "   Oi    ### P 4: XSES CLS NOT FOUND SKIP");
                return;
            }
            XposedHelpers.findAndHookMethod(xsesClass, "isDRed", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    XposedBridge.log(TAG + "--- INTERCEPT: XSES.ISDRED BYPASS FALSE");
                    param.setResult(false);
                }
            });
            XposedHelpers.findAndHookMethod(xsesClass, "processServerPush", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    XposedBridge.log(TAG + "--- INTERCEPT: XSES.PROCESSSERVERPUSH BLOCKED");
                    param.setResult(null);
                }
            });
            XposedBridge.log(TAG + "P 4: XSES HOOK SUCCESS");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "   Oi    ### P 4: XSES HOOK FAIL " + t);
            Class<?> xsesClass = XposedHelpers.findClassIfExists(XSES_SERVICE_CLASS, classLoader);
            logClassMethods(xsesClass, XSES_SERVICE_CLASS);
        }
    }

    private static void hookBehaviorReporting(ClassLoader classLoader) {
        if (isBehaviorHooked) return;

        try {
            final Class<?> behaviorClass = XposedHelpers.findClassIfExists(XTC_BEHAVIOR_BUILDER_CLASS, classLoader);
            if (behaviorClass == null) {
                if (android.os.Process.myUid() < 2000) {
                    XposedBridge.log(TAG + "   Oi    ### BEHAV CLS NOT FOUND SKIP");
                }
                return;
            }
            // 改为 HashMap
            XposedHelpers.findAndHookConstructor(behaviorClass, Context.class, String.class, HashMap.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args[1] == null || param.args[2] == null) return;
                    String eventName = (String) param.args[1];
                    HashMap<String, String> payload = (HashMap<String, String>) param.args[2];

                    if ("System_RestartMonitor".equals(eventName)) {
                        if (payload.containsKey("veritymode") && !"enforcing".equals(payload.get("veritymode"))) {
                            XposedBridge.log(TAG + "--- CENSOR: SYS_RESTART_MOD VERITYMODE TO ENFORCING");
                            payload.put("veritymode", "enforcing");
                        }
                    } else if ("secure_exception_info".equals(eventName) || "secure_device_root".equals(eventName)) {
                        XposedBridge.log(TAG + "--- CENSOR: SENSITIVE REPORT " + eventName + " BLOCKED");
                        param.setResult(null);
                    }
                }
            });
            isBehaviorHooked = true;
            if (android.os.Process.myUid() < 2000) {
                XposedBridge.log(TAG + "BEHAV HOOK SUCCESS");
            }
        } catch (Throwable t) {
            if (android.os.Process.myUid() < 2000) {
                XposedBridge.log(TAG + "   Oi    ### BEHAV HOOK FAIL " + t);
                Class<?> behaviorClass = XposedHelpers.findClassIfExists(XTC_BEHAVIOR_BUILDER_CLASS, classLoader);
                logClassMethods(behaviorClass, XTC_BEHAVIOR_BUILDER_CLASS);
            }
        }
    }

    private static String detectFramework() {
        try {
            Class.forName("org.lsposed.lspd.service.BridgeService");
            return "LSPOSED";
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("de.robv.android.xposed.EdXposedBridge");
            return "EDXPOSED";
        } catch (ClassNotFoundException ignored) {
        }
        if (XposedBridge.getXposedVersion() > 0) {
            return "XPOSED";
        }
        return "UNKNOWN";
    }

    private static boolean isLsposedCompatible() {
        return "LSPOSED".equals(detectFramework());
    }

    private static boolean isModuleInScope(XC_LoadPackage.LoadPackageParam lpparam) {
        return "android".equals(lpparam.packageName);
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XposedBridge.log(TAG + "P 0: ZYGOTE INIT RDY");

        XposedBridge.log(TAG + "P 0.1: BUILD MOD START");
        try {
            final Class<?> buildClass = XposedHelpers.findClassIfExists(BUILD_CLASS, null);
            if (buildClass == null) {
                XposedBridge.log(TAG + "   Oi    ### P 0.1: BUILD CLS NOT FOUND SKIP");
                return;
            }
            XposedHelpers.setStaticBooleanField(buildClass, "isDebugSystem", true);
            XposedBridge.log(TAG + "P 0.1: BUILD.ISDEBUGSYSTEM SET TRUE SUCCESS");
            XposedHelpers.setStaticBooleanField(buildClass, "isHasDePermissions", true);
            XposedBridge.log(TAG + "P 0.1: BUILD.ISHASDEPERMISSIONS SET TRUE SUCCESS");
            XposedBridge.log(TAG + "P 0.1: BUILD MOD SUCCESS");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "   Oi    ### P 0.1: BUILD MOD FAIL " + t);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        String framework = detectFramework();
        XposedBridge.log(TAG + "FRAMEWORK: " + framework + " DETECTED");

        if (isLsposedCompatible() && !isModuleInScope(lpparam)) {
            XposedBridge.log(TAG + "SKIP: NOT IN LSP SCOPE");
            return;
        }

        if (!isBehaviorHooked) {
            hookBehaviorReporting(lpparam.classLoader);
        }

        if (!"android".equals(lpparam.packageName)) {
            return;
        }

        XposedBridge.log(TAG + "P 1: SYS SERVER DETECTED DEPLOY LISTENER");

        try {
            final Class<?> ssmClass = XposedHelpers.findClassIfExists(SYSTEM_SERVICE_MANAGER, lpparam.classLoader);
            if (ssmClass == null) {
                XposedBridge.log(TAG + "   Oi    ### P 2: SSM CLS NOT FOUND   ");
                return;
            }
            XposedHelpers.findAndHookMethod(ssmClass, "startService", java.lang.Class.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Class<?> serviceClass = (Class<?>) param.args[0];
                    if (serviceClass == null) return;
                    String serviceName = serviceClass.getName();
                    XposedBridge.log(TAG + "P 2: SSM START SVC " + serviceName);

                    if (!isInstallerHooked && INSTALLER_CLASS.equals(serviceName)) {
                        isInstallerHooked = true;
                        XposedBridge.log(TAG + "P 3: INSTALLER DETECTED DEPLOY HOOK");
                        hookInstaller(lpparam.classLoader);
                    } else if (!isAmsHooked && AMS_LIFECYCLE_CLASS.equals(serviceName)) {
                        isAmsHooked = true;
                        XposedBridge.log(TAG + "P 3: AMS DETECTED DEPLOY HOOK");
                        hookActivityManagerService(lpparam.classLoader);
                    } else if (!isPmsHooked && PMS_CLASS.equals(serviceName)) {
                        isPmsHooked = true;
                        XposedBridge.log(TAG + "P 3: PMS DETECTED DEPLOY HOOK");
                        hookPowerManagerService(lpparam.classLoader);
                    } else if (!isXsesHooked && XSES_SERVICE_CLASS.equals(serviceName)) {
                        isXsesHooked = true;
                        XposedBridge.log(TAG + "P 3: XSES DETECTED DEPLOY HOOK");
                        hookXsesService(lpparam.classLoader);
                    }
                }
            });
            XposedBridge.log(TAG + "P 2: SSM LISTENER SUCCESS");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "   Oi    ### P 2: SSM LISTENER FAIL " + t);
        }
    }
}