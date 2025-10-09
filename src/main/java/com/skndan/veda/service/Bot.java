package com.skndan.veda.service;

import com.skndan.veda.model.RetrievalResponse;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService(retrievalAugmentor = QdrantRetriever.class)
@ApplicationScoped
@SystemMessage("""
    You are an intelligent agent that answers questions STRICTLY based on the provided context from the knowledge base.
    
    IMPORTANT RULES:
    1. ONLY answer questions using information from the provided context
    2. If the context does not contain relevant information to answer the question, respond with: "I don't have information about that in my knowledge base."
    3. DO NOT make up answers or use general knowledge
    4. DO NOT guess or speculate beyond what is in the context
    5. Be concise and accurate
    """)
public interface Bot {
   @UserMessage("Answer the user's question: {question}")
   public RetrievalResponse answer(String question);
}
