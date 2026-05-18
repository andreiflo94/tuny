package com.heixss.guitartuner.domain.repository

import kotlinx.coroutines.flow.Flow

interface TunerRepository {
    fun frequencyFlow(): Flow<Float?>
}
