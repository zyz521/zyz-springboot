package com.example.xianyu.service;

import com.example.xianyu.entity.Message;
import com.example.xianyu.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * 发送系统消息
     */
    public Message sendSystemMessage(Long userId, String title, String content, String type) {
        Message message = new Message();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type != null ? type : "system");
        message.setReadFlag(false);
        return messageRepository.save(message);
    }

    /**
     * 发送买家-卖家聊天消息
     */
    public Message sendChatMessage(Long senderId, String senderName, Long targetUserId,
                                   Long productId, String productTitle, String content) {
        Message message = new Message();
        message.setUserId(targetUserId);
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setProductId(productId);
        message.setTitle("关于商品：" + (productTitle != null ? productTitle : ""));
        message.setContent(content);
        message.setType("chat");
        message.setReadFlag(false);
        return messageRepository.save(message);
    }

    public List<Message> listByUser(Long userId) {
        return messageRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    public long countUnread(Long userId) {
        Long count = messageRepository.countByUserIdAndReadFlagFalse(userId);
        return count == null ? 0 : count;
    }

    public void markRead(Long id, Long userId) {
        Optional<Message> optional = messageRepository.findById(id);
        if (optional.isPresent()) {
            Message m = optional.get();
            if (m.getUserId().equals(userId) && !Boolean.TRUE.equals(m.getReadFlag())) {
                m.setReadFlag(true);
                messageRepository.save(m);
            }
        }
    }

    public void markAllRead(Long userId) {
        List<Message> list = messageRepository.findByUserIdOrderByCreateTimeDesc(userId);
        boolean changed = false;
        for (Message m : list) {
            if (!Boolean.TRUE.equals(m.getReadFlag())) {
                m.setReadFlag(true);
                changed = true;
            }
        }
        if (changed) {
            messageRepository.saveAll(list);
        }
    }

    /**
     * 会话消息（双向）
     */
    public List<Message> listThread(Long productId, Long userId, Long otherUserId) {
        return messageRepository.findThread(productId, userId, otherUserId);
    }

    /**
     * 标记某个会话中"我收到的"消息为已读
     */
    public void markThreadRead(Long productId, Long me, Long other) {
        List<Message> unread = messageRepository.findUnreadInThread(productId, me, other);
        if (!unread.isEmpty()) {
            unread.forEach(m -> m.setReadFlag(true));
            messageRepository.saveAll(unread);
        }
    }

    /**
     * 发送鱼塘帖子评论消息
     */
    public Message sendPondCommentMessage(Long senderId, String senderName, Long targetUserId,
                                         Long postId, String postContent, String commentContent) {
        Message message = new Message();
        message.setUserId(targetUserId);
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setPostId(postId);
        // 截取帖子内容前30个字符作为标题
        String title = postContent != null && postContent.length() > 30 
            ? postContent.substring(0, 30) + "..." 
            : (postContent != null ? postContent : "鱼塘动态");
        message.setTitle("评论了你的动态：" + title);
        message.setContent(commentContent);
        message.setType("pond");
        message.setReadFlag(false);
        return messageRepository.save(message);
    }

    /**
     * 标记某个帖子的所有未读消息为已读
     */
    public void markPostMessagesRead(Long postId, Long userId) {
        List<Message> unread = messageRepository.findUnreadByPostId(postId, userId);
        if (!unread.isEmpty()) {
            unread.forEach(m -> m.setReadFlag(true));
            messageRepository.saveAll(unread);
        }
    }

    /**
     * 删除消息线程（删除该线程的所有消息）
     */
    public void deleteThread(Long productId, Long postId, Long userId, Long otherUserId) {
        if (postId != null && otherUserId != null) {
            // 删除鱼塘消息线程：删除该用户与该帖子相关的所有消息
            List<Message> allMessages = messageRepository.findByUserIdOrderByCreateTimeDesc(userId);
            List<Message> toDelete = allMessages.stream()
                .filter(m -> m.getPostId() != null && m.getPostId().equals(postId) && 
                    ((m.getUserId().equals(userId) && m.getSenderId() != null && m.getSenderId().equals(otherUserId)) ||
                     (m.getSenderId() != null && m.getSenderId().equals(userId) && m.getUserId().equals(otherUserId))))
                .toList();
            if (!toDelete.isEmpty()) {
                messageRepository.deleteAll(toDelete);
            }
        } else if (productId != null && otherUserId != null) {
            // 删除商品聊天消息线程
            List<Message> threadMessages = messageRepository.findThread(productId, userId, otherUserId);
            if (!threadMessages.isEmpty()) {
                messageRepository.deleteAll(threadMessages);
            }
        } else if (postId != null) {
            // 删除整个帖子的所有消息（无otherUserId时）
            List<Message> allMessages = messageRepository.findByUserIdOrderByCreateTimeDesc(userId);
            List<Message> toDelete = allMessages.stream()
                .filter(m -> m.getPostId() != null && m.getPostId().equals(postId))
                .toList();
            if (!toDelete.isEmpty()) {
                messageRepository.deleteAll(toDelete);
            }
        }
    }
}

