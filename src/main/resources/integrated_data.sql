CREATE TABLE user (
                      id VARCHAR(36) PRIMARY KEY COMMENT '고유 식별자 (UUID 형식)',
                      email VARCHAR(255) UNIQUE NOT NULL COMMENT '이메일 주소 (로그인 ID)',
                      password_hash VARCHAR(255) COMMENT '암호화된 비밀번호 (BCrypt)',
                      name VARCHAR(100) NOT NULL COMMENT '사용자 실명',
                      role VARCHAR(20) DEFAULT 'USER' COMMENT '사용자 권한 (USER/INSTRUCTOR/ADMIN)',
                      social_provider VARCHAR(20) COMMENT '소셜 로그인 제공자 (GOOGLE/KAKAO)',
                      social_id VARCHAR(255) COMMENT '소셜 로그인 고유 ID',
                      current_tickets INT DEFAULT 0 COMMENT '현재 보유 미션 티켓 수',
                      email_verified BOOLEAN DEFAULT FALSE COMMENT '이메일 인증 완료 여부',
                      last_login_at TIMESTAMP COMMENT '마지막 로그인 시간',
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '계정 생성 시간',
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 수정 시간'
) COMMENT='사용자 인증 및 계정 관리 테이블';

-- 토큰 블랙리스트 테이블
CREATE TABLE token_blacklist (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '고유 식별자',
                                 token VARCHAR(500) NOT NULL COMMENT '무효화된 JWT 토큰',
                                 user_id VARCHAR(36) NOT NULL COMMENT '토큰 소유자 ID',
                                 expires_at TIMESTAMP NOT NULL COMMENT '토큰 만료 시간',
                                 blacklisted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '블랙리스트 등록 시간',
                                 reason VARCHAR(100) COMMENT '무효화 사유',
                                 INDEX idx_token (token(100)),
                                 INDEX idx_user_id (user_id),
                                 INDEX idx_expires_at (expires_at),
                                 FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='토큰 무효화 관리 테이블';

-- 로그인 시도 제한 테이블
CREATE TABLE login_attempts (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '고유 식별자',
                                email VARCHAR(255) NOT NULL COMMENT '로그인 시도 이메일',
                                ip_address VARCHAR(45) NOT NULL COMMENT '클라이언트 IP 주소',
                                attempt_count INT DEFAULT 1 COMMENT '연속 실패 횟수',
                                last_attempt_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '마지막 시도 시간',
                                locked_until TIMESTAMP COMMENT '계정 잠금 해제 시간',
                                INDEX idx_email_ip (email, ip_address),
                                INDEX idx_last_attempt (last_attempt_at)
) COMMENT='로그인 시도 추적 및 보안 강화';

-- =============================================
-- 2. USER MANAGEMENT SERVICE DATABASE
-- =============================================
CREATE DATABASE IF NOT EXISTS `devtrip-user-mgmt`;
USE `devtrip-user-mgmt`;

-- 확장된 사용자 프로필 테이블
CREATE TABLE user (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '내부 사용자 ID',
                      auth_user_id VARCHAR(36) UNIQUE NOT NULL COMMENT '인증 서비스 연동 ID',
                      email VARCHAR(255) UNIQUE NOT NULL COMMENT '이메일 주소',
                      name VARCHAR(100) NOT NULL COMMENT '사용자 실명',
                      nickname VARCHAR(50) COMMENT '닉네임',
                      phone VARCHAR(20) COMMENT '전화번호',
                      role VARCHAR(20) DEFAULT 'USER' COMMENT '사용자 역할',
                      status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '계정 상태 (ACTIVE/INACTIVE/SUSPENDED)',
                      subscription_plan VARCHAR(50) COMMENT '구독 플랜 (BASIC/PREMIUM/ENTERPRISE)',
                      profile_image_url VARCHAR(500) COMMENT '프로필 이미지 URL',
                      bio TEXT COMMENT '자기소개',
                      github_url VARCHAR(255) COMMENT 'GitHub 프로필 URL',
                      linkedin_url VARCHAR(255) COMMENT 'LinkedIn 프로필 URL',
                      total_missions_completed INT DEFAULT 0 COMMENT '완료한 미션 총 개수',
                      total_score INT DEFAULT 0 COMMENT '총 획득 점수',
                      current_streak INT DEFAULT 0 COMMENT '연속 학습일 수',
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '가입 일시',
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 업데이트',
                      INDEX idx_auth_user_id (auth_user_id),
                      INDEX idx_email (email),
                      INDEX idx_subscription_plan (subscription_plan)
) COMMENT='확장된 사용자 프로필 관리';

-- 팀 관리 테이블
CREATE TABLE team (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '팀 고유 식별자',
                      name VARCHAR(100) NOT NULL COMMENT '팀명',
                      description TEXT COMMENT '팀 설명',
                      team_code VARCHAR(10) UNIQUE NOT NULL COMMENT '팀 초대 코드',
                      instructor_id BIGINT COMMENT '담당 강사 ID',
                      max_members INT DEFAULT 10 COMMENT '최대 팀원 수',
                      current_members INT DEFAULT 0 COMMENT '현재 팀원 수',
                      status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '팀 상태',
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '팀 생성 일시',
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 업데이트',
                      INDEX idx_team_code (team_code),
                      INDEX idx_instructor_id (instructor_id),
                      FOREIGN KEY (instructor_id) REFERENCES user(id) ON DELETE SET NULL
) COMMENT='팀 기반 학습 관리';

-- 팀 멤버십 테이블
CREATE TABLE team_membership (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '멤버십 고유 식별자',
                                 team_id BIGINT NOT NULL COMMENT '팀 ID',
                                 user_id BIGINT NOT NULL COMMENT '사용자 ID',
                                 role VARCHAR(20) DEFAULT 'MEMBER' COMMENT '팀 내 역할 (LEADER/MEMBER)',
                                 joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '팀 가입 일시',
                                 status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '멤버십 상태',
                                 UNIQUE KEY unique_team_user (team_id, user_id),
                                 INDEX idx_team_id (team_id),
                                 INDEX idx_user_id (user_id),
                                 FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE,
                                 FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='팀-사용자 멤버십 관계';

-- 티켓 거래 내역 테이블
CREATE TABLE ticket_transaction (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '거래 고유 식별자',
                                    user_id BIGINT NOT NULL COMMENT '사용자 ID',
                                    transaction_type VARCHAR(20) NOT NULL COMMENT '거래 유형 (PURCHASE/CONSUME/REFUND/GRANT)',
                                    amount INT NOT NULL COMMENT '티켓 수량 (양수: 획득, 음수: 소비)',
                                    balance_after INT NOT NULL COMMENT '거래 후 잔액',
                                    source VARCHAR(50) COMMENT '티켓 출처 (SUBSCRIPTION/PURCHASE/MISSION/ADMIN)',
                                    source_id VARCHAR(100) COMMENT '출처 관련 ID (결제ID, 미션ID 등)',
                                    description VARCHAR(255) COMMENT '거래 설명',
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '거래 일시',
                                    INDEX idx_user_id_created (user_id, created_at),
                                    INDEX idx_transaction_type (transaction_type),
                                    INDEX idx_source (source, source_id),
                                    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='티켓 사용 및 충전 내역';

-- =============================================
-- 3. MISSION MANAGEMENT SERVICE DATABASE
-- =============================================
CREATE DATABASE IF NOT EXISTS `devtrip-mission`;
USE `devtrip-mission`;

-- 미션 정의 테이블
CREATE TABLE mission (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '미션 고유 식별자',
                         title VARCHAR(255) NOT NULL COMMENT '미션 제목',
                         description TEXT NOT NULL COMMENT '미션 설명',
                         mission_guide TEXT COMMENT '미션 가이드 (Markdown 형식)',
                         category VARCHAR(50) NOT NULL COMMENT '미션 카테고리 (DOCKER/KUBERNETES/CI_CD/MONITORING)',
                         difficulty VARCHAR(20) NOT NULL COMMENT '난이도 (BEGINNER/INTERMEDIATE/ADVANCED/EXPERT)',
                         estimated_duration INT COMMENT '예상 소요 시간 (분)',
                         max_duration INT DEFAULT 120 COMMENT '최대 허용 시간 (분)',

    -- 컨테이너 환경 설정
                         docker_image VARCHAR(255) DEFAULT 'devtrip/base:latest' COMMENT 'Docker 이미지',
                         tools_config JSON COMMENT '사전 설치된 도구 목록',
                         cpu_limit VARCHAR(20) DEFAULT '1000m' COMMENT 'CPU 제한 (millicores)',
                         memory_limit VARCHAR(20) DEFAULT '2Gi' COMMENT '메모리 제한',
                         storage_limit VARCHAR(20) DEFAULT '5Gi' COMMENT '스토리지 제한',

    -- 평가 기준
                         evaluation_criteria JSON NOT NULL COMMENT '평가 기준 및 배점',
                         prerequisites JSON COMMENT '사전 요구사항',
                         learning_objectives JSON COMMENT '학습 목표',

    -- 메타데이터
                         tags JSON COMMENT '검색용 태그',
                         is_team_mission BOOLEAN DEFAULT FALSE COMMENT '팀 미션 여부',
                         min_team_size INT DEFAULT 1 COMMENT '최소 팀 크기',
                         max_team_size INT DEFAULT 1 COMMENT '최대 팀 크기',
                         tickets_required INT DEFAULT 1 COMMENT '소모 티켓 수',

    -- 상태 관리
                         status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '미션 상태 (ACTIVE/INACTIVE/DRAFT)',
                         created_by BIGINT COMMENT '생성자 ID',
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                         INDEX idx_category (category),
                         INDEX idx_difficulty (difficulty),
                         INDEX idx_status (status),
                         INDEX idx_tags ((CAST(tags AS CHAR(255) ARRAY))),
                         FULLTEXT idx_search (title, description)
) COMMENT='미션 정의 및 환경 설정';

-- 미션 시도 테이블
CREATE TABLE mission_attempt (
                                 id VARCHAR(36) PRIMARY KEY COMMENT '시도 고유 식별자 (UUID)',
                                 mission_id BIGINT NOT NULL COMMENT '미션 ID',
                                 user_id BIGINT NOT NULL COMMENT '사용자 ID',
                                 team_id BIGINT COMMENT '팀 ID (팀 미션의 경우)',

    -- 상태 관리
                                 status VARCHAR(20) DEFAULT 'STARTED' COMMENT '시도 상태 (STARTED/PAUSED/COMPLETED/FAILED/TIMEOUT)',

    -- 시간 추적
                                 started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '시작 시간',
                                 paused_at TIMESTAMP COMMENT '일시정지 시간',
                                 resumed_at TIMESTAMP COMMENT '재개 시간',
                                 completed_at TIMESTAMP COMMENT '완료 시간',
                                 expires_at TIMESTAMP COMMENT '만료 시간',
                                 total_duration INT DEFAULT 0 COMMENT '총 소요 시간 (초)',

    -- Kubernetes 리소스 관리
                                 pod_name VARCHAR(100) COMMENT 'Pod 이름',
                                 namespace VARCHAR(100) DEFAULT 'devtrip-user' COMMENT 'Kubernetes 네임스페이스',
                                 pvc_name VARCHAR(100) COMMENT 'PVC 이름',
                                 service_name VARCHAR(100) COMMENT 'Service 이름',
                                 pod_ip VARCHAR(45) COMMENT 'Pod IP 주소',
                                 websocket_url VARCHAR(500) COMMENT 'WebSocket 터미널 URL',

    -- 평가 및 점수
                                 score INT DEFAULT 0 COMMENT '최종 점수',
                                 max_score INT DEFAULT 100 COMMENT '최대 점수',
                                 ai_evaluation_id VARCHAR(36) COMMENT 'AI 평가 결과 ID',

    -- 리소스 사용량
                                 tickets_used INT DEFAULT 1 COMMENT '사용한 티켓 수',
                                 cpu_usage_avg DECIMAL(10,2) COMMENT '평균 CPU 사용률 (%)',
                                 memory_usage_avg DECIMAL(10,2) COMMENT '평균 메모리 사용률 (%)',

    -- 성취 및 보상
                                 stamps_earned JSON COMMENT '획득한 스탬프 목록',
                                 achievements JSON COMMENT '달성한 업적 목록',

    -- 메타데이터
                                 submission_note TEXT COMMENT '제출 시 메모',
                                 instructor_feedback TEXT COMMENT '강사 피드백',

                                 INDEX idx_mission_user (mission_id, user_id),
                                 INDEX idx_user_status (user_id, status),
                                 INDEX idx_started_at (started_at),
                                 INDEX idx_pod_name (pod_name),
                                 FOREIGN KEY (mission_id) REFERENCES mission(id) ON DELETE CASCADE
) COMMENT='미션 수행 시도 및 리소스 관리';

-- 사용자 셸 환경 테이블
CREATE TABLE user_shell_environment (
                                        id VARCHAR(36) PRIMARY KEY COMMENT '환경 고유 식별자',
                                        user_id BIGINT NOT NULL COMMENT '사용자 ID',
                                        mission_attempt_id VARCHAR(36) NOT NULL COMMENT '미션 시도 ID',

    -- 셸 인증 정보
                                        shell_username VARCHAR(50) NOT NULL COMMENT '셸 사용자명',
                                        shell_password VARCHAR(255) NOT NULL COMMENT '셸 비밀번호 (암호화)',
                                        has_root_access BOOLEAN DEFAULT FALSE COMMENT 'root 권한 여부',

    -- Pod 할당 정보
                                        assigned_pod_name VARCHAR(100) COMMENT '할당된 Pod 이름',
                                        assigned_namespace VARCHAR(100) COMMENT '할당된 네임스페이스',
                                        pod_status VARCHAR(20) COMMENT 'Pod 상태',

    -- 접근 제어
                                        access_approved BOOLEAN DEFAULT FALSE COMMENT '접근 승인 여부',
                                        approved_by VARCHAR(100) COMMENT '승인자',
                                        approved_at TIMESTAMP COMMENT '승인 시간',

    -- 세션 관리
                                        last_accessed_at TIMESTAMP COMMENT '마지막 접근 시간',
                                        session_count INT DEFAULT 0 COMMENT '총 세션 수',

    -- 스냅샷 관리
                                        snapshot_count INT DEFAULT 0 COMMENT '생성된 스냅샷 수',
                                        last_snapshot_at TIMESTAMP COMMENT '마지막 스냅샷 시간',

                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '환경 생성 시간',
                                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 업데이트',

                                        INDEX idx_user_id (user_id),
                                        INDEX idx_mission_attempt (mission_attempt_id),
                                        INDEX idx_pod_name (assigned_pod_name),
                                        UNIQUE KEY unique_user_attempt (user_id, mission_attempt_id),
                                        FOREIGN KEY (mission_attempt_id) REFERENCES mission_attempt(id) ON DELETE CASCADE
) COMMENT='사용자별 셸 환경 관리';

-- 미션 제출 테이블
CREATE TABLE mission_submission (
                                    id VARCHAR(36) PRIMARY KEY COMMENT '제출 고유 식별자',
                                    mission_attempt_id VARCHAR(36) NOT NULL COMMENT '미션 시도 ID',

    -- 제출 내용
                                    github_repo_url VARCHAR(500) COMMENT 'GitHub 저장소 URL',
                                    deployment_url VARCHAR(500) COMMENT '배포된 애플리케이션 URL',
                                    submission_note TEXT COMMENT '제출 시 메모',
                                    submission_files JSON COMMENT '제출 파일 목록',

    -- AI 평가 결과
                                    ai_score INT COMMENT 'AI 평가 점수',
                                    ai_feedback JSON COMMENT 'AI 상세 피드백',
                                    ai_evaluated_at TIMESTAMP COMMENT 'AI 평가 완료 시간',

    -- 상태 및 검토
                                    status VARCHAR(20) DEFAULT 'SUBMITTED' COMMENT '제출 상태 (SUBMITTED/REVIEWED/APPROVED/REJECTED)',
                                    reviewed_by BIGINT COMMENT '검토자 ID',
                                    reviewed_at TIMESTAMP COMMENT '검토 완료 시간',
                                    instructor_score INT COMMENT '강사 부여 점수',
                                    instructor_feedback TEXT COMMENT '강사 피드백',

                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '제출 시간',
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 수정 시간',

                                    INDEX idx_attempt_id (mission_attempt_id),
                                    INDEX idx_status (status),
                                    INDEX idx_created_at (created_at),
                                    FOREIGN KEY (mission_attempt_id) REFERENCES mission_attempt(id) ON DELETE CASCADE
) COMMENT='미션 제출 및 검토 관리';

-- 명령어 로그 테이블
CREATE TABLE command_logs (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '로그 고유 식별자',
                              mission_attempt_id VARCHAR(36) NOT NULL COMMENT '미션 시도 ID',
                              user_id BIGINT NOT NULL COMMENT '사용자 ID',

    -- 명령어 정보
                              command TEXT NOT NULL COMMENT '실행된 명령어',
                              command_type VARCHAR(50) COMMENT '명령어 유형 분류',
                              working_directory VARCHAR(500) COMMENT '실행 시 작업 디렉토리',

    -- 실행 결과
                              exit_code INT DEFAULT 0 COMMENT '명령어 종료 코드',
                              stdout TEXT COMMENT '표준 출력',
                              stderr TEXT COMMENT '표준 에러',
                              execution_time_ms BIGINT COMMENT '실행 시간 (밀리초)',

    -- 컨텍스트 정보
                              step_number INT COMMENT '미션 진행 단계',
                              environment_variables JSON COMMENT '환경 변수 스냅샷',
                              process_id VARCHAR(20) COMMENT '프로세스 ID',

    -- AI 분석용 태그
                              is_significant BOOLEAN DEFAULT FALSE COMMENT '중요 명령어 여부 (AI 분석용)',
                              command_category VARCHAR(50) COMMENT '명령어 카테고리 (DOCKER/K8S/GIT/FILE)',
                              risk_level VARCHAR(20) COMMENT '보안 위험 수준 (LOW/MEDIUM/HIGH)',

    -- 타임스탬프
                              executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '명령어 실행 시간',

                              INDEX idx_attempt_id (mission_attempt_id),
                              INDEX idx_user_id (user_id),
                              INDEX idx_executed_at (executed_at),
                              INDEX idx_command_type (command_type),
                              INDEX idx_significant (is_significant),
                              FOREIGN KEY (mission_attempt_id) REFERENCES mission_attempt(id) ON DELETE CASCADE
) COMMENT='셸 명령어 실행 로그 및 AI 분석 데이터';

-- 미션 저장 포인트 테이블
CREATE TABLE mission_save (
                              id VARCHAR(36) PRIMARY KEY COMMENT '저장 고유 식별자',
                              mission_attempt_id VARCHAR(36) NOT NULL COMMENT '미션 시도 ID',

    -- 저장 메타데이터
                              save_sequence INT NOT NULL COMMENT '저장 순서',
                              save_level VARCHAR(20) DEFAULT 'STANDARD' COMMENT '저장 수준 (LIGHTWEIGHT/STANDARD/COMPREHENSIVE)',
                              save_trigger VARCHAR(30) NOT NULL COMMENT '저장 트리거 (AUTO_TIMER/COMMAND_CHECKPOINT/USER_MANUAL/PAUSE_TRIGGER)',
                              save_note VARCHAR(500) COMMENT '저장 시 메모',

    -- S3 저장 정보
                              saved_data VARCHAR(500) NOT NULL COMMENT 'S3 저장 경로',
                              data_size_bytes BIGINT COMMENT '저장된 데이터 크기 (바이트)',
                              compression_enabled BOOLEAN DEFAULT TRUE COMMENT '압축 사용 여부',
                              encryption_enabled BOOLEAN DEFAULT TRUE COMMENT '암호화 사용 여부',

    -- 진행 상황 스냅샷
                              current_step VARCHAR(100) COMMENT '현재 진행 단계',
                              progress_percent INT DEFAULT 0 COMMENT '진행률 (%)',
                              step_score INT DEFAULT 0 COMMENT '단계별 점수',
                              completed_checkpoints JSON COMMENT '완료된 체크포인트 목록',

    -- 환경 상태
                              shell_state JSON COMMENT '셸 환경 상태',
                              file_changes JSON COMMENT '파일 시스템 변경 사항',
                              running_processes JSON COMMENT '실행 중인 프로세스 목록',

    -- 성능 메트릭
                              cpu_usage DECIMAL(5,2) COMMENT '저장 시점 CPU 사용률 (%)',
                              memory_usage DECIMAL(5,2) COMMENT '저장 시점 메모리 사용률 (%)',
                              disk_usage BIGINT COMMENT '디스크 사용량 (바이트)',

                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '저장 시간',
                              restored_at TIMESTAMP COMMENT '복원 시간 (해당하는 경우)',

                              INDEX idx_attempt_sequence (mission_attempt_id, save_sequence),
                              INDEX idx_save_trigger (save_trigger),
                              INDEX idx_created_at (created_at),
                              FOREIGN KEY (mission_attempt_id) REFERENCES mission_attempt(id) ON DELETE CASCADE
) COMMENT='미션 진행 상황 체크포인트 및 복원 시스템';

-- =============================================
-- 4. AI EVALUATION SERVICE DATABASE
-- =============================================
CREATE DATABASE IF NOT EXISTS `devtrip-ai-evaluation`;
USE `devtrip-ai-evaluation`;

-- AI 평가 결과 테이블
CREATE TABLE ai_evaluation (
                               id VARCHAR(36) PRIMARY KEY COMMENT 'AI 평가 고유 식별자 (UUID)',

    -- 연결 정보
                               mission_attempt_id VARCHAR(36) NOT NULL COMMENT '미션 시도 ID',
                               mission_id BIGINT NOT NULL COMMENT '미션 ID (캐시)',
                               user_id BIGINT NOT NULL COMMENT '사용자 ID (캐시)',
                               mission_title VARCHAR(255) COMMENT '미션 제목 (캐시)',

    -- AI 모델 정보
                               ai_model_version VARCHAR(50) DEFAULT 'gemini-1.5-pro' COMMENT '사용된 AI 모델 버전',
                               model_temperature DECIMAL(3,2) DEFAULT 0.7 COMMENT 'AI 모델 온도 설정',
                               max_tokens INT DEFAULT 2000 COMMENT '최대 토큰 수',

    -- 평가 상태
                               status VARCHAR(30) DEFAULT 'PENDING' COMMENT '평가 상태 (PENDING/PROCESSING/COMPLETED/FAILED/TIMEOUT)',
                               processing_started_at TIMESTAMP COMMENT '처리 시작 시간',
                               processing_completed_at TIMESTAMP COMMENT '처리 완료 시간',
                               processing_duration_ms BIGINT COMMENT '처리 소요 시간 (밀리초)',

    -- 입력 데이터
                               s3_log_path VARCHAR(500) COMMENT '명령어 로그 S3 경로',
                               s3_workspace_path VARCHAR(500) COMMENT '작업공간 데이터 S3 경로',
                               command_count INT DEFAULT 0 COMMENT '분석된 명령어 수',
                               file_count INT DEFAULT 0 COMMENT '분석된 파일 수',

    -- DevOps 영역별 점수 (0-100)
                               overall_score INT DEFAULT 0 COMMENT '전체 점수',
                               correctness_score INT DEFAULT 0 COMMENT '정확성 점수',
                               efficiency_score INT DEFAULT 0 COMMENT '효율성 점수',
                               security_score INT DEFAULT 0 COMMENT '보안 점수',
                               best_practices_score INT DEFAULT 0 COMMENT '모범 사례 점수',
                               innovation_score INT DEFAULT 0 COMMENT '창의성 점수',

    -- 상세 분석 결과
                               command_analysis_summary JSON COMMENT '명령어 분석 요약',
                               dockerfile_analysis JSON COMMENT 'Dockerfile 분석 결과',
                               kubernetes_analysis JSON COMMENT 'Kubernetes 매니페스트 분석',
                               security_analysis JSON COMMENT '보안 취약점 분석',
                               performance_analysis JSON COMMENT '성능 분석 결과',

    -- AI 피드백
                               ai_feedback_summary TEXT COMMENT 'AI 종합 피드백',
                               improvement_suggestions JSON COMMENT '개선 제안사항',
                               learning_resources JSON COMMENT '추천 학습 자료',
                               next_level_recommendations JSON COMMENT '다음 단계 추천',

    -- 에러 및 디버깅
                               error_message TEXT COMMENT '에러 메시지 (실패 시)',
                               retry_count INT DEFAULT 0 COMMENT '재시도 횟수',
                               last_retry_at TIMESTAMP COMMENT '마지막 재시도 시간',

    -- 품질 메트릭
                               ai_confidence_score DECIMAL(5,2) COMMENT 'AI 신뢰도 점수 (0-100)',
                               evaluation_complexity VARCHAR(20) COMMENT '평가 복잡도 (LOW/MEDIUM/HIGH)',
                               token_usage INT COMMENT '실제 사용된 토큰 수',

                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '평가 요청 시간',
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 업데이트',

                               INDEX idx_mission_attempt (mission_attempt_id),
                               INDEX idx_status (status),
                               INDEX idx_mission_user (mission_id, user_id),
                               INDEX idx_processing_started (processing_started_at),
                               INDEX idx_overall_score (overall_score)
) COMMENT='AI 기반 미션 평가 결과 및 분석';

-- AI 평가 요약 테이블 (최적화된 조회용)
CREATE TABLE evaluation_summary (
                                    id VARCHAR(36) PRIMARY KEY COMMENT '요약 고유 식별자',
                                    ai_evaluation_id VARCHAR(36) NOT NULL COMMENT 'AI 평가 ID',
                                    mission_attempt_id VARCHAR(36) NOT NULL COMMENT '미션 시도 ID',
                                    user_id BIGINT NOT NULL COMMENT '사용자 ID',

    -- 핵심 점수
                                    overall_score INT NOT NULL COMMENT '전체 점수',
                                    correctness_score INT NOT NULL COMMENT '정확성 점수',
                                    efficiency_score INT NOT NULL COMMENT '효율성 점수',
                                    quality_score INT NOT NULL COMMENT '품질 점수',

    -- 통계 정보
                                    total_commands INT DEFAULT 0 COMMENT '총 명령어 수',
                                    successful_commands INT DEFAULT 0 COMMENT '성공한 명령어 수',
                                    error_commands INT DEFAULT 0 COMMENT '실패한 명령어 수',
                                    average_execution_time DECIMAL(10,3) COMMENT '평균 실행 시간 (초)',

    -- 보안 분석
                                    security_risk_level VARCHAR(20) DEFAULT 'LOW' COMMENT '보안 위험 수준',
                                    security_violations JSON COMMENT '보안 위반 사항',

    -- 성취 및 배지
                                    stamps_earned JSON COMMENT '획득한 스탬프',
                                    badges_unlocked JSON COMMENT '해금된 배지',
                                    points_awarded INT DEFAULT 0 COMMENT '부여된 포인트',

    -- 호환성 필드 (기존 시스템과의 호환성)
                                    legacy_score INT GENERATED ALWAYS AS (overall_score) STORED COMMENT '레거시 점수 (호환성)',
                                    grade VARCHAR(2) GENERATED ALWAYS AS (
                                        CASE
                                            WHEN overall_score >= 90 THEN 'A+'
                                            WHEN overall_score >= 85 THEN 'A'
                                            WHEN overall_score >= 80 THEN 'B+'
                                            WHEN overall_score >= 75 THEN 'B'
                                            WHEN overall_score >= 70 THEN 'C+'
                                            WHEN overall_score >= 65 THEN 'C'
                                            WHEN overall_score >= 60 THEN 'D'
                                            ELSE 'F'
                                            END
                                        ) STORED COMMENT '자동 계산된 등급',

                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '요약 생성 시간',

                                    UNIQUE KEY unique_evaluation (ai_evaluation_id),
                                    INDEX idx_mission_attempt (mission_attempt_id),
                                    INDEX idx_user_score (user_id, overall_score),
                                    INDEX idx_created_at (created_at),
                                    FOREIGN KEY (ai_evaluation_id) REFERENCES ai_evaluation(id) ON DELETE CASCADE
) COMMENT='AI 평가 결과 최적화된 요약 테이블';

-- 평가 이력 테이블
CREATE TABLE evaluation_history (
