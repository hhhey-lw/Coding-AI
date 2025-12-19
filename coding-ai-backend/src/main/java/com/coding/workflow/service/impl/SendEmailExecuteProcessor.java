package com.coding.workflow.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.coding.workflow.enums.NodeStatusEnum;
import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.service.AbstractExecuteProcessor;
import com.coding.workflow.utils.JsonUtils;
import jakarta.mail.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;

import java.util.Map;
import java.util.Properties;

@Slf4j
@Component("SendEmailExecuteProcessor")
public class SendEmailExecuteProcessor extends AbstractExecuteProcessor {

    @Override
    public String getNodeType() {
        return NodeTypeEnum.EMAIL.getCode();
    }

    @Override
    public String getNodeDescription() {
        return NodeTypeEnum.EMAIL.getDesc();
    }

    @Override
    public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) throws InterruptedException {
        NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);

        try {
            // 1. 从node中获取参数
            Map<String, Object> nodeParam = node.getConfig().getNodeParam();
            EmailParam emailParam = JsonUtils.fromMap(nodeParam, EmailParam.class);

            // 2. 参数验证
            if (emailParam == null || StrUtil.isBlank(emailParam.getTo()) || StrUtil.isBlank(emailParam.getFrom()) || StrUtil.isBlank(emailParam.getAuthorization())
                    || StrUtil.isBlank(emailParam.getSubject()) || StrUtil.isBlank(emailParam.getContent())) {
                String failReason = String.format("邮件参数不完整, %s", JSONUtil.toJsonStr(emailParam));
                log.error(failReason);
                nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
                nodeResult.setErrorInfo(failReason);
                return nodeResult;
            }

            // 3. 替换模板变量
            String toEmail = replaceTemplateContent(emailParam.getTo(), context);
            String subject = replaceTemplateContent(emailParam.getSubject(), context);
            String content = replaceTemplateContent(emailParam.getContent(), context);
            String fromEmail = StrUtil.isNotBlank(emailParam.getFrom()) ?
                replaceTemplateContent(emailParam.getFrom(), context) : null;
            String authorization = emailParam.getAuthorization();

            // 4. 发送邮件
            boolean success;
            // 使用自定义认证发送邮件
            success = sendEmailWithAuth(toEmail, subject, content, fromEmail, authorization, emailParam.isHtml());

            // 5. 将结果写入nodeResult中
            nodeResult.setInput(JSONUtil.toJsonStr(Map.of(INPUT_DECORATE_PARAM_KEY, nodeParam)));
            if (success) {
                // nodeResult.setNodeStatus(NodeStatusEnum.SUCCESS.getCode());
                nodeResult.setOutput(JSONUtil.toJsonStr(Map.of(OUTPUT_DECORATE_PARAM_KEY, "邮件发送成功")));
                log.info("邮件发送成功，收件人: {}, 主题: {}", toEmail, subject);
            } else {
                nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
                nodeResult.setErrorInfo("邮件发送失败");
            }

        } catch (Exception e) {
            log.error("发送邮件异常", e);
            nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
            nodeResult.setErrorInfo("发送邮件异常: " + e.getMessage());
        }

        return nodeResult;
    }

    /**
     * 使用自定义认证信息发送邮件
     */
    private boolean sendEmailWithAuth(String to, String subject, String content, String from, String authorization, boolean isHtml) throws MessagingException {
        try {
            // 确定邮件服务器配置
            String mailHost = getMailHostFromEmail(from);
            int mailPort = getMailPortFromHost(mailHost);

            log.info("使用自定义认证发送邮件: from={}, to={}, host={}, port={}", from, to, mailHost, mailPort);

            // 设置邮件服务器属性
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", mailHost);
            props.put("mail.smtp.port", mailPort);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            // 创建认证器
            Authenticator authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(from, authorization);
                }
            };

            // 创建会话
            Session session = Session.getInstance(props, authenticator);

            // 创建消息
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, "UTF-8");

            if (isHtml) {
                message.setContent(content, "text/html; charset=UTF-8");
            } else {
                message.setText(content, "UTF-8");
            }

            // 发送邮件
            Transport.send(message);
            return true;

        } catch (Exception e) {
            log.error("使用自定义认证发送邮件失败, from: {}, to: {}, error: {}", from, to, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 根据发件人邮箱确定邮件服务器
     */
    private String getMailHostFromEmail(String email) {
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();

        // 常见邮件服务商的SMTP服务器配置
        return switch (domain) {
            case "qq.com", "vip.qq.com" -> "smtp.qq.com";
            case "163.com" -> "smtp.163.com";
            case "126.com" -> "smtp.126.com";
            case "gmail.com" -> "smtp.gmail.com";
            case "outlook.com", "hotmail.com" -> "smtp-mail.outlook.com";
            case "sina.com" -> "smtp.sina.com";
            case "sohu.com" -> "smtp.sohu.com";
            default ->
                // 如果无法识别，尝试使用通用格式
                    "smtp." + domain;
        };
    }

    /**
     * 根据邮件服务器确定端口
     */
    private int getMailPortFromHost(String host) {
        if (StrUtil.isBlank(host)) {
            return 587;
        }

        // 大部分现代邮件服务商都使用587端口（STARTTLS）
        if (host.contains("outlook") || host.contains("hotmail")) {
            return 587;
        } else if (host.contains("gmail")) {
            return 587;
        } else if (host.contains("qq")) {
            return 587;
        } else if (host.contains("163") || host.contains("126")) {
            return 25; // 163/126可能使用25端口
        }

        return 587; // 默认使用587端口
    }

    /**
     * 邮件参数配置
     */
    @Data
    public static class EmailParam {
        /**
         * 收件人邮箱
         */
        private String to;

        /**
         * 发件人邮箱（可选，如果不设置则使用默认配置）
         */
        private String from;

        /**
         * 发件人认证令牌/应用密码
         */
        private String authorization;

        /**
         * 邮件主题
         */
        private String subject;

        /**
         * 邮件内容
         */
        private String content;

        /**
         * 是否为HTML格式
         */
        private boolean html = false;
    }
}
