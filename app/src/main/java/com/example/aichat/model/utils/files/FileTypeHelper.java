package com.example.aichat.model.utils.files;

import com.example.aichat.model.entities.FileType;

public class FileTypeHelper {

    public static String getMimeType(String url, FileType type) {
        if (url == null) return "*/*";

        String lower = url.toLowerCase();

        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        if (type == FileType.MessageImage) return "image/*";
        if (type == FileType.VideoMessage) return "video/*";

        return "*/*";
    }

}
