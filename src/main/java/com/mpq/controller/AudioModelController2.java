package com.mpq.controller;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionOptions;


import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;

@RestController
@RequestMapping("/audioChat2")
public class AudioModelController2 {
    private static final String AUDIO_RESOURCE_URL = "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_female2.wav";
    private final DashScopeAudioTranscriptionModel dashScopeAudioTranscriptionModel;

    public AudioModelController2(DashScopeAudioTranscriptionModel dashScopeAudioTranscriptionModel) {
        this.dashScopeAudioTranscriptionModel = dashScopeAudioTranscriptionModel;
    }

    @RequestMapping("/chat")
    public String chat() throws MalformedURLException {
        Resource resource = new UrlResource(AUDIO_RESOURCE_URL);
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(resource,
                DashScopeAudioTranscriptionOptions.builder()
                        .withModel("qwen-audio-asr").build());
        return dashScopeAudioTranscriptionModel.call(prompt).getResult().getOutput();

    }
}
