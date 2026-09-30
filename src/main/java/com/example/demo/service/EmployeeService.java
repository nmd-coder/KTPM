package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionManagementService sessionManagementService;
    private final com.example.demo.repository.OrderRepository orderRepository;
    private final com.example.demo.repository.TimeSheetRepository timeSheetRepository;
    private final com.example.demo.repository.SalaryRepository salaryRepository;
    
    /**
     * Get all employees (STAFF and ADMIN roles only)
     */
    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAllEmployees() {
        List<User> employees = userRepository.findAll().stream()
            .filter(user -> user.getRole() == UserRole.STAFF || user.getRole() == UserRole.ADMIN)
            .collect(Collectors.toList());
        
        return employees.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Get employees with pagination
     */
    @Transactional(readOnly = true)
    public Page<EmployeeDTO> getEmployees(Pageable pageable) {
        // Get all users and filter for employees only
        List<User> allUsers = userRepository.findAll();
        List<User> employees = allUsers.stream()
            .filter(user -> user.getRole() == UserRole.STAFF || user.getRole() == UserRole.ADMIN)
            .collect(Collectors.toList());
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), employees.size());
        
        List<EmployeeDTO> employeeDTOs = employees.subList(start, end).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(employeeDTOs, pageable, employees.size());
    }
    
    /**
     * Get employee statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEmployeeStats() {
        List<User> allEmployees = userRepository.findAll().stream()
            .filter(user -> user.getRole() == UserRole.STAFF || user.getRole() == UserRole.ADMIN)
            .collect(Collectors.toList());
        
        long total = allEmployees.size();
        long active = allEmployees.stream().filter(User::getIsActive).count();
        long staff = allEmployees.stream().filter(u -> u.getRole() == UserRole.STAFF).count();
        long admin = allEmployees.stream().filter(u -> u.getRole() == UserRole.ADMIN).count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("active", active);
        stats.put("staff", staff);
        stats.put("admin", admin);
        
        return stats;
    }
    
    /**
     * Get employee by ID
     */
    @Transactional(readOnly = true)
    public EmployeeDTO getEmployeeById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhân viên với ID: " + id));
        
        if (user.getRole() != UserRole.STAFF && user.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Người dùng không phải là nhân viên");
        }
        
        return convertToDTO(user);
    }
    
    /**
     * Create new employee
     */
    public EmployeeDTO createEmployee(CreateEmployeeRequest request) {
        log.info("Creating new employee with email: {}", request.getEmail());
        
        // Validate email and phone uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
        }
        
        // Validate role
        if (request.getRole() != UserRole.STAFF && request.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Vai trò không hợp lệ cho nhân viên");
        }
        
        User employee = new User();
        employee.setFirstname(request.getFirstname());
        employee.setLastname(request.getLastname());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setPassword(passwordEncoder.encode(request.getPassword()));
        employee.setRole(request.getRole());
        // Set default position and department for staff
        if (request.getRole() == UserRole.STAFF) {
            employee.setPosition("Nhân viên");
            employee.setDepartment("Bán hàng");
        } else if (request.getRole() == UserRole.ADMIN) {
            employee.setPosition("Quản trị viên");
            employee.setDepartment("Quản lý");
        }
        employee.setSalaryPerHour(request.getSalaryPerHour());
        employee.setHireDate(request.getHireDate() != null ? request.getHireDate() : java.time.LocalDate.now());
        employee.setIsActive(true);
        employee.setAvatar(request.getAvatar());
        
        User savedEmployee = userRepository.save(employee);
        log.info("Employee created successfully with ID: {}", savedEmployee.getId());
        
        return convertToDTO(savedEmployee);
    }
    
    /**
     * Update employee
     */
    public EmployeeDTO updateEmployee(Long id, UpdateEmployeeRequest request) {
        log.info("Updating employee with ID: {}", id);
        
        User employee = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhân viên với ID: " + id));
        
        if (employee.getRole() != UserRole.STAFF && employee.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Người dùng không phải là nhân viên");
        }
        
        // Update fields if provided
        if (request.getFirstname() != null) {
            employee.setFirstname(request.getFirstname());
        }
        if (request.getLastname() != null) {
            employee.setLastname(request.getLastname());
        }
        if (request.getEmail() != null && !request.getEmail().equals(employee.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email đã được sử dụng");
            }
            employee.setEmail(request.getEmail());
        }
        if (request.getPhone() != null && !request.getPhone().equals(employee.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
            }
            employee.setPhone(request.getPhone());
        }
        if (request.getRole() != null) {
            if (request.getRole() != UserRole.STAFF && request.getRole() != UserRole.ADMIN) {
                throw new IllegalArgumentException("Vai trò không hợp lệ cho nhân viên");
            }
            employee.setRole(request.getRole());
            // Update position and department based on role
            if (request.getRole() == UserRole.STAFF) {
                employee.setPosition("Nhân viên");
                employee.setDepartment("Bán hàng");
            } else if (request.getRole() == UserRole.ADMIN) {
                employee.setPosition("Quản trị viên");
                employee.setDepartment("Quản lý");
            }
        }
        if (request.getSalaryPerHour() != null) {
            employee.setSalaryPerHour(request.getSalaryPerHour());
        }
        if (request.getHireDate() != null) {
            employee.setHireDate(request.getHireDate());
        }
        if (request.getIsActive() != null) {
            employee.setIsActive(request.getIsActive());
        }
        if (request.getAvatar() != null) {
            employee.setAvatar(request.getAvatar());
        }
        
        User updatedEmployee = userRepository.save(employee);
        log.info("Employee updated successfully with ID: {}", id);
        
        return convertToDTO(updatedEmployee);
    }
    
    /**
     * Delete employee - checks for related data first
     */
    public void deleteEmployee(Long id) {
        log.info("Attempting to delete employee with ID: {}", id);
        
        User employee = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhân viên với ID: " + id));
        
        if (employee.getRole() != UserRole.STAFF && employee.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Người dùng không phải là nhân viên");
        }
        
        // Check for related data before deletion
        String relatedDataError = checkRelatedData(employee);
        if (relatedDataError != null) {
            log.warn("Cannot delete employee ID {} due to related data", id);
            throw new IllegalStateException(relatedDataError);
        }
        
        // Safe to delete - no related data exists
        userRepository.delete(employee);
        log.info("Employee deleted successfully with ID: {}", id);
    }
    
    /**
     * Toggle employee active status
     */
    public EmployeeDTO toggleEmployeeStatus(Long id) {
        log.info("Toggling status for employee with ID: {}", id);
        
        User employee = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhân viên với ID: " + id));
        
        if (employee.getRole() != UserRole.STAFF && employee.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Người dùng không phải là nhân viên");
        }
        
        boolean wasActive = employee.getIsActive();
        employee.setIsActive(!employee.getIsActive());
        User updatedEmployee = userRepository.save(employee);
        
        // If user was disabled (active -> inactive), invalidate all their sessions
        if (wasActive && !updatedEmployee.getIsActive()) {
            log.info("Employee was disabled, invalidating all sessions for: {}", updatedEmployee.getEmail());
            sessionManagementService.invalidateUserSessions(updatedEmployee.getEmail());
        }
        
        log.info("Employee status toggled successfully with ID: {}", id);
        
        return convertToDTO(updatedEmployee);
    }
    
    /**
     * Search employees by keyword
     */
    @Transactional(readOnly = true)
    public List<EmployeeDTO> searchEmployees(String keyword) {
        List<User> users = userRepository.searchUsers(keyword);
        return users.stream()
            .filter(user -> user.getRole() == UserRole.STAFF || user.getRole() == UserRole.ADMIN)
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Search employees by keyword with pagination
     */
    @Transactional(readOnly = true)
    public Page<EmployeeDTO> searchEmployees(String keyword, Pageable pageable) {
        List<User> allUsers = userRepository.searchUsers(keyword);
        List<User> employees = allUsers.stream()
            .filter(user -> user.getRole() == UserRole.STAFF || user.getRole() == UserRole.ADMIN)
            .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), employees.size());
        
        List<EmployeeDTO> employeeDTOs = employees.subList(start, end).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(employeeDTOs, pageable, employees.size());
    }
    
    /**
     * Get employees by role with pagination
     */
    @Transactional(readOnly = true)
    public Page<EmployeeDTO> getEmployeesByRole(String roleStr, Pageable pageable) {
        UserRole role = UserRole.valueOf(roleStr.toUpperCase());
        List<User> allUsers = userRepository.findAll();
        List<User> employees = allUsers.stream()
            .filter(user -> user.getRole() == role)
            .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), employees.size());
        
        List<EmployeeDTO> employeeDTOs = employees.subList(start, end).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(employeeDTOs, pageable, employees.size());
    }
    
    /**
     * Get employees by active status with pagination
     */
    @Transactional(readOnly = true)
    public Page<EmployeeDTO> getEmployeesByStatus(boolean isActive, Pageable pageable) {
        List<User> allUsers = userRepository.findAll();
        List<User> employees = allUsers.stream()
            .filter(user -> (user.getRole() == UserRole.STAFF || user.getRole() == UserRole.ADMIN))
            .filter(user -> user.getIsActive() == isActive)
            .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), employees.size());
        
        List<EmployeeDTO> employeeDTOs = employees.subList(start, end).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(employeeDTOs, pageable, employees.size());
    }
    
    /**
     * Get employees with combined filters (role and/or status)
     */
    @Transactional(readOnly = true)
    public Page<EmployeeDTO> getEmployeesFiltered(String roleStr, Boolean isActive, Pageable pageable) {
        List<User> allUsers = userRepository.findAll();
        
        // Start with all employees (STAFF and ADMIN)
        java.util.stream.Stream<User> stream = allUsers.stream()
            .filter(user -> user.getRole() == UserRole.STAFF || user.getRole() == UserRole.ADMIN);
        
        // Apply role filter if provided
        if (roleStr != null && !roleStr.trim().isEmpty()) {
            UserRole role = UserRole.valueOf(roleStr.toUpperCase());
            stream = stream.filter(user -> user.getRole() == role);
        }
        
        // Apply status filter if provided
        if (isActive != null) {
            stream = stream.filter(user -> user.getIsActive() == isActive);
        }
        
        List<User> employees = stream.collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), employees.size());
        
        List<EmployeeDTO> employeeDTOs = employees.subList(start, end).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(employeeDTOs, pageable, employees.size());
    }
    
    /**
     * Check if user has related data that prevents deletion
     * @return error message if has related data, null if safe to delete
     */
    private String checkRelatedData(User user) {
        List<String> relatedData = new ArrayList<>();
        
        // Check orders
        long orderCount = orderRepository.countByUserId(user.getId());
        if (orderCount > 0) {
            relatedData.add(orderCount + " đơn hàng");
        }
        
        // Check timesheets (for employees)
        if (user.getRole() == UserRole.STAFF || user.getRole() == UserRole.ADMIN) {
            long timesheetCount = timeSheetRepository.countByStaffId(user.getId());
            if (timesheetCount > 0) {
                relatedData.add(timesheetCount + " bản ghi chấm công");
            }
            
            // Check salaries
            long salaryCount = salaryRepository.countByUserId(user.getId());
            if (salaryCount > 0) {
                relatedData.add(salaryCount + " bản ghi lương");
            }
        }
        
        if (!relatedData.isEmpty()) {
            return "Không thể xóa vì có dữ liệu liên quan: " + String.join(", ", relatedData) + 
                   ". Vui lòng sử dụng chức năng 'Vô hiệu hóa' thay vì xóa.";
        }
        
        return null;
    }
    
    /**
     * Convert User entity to EmployeeDTO
     */
    private EmployeeDTO convertToDTO(User user) {
        // Calculate work statistics - safe null handling
        Long totalShifts = 0L;
        java.math.BigDecimal totalHours = java.math.BigDecimal.ZERO;
        Long monthsWorked = 0L;
        
        try {
            if (user.getTimeSheets() != null) {
                totalShifts = (long) user.getTimeSheets().size();
                totalHours = user.getTimeSheets().stream()
                    .map(ts -> ts.getHoursWorked() != null ? ts.getHoursWorked() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                monthsWorked = user.getTimeSheets().stream()
                    .map(ts -> java.time.YearMonth.from(ts.getDate()))
                    .distinct()
                    .count();
            }
        } catch (Exception e) {
            log.warn("Could not load timesheet data for employee {}: {}", user.getId(), e.getMessage());
        }
        
        return EmployeeDTO.builder()
            .id(user.getId())
            .employeeCode(user.getEmployeeCode())
            .firstname(user.getFirstname())
            .lastname(user.getLastname())
            .email(user.getEmail())
            .phone(user.getPhone())
            .avatar(user.getAvatar())
            .role(user.getRole())
            .position(user.getPosition())
            .department(user.getDepartment())
            .salaryPerHour(user.getSalaryPerHour())
            .hireDate(user.getHireDate())
            .isActive(user.getIsActive())
            .lastLogin(user.getLastLogin())
            .createdAt(user.getCreatedAt())
            .totalShifts(totalShifts)
            .totalHoursWorked(totalHours)
            .monthsWorked(monthsWorked)
            .build();
    }
}

