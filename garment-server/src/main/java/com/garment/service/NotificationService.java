package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.garment.common.TenantContext;
import com.garment.dto.SendNotificationDTO;
import com.garment.entity.Notification;
import com.garment.entity.NotificationRead;
import com.garment.entity.User;
import com.garment.entity.UserCompany;
import com.garment.mapper.NotificationMapper;
import com.garment.mapper.NotificationReadMapper;
import com.garment.mapper.UserCompanyMapper;
import com.garment.mapper.UserMapper;
import com.garment.vo.NotificationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationReadMapper notificationReadMapper;
    private final UserCompanyMapper userCompanyMapper;
    private final UserMapper userMapper;

    public NotificationService(NotificationMapper notificationMapper,
                               NotificationReadMapper notificationReadMapper,
                               UserCompanyMapper userCompanyMapper,
                               UserMapper userMapper) {
        this.notificationMapper = notificationMapper;
        this.notificationReadMapper = notificationReadMapper;
        this.userCompanyMapper = userCompanyMapper;
        this.userMapper = userMapper;
    }

    /**
     * 发送通知（老板/主管）
     */
    public void sendNotification(SendNotificationDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();

        Notification notification = new Notification();
        notification.setCompanyId(companyId);
        notification.setSenderId(userId);
        notification.setTitle(dto.getTitle());
        notification.setContent(dto.getContent());
        notification.setTargetType(dto.getTargetType() != null ? dto.getTargetType() : 0);
        notificationMapper.insert(notification);
    }

    /**
     * 通知列表（发送端：老板/主管查看自己发的通知）
     */
    public List<NotificationVO> listSentNotifications() {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getCompanyId, companyId)
                .eq(Notification::getSenderId, userId)
                .orderByDesc(Notification::getCreateTime);
        List<Notification> notifications = notificationMapper.selectList(wrapper);

        // 统计每条通知的已读人数
        if (notifications.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> notifIds = notifications.stream().map(Notification::getId).collect(Collectors.toSet());
        LambdaQueryWrapper<NotificationRead> readWrapper = new LambdaQueryWrapper<>();
        readWrapper.in(NotificationRead::getNotificationId, notifIds);
        List<NotificationRead> reads = notificationReadMapper.selectList(readWrapper);
        Map<Long, Long> readCountMap = reads.stream()
                .collect(Collectors.groupingBy(NotificationRead::getNotificationId, Collectors.counting()));

        // 查询本公司符合条件的总人数
        int totalTarget = countTargetUsers(companyId, null);

        List<NotificationVO> voList = new ArrayList<>();
        for (Notification n : notifications) {
            NotificationVO vo = new NotificationVO();
            vo.setId(n.getId());
            vo.setTitle(n.getTitle());
            vo.setContent(n.getContent());
            vo.setTargetType(n.getTargetType());
            vo.setCreateTime(n.getCreateTime());
            vo.setRead(readCountMap.getOrDefault(n.getId(), 0L) >= totalTarget);
            voList.add(vo);
        }
        return voList;
    }

    /**
     * 员工端：获取通知列表（按时间倒序，标记已读状态）
     */
    public List<NotificationVO> listMyNotifications() {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();

        // 查当前用户角色
        LambdaQueryWrapper<UserCompany> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(UserCompany::getCompanyId, companyId)
                .eq(UserCompany::getUserId, userId)
                .eq(UserCompany::getStatus, 1);
        UserCompany uc = userCompanyMapper.selectOne(roleWrapper);
        String role = uc != null ? uc.getRole() : "worker";

        // 查通知：targetType=0全部，或匹配当前角色
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getCompanyId, companyId)
                .and(w -> {
                    w.eq(Notification::getTargetType, 0); // 全部
                    if ("supervisor".equals(role)) {
                        w.or().eq(Notification::getTargetType, 1); // 仅主管
                    } else {
                        w.or().eq(Notification::getTargetType, 2); // 仅工人
                    }
                })
                .orderByDesc(Notification::getCreateTime);
        List<Notification> notifications = notificationMapper.selectList(wrapper);

        if (notifications.isEmpty()) {
            return new ArrayList<>();
        }

        // 查当前用户已读的通知
        Set<Long> notifIds = notifications.stream().map(Notification::getId).collect(Collectors.toSet());
        LambdaQueryWrapper<NotificationRead> readWrapper = new LambdaQueryWrapper<>();
        readWrapper.eq(NotificationRead::getUserId, userId)
                .in(NotificationRead::getNotificationId, notifIds);
        Set<Long> readIds = notificationReadMapper.selectList(readWrapper).stream()
                .map(NotificationRead::getNotificationId)
                .collect(Collectors.toSet());

        // 查发送人姓名和角色（格式：名字第一个字+角色）
        Set<Long> senderIds = notifications.stream().map(Notification::getSenderId).collect(Collectors.toSet());
        Map<Long, String> senderNameMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            List<User> senders = userMapper.selectBatchIds(senderIds);
            Map<Long, String> userNameMap = new HashMap<>();
            for (User u : senders) {
                userNameMap.put(u.getId(), u.getRealName() != null ? u.getRealName() : u.getUsername());
            }

            LambdaQueryWrapper<UserCompany> senderRoleWrapper = new LambdaQueryWrapper<>();
            senderRoleWrapper.eq(UserCompany::getCompanyId, companyId)
                    .in(UserCompany::getUserId, senderIds)
                    .eq(UserCompany::getStatus, 1);
            List<UserCompany> senderRoles = userCompanyMapper.selectList(senderRoleWrapper);
            Map<Long, String> senderRoleMap = new HashMap<>();
            for (UserCompany senderUc : senderRoles) {
                senderRoleMap.putIfAbsent(senderUc.getUserId(), senderUc.getRole());
            }

            for (Long senderId : senderIds) {
                String name = userNameMap.getOrDefault(senderId, "未知");
                String senderRole = senderRoleMap.getOrDefault(senderId, "worker");
                String firstChar = name.length() > 0 ? name.substring(0, 1) : "";
                String roleLabel = "boss".equals(senderRole) ? "老板" : "supervisor".equals(senderRole) ? "主管" : "员工";
                senderNameMap.put(senderId, firstChar + roleLabel);
            }
        }

        List<NotificationVO> voList = new ArrayList<>();
        for (Notification n : notifications) {
            NotificationVO vo = new NotificationVO();
            vo.setId(n.getId());
            vo.setTitle(n.getTitle());
            vo.setContent(n.getContent());
            vo.setTargetType(n.getTargetType());
            vo.setSenderName(senderNameMap.getOrDefault(n.getSenderId(), "未知"));
            vo.setCreateTime(n.getCreateTime());
            vo.setRead(readIds.contains(n.getId()));
            voList.add(vo);
        }
        return voList;
    }

    /**
     * 标记通知已读
     */
    public void markAsRead(Long notificationId) {
        Long userId = TenantContext.getUserId();

        // 检查是否已读
        LambdaQueryWrapper<NotificationRead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationRead::getNotificationId, notificationId)
                .eq(NotificationRead::getUserId, userId);
        Long count = notificationReadMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            return; // 已读，不重复插入
        }

        NotificationRead read = new NotificationRead();
        read.setNotificationId(notificationId);
        read.setUserId(userId);
        read.setReadTime(LocalDateTime.now());
        notificationReadMapper.insert(read);
    }

    /**
     * 全部标记已读
     */
    @Transactional
    public void markAllAsRead() {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();

        // 查当前用户角色
        LambdaQueryWrapper<UserCompany> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(UserCompany::getCompanyId, companyId)
                .eq(UserCompany::getUserId, userId)
                .eq(UserCompany::getStatus, 1);
        UserCompany uc = userCompanyMapper.selectOne(roleWrapper);
        String role = uc != null ? uc.getRole() : "worker";

        // 查未读通知
        List<NotificationVO> notifications = listMyNotifications();
        List<Long> unreadIds = notifications.stream()
                .filter(n -> !n.isRead())
                .map(NotificationVO::getId)
                .collect(Collectors.toList());

        if (unreadIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Long notifId : unreadIds) {
            NotificationRead read = new NotificationRead();
            read.setNotificationId(notifId);
            read.setUserId(userId);
            read.setReadTime(now);
            notificationReadMapper.insert(read);
        }
    }

    /**
     * 未读通知计数
     */
    public int getUnreadCount() {
        List<NotificationVO> notifications = listMyNotifications();
        return (int) notifications.stream().filter(n -> !n.isRead()).count();
    }

    /**
     * 删除通知（发送人可删除自己发的通知）
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        Long userId = TenantContext.getUserId();
        Long companyId = TenantContext.getCompanyId();
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification != null && notification.getSenderId().equals(userId)
                && (companyId == null || notification.getCompanyId().equals(companyId))) {
            notificationMapper.deleteById(notificationId);
            // 同时删除已读记录
            LambdaQueryWrapper<NotificationRead> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NotificationRead::getNotificationId, notificationId);
            notificationReadMapper.delete(wrapper);
        }
    }

    /**
     * 获取通知已读详情（谁读了、谁没读）
     */
    public Map<String, Object> getNotificationReadDetail(Long notificationId) {
        Long companyId = TenantContext.getCompanyId();
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !notification.getCompanyId().equals(companyId)) {
            return null;
        }

        // 获取目标用户列表
        LambdaQueryWrapper<UserCompany> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(UserCompany::getCompanyId, companyId)
                .eq(UserCompany::getStatus, 1)
                .ne(UserCompany::getRole, "boss");
        if (notification.getTargetType() == 1) {
            userWrapper.eq(UserCompany::getRole, "supervisor");
        } else if (notification.getTargetType() == 2) {
            userWrapper.eq(UserCompany::getRole, "worker");
        }
        List<UserCompany> targetUsers = userCompanyMapper.selectList(userWrapper);

        if (targetUsers.isEmpty()) {
            return Map.of("readList", new ArrayList<>(), "unreadList", new ArrayList<>());
        }

        Set<Long> targetUserIds = targetUsers.stream().map(UserCompany::getUserId).collect(Collectors.toSet());
        Map<Long, String> userRoleMap = targetUsers.stream()
                .collect(Collectors.toMap(UserCompany::getUserId, UserCompany::getRole));

        // 获取已读记录
        LambdaQueryWrapper<NotificationRead> readWrapper = new LambdaQueryWrapper<>();
        readWrapper.eq(NotificationRead::getNotificationId, notificationId);
        List<NotificationRead> reads = notificationReadMapper.selectList(readWrapper);
        Set<Long> readUserIds = reads.stream().map(NotificationRead::getUserId).collect(Collectors.toSet());
        Map<Long, LocalDateTime> readTimeMap = reads.stream()
                .collect(Collectors.toMap(NotificationRead::getUserId, NotificationRead::getReadTime));

        // 获取用户信息
        List<User> users = userMapper.selectBatchIds(targetUserIds);
        Map<Long, String> userNameMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));

        // 分组已读/未读
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        List<Map<String, Object>> readList = new ArrayList<>();
        List<Map<String, Object>> unreadList = new ArrayList<>();

        for (Long userId : targetUserIds) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", userId);
            item.put("userName", userNameMap.getOrDefault(userId, "未知"));
            item.put("role", userRoleMap.getOrDefault(userId, "worker"));
            if (readUserIds.contains(userId)) {
                item.put("readTime", readTimeMap.get(userId) != null ? readTimeMap.get(userId).format(dtf) : "");
                readList.add(item);
            } else {
                unreadList.add(item);
            }
        }

        // 按角色排序：主管在前
        Comparator<Map<String, Object>> roleComparator = (a, b) -> {
            String roleA = (String) a.get("role");
            String roleB = (String) b.get("role");
            if ("supervisor".equals(roleA) && !"supervisor".equals(roleB)) return -1;
            if (!"supervisor".equals(roleA) && "supervisor".equals(roleB)) return 1;
            return 0;
        };
        readList.sort(roleComparator);
        unreadList.sort(roleComparator);

        return Map.of("readList", readList, "unreadList", unreadList);
    }

    /**
     * 统计目标用户数
     */
    private int countTargetUsers(Long companyId, Integer targetType) {
        LambdaQueryWrapper<UserCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCompany::getCompanyId, companyId)
                .eq(UserCompany::getStatus, 1)
                .ne(UserCompany::getRole, "boss");
        if (targetType != null) {
            if (targetType == 1) {
                wrapper.eq(UserCompany::getRole, "supervisor");
            } else if (targetType == 2) {
                wrapper.eq(UserCompany::getRole, "worker");
            }
        }
        Long count = userCompanyMapper.selectCount(wrapper);
        return count != null ? count.intValue() : 0;
    }
}
