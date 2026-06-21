package com.example.aichat.model.utils.files;

import com.example.aichat.R;

public class FilePreviewHelper {

    public static boolean isImage(String mime) {
        return mime != null && mime.startsWith("image/");
    }

    public static boolean isVideo(String mime) {
        return mime != null && mime.startsWith("video/");
    }

    public static boolean isPdf(String mime) {
        return "application/pdf".equals(mime);
    }

    public static int getFileIcon(String mime) {
        if (mime == null) return R.drawable.ic_file;

        if (mime.contains("pdf")) return R.drawable.ic_pdf;
        if (mime.contains("word")) return R.drawable.ic_doc;
        if (mime.contains("excel")) return R.drawable.ic_excel;
        if (mime.contains("zip")) return R.drawable.ic_zip;
        if (mime.contains("text")) return R.drawable.ic_txt;

        return R.drawable.ic_file;
    }
}
