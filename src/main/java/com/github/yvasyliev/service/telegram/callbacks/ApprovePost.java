package com.github.yvasyliev.service.telegram.callbacks;

import com.github.yvasyliev.model.dto.PostApprovedData;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.net.URISyntaxException;

@Service("/approvepost")
public class ApprovePost extends Callback {
    @Override
    public void acceptWithException(CallbackQuery callbackQuery) throws TelegramApiException, URISyntaxException, IOException {
        var message = callbackQuery.getMessage();
        var chatId = message.getChatId();
        var messageId = message.getMessageId();
        var postApprovedData = objectMapper.readValue(callbackQuery.getData(), PostApprovedData.class);
        redTelBot.execute(new ForwardMessage(
                redTelBot.getChannelId(),
                postApprovedData.fromChatId(),
                postApprovedData.messageId()
        ));
        redTelBot.execute(new EditMessageReplyMarkup(
                chatId.toString(),
                messageId,
                callbackQuery.getInlineMessageId(),
                null
        ));
        var editMessageText = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(format("responses/approvepost.md", message.getText()))
                .parseMode(ParseMode.MARKDOWNV2)
                .build();
        redTelBot.execute(editMessageText);
    }
}
