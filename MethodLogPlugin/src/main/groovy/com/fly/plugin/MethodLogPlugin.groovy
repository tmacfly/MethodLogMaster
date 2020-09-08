package com.fly.plugin

import com.android.build.gradle.AppExtension
import com.fly.transform.MethodLogTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

class MethodLogPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def android = project.extensions.getByType(AppExtension)
        android.registerTransform(new MethodLogTransform(project))
    }
}