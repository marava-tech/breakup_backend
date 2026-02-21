package com.breakupstories.service;

import com.breakupstories.dto.TranscriptionResponse;
import com.breakupstories.exception.TranscriptionException;
import com.breakupstories.model.StoryDataStore;
import com.breakupstories.repository.StoryDataStoreRepository;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.speech.v1.*;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;

@Service
public class TranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionService.class);
    private final StoryDataStoreRepository storyRepository;

    // Fixed GCS bucket for temporary audio uploads — must exist in the project
    private static final String GCS_AUDIO_BUCKET = "breakup-stories-audio-temp";

    // Language mapping for Indian languages — Google Cloud Speech-to-Text locale codes
    private static final Map<String, String> LANGUAGE_MAPPING = Map.ofEntries(
            // South Indian
            Map.entry("te", "te-IN"),   // Telugu
            Map.entry("ta", "ta-IN"),   // Tamil
            Map.entry("kn", "kn-IN"),   // Kannada
            Map.entry("ml", "ml-IN"),   // Malayalam
            // North / West Indian
            Map.entry("hi", "hi-IN"),   // Hindi
            Map.entry("mr", "mr-IN"),   // Marathi
            Map.entry("gu", "gu-IN"),   // Gujarati
            Map.entry("pa", "pa-IN"),   // Punjabi
            Map.entry("bn", "bn-IN"),   // Bengali
            Map.entry("ur", "ur-IN"),   // Urdu
            // East Indian
            Map.entry("or", "or-IN"),   // Odia
            // International
            Map.entry("en", "en-US")    // English
    );

    public TranscriptionService(StoryDataStoreRepository storyRepository) {
        this.storyRepository = storyRepository;
    }

    @Autowired
    private SpeechClient speechClient;

    @Autowired
    private Storage storageClient;

    /**
     * Process transcription for a story
     */
    public void processTranscription(StoryDataStore story) throws IOException {
        try {
            log.info("Processing transcription for story: {}", story.getStoryId());

            // Validate audio URL
            if (story.getAudioUrl() == null || story.getAudioUrl().trim().isEmpty()) {
                throw new TranscriptionException("Audio URL is null or empty");
            }

            // Download audio file from URL
            File audioFile = downloadAudioFromUrl(story.getAudioUrl());

            try {
                // Call Google Cloud Speech-to-Text API
                TranscriptionResponse transcriptionResponse = transcribeAudio(audioFile, story.getLanguage());

                // Validate transcription response
                if (transcriptionResponse == null) {
                    throw new TranscriptionException("Transcription response is null");
                }

                if (transcriptionResponse.getTranscription() == null ||
                        transcriptionResponse.getTranscription().trim().isEmpty()) {
                    throw new TranscriptionException("Transcription text is null or empty");
                }

                // Update story with transcription response
                story.setTranscriptionResponse(transcriptionResponse);
                story.setTranscriptionCompletedAt(LocalDateTime.now());
                storyRepository.save(story);

                log.info("Successfully processed transcription for story: {} (transcript length: {} chars)",
                        story.getStoryId(), transcriptionResponse.getTranscription().length());

            } finally {
                // Clean up the temporary audio file
                if (audioFile != null && audioFile.exists()) {
                    if (!audioFile.delete()) {
                        log.warn("Failed to delete temp file: {}", audioFile.getPath());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error processing transcription for story: {}", story.getStoryId(), e);
            story.setTranscriptionError(e.getMessage());
            storyRepository.save(story);
            throw e;
        }
    }

    /**
     * Download audio file from URL with improved error handling
     */
    private File downloadAudioFromUrl(String audioUrl) throws IOException {
        Path tempFile = null;
        java.net.HttpURLConnection connection = null;
        try {
            URL url = new URL(audioUrl);
            tempFile = Files.createTempFile("audio_", ".mp3");

            // Create connection with timeout and user agent
            connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000); // 30 seconds
            connection.setReadTimeout(60000); // 60 seconds
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");

            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                String errorMsg = "HTTP error: " + responseCode + " - " + connection.getResponseMessage();
                try (java.io.InputStream err = connection.getErrorStream()) {
                    if (err != null) {
                        errorMsg += " | " + new String(err.readAllBytes());
                    }
                }
                throw new TranscriptionException(errorMsg);
            }

            // Download the file
            try (var inputStream = connection.getInputStream()) {
                Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // Verify the file was downloaded and has content
            long fileSize = tempFile.toFile().length();
            if (!tempFile.toFile().exists() || fileSize == 0) {
                throw new TranscriptionException(
                        "Downloaded file is empty or doesn't exist. Path: " + tempFile + ", Size: " + fileSize);
            }

            log.info("Downloaded audio file from URL: {} (size: {} bytes)", audioUrl, fileSize);
            return tempFile.toFile();

        } catch (java.nio.file.FileAlreadyExistsException e) {
            log.error("Temp file already exists: {}", tempFile);
            throw new TranscriptionException("Failed to create temporary file: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to download audio file from URL: {} - Error: {} - Type: {}", audioUrl, e.getMessage(),
                    e.getClass().getSimpleName());
            throw new TranscriptionException(
                    "Failed to download audio file from URL: " + audioUrl + " - " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Core transcription method
     */
    public TranscriptionResponse transcribeAudio(File audioFile, String language) {
        try {
            log.info("Starting Google Cloud transcription: {}", audioFile.getName());

            // Try different audio formats and encodings
            return tryTranscribeWithDifferentFormats(audioFile, language);

        } catch (Exception e) {
            log.error("Transcription failed: {}", e.getMessage());
            throw new TranscriptionException("Transcription failed: " + e.getMessage(), e);
        }
    }

    /**
     * Try transcription with different audio formats
     */
    private TranscriptionResponse tryTranscribeWithDifferentFormats(File audioFile, String language) {
        // Try different encodings
        RecognitionConfig.AudioEncoding[] encodings = {
                RecognitionConfig.AudioEncoding.MP3,
                RecognitionConfig.AudioEncoding.FLAC,
                RecognitionConfig.AudioEncoding.LINEAR16,
                RecognitionConfig.AudioEncoding.OGG_OPUS
        };

        for (RecognitionConfig.AudioEncoding encoding : encodings) {
            try {
                log.info("Trying transcription with encoding: {}", encoding);
                return transcribeWithEncoding(audioFile, language, encoding);
            } catch (Exception e) {
                log.warn("Failed with encoding {}: {}", encoding, e.getMessage());
                // Continue to next encoding
            }
        }

        throw new TranscriptionException("All audio encoding attempts failed");
    }

    /**
     * Transcribe with specific encoding
     */
    private TranscriptionResponse transcribeWithEncoding(File audioFile, String language,
            RecognitionConfig.AudioEncoding encoding) {
        try {
            // Read audio file
            byte[] content = Files.readAllBytes(audioFile.toPath());

            // Create recognition audio
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(com.google.protobuf.ByteString.copyFrom(content))
                    .build();

            // Get recognition config with specific encoding
            RecognitionConfig config = getRecognitionConfig(language, encoding);

            // Check duration to choose sync vs async
            long duration = getAudioDuration(audioFile);

            if (duration > 60) { // Use async for long audio
                log.info("Long audio ({}s), using async recognition with encoding {}", duration, encoding);
                return transcribeLongAudioGcs(audioFile, language, encoding);
            } else {
                log.info("Short audio ({}s), using sync recognition with encoding {}", duration, encoding);
                return transcribeShortAudio(audioFile, language, encoding);
            }

        } catch (Exception e) {
            log.error("Transcription failed with encoding {}: {}", encoding, e.getMessage());
            throw new TranscriptionException("Transcription failed with encoding " + encoding + ": " + e.getMessage(),
                    e);
        }
    }

    /**
     * Synchronous transcription for short audio (≤ 1 minute)
     */
    private TranscriptionResponse transcribeShortAudio(File audioFile, String language,
            RecognitionConfig.AudioEncoding encoding) {
        try {
            // Read audio file
            byte[] content = Files.readAllBytes(audioFile.toPath());

            // Create recognition audio
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(com.google.protobuf.ByteString.copyFrom(content))
                    .build();

            // Get recognition config
            RecognitionConfig config = getRecognitionConfig(language, encoding);

            // Perform transcription
            RecognizeResponse response = speechClient.recognize(config, audio);

            return processResults(response.getResultsList(), language);

        } catch (Exception e) {
            log.error("Sync transcription failed: {}", e.getMessage());
            throw new TranscriptionException("Sync transcription failed: " + e.getMessage(), e);
        }
    }

    /**
     * Asynchronous transcription for long audio (> 1 minute)
     */
    private TranscriptionResponse transcribeLongAudioGcs(File audioFile, String language,
            RecognitionConfig.AudioEncoding encoding) {
        String gcsUri = null;
        try {
            // Upload to GCS
            log.info("Uploading to Google Cloud Storage...");
            gcsUri = uploadToGcs(audioFile);

            // Create recognition audio with GCS URI
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setUri(gcsUri)
                    .build();

            // Get recognition config
            RecognitionConfig config = getRecognitionConfig(language, encoding);

            // Start long-running operation
            log.info("Starting LongRunningRecognize...");
            OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> operation = speechClient
                    .longRunningRecognizeAsync(config, audio);

            // Wait for completion
            LongRunningRecognizeResponse operationResult = operation.get();
            log.info("LongRunningRecognize completed");

            return processResults(operationResult.getResultsList(), language);

        } catch (Exception e) {
            log.error("Async transcription failed: {}", e.getMessage());
            throw new TranscriptionException("Async transcription failed: " + e.getMessage(), e);
        } finally {
            // Cleanup GCS
            if (gcsUri != null) {
                cleanupGcs(gcsUri);
            }
        }
    }

    /**
     * Create recognition configuration
     */
    private RecognitionConfig getRecognitionConfig(String language, RecognitionConfig.AudioEncoding encoding) {
        // Get target language
        String targetLanguage = LANGUAGE_MAPPING.getOrDefault(language, "en-US");

        RecognitionConfig.Builder configBuilder = RecognitionConfig.newBuilder()
                .setEncoding(encoding)
                .setSampleRateHertz(16000)
                .setLanguageCode(targetLanguage)
                .setEnableAutomaticPunctuation(true)
                .setEnableWordTimeOffsets(true)
                .setEnableWordConfidence(true)
                .setModel("latest_long")
                .setUseEnhanced(true)
                .setMetadata(buildRecognitionMetadata());

        // Add alternative language hints for better detection
        if (language == null) {
            configBuilder.addAlternativeLanguageCodes("hi-IN")
                    .addAlternativeLanguageCodes("te-IN")
                    .addAlternativeLanguageCodes("ta-IN")
                    .addAlternativeLanguageCodes("kn-IN")
                    .addAlternativeLanguageCodes("ml-IN")
                    .addAlternativeLanguageCodes("en-IN");
        }

        // Add domain-specific phrase hints to improve recognition accuracy
        buildSpeechContexts(language).forEach(ctx -> configBuilder.addSpeechContexts(ctx));

        return configBuilder.build();
    }

    /**
     * Build recognition metadata to help Google's model understand the recording context
     */
    private RecognitionMetadata buildRecognitionMetadata() {
        return RecognitionMetadata.newBuilder()
                .setInteractionType(RecognitionMetadata.InteractionType.DICTATION)
                .setMicrophoneDistance(RecognitionMetadata.MicrophoneDistance.NEARFIELD)
                .setRecordingDeviceType(RecognitionMetadata.RecordingDeviceType.SMARTPHONE)
                .build();
    }

    /**
     * Build language-specific speech context phrase hints.
     * Boosting domain vocabulary (emotional words, Indian names, cities) significantly
     * reduces transcription errors for love/breakup story content.
     */
    private java.util.List<SpeechContext> buildSpeechContexts(String language) {
        java.util.List<SpeechContext> contexts = new java.util.ArrayList<>();

        // Common Indian city names — frequently mentioned in stories
        contexts.add(SpeechContext.newBuilder()
                .addPhrases("Hyderabad").addPhrases("Bangalore").addPhrases("Chennai")
                .addPhrases("Mumbai").addPhrases("Delhi").addPhrases("Vizag")
                .addPhrases("Vijayawada").addPhrases("Coimbatore").addPhrases("Mysore")
                .addPhrases("Kochi").addPhrases("Thiruvananthapuram").addPhrases("Kolkata")
                .addPhrases("Jaipur").addPhrases("Pune").addPhrases("Ahmedabad")
                .setBoost(5.0f)
                .build());

        // Language-specific emotional vocabulary and common character names
        switch (language != null ? language : "en") {
            case "te":
                contexts.add(SpeechContext.newBuilder()
                        .addPhrases("ప్రేమ").addPhrases("హృదయం").addPhrases("జీవితం").addPhrases("దుఃఖం")
                        .addPhrases("ఆనందం").addPhrases("మనసు").addPhrases("విరహం").addPhrases("ఎడబాటు")
                        .addPhrases("ఒంటరిగా").addPhrases("కన్నీళ్ళు").addPhrases("ప్రేమించాను").addPhrases("నమ్మకం")
                        .addPhrases("రాజ్").addPhrases("ప్రియ").addPhrases("అర్జున్").addPhrases("కావ్య")
                        .addPhrases("సాయి").addPhrases("సంధ్య").addPhrases("రవి").addPhrases("లక్ష్మి")
                        .addPhrases("హైదరాబాద్").addPhrases("విజయవాడ").addPhrases("విశాఖపట్నం")
                        .setBoost(10.0f)
                        .build());
                break;
            case "hi":
                contexts.add(SpeechContext.newBuilder()
                        .addPhrases("प्यार").addPhrases("दिल").addPhrases("जिंदगी").addPhrases("दर्द")
                        .addPhrases("आंसू").addPhrases("मोहब्बत").addPhrases("इश्क").addPhrases("जुदाई")
                        .addPhrases("तन्हा").addPhrases("याद").addPhrases("रोना").addPhrases("बिछड़ना")
                        .addPhrases("रोहित").addPhrases("प्रिया").addPhrases("अर्जुन").addPhrases("काव्या")
                        .addPhrases("राज").addPhrases("नीतू").addPhrases("अमित").addPhrases("सीमा")
                        .addPhrases("दिल्ली").addPhrases("मुंबई").addPhrases("लखनऊ").addPhrases("जयपुर")
                        .setBoost(10.0f)
                        .build());
                break;
            case "ta":
                contexts.add(SpeechContext.newBuilder()
                        .addPhrases("காதல்").addPhrases("இதயம்").addPhrases("வாழ்க்கை").addPhrases("துக்கம்")
                        .addPhrases("கண்ணீர்").addPhrases("பிரிவு").addPhrases("தனிமை").addPhrases("நினைவு")
                        .addPhrases("மனசு").addPhrases("உயிர்").addPhrases("வலி").addPhrases("ஆசை")
                        .addPhrases("கார்த்திக்").addPhrases("தீபிகா").addPhrases("ரமேஷ்").addPhrases("காவ்யா")
                        .addPhrases("அர்ஜுன்").addPhrases("பிரியா").addPhrases("ராஜ்").addPhrases("லட்சுமி")
                        .addPhrases("சென்னை").addPhrases("கோயம்புத்தூர்").addPhrases("மதுரை")
                        .setBoost(10.0f)
                        .build());
                break;
            case "kn":
                contexts.add(SpeechContext.newBuilder()
                        .addPhrases("ಪ್ರೀತಿ").addPhrases("ಹೃದಯ").addPhrases("ಜೀವ").addPhrases("ದುಃಖ")
                        .addPhrases("ಕಣ್ಣೀರು").addPhrases("ಅಗಲಿಕೆ").addPhrases("ಒಂಟಿ").addPhrases("ನೆನಪು")
                        .addPhrases("ಮನಸ್ಸು").addPhrases("ನೋವು").addPhrases("ಬಯಕೆ").addPhrases("ನಂಬಿಕೆ")
                        .addPhrases("ರಾಜ್").addPhrases("ಪ್ರಿಯಾ").addPhrases("ಅರ್ಜುನ್").addPhrases("ಕಾವ್ಯ")
                        .addPhrases("ರವಿ").addPhrases("ಲಕ್ಷ್ಮಿ").addPhrases("ಸಾಯಿ").addPhrases("ಸಂಧ್ಯಾ")
                        .addPhrases("ಬೆಂಗಳೂರು").addPhrases("ಮೈಸೂರು").addPhrases("ಮಂಗಳೂರು")
                        .setBoost(10.0f)
                        .build());
                break;
            case "ml":
                contexts.add(SpeechContext.newBuilder()
                        .addPhrases("പ്രണയം").addPhrases("ഹൃദയം").addPhrases("ജീവൻ").addPhrases("ദുഃഖം")
                        .addPhrases("കണ്ണുനീർ").addPhrases("വേർപാട്").addPhrases("ഒറ്റ").addPhrases("ഓർമ്മ")
                        .addPhrases("മനസ്സ്").addPhrases("വേദന").addPhrases("ആഗ്രഹം").addPhrases("വിശ്വാസം")
                        .addPhrases("അർജുൻ").addPhrases("ദീപ").addPhrases("രമേഷ്").addPhrases("കാവ്യ")
                        .addPhrases("രാജ്").addPhrases("ലക്ഷ്മി").addPhrases("സാജൻ").addPhrases("സന്ധ്യ")
                        .addPhrases("തിരുവനന്തപുരം").addPhrases("കൊച്ചി").addPhrases("കോഴിക്കോട്")
                        .setBoost(10.0f)
                        .build());
                break;
            default: // English
                contexts.add(SpeechContext.newBuilder()
                        .addPhrases("love").addPhrases("heartbreak").addPhrases("breakup").addPhrases("relationship")
                        .addPhrases("feelings").addPhrases("tears").addPhrases("goodbye").addPhrases("forever")
                        .addPhrases("loneliness").addPhrases("memories").addPhrases("missing").addPhrases("betrayal")
                        .addPhrases("trust").addPhrases("cheating").addPhrases("distance").addPhrases("together")
                        .setBoost(8.0f)
                        .build());
        }

        return contexts;
    }

    /**
     * Create recognition configuration with default encoding (for backward
     * compatibility)
     */
    private RecognitionConfig getRecognitionConfig(String language) {
        return getRecognitionConfig(language, RecognitionConfig.AudioEncoding.MP3);
    }

    /**
     * Process recognition results
     */
    private TranscriptionResponse processResults(
            java.util.List<SpeechRecognitionResult> results, String language) {

        StringBuilder transcript = new StringBuilder();
        double confidence = 0.0;
        int resultsCount = 0;

        for (SpeechRecognitionResult result : results) {
            if (result.getAlternativesCount() > 0) {
                SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                transcript.append(alternative.getTranscript()).append(" ");
                confidence += alternative.getConfidence();
                resultsCount++;
            }
        }

        // Calculate average confidence
        if (resultsCount > 0) {
            confidence = confidence / resultsCount;
        }

        // Clean the transcript
        String cleanTranscript = transcript.toString().trim();

        // Check for transcription quality issues
        if (cleanTranscript.isEmpty()) {
            throw new TranscriptionException(
                    "No speech detected. The audio may be too noisy, silent, or contain no recognizable speech.");
        }

        if (confidence < 0.5) {
            log.warn("Low confidence transcription ({}). Audio may be noisy or unclear.", confidence);
        }

        // Determine detected language
        String detectedLanguage = language != null ? language : "unknown";

        log.info("Transcription complete. Language: {}, Confidence: {}", detectedLanguage, confidence);

        return TranscriptionResponse.builder()
                .transcription(cleanTranscript)
                .language(detectedLanguage)
                .confidence(confidence)
                .status("SUCCESS")
                .build();
    }

    /**
     * Convert audio to FLAC format
     */
    private File convertAudioToFlac(File audioFile) throws IOException {
        // Create temporary file
        Path tempFile = Files.createTempFile("audio_", ".flac");

        // For now, we'll just copy the file and hope it's already in a compatible
        // format
        // In production, you should use FFmpeg or another audio conversion library
        Files.copy(audioFile.toPath(), tempFile);

        log.info("Converted {} to FLAC: {}", audioFile.getName(), tempFile);
        return tempFile.toFile();
    }

    /**
     * Get audio duration (simplified implementation)
     */
    private long getAudioDuration(File audioFile) {
        // This is a simplified implementation
        // In production, you might want to use FFmpeg to get actual duration
        return audioFile.length() / 16000; // Rough estimate
    }

    /**
     * Upload audio to Google Cloud Storage using a single fixed bucket.
     * Each file gets a unique object name so concurrent uploads never conflict.
     */
    private String uploadToGcs(File audioFile) throws IOException {
        if (storageClient == null) {
            throw new TranscriptionException("Google Cloud Storage client not initialized.");
        }

        String objectName = "audio/" + UUID.randomUUID() + ".mp3";

        BlobId blobId = BlobId.of(GCS_AUDIO_BUCKET, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storageClient.create(blobInfo, Files.readAllBytes(audioFile.toPath()));

        String gcsUri = "gs://" + GCS_AUDIO_BUCKET + "/" + objectName;
        log.info("Uploaded audio to GCS: {}", gcsUri);

        return gcsUri;
    }

    /**
     * Clean up GCS object
     */
    private void cleanupGcs(String gcsUri) {
        try {
            if (storageClient == null) {
                return;
            }

            // Parse the GCS URI
            if (gcsUri.startsWith("gs://")) {
                String path = gcsUri.substring(5);
                String[] parts = path.split("/", 2);
                if (parts.length == 2) {
                    String bucketName = parts[0];
                    String objectName = parts[1];
                    BlobId blobId = BlobId.of(bucketName, objectName);
                    storageClient.delete(blobId);
                    log.info("Cleaned up GCS object: {}", gcsUri);
                }
            }
        } catch (Exception e) {
            log.error("Failed to cleanup GCS object {} — resource may have leaked: {}", gcsUri, e.getMessage());
        }
    }
}
