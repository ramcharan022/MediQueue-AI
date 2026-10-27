package com.queueless.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.queueless.security.JwtService;
import com.queueless.queue.Queue;
import com.queueless.queue.QueueRepository;
import com.queueless.user.StaffAssignment;
import com.queueless.user.StaffAssignmentRepository;
import com.queueless.user.User;
import com.queueless.user.UserRepository;
import org.springframework.web.socket.config.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Pattern STAFF_QUEUE_TOPIC = Pattern.compile("^/topic/queues/\\d+/updates$");
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final QueueRepository queueRepository;
    private final StaffAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final List<String> allowedOrigins;

    public WebSocketConfig(JwtService jwtService, UserDetailsService userDetailsService,
                           QueueRepository queueRepository, StaffAssignmentRepository assignmentRepository,
                           UserRepository userRepository,
                           @Value("${app.cors.allowed-origins}") String origins) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.queueRepository = queueRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.allowedOrigins = Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) return message;
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authorization = accessor.getFirstNativeHeader("Authorization");
                    if (authorization == null || !authorization.startsWith("Bearer ")) {
                        throw new AccessDeniedException("A bearer token is required.");
                    }
                    String jwt = authorization.substring(7);
                    String email = jwtService.extractUsername(jwt);
                    UserDetails user = userDetailsService.loadUserByUsername(email);
                    if (!user.isEnabled() || !jwtService.isTokenValid(jwt, user)) {
                        throw new AccessDeniedException("The bearer token is invalid or expired.");
                    }
                    accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    if (!(accessor.getUser() instanceof Authentication authentication)) {
                        throw new AccessDeniedException("Authentication is required.");
                    }
                    String destination = accessor.getDestination();
                    boolean patientUpdates = "/user/queue/updates".equals(destination)
                            && AuthorityUtils.authorityListToSet(authentication.getAuthorities()).contains("ROLE_PATIENT");
                    boolean staffUpdates = isAuthorizedStaffSubscription(authentication, destination);
                    if (!patientUpdates && !staffUpdates) throw new AccessDeniedException("Subscription is not permitted.");
                } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                    throw new AccessDeniedException("Client messages are not supported.");
                }
                return message;
            }
        });
    }

    private boolean isAuthorizedStaffSubscription(Authentication authentication, String destination) {
        var authorities = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
        if (authorities.contains("ROLE_ADMIN")) return destination != null && STAFF_QUEUE_TOPIC.matcher(destination).matches();
        if (!authorities.contains("ROLE_STAFF") || destination == null) return false;
        var matcher = STAFF_QUEUE_TOPIC.matcher(destination);
        if (!matcher.matches()) return false;

        Long queueId = Long.valueOf(matcher.group(1));
        Queue queue = queueRepository.findById(queueId).orElse(null);
        User staff = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (queue == null || staff == null) return false;
        StaffAssignment assignment = assignmentRepository.findByUserId(staff.getId()).orElse(null);
        return assignment != null
                && assignment.getBranchId().equals(queue.getBranchId())
                && assignment.getDepartmentId().equals(queue.getDepartmentId());
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new))
                .withSockJS();
        registry.addEndpoint("/ws-native")
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }
}
