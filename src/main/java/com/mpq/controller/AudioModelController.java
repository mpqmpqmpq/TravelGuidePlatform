package com.mpq.controller;

import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.tts.SpeechSynthesizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.ByteBuffer;

@RestController
@RequestMapping("/audioChat")
public class AudioModelController {
    private final SpeechSynthesisModel speechSynthesisModel;

    @Autowired
    public AudioModelController(SpeechSynthesisModel speechSynthesisModel) {
        this.speechSynthesisModel = speechSynthesisModel;
    }

    @GetMapping("/chat")
    public ResponseEntity<byte[]> synthesizeSpeech(@RequestParam String text) {
        SpeechSynthesisPrompt prompt = new SpeechSynthesisPrompt(text);
        SpeechSynthesisResponse response = speechSynthesisModel.call(prompt);
        ByteBuffer audioData = response.getResult().getOutput().getAudio();
        byte[] audioBytes = new byte[audioData.remaining()];
        audioData.get(audioBytes);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"audio.mp3\"")
                .body(audioBytes);
    }

}
