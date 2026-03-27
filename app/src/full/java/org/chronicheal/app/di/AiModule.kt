package org.chronicheal.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.chronicheal.app.data.ai.FullLlmManager
import org.chronicheal.app.data.ai.LlmManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    @Provides
    @Singleton
    fun provideLlmManager(@ApplicationContext context: Context): LlmManager =
        FullLlmManager(context)
}
