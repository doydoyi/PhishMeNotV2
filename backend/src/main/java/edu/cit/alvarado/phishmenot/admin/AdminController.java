package edu.cit.alvarado.phishmenot.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Administrator endpoints. Only users with the ADMIN role reach these; others receive 403. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public AdminService.AdminDashboardDto dashboard() {
        return adminService.dashboard();
    }

    @GetMapping("/users")
    public AdminService.UserPageDto users(@RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return adminService.listUsers(query, page, size);
    }
}
