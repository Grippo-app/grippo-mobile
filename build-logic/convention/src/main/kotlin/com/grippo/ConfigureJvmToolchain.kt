package com.grippo

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

fun Project.configureJvmToolchain(version: Int) {
    val kotlinExt = extensions.findByName("kotlin")
    if (kotlinExt is KotlinJvmProjectExtension ||
        kotlinExt is KotlinAndroidProjectExtension ||
        kotlinExt is KotlinMultiplatformExtension
    ) {
        (kotlinExt as KotlinProjectExtension).jvmToolchain(version)
    }
}