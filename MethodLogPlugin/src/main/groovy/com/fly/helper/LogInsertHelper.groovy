package com.fly.helper

import com.android.SdkConstants
import com.android.build.api.transform.JarInput
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.Modifier
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class LogInsertHelper {

    private static final ClassPool sClassPool = ClassPool.getDefault()
    private static final String LOGTAG = "TagMethod"

    static void loadClassPath(String path, Project project) {
        sClassPool.appendClassPath(path)
        sClassPool.appendClassPath(project.android.bootClasspath[0].toString())
        sClassPool.importPackage('android.os.Bundle')
    }

    static void injectLog2Class(String path) {
        File dir = new File(path)
        boolean isCodeChanged = false
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->
                isCodeChanged = false
                boolean flag = checkClassFile(file.name)
                String debugStr = "debug"
                if (flag && dir.absolutePath.toLowerCase().contains(debugStr)) {
                    String tempStr = file.getCanonicalPath()
                    String fullpath = tempStr.substring(dir.absolutePath.length() + 1, tempStr.length())
                    String className = fullpath.replace("/", ".")
                    if (className.endsWith(".class")) {
                        className = className.replace(".class", "")
                    }
                    CtClass ctClass = sClassPool.getCtClass(className)
                    System.println("Changed class name = " + ctClass.getName())
                    if (ctClass.isFrozen()) {
                        ctClass.defrost()
                    }
                    ctClass.getDeclaredMethods().each {
                        CtMethod ctMethod ->
                            //System.println("MethodName : " + ctMethod.getName())
                            if (!ctMethod.isEmpty() && !isNative(ctMethod) && filterMethodName(ctMethod)) {
                                ctMethod.addLocalVariable("startTime", CtClass.longType)
                                String name = ctMethod.getName()
                                String curClass = ctClass.getName()
                                String insertStr = "startTime = 0;startTime = System.currentTimeMillis();"
                                String lastStr = """if(System.currentTimeMillis() - startTime > 10){android.util.Log.i(\"$LOGTAG\","【ClassName: " + \"$curClass\" + "】***【 MethodName:" + \"$name\" + "】***【DuringTime:" + (System.currentTimeMillis() - startTime) + "ms】***【IsMainThread:" + (android.os.Looper.getMainLooper() == android.os.Looper.myLooper()) + "】");}"""
                                ctMethod.insertBefore(insertStr)
                                ctMethod.insertAfter(lastStr)
                                isCodeChanged = true
                            }
                    }
                    if (isCodeChanged) {
                        ctClass.writeFile(path)
                    }
                    ctClass.detach()
                }
            }
        }
    }

    static void injectLog2Jar(JarInput jarInput, File dest) {
        if (jarInput.file.absolutePath.endsWith(".jar")) {
            JarFile jarFile = new JarFile(jarInput.file)
            Enumeration enumeration = jarFile.entries()
            File tmpFile = new File(jarInput.file.getParent() + File.separator + "classes_temp.jar")
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile))
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement()
                String entryName = jarEntry.getName()
                String[] classNames = entryName.split("/")
                ZipEntry zipEntry = new ZipEntry(entryName)
                InputStream inputStream = jarFile.getInputStream(jarEntry)
                if (classNames.length > 0 && checkJarFile(entryName) && checkClassFile(classNames[classNames.length - 1])) {
                    jarOutputStream.putNextEntry(zipEntry)
                    entryName = entryName.replace("/", ".").substring(0, entryName.length() - SdkConstants.DOT_CLASS.length())
                    System.out.println("Jar ClassName = " + entryName)
                    CtClass ctClass = sClassPool.getCtClass(entryName)
                    if (ctClass.isFrozen()) {
                        ctClass.defrost()
                    }
                    ctClass.getDeclaredMethods().each {
                        CtMethod ctMethod ->
                            //System.println("Jar MethodName : " + ctMethod.getName())
                            if (!ctMethod.isEmpty() && !isNative(ctMethod) && filterMethodName(ctMethod)) {
                                ctMethod.addLocalVariable("startTime", CtClass.longType)
                                String name = ctMethod.getName()
                                String curClass = ctClass.getName()
                                String insertStr = "startTime = 0;startTime = System.currentTimeMillis();"
                                String lastStr = """if(System.currentTimeMillis() - startTime >= 10){android.util.Log.i(\"$LOGTAG\","【ClassName: " + \"$curClass\" + "】***【 MethodName:" + \"$name\" + "】***【DuringTime:" + (System.currentTimeMillis() - startTime) + "ms】");}"""
                                ctMethod.insertBefore(insertStr)
                                ctMethod.insertAfter(lastStr)
                            }
                    }
                    jarOutputStream.write(ctClass.toBytecode())
                } else {
                    jarOutputStream.putNextEntry(zipEntry)
                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
                }
                jarOutputStream.closeEntry()
            }
            jarOutputStream.close()
            jarFile.close()

            FileUtils.copyFile(tmpFile, dest)
            tmpFile.delete()
        }
    }

    static boolean checkClassFile(String name) {
        //只处理需要的class文件
        return (name.endsWith(".class") && !name.startsWith("R\$") && !name.startsWith("R2\$")
                && !"R.class".equals(name) && !"BuildConfig.class".equals(name)
                && !"android/support/v4/app/FragmentActivity.class".equals(name))
    }

    static boolean checkJarFile(String name) {
        return !name.startsWith("android") && !name.startsWith("javassist")
    }

    static boolean isNative(CtMethod method) {
        return Modifier.isNative(method.getModifiers())
    }

    static boolean filterMethodName(CtMethod ctMethod) {
        String name = ctMethod.getName()
        if (name.contains("\$") && name.contains("isLoggable")) {
            return false
        }
        return true
    }
}
