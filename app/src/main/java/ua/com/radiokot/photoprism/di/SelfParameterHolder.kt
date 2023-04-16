package ua.com.radiokot.photoprism.di

import org.koin.core.parameter.ParametersHolder

open class SelfParameterHolder : ParametersHolder(mutableListOf(this))