package com.fly.transform

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.fly.helper.LogInsertHelper
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class MethodLogTransform extends Transform {

    private Project mProject

    MethodLogTransform(Project project) {
        this.mProject = project
    }

    @Override
    String getName() {
        return 'MethodLogTransform'
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return true
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs,
                   Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider,
                   boolean isIncremental) throws IOException, TransformException, InterruptedException {
        println '--------------------MethodLogTransform Begin-------------------'
        long beginTime = System.currentTimeMillis()

        if (outputProvider != null) {
            outputProvider.deleteAll();
        }

        inputs.each {
            TransformInput input ->
                input.directoryInputs.each {
                    DirectoryInput directoryInput ->
                        LogInsertHelper.loadClassPath(directoryInput.file.absolutePath, mProject)
                }

                input.jarInputs.each {
                    JarInput jarInput ->
                        LogInsertHelper.loadClassPath(jarInput.file.absolutePath, mProject)
                }

                //Class文件插桩
                input.directoryInputs.each {
                    DirectoryInput directoryInput ->
                        def dest = outputProvider.getContentLocation(directoryInput.name,
                                directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                        LogInsertHelper.injectLog2Class(directoryInput.file.absolutePath)
                        FileUtils.copyDirectory(directoryInput.file, dest)
                }

                //Jar包插桩
                input.jarInputs.each {
                    JarInput jarInput ->
                        def dest = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                        //添加jar包所在的Module名字 包括aar包
                        String ModuleName = "xxxxx"
                        if (jarInput.name.contains(ModuleName)) {
                            LogInsertHelper.injectLog2Jar(jarInput,dest)
                        } else {
                            FileUtils.copyFile(jarInput.file, dest)
                        }
                }
        }

        long duringTime = System.currentTimeMillis() - beginTime
        println("MethodLogTransform Using Time :" + duringTime + "ms")
        println '---------------------MethodLogTransform End------------------- '
    }
}
