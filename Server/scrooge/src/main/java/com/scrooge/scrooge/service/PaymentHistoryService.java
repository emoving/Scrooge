package com.scrooge.scrooge.service;

import com.scrooge.scrooge.domain.PaymentHistory;
import com.scrooge.scrooge.domain.member.Member;
import com.scrooge.scrooge.dto.DateTimeReqDto;
import com.scrooge.scrooge.dto.paymentHistory.PaymentHistoryDto;
import com.scrooge.scrooge.dto.member.MemberDto;
import com.scrooge.scrooge.repository.member.MemberOwningBadgeRepository;
import com.scrooge.scrooge.dto.paymentHistory.PaymentHistoryRespDto;
import com.scrooge.scrooge.repository.member.MemberRepository;
import com.scrooge.scrooge.repository.PaymentHistoryRepository;
import com.scrooge.scrooge.repository.member.MemberSelectedQuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentHistoryService {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final MemberRepository memberRepository;
    private final QuestService questService;
    private final MemberSelectedQuestRepository memberSelectedQuestRepository;
    private final BadgeService badgeService;
    private final MemberOwningBadgeRepository memberOwningBadgeRepository;

    @Transactional
    public PaymentHistoryRespDto addPaymentHistory(Long memberId, PaymentHistoryDto paymentHistoryDto) {
        PaymentHistory paymentHistory = new PaymentHistory();
        PaymentHistoryRespDto paymentHistoryRespDto = new PaymentHistoryRespDto();

        paymentHistory.setAmount(paymentHistoryDto.getAmount());
        paymentHistory.setUsedAt(paymentHistoryDto.getUsedAt());
        paymentHistory.setCardName(paymentHistoryDto.getCardName());

        /* 연결 */

        // memberId에 맞는 member 가져오기
        Optional<Member> member = memberRepository.findById(memberId);
        if(member.isPresent()) {
            paymentHistory.setMember(member.get());

            member.get().setWeeklyConsum(member.get().getWeeklyConsum() + paymentHistoryDto.getAmount()); // 주간 소비량에 소비내역 가격 더해주기
            memberRepository.save(member.get());

            paymentHistoryRepository.save(paymentHistory);

            paymentHistoryRespDto.setSuccess(1);
            paymentHistoryRespDto.setId(paymentHistory.getId());

            return paymentHistoryRespDto;
        }
        else {
            throw new NotFoundException(memberId + "에 해당하는 member를 찾을 수 없습니다.");
        }
    }

    // userId에 따른 전체 소비 내역 조회
    public List<PaymentHistoryDto> getPaymentHistoryByMemberId(Long memberId, DateTimeReqDto dateTimeReqDto) {
        String dateStr = dateTimeReqDto.getDate();

        // 날짜 문자열을 LocalDate로 변환하기
        LocalDate date = LocalDate.parse(dateStr);
        System.out.println(date);

        List<PaymentHistory> paymentHistories = paymentHistoryRepository.findByMemberId(memberId);

        List<PaymentHistory> filteredPaymentHistories = paymentHistories.stream()
                .filter(paymentHistory -> paymentHistory.getPaidAt().toLocalDate().equals(date))
                .collect(Collectors.toList());

        return filteredPaymentHistories.stream()
                .map(PaymentHistoryDto::new)
                .collect(Collectors.toList());
    }

    // userId의 오늘 전체 소비 내역 조회
    public List<PaymentHistoryDto> getPaymentHistoryByMemberIdToday(Long memberId) {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        List<PaymentHistory> paymentHistories = paymentHistoryRepository.findByMemberIdAndPaidAtBetween(memberId, todayStart, todayEnd);
        return paymentHistories.stream()
                .map(PaymentHistoryDto::new)
                .collect(Collectors.toList());
    }

    // userId 각각의 paymentHistory 조회
    public PaymentHistoryDto getPaymentHistoryEach(Long memberId, Long paymentHistoryId) {
        PaymentHistory paymentHistory = paymentHistoryRepository.findByIdAndMemberId(paymentHistoryId, memberId);
        if(paymentHistory == null) return null;
        return new PaymentHistoryDto(paymentHistory);
    }

    // 소비내역을 수정하는 비즈니스 로직
    public PaymentHistory updatePaymentHistory(Long memberId, PaymentHistoryDto paymentHistoryDto) throws AccessDeniedException {
        PaymentHistory paymentHistory = paymentHistoryRepository.findById(paymentHistoryDto.getId())
                .orElseThrow(() -> new EntityNotFoundException("PaymentHistory not found with id: " + paymentHistoryDto.getId()));

        // 입력받은 memberId와 findById로 찾은 paymentHistory의 memberId가 같은지 확인
        if(!paymentHistory.getMember().getId().equals(memberId)) {
            throw new AccessDeniedException("memberId가 PaymentHistory의 userId와 다릅니다.");
        }

        // 이전 : paymentHistory 수정 후 : paymentHistoryDto
        if(!paymentHistory.getAmount().equals(paymentHistoryDto.getAmount())) {
            // memberId에 맞는 member 가져오기
            Optional<Member> member = memberRepository.findById(memberId);
            if(member.isPresent()) {
                member.get().setWeeklyConsum(member.get().getWeeklyConsum() - paymentHistory.getAmount() + paymentHistoryDto.getAmount());
                memberRepository.save(member.get());
            }
            else {
                throw new NotFoundException(memberId + "를 가진 사용자는 존재하지 않습니다.");
            }
        }

        // 변경사항 반영
        paymentHistory.setAmount(paymentHistoryDto.getAmount());
        paymentHistory.setCardName(paymentHistoryDto.getCardName());
        paymentHistory.setCategory(paymentHistoryDto.getCategory());
        paymentHistory.setUsedAt(paymentHistoryDto.getUsedAt());

        return paymentHistoryRepository.save(paymentHistory);
    }


    public MemberDto updateExpAfterDailySettlement(Long memberId) {
        Optional<Member> member =  memberRepository.findById(memberId);
        if(member.isPresent()) {
            // 경험치 +100 정산해주기
            member.get().setExp(member.get().getExp() + 100);
            // streak 1 증가
            int newStreak = member.get().getStreak() + 1;
            member.get().setStreak(newStreak);

            // 정산하기 관련 퀘스트 소지 시 퀘스트 완료 진행
            if (memberSelectedQuestRepository.existsByMemberIdAndQuestId(memberId, 1L)) {
                questService.completeQuest(1L, memberId);
            }

            // 정산하기 관련 뱃지 획득
            // 첫번째 정산하기 완료시 뱃지 부여
            if (newStreak == 1 || memberOwningBadgeRepository.existsByBadgeIdAndMemberId(1L, memberId)) {
                badgeService.giveBadge(1L, memberId);
            }
            // 7번째 정산하기 완료시 뱃시 증정
            if (newStreak == 7 || memberOwningBadgeRepository.existsByBadgeIdAndMemberId(2L, memberId)) {
                badgeService.giveBadge(2L, memberId);
            }
            // 30번째 정산하기 완료시 뱃지 증정
            if (newStreak == 30 || memberOwningBadgeRepository.existsByBadgeIdAndMemberId(3L, memberId)) {
                badgeService.giveBadge(3L, memberId);
            }

            Member updatedMember = memberRepository.save(member.get());
            return new MemberDto(updatedMember);
        }
        else {
            throw new NotFoundException("해당 Member가 존재하지 않습니다.");
        }
    }

    // 하루 전체 소비 금액 조회하는 API
    public Integer getTodayTotalConsumption(Long memberId) {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        List<PaymentHistory> paymentHistories = paymentHistoryRepository.findByMemberIdAndPaidAtBetween(memberId, todayStart, todayEnd);

        Integer todayTotalConsumption = 0;

        for(PaymentHistory paymentHistory : paymentHistories) {
            todayTotalConsumption += paymentHistory.getAmount();
        }

        return todayTotalConsumption;
    }
}
