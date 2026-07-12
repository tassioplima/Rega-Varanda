package com.tassiolima.regavaranda.data.local

import androidx.room.TypeConverter
import com.tassiolima.regavaranda.data.model.ChatRole
import com.tassiolima.regavaranda.data.model.HealthState
import com.tassiolima.regavaranda.data.model.Orientation
import com.tassiolima.regavaranda.data.model.PlantCategory

class Converters {
    @TypeConverter
    fun categoryToString(category: PlantCategory): String = category.name

    @TypeConverter
    fun stringToCategory(value: String): PlantCategory =
        PlantCategory.entries.firstOrNull { it.name == value } ?: PlantCategory.OUTRA

    @TypeConverter
    fun orientationToString(orientation: Orientation?): String? = orientation?.name

    @TypeConverter
    fun stringToOrientation(value: String?): Orientation? =
        value?.let { name -> Orientation.entries.firstOrNull { it.name == name } }

    @TypeConverter
    fun healthStateToString(state: HealthState?): String? = state?.name

    @TypeConverter
    fun stringToHealthState(value: String?): HealthState? =
        value?.let { name -> HealthState.entries.firstOrNull { it.name == name } }

    @TypeConverter
    fun analysisStatusToString(status: AnalysisStatus): String = status.name

    @TypeConverter
    fun stringToAnalysisStatus(value: String): AnalysisStatus =
        AnalysisStatus.entries.firstOrNull { it.name == value } ?: AnalysisStatus.PENDING

    @TypeConverter
    fun chatRoleToString(role: ChatRole): String = role.name

    @TypeConverter
    fun stringToChatRole(value: String): ChatRole =
        ChatRole.entries.firstOrNull { it.name == value } ?: ChatRole.USER
}
