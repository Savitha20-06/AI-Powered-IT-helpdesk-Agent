package com.helpdesk.itagent.web;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.helpdesk.itagent.dto.ChatDtos;
import com.helpdesk.itagent.security.CurrentUserService;
import com.helpdesk.itagent.service.ChatService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final CurrentUserService currentUserService;

    public ChatController(ChatService chatService, CurrentUserService currentUserService) {
        this.chatService = chatService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/conversations")
    public List<ChatDtos.ConversationSummary> conversations() {
        return chatService.listConversations(currentUserService.requireUser());
    }

    @GetMapping("/conversations/{id}/messages")
    public List<ChatDtos.MessageView> messages(@PathVariable Long id) {
        return chatService.messages(id, currentUserService.requireUser());
    }

    @DeleteMapping("/conversations/{id}")
    public void deleteConversation(@PathVariable Long id) {
        chatService.deleteConversation(id, currentUserService.requireUser());
    }

    @DeleteMapping("/conversations")
    public void deleteAllConversations() {
        chatService.deleteAllConversations(currentUserService.requireUser());
    }

    @PostMapping("/ask")
    public ChatDtos.AskResponse ask(@RequestBody ChatDtos.AskRequest request) {
        return chatService.ask(currentUserService.requireUser(), request);
    }
}
