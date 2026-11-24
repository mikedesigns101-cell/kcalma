package com.mikel.kcalma.datos

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.LocalDateTime

/** Pasos del día desde Health Connect (Samsung Health, Google Fit, relojes…). Solo lectura. */
class Salud(private val contexto: Context) {
    val permisos = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    enum class Estado { NO_DISPONIBLE, HAY_QUE_INSTALAR, SIN_PERMISO, CONECTADO }

    private fun cliente(): HealthConnectClient? =
        if (HealthConnectClient.getSdkStatus(contexto) == HealthConnectClient.SDK_AVAILABLE) HealthConnectClient.getOrCreate(contexto) else null

    suspend fun estado(): Estado = when (HealthConnectClient.getSdkStatus(contexto)) {
        HealthConnectClient.SDK_AVAILABLE -> runCatching {
            if (cliente()!!.permissionController.getGrantedPermissions().containsAll(permisos)) Estado.CONECTADO else Estado.SIN_PERMISO
        }.getOrDefault(Estado.SIN_PERMISO)
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Estado.HAY_QUE_INSTALAR
        else -> Estado.NO_DISPONIBLE
    }

    /** Pasos de cada día desde [desde] (fecha → pasos); vacío si no hay permiso o falla. */
    suspend fun pasosPorDia(desde: LocalDate): Map<String, Long> = runCatching {
        val c = cliente() ?: return emptyMap()
        c.aggregateGroupByPeriod(
            androidx.health.connect.client.request.AggregateGroupByPeriodRequest(
                setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(desde.atStartOfDay(), LocalDateTime.now()), java.time.Period.ofDays(1),
            )
        ).associate { it.startTime.toLocalDate().toString() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0L) }
    }.onFailure { Registro.error("Health Connect", it) }.getOrDefault(emptyMap())

    /** Pasos de hoy, o null si no hay permiso o falla. */
    suspend fun pasosHoy(): Long? = runCatching {
        val c = cliente() ?: return null
        val hoy = LocalDate.now().atStartOfDay()
        c.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(hoy, LocalDateTime.now())))[StepsRecord.COUNT_TOTAL] ?: 0L
    }.onFailure { Registro.error("Health Connect", it) }.getOrNull()
}
