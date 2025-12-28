package com.example.kisandost.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kisandost.diagnosis.CropType
import com.example.kisandost.diagnosis.DiagnosisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {
    private val _diagnosisResult = MutableStateFlow<DiagnosisResult?>(null)
    val diagnosisResult: StateFlow<DiagnosisResult?> = _diagnosisResult.asStateFlow()
    
    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()
    
    private val _selectedCrop = MutableStateFlow<CropType>(CropType.getDefault())
    val selectedCrop: StateFlow<CropType> = _selectedCrop.asStateFlow()
    
    fun setDiagnosisResult(result: DiagnosisResult) {
        _diagnosisResult.value = result
    }
    
    fun captureImage() {
        _isCapturing.value = true
    }
    
    fun resetCaptureState() {
        _isCapturing.value = false
    }
    
    fun clearDiagnosisResult() {
        _diagnosisResult.value = null
    }
    
    fun setSelectedCrop(cropType: CropType) {
        _selectedCrop.value = cropType
    }
}

