package ac.su.kdt.bepaymentservice.repository;

import ac.su.kdt.bepaymentservice.entity.UserTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("UserTicketRepository 테스트")
class UserTicketRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserTicketRepository userTicketRepository;
    
    private UserTicket testUserTicket;
    
    @BeforeEach
    void setUp() {
        testUserTicket = UserTicket.builder()
                .userId(1L)
                .currentTickets(5)
                .lastTicketRefill(LocalDateTime.now().minusHours(2))
                .nextRefillAt(LocalDateTime.now().plusHours(22))
                .build();
        
        testUserTicket = entityManager.persistAndFlush(testUserTicket);
        entityManager.clear();
    }
    
    @Test
    @DisplayName("사용자 ID로 티켓 정보를 조회할 수 있다")
    void findByUserId_Success() {
        // When
        Optional<UserTicket> result = userTicketRepository.findByUserId(1L);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(1L);
        assertThat(result.get().getCurrentTickets()).isEqualTo(5);
        assertThat(result.get().getLastTicketRefill()).isNotNull();
        assertThat(result.get().getNextRefillAt()).isNotNull();
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회 시 빈 결과를 반환한다")
    void findByUserId_NotFound_ReturnsEmpty() {
        // When
        Optional<UserTicket> result = userTicketRepository.findByUserId(999L);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("충전이 필요한 사용자들을 조회할 수 있다")
    void findUsersEligibleForRefill_Success() {
        // Given - 충전이 필요한 사용자 추가 생성
        UserTicket eligibleUser1 = UserTicket.builder()
                .userId(2L)
                .currentTickets(2)
                .lastTicketRefill(LocalDateTime.now().minusHours(25))
                .nextRefillAt(LocalDateTime.now().minusHours(1)) // 이미 충전 시간이 지남
                .build();
        
        UserTicket eligibleUser2 = UserTicket.builder()
                .userId(3L)
                .currentTickets(1)
                .lastTicketRefill(LocalDateTime.now().minusHours(30))
                .nextRefillAt(LocalDateTime.now().minusMinutes(30)) // 이미 충전 시간이 지남
                .build();
        
        UserTicket notEligibleUser = UserTicket.builder()
                .userId(4L)
                .currentTickets(3)
                .lastTicketRefill(LocalDateTime.now().minusHours(1))
                .nextRefillAt(LocalDateTime.now().plusHours(23)) // 아직 충전 시간이 안 됨
                .build();
        
        entityManager.persistAndFlush(eligibleUser1);
        entityManager.persistAndFlush(eligibleUser2);
        entityManager.persistAndFlush(notEligibleUser);
        entityManager.clear();
        
        LocalDateTime now = LocalDateTime.now();
        
        // When
        List<UserTicket> result = userTicketRepository.findUsersEligibleForRefill(now);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserTicket::getUserId)
                .containsExactlyInAnyOrder(2L, 3L);
        
        // 모든 결과의 nextRefillAt이 현재 시간보다 이전이어야 함
        assertThat(result).allMatch(userTicket -> userTicket.getNextRefillAt().isBefore(now));
    }
    
    @Test
    @DisplayName("최소 티켓 수 이상을 보유한 사용자 수를 조회할 수 있다")
    void countUsersWithMinimumTickets_Success() {
        // Given - 다양한 티켓 수를 가진 사용자들 생성
        UserTicket user2 = UserTicket.builder()
                .userId(2L)
                .currentTickets(3)
                .build();
        
        UserTicket user3 = UserTicket.builder()
                .userId(3L)
                .currentTickets(1)
                .build();
        
        UserTicket user4 = UserTicket.builder()
                .userId(4L)
                .currentTickets(7)
                .build();
        
        entityManager.persistAndFlush(user2);
        entityManager.persistAndFlush(user3);
        entityManager.persistAndFlush(user4);
        entityManager.clear();
        
        // When
        Long count = userTicketRepository.countUsersWithMinimumTickets(3);
        
        // Then
        assertThat(count).isEqualTo(3L); // testUserTicket(5), user2(3), user4(7)
    }
    
    @Test
    @DisplayName("평균 티켓 잔액을 조회할 수 있다")
    void getAverageTicketBalance_Success() {
        // Given - 추가 사용자들 생성
        UserTicket user2 = UserTicket.builder()
                .userId(2L)
                .currentTickets(3)
                .build();
        
        UserTicket user3 = UserTicket.builder()
                .userId(3L)
                .currentTickets(7)
                .build();
        
        entityManager.persistAndFlush(user2);
        entityManager.persistAndFlush(user3);
        entityManager.clear();
        
        // When
        Double average = userTicketRepository.getAverageTicketBalance();
        
        // Then
        assertThat(average).isEqualTo(5.0); // (5 + 3 + 7) / 3 = 5.0
    }
    
    @Test
    @DisplayName("티켓이 0인 사용자들을 조회할 수 있다")
    void findUsersWithZeroTickets_Success() {
        // Given - 티켓이 0인 사용자들 생성
        UserTicket zeroTicketUser1 = UserTicket.builder()
                .userId(2L)
                .currentTickets(0)
                .lastTicketRefill(LocalDateTime.now().minusHours(1))
                .nextRefillAt(LocalDateTime.now().plusHours(23))
                .build();
        
        UserTicket zeroTicketUser2 = UserTicket.builder()
                .userId(3L)
                .currentTickets(0)
                .lastTicketRefill(LocalDateTime.now().minusHours(3))
                .nextRefillAt(LocalDateTime.now().plusHours(21))
                .build();
        
        UserTicket nonZeroTicketUser = UserTicket.builder()
                .userId(4L)
                .currentTickets(2)
                .build();
        
        entityManager.persistAndFlush(zeroTicketUser1);
        entityManager.persistAndFlush(zeroTicketUser2);
        entityManager.persistAndFlush(nonZeroTicketUser);
        entityManager.clear();
        
        // When
        List<UserTicket> result = userTicketRepository.findUsersWithZeroTickets();
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserTicket::getUserId)
                .containsExactlyInAnyOrder(2L, 3L);
        assertThat(result).allMatch(userTicket -> userTicket.getCurrentTickets() == 0);
    }
    
    @Test
    @DisplayName("데이터가 없을 때 평균 티켓 잔액이 null을 반환한다")
    void getAverageTicketBalance_NoData_ReturnsNull() {
        // Given - 모든 데이터 삭제
        entityManager.getEntityManager()
                .createQuery("DELETE FROM UserTicket")
                .executeUpdate();
        entityManager.clear();
        
        // When
        Double average = userTicketRepository.getAverageTicketBalance();
        
        // Then
        assertThat(average).isNull();
    }
    
    @Test
    @DisplayName("충전이 필요한 사용자가 없을 때 빈 리스트를 반환한다")
    void findUsersEligibleForRefill_NoEligibleUsers_ReturnsEmpty() {
        // Given - 모든 사용자의 nextRefillAt을 미래로 설정
        testUserTicket.setNextRefillAt(LocalDateTime.now().plusHours(5));
        entityManager.merge(testUserTicket);
        entityManager.flush();
        entityManager.clear();
        
        LocalDateTime now = LocalDateTime.now();
        
        // When
        List<UserTicket> result = userTicketRepository.findUsersEligibleForRefill(now);
        
        // Then
        assertThat(result).isEmpty();
    }
}