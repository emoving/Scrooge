package com.scrooge.scrooge.service.member;

import com.scrooge.scrooge.domain.*;
import com.scrooge.scrooge.domain.member.Member;
import com.scrooge.scrooge.domain.member.MemberOwningAvatar;
import com.scrooge.scrooge.dto.member.*;
import com.scrooge.scrooge.config.jwt.JwtTokenProvider;
import com.scrooge.scrooge.repository.*;
import com.scrooge.scrooge.repository.member.MemberOwningAvatarRepository;
import com.scrooge.scrooge.repository.member.MemberOwningBadgeRepository;
import com.scrooge.scrooge.repository.member.MemberRepository;
import com.scrooge.scrooge.repository.member.MemberSelectedQuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final LevelRepository levelRepository;
    private final MemberOwningAvatarRepository memberOwningAvatarRepository;
    private final AvatarRepository avatarRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtTokenProvider jwtTokenProvider;


    public LoginResponseDto login(LoginRequestDto loginRequestDto) {

        String email = loginRequestDto.getEmail();
        String password = loginRequestDto.getPassword();

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("해당 이메일을 찾을 수 없습니다."));

        if (bCryptPasswordEncoder.matches(password, member.getPassword())) {
            LoginResponseDto loginResponseDto = new LoginResponseDto();
            loginResponseDto.setToken(jwtTokenProvider.createToken(email, member.getId()));
            loginResponseDto.setMemberId(member.getId());
            return loginResponseDto;
        } else {
            throw new NotFoundException("비밀번호가 일치하지 않습니다.");
        }

    }

    public void signUp(SignUpRequestDto signUpRequestDto) {
        if (memberRepository.existsMemberByEmail(signUpRequestDto.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 회원입니다.");
        }

        if (!signUpRequestDto.getPassword1().equals(signUpRequestDto.getPassword2())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        Avatar avatar = avatarRepository.findById(1L).orElse(null);

        String encodedPassword = bCryptPasswordEncoder.encode(signUpRequestDto.getPassword1());

        Member member = new Member();
//        member.setName(signUpRequestDto.getName());
        member.setNickname(signUpRequestDto.getNickname());
        member.setEmail(signUpRequestDto.getEmail());
        member.setPassword(encodedPassword);

        // 기본 값
        member.setMessage("상태메시지를 설정해주세요.");
        member.setExp(0);
        member.setStreak(0);
        member.setWeeklyConsum(0);
        member.setWeeklyGoal(0);
        member.setMainAvatar(avatar);
        member.setRemainGacha(2);
        member.setJoinedAt(LocalDateTime.now());

        Level defaultLevel = levelRepository.findById(1L).orElse(null);
        member.setLevel(defaultLevel);

        memberRepository.save(member);

        MemberOwningAvatar memberOwningAvatar = new MemberOwningAvatar();
        memberOwningAvatar.setMember(member);
        memberOwningAvatar.setAvatar(avatar);
        memberOwningAvatar.setAcquiredAt(LocalDateTime.now());
        memberOwningAvatarRepository.save(memberOwningAvatar);
    }

    public Optional<MemberDto> getInfo(String email) {
        return memberRepository.findWithRelatedEntitiesByEmail(email).map(MemberDto::new);
    }

    public MemberDto updateWeeklyGoal(UpdateWeeklyGoalDto updateWeeklyGoalDto, Long memberId) {
        Optional<Member> optionalMember = memberRepository.findWithRelatedEntitiesById(memberId);
        if (optionalMember.isPresent()) {
            // 주간 목표 설정
            Member member = optionalMember.get();
            member.setWeeklyGoal(updateWeeklyGoalDto.getWeeklyGoal());

            // DB에 업데이트 된 사용자 저장
            memberRepository.save(member);
            return new MemberDto(member);
        }
        else {
            throw new NotFoundException(memberId + "의 ID을 가진 사용자를 찾을 수 없습니다.");
        }
    }

    public MemberDto updatePassword(UpdatePasswordDto updatePasswordDto, Long memberId) {
        Member member = memberRepository.findWithRelatedEntitiesById(memberId)
                .orElseThrow(() -> new NotFoundException("해당 유저를 찾을 수 없습니다."));

        if (!bCryptPasswordEncoder.matches(updatePasswordDto.getCurrentPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호를 다시 입력해주세요.");
        }

        member.setPassword(bCryptPasswordEncoder.encode(updatePasswordDto.getNewPassword()));
        return new MemberDto(member);
    }

    public void deleteMember(Long memberId) {
        memberRepository.deleteById(memberId);
    }

    public MemberDto updateMessage(Long memberId, String message) {
        Member member = memberRepository.findWithRelatedEntitiesById(memberId)
                .orElseThrow(() -> new NotFoundException("해당 멤버를 찾을 수 없습니다."));

        member.setMessage(message);
        memberRepository.save(member);
        return new MemberDto(member);
    }
}


