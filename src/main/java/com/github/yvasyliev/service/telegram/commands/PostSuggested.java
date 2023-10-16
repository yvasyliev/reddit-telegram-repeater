package com.github.yvasyliev.service.telegram.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.yvasyliev.model.dto.CallbackData;
import com.github.yvasyliev.model.dto.PostApprovedData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

@Service("/postsuggested")
public class PostSuggested extends Command {
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void acceptWithException(Message message) throws TelegramApiException, IOException, URISyntaxException {
        var sourceChatId = message.getChatId().toString();
        var sourceMessageId = message.getMessageId();

        redTelBot.execute(new ForwardMessage(
                redTelBot.getAdminId(),
                sourceChatId,
                sourceMessageId
        ));

        var approveButton = InlineKeyboardButton.builder()
                .text("✅ Approve")
                .callbackData(objectMapper.writeValueAsString(new PostApprovedData(
                        "/approvepost",
                        sourceChatId,
                        sourceMessageId
                )))
                .build();

        var denyButton = InlineKeyboardButton.builder()
                .text("🚫 Reject")
                .callbackData(objectMapper.writeValueAsString(new CallbackData("/rejectpost")))
                .build();

        var sendMessage = SendMessage.builder()
                .chatId(redTelBot.getAdminId())
                .text("👆 Shall I publish the post above?")
                .replyMarkup(new InlineKeyboardMarkup(List.of(List.of(
                        approveButton,
                        denyButton
                ))))
                .build();
        redTelBot.execute(sendMessage);

        reply(message, "responses/postsuggested.md");
    }
}
