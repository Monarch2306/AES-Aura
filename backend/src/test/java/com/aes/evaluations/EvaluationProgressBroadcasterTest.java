package com.aes.evaluations;

import com.aes.evaluations.dto.EvaluationProgressEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
class EvaluationProgressBroadcasterTest {

    private EvaluationProgressBroadcaster broadcaster;
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        broadcaster = new EvaluationProgressBroadcaster();
        session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn("sess-1");
    }

    @Test
    void testSessionRegistrationAndBroadcast() throws Exception {
        when(session.getUri()).thenReturn(URI.create("ws://localhost:8080/ws/evaluations/42"));

        broadcaster.afterConnectionEstablished(session);

        EvaluationProgressEvent event = EvaluationProgressEvent.of(42L, "OCR_PREPROCESSING", 1, 30, "Testing OCR step");
        broadcaster.broadcast(42L, event);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(1)).sendMessage(captor.capture());

        String payload = captor.getValue().getPayload();
        assertTrue(payload.contains("\"sheetId\":42"));
        assertTrue(payload.contains("\"stage\":\"OCR_PREPROCESSING\""));
        assertTrue(payload.contains("\"stepIndex\":1"));
        assertTrue(payload.contains("\"progressPercent\":30"));
        assertTrue(payload.contains("Testing OCR step"));
    }

    @Test
    void testSessionCleanupOnClose() throws Exception {
        when(session.getUri()).thenReturn(URI.create("ws://localhost:8080/ws/evaluations/42"));
        broadcaster.afterConnectionEstablished(session);
        broadcaster.afterConnectionClosed(session, CloseStatus.NORMAL);

        EvaluationProgressEvent event = EvaluationProgressEvent.of(42L, "COMPLETED", 6, 100, "Done");
        broadcaster.broadcast(42L, event);

        verify(session, never()).sendMessage(any());
    }

    @Test
    void testUnrelatedSheetIdDoesNotReceiveEvent() throws Exception {
        when(session.getUri()).thenReturn(URI.create("ws://localhost:8080/ws/evaluations/99"));
        broadcaster.afterConnectionEstablished(session);

        EvaluationProgressEvent event = EvaluationProgressEvent.of(42L, "OCR_PREPROCESSING", 1, 30, "Different sheet");
        broadcaster.broadcast(42L, event);

        verify(session, never()).sendMessage(any());
    }
}