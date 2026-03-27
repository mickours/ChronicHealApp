package org.chronicheal.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.chronicheal.app.data.ai.LlmManager
import org.chronicheal.app.data.ai.LiteLlmManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    @Provides
    @Singleton
    fun provideLlmManager(): LlmManager = LiteLlmManager()
}
