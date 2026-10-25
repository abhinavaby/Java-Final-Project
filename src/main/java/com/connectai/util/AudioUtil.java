package com.connectai.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

public class AudioUtil {
    private TargetDataLine targetDataLine;
    private ByteArrayOutputStream recordStream;
    private volatile boolean isRecording = false;
    private AudioFormat recordingFormat;
    private Thread recordThread;
    private Clip currentClip;
    private long recordStartTime;
    private long recordedDurationMs;

    public AudioFormat getStandardAudioFormat() {
        // 16000 Hz, 16 bit, Mono, Signed, Little-Endian (Standard clean voice recording format)
        return new AudioFormat(16000.0f, 16, 1, true, false);
    }

    public synchronized boolean startRecording() throws Exception {
        if (isRecording) return true;

        targetDataLine = openCaptureLine();
        recordingFormat = targetDataLine.getFormat();
        targetDataLine.start();

        recordStream = new ByteArrayOutputStream();
        isRecording = true;
        recordStartTime = System.currentTimeMillis();
        TargetDataLine captureLine = targetDataLine;
        ByteArrayOutputStream captureStream = recordStream;

        recordThread = new Thread(() -> {
            byte[] buffer = new byte[2048];
            while (isRecording) {
                try {
                    int read = captureLine.read(buffer, 0, buffer.length);
                    if (read > 0) captureStream.write(buffer, 0, read);
                } catch (RuntimeException ex) {
                    if (isRecording) System.err.println("Microphone capture stopped: " + ex.getMessage());
                    break;
                }
            }
        }, "connectai-voice-recorder");
        recordThread.setDaemon(true);
        recordThread.start();
        return true;
    }

    private TargetDataLine openCaptureLine() throws Exception {
        AudioFormat[] formats = {
                getStandardAudioFormat(),
                new AudioFormat(44100.0f, 16, 1, true, false),
                new AudioFormat(48000.0f, 16, 1, true, false),
                new AudioFormat(44100.0f, 16, 2, true, false),
                new AudioFormat(48000.0f, 16, 2, true, false)
        };
        Exception lastError = null;

        for (AudioFormat format : formats) {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (!mixer.isLineSupported(info)) continue;
                try {
                    TargetDataLine line = (TargetDataLine) mixer.getLine(info);
                    line.open(format);
                    return line;
                } catch (Exception ex) {
                    lastError = ex;
                }
            }
        }

        if (lastError != null) {
            throw new IllegalStateException("No supported microphone format is available. Check microphone permission and try again.", lastError);
        }
        throw new IllegalStateException("No supported microphone format is available. Check microphone permission and try again.");
    }

    public synchronized byte[] stopRecording() {
        if (!isRecording) return new byte[0];

        isRecording = false;
        recordedDurationMs = System.currentTimeMillis() - recordStartTime;

        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }

        if (recordThread != null) {
            try {
                recordThread.join(1000);
            } catch (InterruptedException ignored) {}
        }

        byte[] rawAudioBytes = recordStream.toByteArray();
        return convertRawToWav(rawAudioBytes, recordingFormat);
    }

    public byte[] convertRawToWav(byte[] rawAudioBytes) {
        return convertRawToWav(rawAudioBytes, getStandardAudioFormat());
    }

    private byte[] convertRawToWav(byte[] rawAudioBytes, AudioFormat format) {
        if (rawAudioBytes == null || rawAudioBytes.length == 0) return new byte[0];
        try {
            int frameSize = format.getFrameSize();
            int validLength = rawAudioBytes.length - (rawAudioBytes.length % frameSize);
            if (validLength == 0) return new byte[0];
            ByteArrayInputStream bais = new ByteArrayInputStream(rawAudioBytes, 0, validLength);
            AudioInputStream ais = new AudioInputStream(bais, format, validLength / frameSize);
            ByteArrayOutputStream wavBaos = new ByteArrayOutputStream();
            int written = AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavBaos);
            if (written <= 44) throw new IllegalStateException("The microphone did not capture audio data.");
            return wavBaos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not encode the voice recording as WAV audio.", e);
        }
    }

    public void playWavBytes(byte[] wavBytes, Runnable onComplete) {
        stopPlayback();
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavBytes));
            currentClip = AudioSystem.getClip();
            currentClip.open(ais);
            currentClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    currentClip.close();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
            currentClip.start();
        } catch (Exception e) {
            System.err.println("Failed to play audio clip: " + e.getMessage());
        }
    }

    public void playWavFile(File wavFile, Runnable onComplete) {
        stopPlayback();
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(wavFile);
            currentClip = AudioSystem.getClip();
            currentClip.open(ais);
            currentClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    currentClip.close();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
            currentClip.start();
        } catch (Exception e) {
            System.err.println("Failed to play audio file: " + e.getMessage());
        }
    }

    public void stopPlayback() {
        if (currentClip != null && currentClip.isRunning()) {
            currentClip.stop();
            currentClip.close();
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public long getRecordedDurationSeconds() {
        if (isRecording) {
            return (System.currentTimeMillis() - recordStartTime) / 1000;
        }
        return recordedDurationMs / 1000;
    }
}
