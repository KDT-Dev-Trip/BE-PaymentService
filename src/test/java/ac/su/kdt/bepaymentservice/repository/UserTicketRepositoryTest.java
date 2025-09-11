package ac.su.kdt.bepaymentservice.repository;

import ac.su.kdt.bepaymentservice.entity.UserTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserTicket Repository 테스트")
class UserTicketRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserTicketRepository userTicketRepository;
    
    private UserTicket testUserTicket;
    
    @BeforeEach
    void setUp() {
        testUserTicket = UserTicket.builder()
                .userId(123L)
                .currentTickets(100)
                .build();
    }
    
    @Test
    @DisplayName("사용자 티켓 저장 및 조회")
    void saveAndFindUserTicket_Success() {
        // given
        UserTicket savedTicket = entityManager.persistAndFlush(testUserTicket);
        
        // when
        Optional<UserTicket> foundTicket = userTicketRepository.findById(savedTicket.getId());
        
        // then
        assertThat(foundTicket).isPresent();
        assertThat(foundTicket.get().getUserId()).isEqualTo(123L);
        assertThat(foundTicket.get().getCurrentTickets()).isEqualTo(100);
    }
    
    @Test
    @DisplayName("사용자 ID로 티켓 조회")
    void findByUserId_ReturnsTicket() {
        // given
        entityManager.persistAndFlush(testUserTicket);
        
        // when
        Optional<UserTicket> foundTicket = userTicketRepository.findByUserId(123L);
        
        // then
        assertThat(foundTicket).isPresent();
        assertThat(foundTicket.get().getCurrentTickets()).isEqualTo(100);
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자의 티켓 조회")
    void findByNonExistentUserId_ReturnsEmpty() {
        // when
        Optional<UserTicket> ticket = userTicketRepository.findByUserId(999L);
        
        // then
        assertThat(ticket).isEmpty();
    }
}