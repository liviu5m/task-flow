import { User } from "better-auth";
import { Resend } from "resend";

export const resend = new Resend(process.env.RESEND_API_KEY);

export async function sendEmailVerification(user: User, url: string) {
  await resend.emails.send({
    from: "TaskFlow <onboarding@resend.dev>",
    to: user.email,
    subject: "Verify your email address",
    html: `
  <!DOCTYPE html>
  <html lang="en">
  <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>Verify Your Email</title>
  </head>
  <body style="margin: 0; padding: 0; background-color: #0f172a; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color: #0f172a; padding: 40px 0;">
          <tr>
              <td align="center">
                  <table role="presentation" width="100%" max-width="600px" cellspacing="0" cellpadding="0" style="max-width: 600px; background-color: #1e293b; border-radius: 12px; border: 1px solid #334155; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);">
                      <tr>
                          <td style="padding: 32px 40px 20px 40px; text-align: center;">
                              <span style="font-size: 24px; font-weight: 700; color: #f8fafc; letter-spacing: -0.5px;">TaskFlow</span>
                          </td>
                      </tr>
                      <tr>
                          <td style="padding: 0 40px 32px 40px; text-align: left;">
                              <h1 style="margin: 0 0 16px 0; font-size: 20px; font-weight: 600; color: #f8fafc;">Verify your email address</h1>
                              <p style="margin: 0 0 24px 0; font-size: 15px; line-height: 24px; color: #94a3b8;">
                                  Hello <span style="color: #f8fafc; font-weight: 500;">${user.name}</span>,<br>
                                  Thanks for signing up for TaskFlow! Please click the button below to confirm your email address and activate your account.
                              </p>
                              <table role="presentation" cellspacing="0" cellpadding="0" style="margin: 0 auto;">
                                  <tr>
                                      <td align="center" style="border-radius: 8px; background-color: #4f46e5;">
                                          <a href="${url}/auth/login" target="_blank" style="font-size: 15px; font-weight: 600; color: #ffffff; text-decoration: none; padding: 12px 28px; border-radius: 8px; border: 1px solid #4f46e5; display: inline-block; background-color: #4f46e5;">Verify Email Address</a>
                                      </td>
                                  </tr>
                              </table>
                              <p style="margin: 32px 0 0 0; font-size: 13px; line-height: 20px; color: #64748b;">
                                  If you didn't create an account with TaskFlow, you can safely ignore this email. This link will expire shortly.
                              </p>
                          </td>
                      </tr>
                      <tr>
                          <td style="padding: 24px 40px; background-color: #0f172a; border-top: 1px solid #334155; text-align: center;">
                              <p style="margin: 0; font-size: 12px; color: #64748b; line-height: 18px;">
                                  &copy; 2026 TaskFlow. All rights reserved.
                              </p>
                          </td>
                      </tr>
                  </table>
              </td>
          </tr>
      </table>
  </body>
  </html>
                  `,
  });
}
