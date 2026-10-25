package com.connectai.service;

import com.connectai.model.Message;
import com.connectai.model.MessageType;
import com.connectai.util.AudioUtil;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

public class AudioService {
    private static final AudioService instance = new AudioService();
    private final AudioUtil audioUtil;

    private AudioService() {
        this.audioUtil = new AudioUtil();
    }

    public static AudioService getInstance() {
        return instance;
    }

    public void startVoiceRecording() throws Exception {
        audioUtil.startRecording();
    }

    public byte[] stopVoiceRecording() {
        return audioUtil.stopRecording();
    }

    public boolean isRecording() {
        return audioUtil.isRecording();
    }

    public long getRecordedDurationSeconds() {
        return audioUtil.getRecordedDurationSeconds();
    }

    public CompletableFuture<Message> sendVoiceNote(String conversationId, byte[] wavBytes, int durationSeconds) {
        try {
            if (conversationId == null || conversationId.isBlank()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Open a conversation before sending a voice note."));
            }
            if (wavBytes == null || wavBytes.length <= 44
                    || wavBytes[0] != 'R' || wavBytes[1] != 'I' || wavBytes[2] != 'F' || wavBytes[3] != 'F') {
                return CompletableFuture.failedFuture(new IllegalArgumentException("The recording is empty or invalid. Record your voice again."));
            }
            File tempFile = File.createTempFile("voice_note_", ".wav");
            tempFile.deleteOnExit();
            Files.write(tempFile.toPath(), wavBytes);

            return AttachmentService.getInstance().uploadAndSendFile(
                    conversationId,
                    tempFile,
                    "Voice Note (" + durationSeconds + "s)",
                    MessageType.VOICE,
                    durationSeconds
            );
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public void playAudio(byte[] wavBytes, Runnable onComplete) {
        audioUtil.playWavBytes(wavBytes, onComplete);
    }

    public void stopPlayback() {
        audioUtil.stopPlayback();
    }
}
