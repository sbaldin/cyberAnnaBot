package com.github.sbaldin.tbot

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@Qualifier
@Target(VALUE_PARAMETER)
@Retention(RUNTIME)
annotation class AppConfPathOnDisk

@Qualifier
@Target(VALUE_PARAMETER)
@Retention(RUNTIME)
annotation class AppConfPathInResources

@Qualifier
@Target(VALUE_PARAMETER, FUNCTION)
@Retention(RUNTIME)
annotation class BotLocale

@Qualifier
@Target(VALUE_PARAMETER, FUNCTION)
@Retention(RUNTIME)
annotation class BotName

@Qualifier
@Target(VALUE_PARAMETER, FUNCTION)
@Retention(RUNTIME)
annotation class BotToken

@Qualifier
@Target(VALUE_PARAMETER, FUNCTION)
@Retention(RUNTIME)
annotation class PhotoDestinationDirectory
