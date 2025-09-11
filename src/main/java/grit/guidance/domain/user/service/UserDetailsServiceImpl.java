package grit.guidance.domain.user.service;

import grit.guidance.domain.user.entity.Users;
import grit.guidance.domain.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsersRepository usersRepository;

    @Override
    public UserDetails loadUserByUsername(String studentId) throws UsernameNotFoundException {
        Users user = usersRepository.findByStudentId(studentId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + studentId));

        return new User(
                user.getStudentId(),
                "", // 비밀번호는 JWT에서 검증하므로 빈 문자열
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
