package com.example.aichat.model.utils.media.audio;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.utils.media.ChatMediaMarkers;
import java.io.File;
import java.nio.ByteBuffer;

public final class AudioTrimUtils {

    private AudioTrimUtils() {
    }

    @Nullable
    public static File trimM4a(
            @NonNull Context context,
            @NonNull File source,
            long startMs,
            long endMs
    ) throws Exception {
        if (!source.exists() || source.length() <= 0L) {
            return null;
        }

        long durationMs = getDurationMs(source);
        long safeStartMs = Math.max(0L, Math.min(startMs, durationMs));
        long safeEndMs = Math.max(safeStartMs + 300L, Math.min(endMs, durationMs));

        if (safeStartMs <= 0L && safeEndMs >= durationMs - 80L) {
            return source;
        }

        File dir = new File(context.getCacheDir(), "voice_messages");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Cannot create voice cache dir: " + dir.getAbsolutePath());
        }

        File output = new File(dir, ChatMediaMarkers.buildVoiceFileName());

        MediaExtractor extractor = new MediaExtractor();
        MediaMuxer muxer = null;

        try {
            extractor.setDataSource(source.getAbsolutePath());

            int audioTrackIndex = findAudioTrack(extractor);
            if (audioTrackIndex < 0) {
                return source;
            }

            MediaFormat inputFormat = extractor.getTrackFormat(audioTrackIndex);
            extractor.selectTrack(audioTrackIndex);
            extractor.seekTo(safeStartMs * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

            muxer = new MediaMuxer(output.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int muxerTrack = muxer.addTrack(inputFormat);
            muxer.start();

            int maxInputSize = 256 * 1024;
            if (inputFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                maxInputSize = Math.max(maxInputSize, inputFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE));
            }

            ByteBuffer buffer = ByteBuffer.allocate(maxInputSize);
            android.media.MediaCodec.BufferInfo info = new android.media.MediaCodec.BufferInfo();
            long firstSampleTimeUs = -1L;
            long endUs = safeEndMs * 1000L;

            while (true) {
                int sampleSize = extractor.readSampleData(buffer, 0);

                if (sampleSize < 0) {
                    break;
                }

                long sampleTimeUs = extractor.getSampleTime();

                if (sampleTimeUs < 0L || sampleTimeUs > endUs) {
                    break;
                }

                if (firstSampleTimeUs < 0L) {
                    firstSampleTimeUs = sampleTimeUs;
                }

                info.set(
                        0,
                        sampleSize,
                        Math.max(0L, sampleTimeUs - firstSampleTimeUs),
                        extractor.getSampleFlags()
                );

                muxer.writeSampleData(muxerTrack, buffer, info);
                extractor.advance();
            }

            return output.exists() && output.length() > 0L ? output : source;

        } finally {
            try {
                extractor.release();
            } catch (Exception ignored) {
            }

            if (muxer != null) {
                try {
                    muxer.stop();
                } catch (Exception ignored) {
                }

                try {
                    muxer.release();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static long getDurationMs(@NonNull File file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(file.getAbsolutePath());
            String raw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return raw != null ? Long.parseLong(raw) : 0L;
        } catch (Exception ignored) {
            return 0L;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    private static int findAudioTrack(@NonNull MediaExtractor extractor) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.containsKey(MediaFormat.KEY_MIME)
                    ? format.getString(MediaFormat.KEY_MIME)
                    : null;

            if (mime != null && mime.startsWith("audio/")) {
                return i;
            }
        }

        return -1;
    }
}
