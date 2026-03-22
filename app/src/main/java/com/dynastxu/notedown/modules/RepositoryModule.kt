package com.dynastxu.notedown.modules

import android.content.Context
import com.dynastxu.notedown.repository.NoteRepository
import com.dynastxu.notedown.repository.NoteRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
object RepositoryModule {
    @Provides
    @Singleton
    fun provideNoteRepository(
        @ApplicationContext context: Context
    ): NoteRepository {
        return NoteRepositoryImpl(context)
    }
}