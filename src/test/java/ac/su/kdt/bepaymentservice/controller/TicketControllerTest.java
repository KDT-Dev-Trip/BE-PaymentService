package ac.su.kdt.bepaymentservice.controller;

import ac.su.kdt.bepaymentservice.dto.TicketDto;
import ac.su.kdt.bepaymentservice.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
@DisplayName("TicketController 통합 테스트")
class TicketControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private TicketService ticketService;
    
    private TicketDto testTicketDto;
    
    @BeforeEach
    void setUp() {
        testTicketDto = TicketDto.builder()
                .id(1L)
                .userId(1L)
                .currentTickets(5)
                .lastTicketRefill(LocalDateTime.now().minusHours(1))
                .nextRefillAt(LocalDateTime.now().plusHours(23))
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    @DisplayName("사용자 티켓 조회 API가 정상적으로 동작한다")
    void getUserTickets_Success() throws Exception {
        // Given
        given(ticketService.getUserTickets(1L))
                .willReturn(testTicketDto);
        
        // When & Then
        mockMvc.perform(get("/api/v1/tickets/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.currentTickets").value(5))
                .andExpect(jsonPath("$.lastTicketRefill").exists())
                .andExpect(jsonPath("$.nextRefillAt").exists());
        
        verify(ticketService).getUserTickets(1L);
    }
    
    @Test
    @DisplayName("티켓 사용 API가 정상적으로 동작한다")
    void useTickets_Success() throws Exception {
        // Given
        TicketDto updatedTicketDto = TicketDto.builder()
                .id(1L)
                .userId(1L)
                .currentTickets(3) // 5 - 2
                .lastTicketRefill(testTicketDto.getLastTicketRefill())
                .nextRefillAt(testTicketDto.getNextRefillAt())
                .createdAt(testTicketDto.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        
        given(ticketService.useTickets(1L, 2, 123L, "Mission attempt"))
                .willReturn(true);
        given(ticketService.getUserTickets(1L))
                .willReturn(updatedTicketDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/tickets/users/1/use")
                        .param("amount", "2")
                        .param("attemptId", "123")
                        .param("reason", "Mission attempt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tickets used successfully"))
                .andExpect(jsonPath("$.tickets.currentTickets").value(3));
        
        verify(ticketService).useTickets(1L, 2, 123L, "Mission attempt");
        verify(ticketService).getUserTickets(1L);
    }
    
    @Test
    @DisplayName("티켓 부족 시 400 에러와 적절한 메시지를 반환한다")
    void useTickets_InsufficientTickets_Returns400() throws Exception {
        // Given
        given(ticketService.useTickets(1L, 10, 123L, "Mission attempt"))
                .willReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/v1/tickets/users/1/use")
                        .param("amount", "10")
                        .param("attemptId", "123")
                        .param("reason", "Mission attempt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Not enough tickets available"));
        
        verify(ticketService).useTickets(1L, 10, 123L, "Mission attempt");
        verify(ticketService, never()).getUserTickets(anyLong());
    }
    
    @Test
    @DisplayName("티켓 환불 API가 정상적으로 동작한다")
    void refundTickets_Success() throws Exception {
        // Given
        TicketDto refundedTicketDto = TicketDto.builder()
                .id(1L)
                .userId(1L)
                .currentTickets(7) // 5 + 2
                .lastTicketRefill(testTicketDto.getLastTicketRefill())
                .nextRefillAt(testTicketDto.getNextRefillAt())
                .createdAt(testTicketDto.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        
        doNothing().when(ticketService).refundTickets(1L, 2, 123L, "Failed mission");
        given(ticketService.getUserTickets(1L))
                .willReturn(refundedTicketDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/tickets/users/1/refund")
                        .param("amount", "2")
                        .param("attemptId", "123")
                        .param("reason", "Failed mission"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tickets refunded successfully"))
                .andExpect(jsonPath("$.tickets.currentTickets").value(7));
        
        verify(ticketService).refundTickets(1L, 2, 123L, "Failed mission");
        verify(ticketService).getUserTickets(1L);
    }
    
    @Test
    @DisplayName("티켓 조정 API가 정상적으로 동작한다")
    void adjustTickets_Success() throws Exception {
        // Given
        TicketDto adjustedTicketDto = TicketDto.builder()
                .id(1L)
                .userId(1L)
                .currentTickets(8) // 5 + 3
                .lastTicketRefill(testTicketDto.getLastTicketRefill())
                .nextRefillAt(testTicketDto.getNextRefillAt())
                .createdAt(testTicketDto.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        
        doNothing().when(ticketService).adjustTickets(1L, 3, "Admin bonus");
        given(ticketService.getUserTickets(1L))
                .willReturn(adjustedTicketDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/tickets/users/1/adjust")
                        .param("adjustment", "3")
                        .param("reason", "Admin bonus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tickets adjusted successfully"))
                .andExpect(jsonPath("$.tickets.currentTickets").value(8));
        
        verify(ticketService).adjustTickets(1L, 3, "Admin bonus");
        verify(ticketService).getUserTickets(1L);
    }
    
    @Test
    @DisplayName("음수 조정으로 티켓을 차감할 수 있다")
    void adjustTickets_NegativeAdjustment_Success() throws Exception {
        // Given
        TicketDto adjustedTicketDto = TicketDto.builder()
                .id(1L)
                .userId(1L)
                .currentTickets(3) // 5 - 2
                .lastTicketRefill(testTicketDto.getLastTicketRefill())
                .nextRefillAt(testTicketDto.getNextRefillAt())
                .createdAt(testTicketDto.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        
        doNothing().when(ticketService).adjustTickets(1L, -2, "Admin penalty");
        given(ticketService.getUserTickets(1L))
                .willReturn(adjustedTicketDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/tickets/users/1/adjust")
                        .param("adjustment", "-2")
                        .param("reason", "Admin penalty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tickets adjusted successfully"))
                .andExpect(jsonPath("$.tickets.currentTickets").value(3));
        
        verify(ticketService).adjustTickets(1L, -2, "Admin penalty");
        verify(ticketService).getUserTickets(1L);
    }
    
    @Test
    @DisplayName("티켓 자동 충전 API가 정상적으로 동작한다")
    void processTicketRefills_Success() throws Exception {
        // Given
        doNothing().when(ticketService).processTicketRefills();
        
        // When & Then
        mockMvc.perform(post("/api/v1/tickets/refill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value("true"))
                .andExpect(jsonPath("$.message").value("Ticket refills processed successfully"));
        
        verify(ticketService).processTicketRefills();
    }
    
    @Test
    @DisplayName("서비스 에러 발생 시 500 에러를 반환한다")
    void getUserTickets_ServiceError_Returns500() throws Exception {
        // Given
        given(ticketService.getUserTickets(1L))
                .willThrow(new RuntimeException("Database connection error"));
        
        // When & Then
        mockMvc.perform(get("/api/v1/tickets/users/1"))
                .andExpect(status().isInternalServerError());
        
        verify(ticketService).getUserTickets(1L);
    }
    
    @Test
    @DisplayName("필수 파라미터 없이 티켓 사용 시 적절한 에러를 반환한다")
    void useTickets_MissingRequiredParam_Returns400() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/tickets/users/1/use"))
                .andExpect(status().isBadRequest());
        
        verify(ticketService, never()).useTickets(anyLong(), anyInt(), anyLong(), anyString());
    }
    
    @Test
    @DisplayName("선택적 파라미터 없이도 티켓 사용이 가능하다")
    void useTickets_WithoutOptionalParams_Success() throws Exception {
        // Given
        TicketDto updatedTicketDto = TicketDto.builder()
                .id(1L)
                .userId(1L)
                .currentTickets(4) // 5 - 1
                .build();
        
        given(ticketService.useTickets(1L, 1, null, null))
                .willReturn(true);
        given(ticketService.getUserTickets(1L))
                .willReturn(updatedTicketDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/tickets/users/1/use")
                        .param("amount", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.tickets.currentTickets").value(4));
        
        verify(ticketService).useTickets(1L, 1, null, null);
        verify(ticketService).getUserTickets(1L);
    }
}