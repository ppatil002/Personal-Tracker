package com.example.pp.utils;

import androidx.room.TypeConverter;
import com.example.pp.core.enums.TaskStatus;
import com.example.pp.core.enums.TaskType;

public class TaskTypeConverters {
    @TypeConverter
    public static String fromStatus(TaskStatus status) {
        return status == null ? null : status.name();
    }

    @TypeConverter
    public static TaskStatus toStatus(String status) {
        return status == null ? null : TaskStatus.valueOf(status);
    }

    @TypeConverter
    public static String fromTaskType(TaskType type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    public static TaskType toTaskType(String type) {
        return type == null ? null : TaskType.valueOf(type);
    }
}
