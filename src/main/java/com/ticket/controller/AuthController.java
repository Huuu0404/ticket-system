package com.ticket.controller;

import com.ticket.dto.AuthResponse;
import com.ticket.dto.LoginRequest;
import com.ticket.entity.User;
import com.ticket.service.UserService;
import com.ticket.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final UserService userService;
    private final JwtUtil jwtUtil;
    

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }


    /**
     * 用戶註冊
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User user) {
        try {
            User registeredUser = userService.register(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "註冊成功");
            response.put("userId", registeredUser.getId());
            response.put("username", registeredUser.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 用戶登入
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        var userOpt = userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
        
        if (userOpt.isPresent()) {
            String token = jwtUtil.generateToken(loginRequest.getUsername());
            
            return ResponseEntity.ok(new AuthResponse(token, loginRequest.getUsername()));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "用戶名或密碼錯誤"));
        }
    }
    
    /**
     * 測試Token驗證的端點
     */
    @GetMapping("/test")
    public ResponseEntity<?> testAuth() {
        return ResponseEntity.ok(Map.of("message", "認證成功！這個端點需要有效的JWT Token"));
    }
}