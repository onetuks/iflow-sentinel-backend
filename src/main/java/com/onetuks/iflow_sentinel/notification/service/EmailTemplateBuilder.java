package com.onetuks.iflow_sentinel.notification.service;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.reprocess.dto.MplFailureResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class EmailTemplateBuilder {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String dashboardBaseUrl;

    public EmailTemplateBuilder(@Value("${app.dashboard-base-url:http://localhost:3000}") String dashboardBaseUrl) {
        this.dashboardBaseUrl = dashboardBaseUrl;
    }

    /**
     * 테넌트 실패 메시지 리포트 HTML 본문 생성
     */
    public String buildFailureReportHtml(Tenant tenant, List<MplFailureResponse> failures, LocalDateTime reportTime) {
        String formattedReportTime = reportTime != null ? reportTime.format(TIME_FORMATTER) : LocalDateTime.now().format(TIME_FORMATTER);
        String tenantName = HtmlUtils.htmlEscape(tenant.getName());
        int totalCount = failures.size();

        StringBuilder rows = new StringBuilder();
        int displayLimit = Math.min(totalCount, 30);

        for (int i = 0; i < displayLimit; i++) {
            MplFailureResponse f = failures.get(i);
            String artifactName = HtmlUtils.htmlEscape(f.artifactName() != null ? f.artifactName() : (f.artifactId() != null ? f.artifactId() : "Unknown"));
            String messageId = HtmlUtils.htmlEscape(f.messageId() != null ? f.messageId() : "-");
            String status = HtmlUtils.htmlEscape(f.status() != null ? f.status() : "FAILED");
            String errorDetail = HtmlUtils.htmlEscape(f.errorDetail() != null && !f.errorDetail().isBlank()
                    ? f.errorDetail()
                    : "No error detail returned by SAP Integration Suite");
            String logTime = f.logStart() != null ? f.logStart().format(TIME_FORMATTER) : "-";

            String statusBgColor = "#fee2e2";
            String statusTextColor = "#991b1b";
            if ("ESCALATED".equalsIgnoreCase(status)) {
                statusBgColor = "#fef3c7";
                statusTextColor = "#92400e";
            } else if ("CANCELLED".equalsIgnoreCase(status)) {
                statusBgColor = "#f3f4f6";
                statusTextColor = "#374151";
            }

            rows.append(String.format("""
                <tr style="border-bottom: 2px solid #f1f5f9;">
                    <td style="padding: 16px 14px;">
                        <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="margin-bottom: 8px;">
                            <tr>
                                <td style="font-size: 15px; font-weight: 700; color: #0f172a;">
                                    📦 %s
                                </td>
                                <td align="right">
                                    <span style="display: inline-block; padding: 3px 10px; border-radius: 9999px; font-size: 12px; font-weight: 700; background-color: %s; color: %s; margin-right: 12px;">%s</span>
                                    <span style="font-size: 13px; color: #64748b;">🕒 %s</span>
                                </td>
                            </tr>
                        </table>
                        <div style="font-size: 12px; color: #475569; margin-bottom: 8px; font-family: monospace; background-color: #f8fafc; padding: 6px 10px; border-radius: 4px; border: 1px solid #e2e8f0; word-break: break-all;">
                            <strong>Message GUID:</strong> %s
                        </div>
                        <div style="background-color: #fff1f2; border-left: 4px solid #f43f5e; padding: 12px 14px; border-radius: 4px; font-size: 13px; color: #9f1239; line-height: 1.5; word-break: break-word; white-space: pre-wrap;">%s</div>
                    </td>
                </tr>
                """, artifactName, statusBgColor, statusTextColor, status, logTime, messageId, errorDetail));
        }

        String countNotice = totalCount > displayLimit
                ? String.format("<p style=\"font-size: 13px; color: #6b7280; margin-top: 12px;\">※ 총 %d건 중 최신 %d건이 표시되었습니다. 전체 내역은 대시보드에서 확인하세요.</p>", totalCount, displayLimit)
                : "";

        String dashboardUrl = dashboardBaseUrl + "/reprocess?tenantId=" + tenant.getId();

        return String.format("""
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <title>iFlow Sentinel 실패 리포트</title>
            </head>
            <body style="margin: 0; padding: 24px; background-color: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 900px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);">
                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #1e293b 0%%, #0f172a 100%%); padding: 28px 32px; color: #ffffff;">
                            <h1 style="margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.025em; color: #f8fafc;">
                                🛡️ iFlow Sentinel <span style="font-size: 15px; font-weight: 400; color: #94a3b8; margin-left: 8px;">테넌트 실패 메시지 리포트</span>
                            </h1>
                        </td>
                    </tr>
                    <!-- Summary Card -->
                    <tr>
                        <td style="padding: 24px 32px;">
                            <div style="background-color: #fef2f2; border-left: 4px solid #ef4444; padding: 18px 20px; border-radius: 6px; margin-bottom: 24px;">
                                <table width="100%%" border="0" cellspacing="0" cellpadding="0">
                                    <tr>
                                        <td>
                                            <div style="font-size: 13px; color: #991b1b; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em;">경고: 실패 메시지 감지됨</div>
                                            <div style="font-size: 18px; color: #1f2937; font-weight: 700; margin-top: 4px;">
                                                [%s] 테넌트에서 총 <span style="color: #dc2626;">%d건</span>의 실패 메시지가 발생했습니다.
                                            </div>
                                            <div style="font-size: 13px; color: #64748b; margin-top: 4px;">발송 시각: %s</div>
                                        </td>
                                        <td align="right" valign="middle">
                                            <a href="%s" style="display: inline-block; background-color: #2563eb; color: #ffffff; text-decoration: none; padding: 10px 18px; border-radius: 6px; font-weight: 600; font-size: 13px; box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);">
                                                대시보드 바로가기 &rarr;
                                            </a>
                                        </td>
                                    </tr>
                                </table>
                            </div>

                            <!-- Failure List Card Table -->
                            <h2 style="font-size: 16px; font-weight: 700; color: #111827; margin: 0 0 16px 0;">발생한 실패 메시지 상세 내역</h2>
                            <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="border-collapse: collapse; width: 100%%; text-align: left;">
                                <tbody>
                                    %s
                                </tbody>
                            </table>
                            %s
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="padding: 20px 32px; background-color: #f8fafc; border-top: 1px solid #e2e8f0; text-align: center; color: #94a3b8; font-size: 12px;">
                            본 메일은 iFlow Sentinel 시스템에 의해 자동으로 발송되었습니다. 수신 설정 변경은 관리자 대시보드에서 가능합니다.
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """, tenantName, totalCount, formattedReportTime, dashboardUrl, rows.toString(), countNotice);
    }

    /**
     * 테스트 메일 HTML 생성
     */
    public String buildTestEmailHtml(Tenant tenant, String targetEmail) {
        String tenantName = HtmlUtils.htmlEscape(tenant.getName());
        String formattedTime = LocalDateTime.now().format(TIME_FORMATTER);

        return String.format("""
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <title>iFlow Sentinel 테스트 알림</title>
            </head>
            <body style="margin: 0; padding: 24px; background-color: #f9fafb; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
                    <tr>
                        <td style="background: #1e293b; padding: 24px; color: #ffffff;">
                            <h1 style="margin: 0; font-size: 20px; font-weight: 700; color: #f8fafc;">
                                ✉️ iFlow Sentinel 메일 발송 테스트
                            </h1>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 24px; font-size: 14px; color: #374151; line-height: 1.6;">
                            <p>안녕하세요,</p>
                            <p><strong>[%s]</strong> 테넌트의 실패 알림 수신 테스트 메일이 정상적으로 수신되었습니다.</p>
                            <div style="background-color: #f0fdf4; border-left: 4px solid #22c55e; padding: 12px 16px; border-radius: 6px; margin: 16px 0; color: #166534;">
                                ✔ SMTP 설정 및 메일 발송 파이프라인이 정상 동작 중입니다.
                            </div>
                            <p style="color: #6b7280; font-size: 13px;">발송 시각: %s<br>수신 대상: %s</p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 16px 24px; background-color: #f8fafc; border-top: 1px solid #e2e8f0; text-align: center; color: #94a3b8; font-size: 12px;">
                            iFlow Sentinel Automated Notification System
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """, tenantName, formattedTime, HtmlUtils.htmlEscape(targetEmail));
    }
}
