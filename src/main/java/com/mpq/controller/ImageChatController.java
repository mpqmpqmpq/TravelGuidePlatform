package com.mpq.controller;

import org.springframework.ai.image.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatImage")
public class ImageChatController {
    private final ImageModel imageModel;

    public ImageChatController(@Qualifier("dashScopeImageModel") ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    @GetMapping("/chat")
    public String chat(String input) {
        ImageOptions options = ImageOptionsBuilder.builder()
                .model("wanx2.1-t2i-plus")
                .height(1024)
                .width(1024)
                .build();
        ImagePrompt prompt = new ImagePrompt(input, options);
        ImageResponse response = imageModel.call(prompt);
        String imageUrl = response.getResult().getOutput().getUrl();
        return "图片链接：" + imageUrl;
    }
}
