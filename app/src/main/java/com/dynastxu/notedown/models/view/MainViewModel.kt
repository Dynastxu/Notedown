package com.dynastxu.notedown.models.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.dynastxu.notedown.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repository: NoteRepository
) : AndroidViewModel(application) {
}