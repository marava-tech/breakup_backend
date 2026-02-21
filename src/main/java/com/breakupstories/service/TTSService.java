package com.breakupstories.service;

import com.breakupstories.dto.TTSResponse;
import com.breakupstories.util.CloudinaryAudioUtil;
import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class TTSService {

    private static final Logger log = LoggerFactory.getLogger(TTSService.class);

    private final TextToSpeechClient textToSpeechClient;
    private final PromptConfigurationService promptConfig;

    // Maximum bytes for TTS input (Google's limit is 5000)
    private static final int MAX_INPUT_BYTES = 4500; // Leave some buffer

    // Issue #23: pre-compiled regex patterns for emotional-word emphasis (avoids recompile per iteration)
    private final Map<String, Pattern> emotionalWordPatterns = new ConcurrentHashMap<>();

    // Language and voice mapping — default female (Standard-A) voice per language
    private static final Map<String, String> LANGUAGE_VOICE_MAP = new HashMap<>();
    static {
        // South Indian
        LANGUAGE_VOICE_MAP.put("te", "te-IN-Standard-A"); // Telugu
        LANGUAGE_VOICE_MAP.put("ta", "ta-IN-Standard-A"); // Tamil
        LANGUAGE_VOICE_MAP.put("kn", "kn-IN-Standard-A"); // Kannada
        LANGUAGE_VOICE_MAP.put("ml", "ml-IN-Standard-A"); // Malayalam
        // North / West Indian
        LANGUAGE_VOICE_MAP.put("hi", "hi-IN-Standard-A"); // Hindi
        LANGUAGE_VOICE_MAP.put("mr", "mr-IN-Standard-A"); // Marathi
        LANGUAGE_VOICE_MAP.put("gu", "gu-IN-Standard-A"); // Gujarati
        LANGUAGE_VOICE_MAP.put("pa", "pa-IN-Standard-A"); // Punjabi
        LANGUAGE_VOICE_MAP.put("bn", "bn-IN-Standard-A"); // Bengali
        LANGUAGE_VOICE_MAP.put("ur", "ur-IN-Standard-A"); // Urdu
        // East Indian
        LANGUAGE_VOICE_MAP.put("or", "or-IN-Standard-A"); // Odia
        // International
        LANGUAGE_VOICE_MAP.put("en", "en-US-Standard-A"); // English
    }

    @Autowired
    public TTSService(TextToSpeechClient textToSpeechClient, PromptConfigurationService promptConfig) {
        this.textToSpeechClient = textToSpeechClient;
        this.promptConfig = promptConfig;
    }

    public TTSResponse generateAudio(String text, String language, String gender) {
        try {
            log.info("Generating audio for text: {}, language: {}, gender: {}", text, language, gender);

            // Validate input
            if (text == null || text.trim().isEmpty()) {
                return new TTSResponse("ERROR", promptConfig.getPrompt("tts_error_text_required"), true);
            }

            if (language == null || !LANGUAGE_VOICE_MAP.containsKey(language)) {
                Map<String, String> params = new HashMap<>();
                params.put("language", language);
                return new TTSResponse("ERROR", promptConfig.formatPrompt("tts_error_unsupported_language", params), true);
            }

            // Get voice name based on language and gender
            String voiceName = getVoiceName(language, gender);

            // Check if text needs to be chunked
            String ssmlText = convertToSSML(text, language);
            if (ssmlText.getBytes("UTF-8").length <= MAX_INPUT_BYTES) {
                // Text is short enough, process normally
                return generateAudioForText(ssmlText, language, voiceName);
            } else {
                // Text is too long, need to chunk it
                log.info("Text is too long ({} bytes), chunking into smaller pieces", ssmlText.getBytes("UTF-8").length);
                return generateAudioForLongText(text, language, voiceName);
            }

        } catch (Exception e) {
            log.error("Error generating audio: {}", e.getMessage(), e);
            Map<String, String> params = new HashMap<>();
            params.put("errorMessage", e.getMessage());
            return new TTSResponse("ERROR", promptConfig.formatPrompt("tts_error_generation_failed", params), true);
        }
    }

    private TTSResponse generateAudioForText(String ssmlText, String language, String voiceName) throws IOException {
        // Create synthesis input with SSML for emotional effect
        SynthesisInput input = SynthesisInput.newBuilder()
            .setSsml(ssmlText)
            .build();

        // Create voice selection
        VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
            .setLanguageCode(language)
            .setName(voiceName)
            .build();

        // Create audio config with emotional parameters
        AudioConfig audioConfig = AudioConfig.newBuilder()
            .setAudioEncoding(AudioEncoding.MP3)
            .setSpeakingRate(0.85)  // Slightly slower for emotional effect
            .setPitch(2.0)          // Higher pitch for emotional expression
            .setVolumeGainDb(2.0)   // Slightly louder for emphasis
            .build();

        // Perform the text-to-speech request
        SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

        // Get the audio content
        ByteString audioContent = response.getAudioContent();
        byte[] audioBytes = audioContent.toByteArray();

        log.info("Successfully generated audio ({} bytes)", audioBytes.length);

        // Convert to base64 for storage in metadata
        String base64Audio = java.util.Base64.getEncoder().encodeToString(audioBytes);

        log.info("Successfully encoded audio to base64 for metadata storage");

        return new TTSResponse(base64Audio, promptConfig.getPrompt("tts_success"));
    }

    private TTSResponse generateAudioForLongText(String text, String language, String voiceName) throws IOException {
        // Split text into sentences
        String[] sentences = text.split("(?<=[.!?])\\s+");
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        // Group sentences into chunks that fit within byte limit
        for (String sentence : sentences) {
            String testChunk = currentChunk.toString() + " " + sentence;
            String testSsml = convertToSSML(testChunk.trim(), language);

            if (testSsml.getBytes("UTF-8").length <= MAX_INPUT_BYTES) {
                currentChunk.append(" ").append(sentence);
            } else {
                // Current chunk is full, save it and start new chunk
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                }
                currentChunk = new StringBuilder(sentence);
            }
        }

        // Add the last chunk if it has content
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        log.info("Split text into {} chunks", chunks.size());

        // Generate audio for each chunk
        List<byte[]> audioChunks = new ArrayList<>();
        try {
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                log.info("Processing chunk {}/{}: {} characters", i + 1, chunks.size(), chunk.length());

                String ssmlChunk = convertToSSML(chunk, language);
                TTSResponse chunkResponse = generateAudioForText(ssmlChunk, language, voiceName);

                if ("ERROR".equals(chunkResponse.getStatus())) {
                    audioChunks.clear();
                    return chunkResponse; // Return error if any chunk fails
                }

                // Decode base64 audio back to bytes
                byte[] audioBytes = java.util.Base64.getDecoder().decode(chunkResponse.getAudioData());
                audioChunks.add(audioBytes);
            }
        } catch (Exception e) {
            audioChunks.clear();
            throw e;
        }

        // Combine all audio chunks
        byte[] combinedAudio = combineAudioChunks(audioChunks);

        log.info("Successfully combined {} audio chunks into {} bytes", audioChunks.size(), combinedAudio.length);

        // Convert to base64 for storage in metadata
        String base64Audio = java.util.Base64.getEncoder().encodeToString(combinedAudio);

        return new TTSResponse(base64Audio, promptConfig.getPrompt("tts_success"));
    }

    private byte[] combineAudioChunks(List<byte[]> audioChunks) throws IOException {
        // For MP3 files, we need to concatenate them properly
        // This is a simplified approach - in production you might want to use a proper audio library
        ByteArrayOutputStream combinedStream = new ByteArrayOutputStream();

        for (int i = 0; i < audioChunks.size(); i++) {
            byte[] chunk = audioChunks.get(i);

            // Skip MP3 header for all chunks except the first
            if (i > 0) {
                // Find the end of MP3 header (simplified - look for first frame)
                int headerEnd = findMp3HeaderEnd(chunk);
                if (headerEnd > 0) {
                    combinedStream.write(chunk, headerEnd, chunk.length - headerEnd);
                } else {
                    // Fallback: just append the chunk
                    combinedStream.write(chunk);
                }
            } else {
                // First chunk: write everything
                combinedStream.write(chunk);
            }
        }

        return combinedStream.toByteArray();
    }

    private int findMp3HeaderEnd(byte[] audioData) {
        // MP3 frame sync bytes (0xFF 0xFB or 0xFF 0xFA) always appear near the start;
        // Issue #24: limit scan to first 256 bytes to avoid O(n) traversal of full audio data
        int scanLimit = Math.min(256, audioData.length - 1);
        for (int i = 0; i < scanLimit; i++) {
            if ((audioData[i] & 0xFF) == 0xFF &&
                ((audioData[i + 1] & 0xFF) == 0xFB || (audioData[i + 1] & 0xFF) == 0xFA)) {
                return i;
            }
        }
        return -1; // Not found
    }

    private String getVoiceName(String language, String gender) {
        // All Indian-locale languages: -A is female, -B is male (Standard voices)
        // Covers: te, ta, kn, ml, hi, mr, gu, pa, bn, ur, or
        if (LANGUAGE_VOICE_MAP.containsKey(language) && !"en".equals(language)) {
            if ("female".equalsIgnoreCase(gender)) {
                return language + "-IN-Standard-A";
            } else {
                return language + "-IN-Standard-B";
            }
        }
        // English uses US locale
        if ("en".equals(language)) {
            if ("female".equalsIgnoreCase(gender)) {
                return "en-US-Standard-A";
            } else {
                return "en-US-Standard-B";
            }
        }
        // Default fallback
        return LANGUAGE_VOICE_MAP.getOrDefault(language, "en-US-Standard-A");
    }

    /**
     * Convert text to SSML for emotional voice synthesis
     */
    private String convertToSSML(String text, String language) {
        StringBuilder ssml = new StringBuilder();

        // Start SSML with voice and language settings
        ssml.append("<speak>");

        // Add emotional pauses and emphasis for better expression
        String[] sentences = text.split("(?<=[.!?])\\s+");

        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            if (!sentence.isEmpty()) {
                // Add emphasis to emotional words
                sentence = addEmotionalEmphasis(sentence, language);

                ssml.append(sentence);

                // Add pause between sentences for emotional effect
                if (i < sentences.length - 1) {
                    ssml.append("<break time=\"500ms\"/>");
                }
            }
        }

        ssml.append("</speak>");

        return ssml.toString();
    }

    /**
     * Add emotional emphasis to key words
     */
    private String addEmotionalEmphasis(String sentence, String language) {
        // Common emotional words in different languages
        String[] emotionalWords = getEmotionalWords(language);

        for (String word : emotionalWords) {
            if (sentence.toLowerCase().contains(word.toLowerCase())) {
                Pattern pattern = emotionalWordPatterns.computeIfAbsent(word,
                        w -> Pattern.compile("(?i)" + Pattern.quote(w)));
                sentence = pattern.matcher(sentence)
                        .replaceAll("<emphasis level=\"strong\">" + word + "</emphasis>");
            }
        }

        return sentence;
    }

    /**
     * Get emotional words for different languages
     */
    private String[] getEmotionalWords(String language) {
        switch (language) {
            // South Indian
            case "te": // Telugu
                return new String[]{"ప్రేమ", "ఆనందం", "దుఃఖం", "భయం", "కోపం", "ఆశ", "నిరాశ", "సంతోషం", "వేదన", "హృదయం"};
            case "ta": // Tamil
                return new String[]{"காதல்", "மகிழ்ச்சி", "துக்கம்", "பயம்", "கோபம்", "நம்பிக்கை", "ஆசை", "சந்தோஷம்", "வேதனை", "இதயம்"};
            case "kn": // Kannada
                return new String[]{"ಪ್ರೀತಿ", "ಸಂತೋಷ", "ದುಃಖ", "ಭಯ", "ಕೋಪ", "ಆಶೆ", "ನಿರಾಶೆ", "ಸುಖ", "ನೋವು", "ಆನಂದ"};
            case "ml": // Malayalam
                return new String[]{"പ്രണയം", "സന്തോഷം", "ദുഃഖം", "ഭയം", "കോപം", "ആശ", "നിരാശ", "സുഖം", "വേദന", "ആനന്ദം"};
            // North / West Indian
            case "hi": // Hindi
                return new String[]{"प्यार", "खुशी", "दुख", "डर", "गुस्सा", "आशा", "निराशा", "सुख", "दर्द", "आनंद"};
            case "mr": // Marathi
                return new String[]{"प्रेम", "आनंद", "दुःख", "भय", "राग", "आशा", "निराशा", "सुख", "वेदना", "हृदय"};
            case "gu": // Gujarati
                return new String[]{"પ્રેમ", "આનંદ", "દુઃખ", "ભય", "ક્રોધ", "આશા", "નિરાશા", "સુખ", "વ્યથા", "હૃદય"};
            case "pa": // Punjabi
                return new String[]{"ਪਿਆਰ", "ਖੁਸ਼ੀ", "ਦੁੱਖ", "ਡਰ", "ਗੁੱਸਾ", "ਆਸ", "ਨਿਰਾਸ਼ਾ", "ਸੁਖ", "ਦਰਦ", "ਦਿਲ"};
            case "bn": // Bengali
                return new String[]{"প্রেম", "আনন্দ", "দুঃখ", "ভয়", "রাগ", "আশা", "হতাশা", "সুখ", "ব্যথা", "হৃদয়"};
            case "ur": // Urdu
                return new String[]{"محبت", "خوشی", "غم", "خوف", "غصہ", "امید", "مایوسی", "سکون", "درد", "دل"};
            // East Indian
            case "or": // Odia
                return new String[]{"ପ୍ରେମ", "ଆନନ୍ଦ", "ଦୁଃଖ", "ଭୟ", "ରାଗ", "ଆଶା", "ନିରାଶ", "ସୁଖ", "ଯନ୍ତ୍ରଣା", "ହୃଦୟ"};
            // International
            case "en":
            default:
                return new String[]{"love", "happiness", "sadness", "fear", "anger", "hope", "despair", "joy", "pain", "heart"};
        }
    }
}
