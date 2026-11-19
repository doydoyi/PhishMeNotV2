package edu.cit.alvarado.phishmenot.admin;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.alvarado.phishmenot.user.Role;
import edu.cit.alvarado.phishmenot.user.UserDto;
import edu.cit.alvarado.phishmenot.user.UserRepository;

/**
 * Administrator operations. Guarded by both the URL rule in SecurityConfig and method security here,
 * so a mis-mapped endpoint still cannot leak admin data (FR-039, NFR-006).
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminService {

    public static final int MAX_PAGE_SIZE = 50;

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public record AdminDashboardDto(long totalUsers, long generalUsers, long administrators,
            long newUsersLast7Days, List<UserDto> recentUsers) {}

    public record UserPageDto(List<UserDto> users, int page, int size, long totalElements, int totalPages) {}

    @Transactional(readOnly = true)
    public AdminDashboardDto dashboard() {
        return new AdminDashboardDto(
                userRepository.count(),
                userRepository.countByRole(Role.USER),
                userRepository.countByRole(Role.ADMIN),
                userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(7)),
                userRepository.findTop5ByOrderByCreatedAtDesc().stream().map(UserDto::from).toList());
    }

    @Transactional(readOnly = true)
    public UserPageDto listUsers(String query, int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page must be >= 0 and size >= 1");
        }
        PageRequest pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "createdAt"));
        String q = query == null ? "" : query.trim();
        Page<UserDto> result = (q.isEmpty()
                ? userRepository.findAll(pageable)
                : userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q, pageable))
                .map(UserDto::from);
        return new UserPageDto(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
